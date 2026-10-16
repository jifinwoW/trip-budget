package com.tripbudget.app.cloud

import android.util.Log
import com.tripbudget.app.data.SyncPreferences
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.data.entity.RouteStopEntity
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.ExpenseCategory
import com.tripbudget.app.domain.PaymentSource
import com.tripbudget.app.domain.RouteStopStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

class SyncEngine(
    private val client: SupabaseClient?,
    private val authRepository: AuthRepository,
    private val repository: TripRepository,
    private val syncPreferences: SyncPreferences,
) : LiveCloudPush {
    private val liveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var realtimeChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null
    private var debouncedPullJob: Job? = null
    private var lastRefreshAtMs: Long = 0L

    suspend fun sync(): Result<Unit> = runCatching {
        val supabase = client
            ?: error("Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties.")
        val ownerId = authRepository.currentUserIdOrNull()
            ?: error("Sign in to sync.")

        pushTrips(supabase, ownerId)
        pushMembers(supabase)
        pushContributions(supabase)
        pushExpenses(supabase)
        pushRouteStops(supabase)

        // Pull every trip this user can access (owned + joined).
        pullAll(supabase)

        syncPreferences.setLastSyncedAt(System.currentTimeMillis())
    }

    /** Pull latest from cloud into the local cache (online source of truth). */
    suspend fun pullFromCloud(): Result<Unit> = runCatching {
        val supabase = client
            ?: error("Supabase is not configured.")
        if (authRepository.currentUserIdOrNull() == null) {
            error("Sign in to refresh.")
        }
        pullAll(supabase)
        syncPreferences.setLastSyncedAt(System.currentTimeMillis())
    }

    /**
     * Online refresh used by swipe-down and tab changes.
     * Debounced so rapid page switches / realtime bursts do not spam the API.
     */
    suspend fun refresh(force: Boolean = false): Result<Unit> {
        if (authRepository.currentUserIdOrNull() == null) {
            return Result.success(Unit)
        }
        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshAtMs < MIN_REFRESH_GAP_MS) {
            return Result.success(Unit)
        }
        return refreshMutex.withLock {
            val lockedNow = System.currentTimeMillis()
            if (!force && lockedNow - lastRefreshAtMs < MIN_REFRESH_GAP_MS) {
                return@withLock Result.success(Unit)
            }
            _isRefreshing.value = true
            try {
                pullFromCloud().also {
                    lastRefreshAtMs = System.currentTimeMillis()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun requestRefresh(force: Boolean = false) {
        liveScope.launch { refresh(force = force) }
    }

    /** Subscribe to Postgres changes; any remote edit triggers a live pull. */
    fun startRealtime() {
        val supabase = client ?: return
        val userId = authRepository.currentUserIdOrNull() ?: return
        stopRealtime()
        realtimeJob = liveScope.launch {
            runCatching {
                val channel = supabase.channel("trip-live-$userId")
                realtimeChannel = channel
                listOf("trips", "members", "contributions", "expenses", "route_stops").forEach { tableName ->
                    channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = tableName
                    }.onEach {
                        scheduleDebouncedPull()
                    }.launchIn(this)
                }
                channel.subscribe()
                Log.i(TAG, "Realtime subscribed for $userId")
            }.onFailure { err ->
                Log.w(TAG, "Realtime subscribe failed", err)
            }
        }
    }

    fun stopRealtime() {
        debouncedPullJob?.cancel()
        debouncedPullJob = null
        realtimeJob?.cancel()
        realtimeJob = null
        val channel = realtimeChannel
        realtimeChannel = null
        if (channel != null) {
            liveScope.launch {
                runCatching {
                    channel.unsubscribe()
                    client?.realtime?.removeChannel(channel)
                }.onFailure { err ->
                    Log.w(TAG, "Realtime unsubscribe failed", err)
                }
            }
        }
    }

    private fun scheduleDebouncedPull() {
        debouncedPullJob?.cancel()
        debouncedPullJob = liveScope.launch {
            delay(REALTIME_DEBOUNCE_MS)
            refresh(force = true)
        }
    }

    private suspend fun pullAll(supabase: SupabaseClient) {
        pullTrips(supabase)
        pullMembers(supabase)
        pullContributions(supabase)
        pullExpenses(supabase)
        pullRouteStops(supabase)
    }

    /** Upload trip (if needed) and return its share code. */
    suspend fun ensureInviteCode(localTripId: Long): Result<String> = runCatching {
        if (authRepository.currentUserIdOrNull() == null) {
            error("Sign in to share a trip.")
        }
        upsertTrip(localTripId)
        repository.getTrip(localTripId)?.inviteCode?.takeIf { it.isNotBlank() }
            ?: error("Could not create invite code. Swipe down to refresh, then try again.")
    }

    /** Friend enters invite code → gains access → full refresh pulls the trip. */
    suspend fun joinTripByCode(code: String): Result<Unit> = runCatching {
        val supabase = client
            ?: error("Supabase is not configured.")
        if (authRepository.currentUserIdOrNull() == null) {
            error("Sign in to join a trip.")
        }
        val trimmed = code.trim().uppercase()
        if (trimmed.length < 4) error("Enter a valid invite code")
        supabase.postgrest.rpc(
            function = "join_trip_by_code",
            parameters = JoinTripParams(code = trimmed),
        )
        sync().getOrThrow()
    }

    private fun newInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(6) {
            repeat(6) { append(alphabet[Random.nextInt(alphabet.length)]) }
        }
    }

    private fun toRemoteTrip(trip: TripEntity, currentUserId: String): RemoteTrip {
        val remoteId = trip.remoteId ?: UUID.randomUUID().toString()
        val ownerId = trip.remoteOwnerId ?: currentUserId
        val invite = trip.inviteCode?.takeIf { it.isNotBlank() } ?: newInviteCode()
        return RemoteTrip(
            id = remoteId,
            ownerId = ownerId,
            name = trip.name,
            destination = trip.destination,
            plannedBudgetPaise = trip.plannedBudgetPaise,
            startDateEpochDay = trip.startDateEpochDay,
            endDateEpochDay = trip.endDateEpochDay,
            createdAtMillis = trip.createdAt,
            inviteCode = invite.uppercase(),
            routeTrackingActive = trip.routeTrackingActive,
            updatedAt = CloudTime.toIso(trip.updatedAt),
        )
    }

    private suspend fun pushTrips(supabase: SupabaseClient, ownerId: String) {
        val locals = repository.getAllTrips()
        val payloads = locals.map { trip ->
            toRemoteTrip(trip, ownerId).also { remote ->
                val needsLocalUpdate =
                    trip.remoteId == null ||
                        trip.remoteOwnerId == null ||
                        trip.inviteCode.isNullOrBlank()
                if (needsLocalUpdate) {
                    repository.putTripSynced(
                        trip.copy(
                            remoteId = remote.id,
                            remoteOwnerId = remote.ownerId,
                            inviteCode = remote.inviteCode,
                        ),
                    )
                }
            }
        }
        // Only upsert trips this user owns. Editors must not INSERT with another owner_id (RLS).
        val ownedPayloads = payloads.filter { it.ownerId == ownerId }
        if (ownedPayloads.isNotEmpty()) {
            supabase.from("trips").upsert(ownedPayloads)
        }
        for (remote in payloads.filter { it.ownerId != ownerId }) {
            supabase.from("trips").update(
                {
                    set("name", remote.name)
                    set("destination", remote.destination)
                    set("planned_budget_paise", remote.plannedBudgetPaise)
                    set("start_date_epoch_day", remote.startDateEpochDay)
                    set("end_date_epoch_day", remote.endDateEpochDay)
                    set("route_tracking_active", remote.routeTrackingActive)
                    set("updated_at", remote.updatedAt)
                },
            ) {
                filter { eq("id", remote.id) }
            }
        }
    }

    private suspend fun pushMembers(supabase: SupabaseClient) {
        val tripsByLocalId = repository.getAllTrips().associateBy { it.id }
        val locals = repository.getAllMembers()
        val payloads = mutableListOf<RemoteMember>()
        for (member in locals) {
            val tripRemote = tripsByLocalId[member.tripId]?.remoteId ?: continue
            val remoteId = member.remoteId ?: UUID.randomUUID().toString()
            val updated = member.copy(remoteId = remoteId)
            // UPDATE only — never REPLACE (cascades wipe contributions/expenses).
            if (member.remoteId == null) {
                repository.putMemberSynced(updated)
            }
            payloads += RemoteMember(
                id = remoteId,
                tripId = tripRemote,
                name = updated.name,
                expectedContributionPaise = updated.expectedContributionPaise,
                updatedAt = CloudTime.toIso(updated.updatedAt),
            )
        }
        if (payloads.isNotEmpty()) {
            supabase.from("members").upsert(payloads)
        }
    }

    private suspend fun pushContributions(supabase: SupabaseClient) {
        // Re-read after members got remoteIds assigned above.
        val tripsByLocalId = repository.getAllTrips().associateBy { it.id }
        val membersByLocalId = repository.getAllMembers().associateBy { it.id }
        val locals = repository.getAllContributions()
        val payloads = mutableListOf<RemoteContribution>()
        val skipped = mutableListOf<String>()
        for (row in locals) {
            val tripRemote = tripsByLocalId[row.tripId]?.remoteId
            val memberRemote = membersByLocalId[row.memberId]?.remoteId
            if (tripRemote == null || memberRemote == null) {
                skipped += "contribution localId=${row.id}"
                continue
            }
            val remoteId = row.remoteId ?: UUID.randomUUID().toString()
            val updated = row.copy(remoteId = remoteId)
            if (row.remoteId == null) {
                repository.putContributionSynced(updated)
            }
            payloads += RemoteContribution(
                id = remoteId,
                tripId = tripRemote,
                memberId = memberRemote,
                amountPaise = updated.amountPaise,
                paidAtMillis = updated.paidAt,
                note = updated.note,
                updatedAt = CloudTime.toIso(updated.updatedAt),
            )
        }
        if (payloads.isNotEmpty()) {
            supabase.from("contributions").upsert(payloads)
        }
        if (skipped.isNotEmpty() && payloads.isEmpty() && locals.isNotEmpty()) {
            error(
                "Contributions could not sync (missing trip/member remote ids). " +
                    "Sync again after members are uploaded.",
            )
        }
    }

    private suspend fun pushExpenses(supabase: SupabaseClient) {
        val tripsByLocalId = repository.getAllTrips().associateBy { it.id }
        val membersByLocalId = repository.getAllMembers().associateBy { it.id }
        val locals = repository.getAllExpenses()
        val payloads = mutableListOf<RemoteExpense>()
        for (row in locals) {
            val tripRemote = tripsByLocalId[row.tripId]?.remoteId ?: continue
            val paidByRemote = row.paidByMemberId?.let { membersByLocalId[it]?.remoteId }
            val remoteId = row.remoteId ?: UUID.randomUUID().toString()
            val updated = row.copy(remoteId = remoteId)
            if (row.remoteId == null) {
                repository.putExpenseSynced(updated)
            }
            payloads += RemoteExpense(
                id = remoteId,
                tripId = tripRemote,
                title = updated.title,
                amountPaise = updated.amountPaise,
                category = updated.category.name,
                dateEpochDay = updated.dateEpochDay,
                paidByMemberId = paidByRemote,
                paymentSource = updated.paymentSource.name,
                notes = updated.notes,
                updatedAt = CloudTime.toIso(updated.updatedAt),
            )
        }
        if (payloads.isNotEmpty()) {
            supabase.from("expenses").upsert(payloads)
        }
    }

    private suspend fun pullTrips(supabase: SupabaseClient) {
        val remotes = supabase.from("trips").select().decodeList<RemoteTrip>()
        for (remote in remotes) {
            val remoteUpdated = CloudTime.fromIso(remote.updatedAt)
            val existing = repository.getTripByRemoteId(remote.id)
            if (existing != null && existing.updatedAt > remoteUpdated) {
                // Still refresh invite / owner metadata if missing.
                if (existing.inviteCode.isNullOrBlank() || existing.remoteOwnerId == null) {
                    repository.putTripSynced(
                        existing.copy(
                            remoteOwnerId = remote.ownerId,
                            inviteCode = remote.inviteCode,
                        ),
                    )
                }
                continue
            }
            val entity = TripEntity(
                id = existing?.id ?: 0,
                name = remote.name,
                destination = remote.destination,
                plannedBudgetPaise = remote.plannedBudgetPaise,
                startDateEpochDay = remote.startDateEpochDay,
                endDateEpochDay = remote.endDateEpochDay,
                createdAt = remote.createdAtMillis,
                remoteId = remote.id,
                remoteOwnerId = remote.ownerId,
                inviteCode = remote.inviteCode,
                routeTrackingActive = remote.routeTrackingActive,
                updatedAt = remoteUpdated,
            )
            repository.putTripSynced(entity)
        }
        repository.deleteSyncedTripsNotIn(remotes.map { it.id })
    }

    private suspend fun pullMembers(supabase: SupabaseClient) {
        val remotes = supabase.from("members").select().decodeList<RemoteMember>()
        val tripsByRemote = repository.getAllTrips().mapNotNull { t ->
            t.remoteId?.let { it to t }
        }.toMap()
        for (remote in remotes) {
            val trip = tripsByRemote[remote.tripId] ?: continue
            val remoteUpdated = CloudTime.fromIso(remote.updatedAt)
            val existing = repository.getMemberByRemoteId(remote.id)
            if (existing != null && existing.updatedAt > remoteUpdated) continue
            val entity = MemberEntity(
                id = existing?.id ?: 0,
                tripId = trip.id,
                name = remote.name,
                expectedContributionPaise = remote.expectedContributionPaise,
                remoteId = remote.id,
                updatedAt = remoteUpdated,
            )
            repository.putMemberSynced(entity)
        }
        repository.deleteSyncedMembersNotIn(remotes.map { it.id })
    }

    private suspend fun pullContributions(supabase: SupabaseClient) {
        val remotes = supabase.from("contributions").select().decodeList<RemoteContribution>()
        val tripsByRemote = repository.getAllTrips().mapNotNull { t ->
            t.remoteId?.let { it to t }
        }.toMap()
        val membersByRemote = repository.getAllMembers().mapNotNull { m ->
            m.remoteId?.let { it to m }
        }.toMap()
        for (remote in remotes) {
            val trip = tripsByRemote[remote.tripId] ?: continue
            val member = membersByRemote[remote.memberId] ?: continue
            val remoteUpdated = CloudTime.fromIso(remote.updatedAt)
            val existing = repository.getContributionByRemoteId(remote.id)
            if (existing != null && existing.updatedAt > remoteUpdated) continue
            val entity = ContributionEntity(
                id = existing?.id ?: 0,
                tripId = trip.id,
                memberId = member.id,
                amountPaise = remote.amountPaise,
                paidAt = remote.paidAtMillis,
                note = remote.note,
                remoteId = remote.id,
                updatedAt = remoteUpdated,
            )
            repository.putContributionSynced(entity)
        }
        repository.deleteSyncedContributionsNotIn(remotes.map { it.id })
    }

    private suspend fun pullExpenses(supabase: SupabaseClient) {
        val remotes = supabase.from("expenses").select().decodeList<RemoteExpense>()
        val tripsByRemote = repository.getAllTrips().mapNotNull { t ->
            t.remoteId?.let { it to t }
        }.toMap()
        val membersByRemote = repository.getAllMembers().mapNotNull { m ->
            m.remoteId?.let { it to m }
        }.toMap()
        for (remote in remotes) {
            val trip = tripsByRemote[remote.tripId] ?: continue
            val paidBy = remote.paidByMemberId?.let { membersByRemote[it]?.id }
            val remoteUpdated = CloudTime.fromIso(remote.updatedAt)
            val existing = repository.getExpenseByRemoteId(remote.id)
            if (existing != null && existing.updatedAt > remoteUpdated) continue
            val category = runCatching { ExpenseCategory.valueOf(remote.category) }
                .getOrDefault(ExpenseCategory.OTHER)
            val source = runCatching { PaymentSource.valueOf(remote.paymentSource) }
                .getOrDefault(PaymentSource.SHARED)
            val entity = ExpenseEntity(
                id = existing?.id ?: 0,
                tripId = trip.id,
                title = remote.title,
                amountPaise = remote.amountPaise,
                category = category,
                dateEpochDay = remote.dateEpochDay,
                paidByMemberId = paidBy,
                paymentSource = source,
                notes = remote.notes,
                remoteId = remote.id,
                updatedAt = remoteUpdated,
            )
            repository.putExpenseSynced(entity)
        }
        repository.deleteSyncedExpensesNotIn(remotes.map { it.id })
    }

    private suspend fun pushRouteStops(supabase: SupabaseClient) {
        val tripsByLocalId = repository.getAllTrips().associateBy { it.id }
        val locals = repository.getAllRouteStops()
        val payloads = mutableListOf<RemoteRouteStop>()
        for (row in locals) {
            val tripRemote = tripsByLocalId[row.tripId]?.remoteId ?: continue
            val remoteId = row.remoteId ?: UUID.randomUUID().toString()
            val updated = row.copy(remoteId = remoteId)
            if (row.remoteId == null) {
                repository.putRouteStopSynced(updated)
            }
            payloads += RemoteRouteStop(
                id = remoteId,
                tripId = tripRemote,
                sortOrder = updated.sortOrder,
                title = updated.title,
                subtitle = updated.subtitle,
                lat = updated.lat,
                lng = updated.lng,
                osmPlaceId = updated.osmPlaceId,
                status = updated.status.name,
                note = updated.note,
                updatedAt = CloudTime.toIso(updated.updatedAt),
            )
        }
        if (payloads.isNotEmpty()) {
            supabase.from("route_stops").upsert(payloads)
        }
    }

    private suspend fun pullRouteStops(supabase: SupabaseClient) {
        val remotes = supabase.from("route_stops").select().decodeList<RemoteRouteStop>()
        val tripsByRemote = repository.getAllTrips().mapNotNull { t ->
            t.remoteId?.let { it to t }
        }.toMap()
        for (remote in remotes) {
            val trip = tripsByRemote[remote.tripId] ?: continue
            val remoteUpdated = CloudTime.fromIso(remote.updatedAt)
            val existing = repository.getRouteStopByRemoteId(remote.id)
            if (existing != null && existing.updatedAt > remoteUpdated) continue
            val status = runCatching { RouteStopStatus.valueOf(remote.status) }
                .getOrDefault(RouteStopStatus.UPCOMING)
            val entity = RouteStopEntity(
                id = existing?.id ?: 0,
                tripId = trip.id,
                sortOrder = remote.sortOrder,
                title = remote.title,
                subtitle = remote.subtitle,
                lat = remote.lat,
                lng = remote.lng,
                osmPlaceId = remote.osmPlaceId,
                status = status,
                note = remote.note,
                remoteId = remote.id,
                updatedAt = remoteUpdated,
            )
            repository.putRouteStopSynced(entity)
        }
        repository.deleteSyncedRouteStopsNotIn(remotes.map { it.id })
    }

    // --- Live push after each local write (when signed in) ---

    override fun onTripUpserted(localId: Long) = liveLaunch { upsertTrip(localId) }

    override fun onTripDeleted(remoteId: String?) = liveLaunch { deleteRemoteRow("trips", remoteId) }

    override fun onMemberUpserted(localId: Long) = liveLaunch { upsertMember(localId) }

    override fun onMemberDeleted(remoteId: String?) =
        liveLaunch { deleteRemoteRow("members", remoteId) }

    override fun onContributionUpserted(localId: Long) =
        liveLaunch { upsertContribution(localId) }

    override fun onContributionDeleted(remoteId: String?) =
        liveLaunch { deleteRemoteRow("contributions", remoteId) }

    override fun onExpenseUpserted(localId: Long) = liveLaunch { upsertExpense(localId) }

    override fun onExpenseDeleted(remoteId: String?) =
        liveLaunch { deleteRemoteRow("expenses", remoteId) }

    override fun onRouteStopUpserted(localId: Long) = liveLaunch { upsertRouteStop(localId) }

    override fun onRouteStopDeleted(remoteId: String?) =
        liveLaunch { deleteRemoteRow("route_stops", remoteId) }

    private fun liveLaunch(block: suspend () -> Unit) {
        liveScope.launch {
            runCatching { block() }
                .onSuccess { syncPreferences.setLastSyncedAt(System.currentTimeMillis()) }
                .onFailure { err -> Log.w(TAG, "Live cloud push failed", err) }
        }
    }

    private fun requireClientAndOwner(): Pair<SupabaseClient, String>? {
        val supabase = client ?: return null
        val ownerId = authRepository.currentUserIdOrNull() ?: return null
        return supabase to ownerId
    }

    private suspend fun ensureTripRemoteId(localTripId: Long): String? {
        val trip = repository.getTrip(localTripId) ?: return null
        if (trip.remoteId != null) return trip.remoteId
        upsertTrip(localTripId)
        return repository.getTrip(localTripId)?.remoteId
    }

    private suspend fun ensureMemberRemoteId(localMemberId: Long): String? {
        val member = repository.getMember(localMemberId) ?: return null
        if (member.remoteId != null) return member.remoteId
        upsertMember(localMemberId)
        return repository.getMember(localMemberId)?.remoteId
    }

    private suspend fun upsertTrip(localId: Long) {
        val (supabase, currentUserId) = requireClientAndOwner() ?: return
        val trip = repository.getTrip(localId) ?: return
        val remote = toRemoteTrip(trip, currentUserId)
        repository.putTripSynced(
            trip.copy(
                remoteId = remote.id,
                remoteOwnerId = remote.ownerId,
                inviteCode = remote.inviteCode,
            ),
        )
        if (remote.ownerId == currentUserId) {
            supabase.from("trips").upsert(remote)
        } else {
            supabase.from("trips").update(
                {
                    set("name", remote.name)
                    set("destination", remote.destination)
                    set("planned_budget_paise", remote.plannedBudgetPaise)
                    set("start_date_epoch_day", remote.startDateEpochDay)
                    set("end_date_epoch_day", remote.endDateEpochDay)
                    set("route_tracking_active", remote.routeTrackingActive)
                    set("updated_at", remote.updatedAt)
                },
            ) {
                filter { eq("id", remote.id) }
            }
        }
    }

    private suspend fun upsertMember(localId: Long) {
        val (supabase, _) = requireClientAndOwner() ?: return
        val member = repository.getMember(localId) ?: return
        val tripRemote = ensureTripRemoteId(member.tripId) ?: return
        val remoteId = member.remoteId ?: UUID.randomUUID().toString()
        if (member.remoteId == null) {
            repository.putMemberSynced(member.copy(remoteId = remoteId))
        }
        val latest = repository.getMember(localId) ?: return
        supabase.from("members").upsert(
            RemoteMember(
                id = remoteId,
                tripId = tripRemote,
                name = latest.name,
                expectedContributionPaise = latest.expectedContributionPaise,
                updatedAt = CloudTime.toIso(latest.updatedAt),
            ),
        )
    }

    private suspend fun upsertContribution(localId: Long) {
        val (supabase, _) = requireClientAndOwner() ?: return
        val row = repository.getContribution(localId) ?: return
        val tripRemote = ensureTripRemoteId(row.tripId) ?: return
        val memberRemote = ensureMemberRemoteId(row.memberId) ?: return
        val remoteId = row.remoteId ?: UUID.randomUUID().toString()
        if (row.remoteId == null) {
            repository.putContributionSynced(row.copy(remoteId = remoteId))
        }
        val latest = repository.getContribution(localId) ?: return
        supabase.from("contributions").upsert(
            RemoteContribution(
                id = remoteId,
                tripId = tripRemote,
                memberId = memberRemote,
                amountPaise = latest.amountPaise,
                paidAtMillis = latest.paidAt,
                note = latest.note,
                updatedAt = CloudTime.toIso(latest.updatedAt),
            ),
        )
    }

    private suspend fun upsertExpense(localId: Long) {
        val (supabase, _) = requireClientAndOwner() ?: return
        val row = repository.getExpense(localId) ?: return
        val tripRemote = ensureTripRemoteId(row.tripId) ?: return
        val paidByRemote = row.paidByMemberId?.let { ensureMemberRemoteId(it) }
        val remoteId = row.remoteId ?: UUID.randomUUID().toString()
        if (row.remoteId == null) {
            repository.putExpenseSynced(row.copy(remoteId = remoteId))
        }
        val latest = repository.getExpense(localId) ?: return
        supabase.from("expenses").upsert(
            RemoteExpense(
                id = remoteId,
                tripId = tripRemote,
                title = latest.title,
                amountPaise = latest.amountPaise,
                category = latest.category.name,
                dateEpochDay = latest.dateEpochDay,
                paidByMemberId = paidByRemote,
                paymentSource = latest.paymentSource.name,
                notes = latest.notes,
                updatedAt = CloudTime.toIso(latest.updatedAt),
            ),
        )
    }

    private suspend fun upsertRouteStop(localId: Long) {
        val (supabase, _) = requireClientAndOwner() ?: return
        val row = repository.getRouteStop(localId) ?: return
        val tripRemote = ensureTripRemoteId(row.tripId) ?: return
        val remoteId = row.remoteId ?: UUID.randomUUID().toString()
        if (row.remoteId == null) {
            repository.putRouteStopSynced(row.copy(remoteId = remoteId))
        }
        val latest = repository.getRouteStop(localId) ?: return
        supabase.from("route_stops").upsert(
            RemoteRouteStop(
                id = remoteId,
                tripId = tripRemote,
                sortOrder = latest.sortOrder,
                title = latest.title,
                subtitle = latest.subtitle,
                lat = latest.lat,
                lng = latest.lng,
                osmPlaceId = latest.osmPlaceId,
                status = latest.status.name,
                note = latest.note,
                updatedAt = CloudTime.toIso(latest.updatedAt),
            ),
        )
    }

    private suspend fun deleteRemoteRow(table: String, remoteId: String?) {
        if (remoteId.isNullOrBlank()) return
        val (supabase, _) = requireClientAndOwner() ?: return
        supabase.from(table).delete {
            filter {
                eq("id", remoteId)
            }
        }
    }

    companion object {
        private const val TAG = "SyncEngine"
        private const val MIN_REFRESH_GAP_MS = 750L
        private const val REALTIME_DEBOUNCE_MS = 400L
    }
}
