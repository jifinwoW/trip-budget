package com.tripbudget.app.cloud

import android.content.Context
import android.util.Log
import com.tripbudget.app.data.SyncPreferences
import com.tripbudget.app.data.TripCoverStore
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.data.entity.ContributionEntity
import com.tripbudget.app.data.entity.ExpenseEntity
import com.tripbudget.app.data.entity.MemberEntity
import com.tripbudget.app.data.entity.RouteStopEntity
import com.tripbudget.app.data.entity.TripEntity
import com.tripbudget.app.domain.ExpenseCategory
import com.tripbudget.app.domain.Money
import com.tripbudget.app.domain.PaymentSource
import com.tripbudget.app.domain.RouteStopStatus
import com.tripbudget.app.domain.TripTotals
import com.tripbudget.app.notify.TripNotifications
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.HasRecord
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.decodeOldRecordOrNull
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.io.File
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
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SyncEngine(
    private val appContext: Context,
    private val client: SupabaseClient?,
    private val authRepository: AuthRepository,
    private val repository: TripRepository,
    private val syncPreferences: SyncPreferences,
    private val showNotification: (title: String, body: String, evenInForeground: Boolean, tag: String) -> Unit,
) : LiveCloudPush {
    private val liveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var realtimeChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null
    private var debouncedPullJob: Job? = null
    private var lastRefreshAtMs: Long = 0L
    private var muteNotificationsUntilMs: Long = 0L
    private val budgetBaselines = java.util.concurrent.ConcurrentHashMap<String, TripTotals>()
    private val reachedStopIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val announcedJoins = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

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
                listOf(
                    "trips",
                    "members",
                    "contributions",
                    "expenses",
                    "route_stops",
                    "trip_access",
                    "trip_announcements",
                ).forEach { tableName ->
                    channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = tableName
                    }.onEach { action ->
                        scheduleDebouncedPull()
                        runCatching { notifyRemoteChange(tableName, action) }
                            .onFailure { err -> Log.w(TAG, "Notification skipped", err) }
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
            if (refresh(force = true).isSuccess) {
                reportBudgetChanges()
            }
        }
    }

    private suspend fun pullAll(supabase: SupabaseClient) {
        pullTrips(supabase)
        pullMembers(supabase)
        pullContributions(supabase)
        pullExpenses(supabase)
        pullRouteStops(supabase)
        reportNewJoins(supabase)
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
    suspend fun joinTripByCode(code: String, name: String): Result<Unit> = runCatching {
        val supabase = client
            ?: error("Supabase is not configured.")
        if (authRepository.currentUserIdOrNull() == null) {
            error("Sign in to join a trip.")
        }
        val trimmed = code.trim().uppercase()
        val trimmedName = name.trim()
        if (trimmed.length < 4) error("Enter a valid invite code")
        if (trimmedName.isEmpty()) error("Enter your name")
        if (trimmedName.length > 40) error("Keep your name under 40 characters")
        supabase.postgrest.rpc(
            function = "join_trip_by_code",
            parameters = JoinTripParams(code = trimmed, name = trimmedName),
        )
        sync().getOrThrow()
    }

    /** Names of people who joined this trip with the code. The owner is not included. */
    suspend fun joinerNames(localTripId: Long): List<String> {
        val supabase = client ?: return emptyList()
        val remoteId = repository.getTrip(localTripId)?.remoteId ?: return emptyList()
        return runCatching {
            supabase.postgrest.rpc(
                function = "trip_joiner_names",
                parameters = TripIdParams(tripId = remoteId),
            ).decodeList<JoinerNameRow>()
        }.getOrDefault(emptyList()).map { it.displayName.trim() }.filter { it.isNotEmpty() }
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
            placeLat = trip.placeLat,
            placeLng = trip.placeLng,
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
                val saved = trip.copy(
                    remoteId = remote.id,
                    remoteOwnerId = remote.ownerId,
                    inviteCode = remote.inviteCode,
                )
                val needsLocalUpdate =
                    trip.remoteId == null ||
                        trip.remoteOwnerId == null ||
                        trip.inviteCode.isNullOrBlank()
                if (needsLocalUpdate) {
                    repository.putTripSynced(saved)
                }
            }
        }
        // Only upsert trips this user owns. Editors must not INSERT with another owner_id (RLS).
        val ownedPayloads = payloads.filter { it.ownerId == ownerId }
        if (ownedPayloads.isNotEmpty()) {
            supabase.from("trips").upsert(ownedPayloads)
            for (remote in ownedPayloads) {
                val trip = repository.getTripByRemoteId(remote.id) ?: continue
                publishCover(supabase, trip, remote.id, isOwner = true)
            }
        }
        for (remote in payloads.filter { it.ownerId != ownerId }) {
            supabase.from("trips").update(
                {
                    set("name", remote.name)
                    set("destination", remote.destination)
                    set("place_lat", remote.placeLat)
                    set("place_lng", remote.placeLng)
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
                // Still refresh invite / owner metadata if missing, and pick up a shared hero.
                val withMeta = if (existing.inviteCode.isNullOrBlank() || existing.remoteOwnerId == null) {
                    existing.copy(
                        remoteOwnerId = remote.ownerId,
                        inviteCode = remote.inviteCode,
                    )
                } else {
                    existing
                }
                val covered = attachRemoteCover(supabase, withMeta, remote.id, remote.coverPath)
                if (covered != existing) {
                    repository.putTripSynced(covered)
                }
                continue
            }
            val entity = attachRemoteCover(
                supabase,
                TripEntity(
                    id = existing?.id ?: 0,
                    name = remote.name,
                    destination = remote.destination,
                    placeLat = remote.placeLat,
                    placeLng = remote.placeLng,
                    plannedBudgetPaise = remote.plannedBudgetPaise,
                    startDateEpochDay = remote.startDateEpochDay,
                    endDateEpochDay = remote.endDateEpochDay,
                    createdAt = remote.createdAtMillis,
                    remoteId = remote.id,
                    remoteOwnerId = remote.ownerId,
                    inviteCode = remote.inviteCode,
                    routeTrackingActive = remote.routeTrackingActive,
                    updatedAt = remoteUpdated,
                    coverImagePath = existing?.coverImagePath,
                    coverObjectKey = existing?.coverObjectKey,
                ),
                remote.id,
                remote.coverPath,
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

    /** Saves this device's Firebase token so a later sender can reach it. */
    fun registerPushToken() {
        liveScope.launch {
            runCatching { uploadPushToken() }
                .onFailure { err -> Log.w(TAG, "Couldn't save notification token", err) }
        }
    }

    /** Removes the token while the session is still valid. Call before sign-out. */
    suspend fun unregisterPushToken() {
        runCatching {
            val supabase = client ?: return
            if (authRepository.currentUserIdOrNull() == null) return
            val token = FirebaseMessaging.getInstance().token.await()
            supabase.from("device_tokens").delete {
                filter { eq("token", token) }
            }
        }.onFailure { err ->
            Log.w(TAG, "Couldn't remove notification token", err)
        }
    }

    private suspend fun uploadPushToken() {
        val supabase = client ?: return
        val userId = authRepository.currentUserIdOrNull() ?: return
        val token = FirebaseMessaging.getInstance().token.await()
        supabase.from("device_tokens").upsert(
            DeviceTokenRow(token = token, userId = userId),
        )
    }

    private suspend fun notifyRemoteChange(table: String, action: PostgresAction) {
        if (System.currentTimeMillis() < muteNotificationsUntilMs) return
        val record = rowOf(action) ?: return
        rememberBudgetBaseline(table, record)
        val copy = describeRemoteChange(table, action, record) ?: return
        val banner = table == "trip_announcements" || table == "trip_access"
        showNotification(copy.first, copy.second, banner, alertTag(table, action, record))
    }

    private fun alertTag(table: String, action: PostgresAction, record: NotifyRow): String {
        val op = when (action) {
            is PostgresAction.Insert -> "INSERT"
            is PostgresAction.Update -> "UPDATE"
            is PostgresAction.Delete -> "DELETE"
            else -> "OTHER"
        }
        return when (table) {
            "trip_access" -> "join:${record.tripId}:${record.userId}"
            "trip_announcements" -> "announce:${record.tripId}:${record.title?.take(40)}"
            else -> "$table:${record.id}:$op"
        }
    }

    private fun rowOf(action: PostgresAction): NotifyRow? = when (action) {
        is PostgresAction.Delete -> action.decodeOldRecordOrNull()
        is HasRecord -> action.decodeRecordOrNull()
        else -> null
    }

    private suspend fun rememberBudgetBaseline(table: String, record: NotifyRow) {
        if (table != "expenses" && table != "contributions" && table != "members") return
        val remoteTripId = record.tripId ?: localTripRemoteId(table, record.id) ?: return
        if (budgetBaselines.containsKey(remoteTripId)) return
        val trip = repository.getTripByRemoteId(remoteTripId) ?: return
        val totals = repository.totalsFor(trip.id) ?: return
        budgetBaselines.putIfAbsent(remoteTripId, totals)
    }

    private suspend fun localTripRemoteId(table: String, remoteRowId: String?): String? {
        if (remoteRowId.isNullOrBlank()) return null
        val tripId = when (table) {
            "expenses" -> repository.getExpenseByRemoteId(remoteRowId)?.tripId
            "contributions" -> repository.getContributionByRemoteId(remoteRowId)?.tripId
            "members" -> repository.getMemberByRemoteId(remoteRowId)?.tripId
            else -> null
        } ?: return null
        return repository.getTrip(tripId)?.remoteId
    }

    private suspend fun reportBudgetChanges() {
        val pending = budgetBaselines.keys.toList()
        for (remoteTripId in pending) {
            val before = budgetBaselines.remove(remoteTripId) ?: continue
            val trip = repository.getTripByRemoteId(remoteTripId) ?: continue
            val after = repository.totalsFor(trip.id) ?: continue
            if (!before.isOverBudget && after.isOverBudget) {
                val over = Money.formatInr(-after.budgetRemainingPaise)
                showNotification("Over budget", "${trip.name} is $over over plan", false, "over:$remoteTripId")
            }
            val beforeOwed = before.memberBalances.associate { it.memberId to it.outstandingPaise }
            val newlyOwing = after.memberBalances.filter { member ->
                member.outstandingPaise > 0 && member.outstandingPaise > (beforeOwed[member.memberId] ?: 0L)
            }
            if (newlyOwing.isNotEmpty()) {
                val line = newlyOwing.joinToString { member ->
                    "${member.name} owes ${Money.formatInr(member.outstandingPaise)}"
                }
                showNotification("Money still owed", "${trip.name}: $line", false, "owed:$remoteTripId")
            }
        }
    }

    private suspend fun describeRemoteChange(
        table: String,
        action: PostgresAction,
        record: NotifyRow,
    ): Pair<String, String>? {
        if (action is PostgresAction.Select) return null
        val remoteTripId = if (table == "trips") record.id else record.tripId
        val trip = remoteTripId?.let { repository.getTripByRemoteId(it) }
        val tripName = trip?.name
            ?: record.name?.takeIf { table == "trips" }
            ?: "Your trip"
        return when (table) {
            "expenses" -> expenseCopy(action, record, tripName)
            "contributions" -> contributionCopy(action, record, tripName)
            "members" -> if (action is PostgresAction.Insert) {
                val who = record.name?.takeIf { it.isNotBlank() }
                "New member" to if (who != null) "$who joined $tripName" else tripName
            } else {
                null
            }
            "trips" -> if (action is PostgresAction.Update) {
                "Trip updated" to (record.name ?: tripName)
            } else {
                null
            }
            "trip_access" -> joinCopy(action, record, tripName)
            "route_stops" -> reachedCopy(action, record, trip)
            "trip_announcements" -> announcementCopy(action, record)
            else -> null
        }
    }

    private suspend fun expenseCopy(
        action: PostgresAction,
        record: NotifyRow,
        tripName: String,
    ): Pair<String, String>? {
        val local = record.id?.let { repository.getExpenseByRemoteId(it) }
        val title = record.title ?: local?.title
        val amount = (record.amountPaise ?: local?.amountPaise)?.let { Money.formatInr(it) }
        val name = if (tripName == "Your trip") {
            local?.tripId?.let { repository.getTrip(it)?.name } ?: tripName
        } else {
            tripName
        }
        val detail = listOfNotNull(name, title, amount).joinToString(" · ")
        return when (action) {
            is PostgresAction.Insert -> "New expense" to detail
            is PostgresAction.Update -> "Expense updated" to detail
            is PostgresAction.Delete -> "Expense removed" to detail
            else -> null
        }
    }

    private suspend fun contributionCopy(
        action: PostgresAction,
        record: NotifyRow,
        tripName: String,
    ): Pair<String, String>? {
        val local = record.id?.let { repository.getContributionByRemoteId(it) }
        val amount = (record.amountPaise ?: local?.amountPaise)?.let { Money.formatInr(it) }
        val name = if (tripName == "Your trip") {
            local?.tripId?.let { repository.getTrip(it)?.name } ?: tripName
        } else {
            tripName
        }
        val note = record.note ?: local?.note
        val detail = listOfNotNull(name, note, amount).joinToString(" · ")
        return when (action) {
            is PostgresAction.Insert -> "New contribution" to detail
            is PostgresAction.Update -> "Contribution updated" to detail
            is PostgresAction.Delete -> "Contribution removed" to detail
            else -> null
        }
    }

    /** Owner message to everyone on the trip, including the owner's phone. */
    suspend fun sendAnnouncement(localTripId: Long, title: String, body: String): Result<Unit> =
        runCatching {
            val supabase = client ?: error("Sign in to sync, then try again.")
            val senderId = authRepository.currentUserIdOrNull()
                ?: error("Sign in to send a notification.")
            val trimmedTitle = title.trim()
            val trimmedBody = body.trim()
            if (trimmedTitle.isEmpty() || trimmedBody.isEmpty()) {
                error("Write a title and a message.")
            }
            if (trimmedTitle.length > 80 || trimmedBody.length > 240) {
                error("Keep the title under 80 characters and the message under 240.")
            }
            val trip = repository.getTrip(localTripId) ?: error("That trip is no longer on this phone.")
            val ownerId = trip.remoteOwnerId
            val ownsTrip = when {
                ownerId != null -> ownerId == senderId
                trip.remoteId == null -> true
                else -> false
            }
            if (!ownsTrip) error("Only the trip owner can send this.")
            val remoteTripId = trip.remoteId
                ?: error("This trip isn't online yet. Pull to refresh, then try again.")
            supabase.from("trip_announcements").insert(
                TripAnnouncementRow(
                    tripId = remoteTripId,
                    senderId = senderId,
                    title = trimmedTitle,
                    body = trimmedBody,
                ),
            )
            showNotification(trimmedTitle, trimmedBody, true, "announce:$remoteTripId:${trimmedTitle.take(40)}")
            if (!TripNotifications.areEnabled(appContext)) {
                error("Sent. Turn on notifications for Chilavu to see it on this phone.")
            }
        }

    private fun announcementCopy(
        action: PostgresAction,
        record: NotifyRow,
    ): Pair<String, String>? {
        if (action !is PostgresAction.Insert) return null
        if (record.senderId != null && record.senderId == authRepository.currentUserIdOrNull()) {
            return null
        }
        val headline = record.title?.takeIf { it.isNotBlank() } ?: return null
        val message = record.body?.takeIf { it.isNotBlank() } ?: return null
        return headline to message
    }

    private fun joinCopy(
        action: PostgresAction,
        record: NotifyRow,
        tripName: String,
    ): Pair<String, String>? {
        if (action !is PostgresAction.Insert) return null
        if (record.role == "owner") return null
        if (record.userId != null && record.userId == authRepository.currentUserIdOrNull()) return null
        val tripId = record.tripId
        val joinedUserId = record.userId
        if (!tripId.isNullOrBlank() && !joinedUserId.isNullOrBlank() &&
            !markJoinAlerted(tripId, joinedUserId)
        ) {
            return null
        }
        val who = record.displayName?.trim()?.takeIf { it.isNotEmpty() }
        val body = if (who != null) "$who · $tripName" else tripName
        return "Friend joined" to body
    }

    /** Owner hears about people who joined, including joins the live event missed. */
    private suspend fun reportNewJoins(supabase: SupabaseClient) {
        val rows = runCatching {
            supabase.from("trip_access").select().decodeList<RemoteTripAccess>()
        }.getOrElse { err ->
            Log.w(TAG, "Couldn't read who joined", err)
            return
        }
        val userId = authRepository.currentUserIdOrNull()
        for (row in rows) {
            if (row.role == "owner" || row.userId == userId) continue
            val trip = repository.getTripByRemoteId(row.tripId) ?: continue
            if (!ownsTrip(trip, userId)) continue
            if (!markJoinAlerted(row.tripId, row.userId)) continue
            val who = row.displayName?.trim()?.takeIf { it.isNotEmpty() }
            val body = if (who != null) "$who · ${trip.name}" else trip.name
            showNotification("Friend joined", body, true, "join:${row.tripId}:${row.userId}")
        }
    }

    private fun ownsTrip(trip: TripEntity, userId: String?): Boolean {
        if (userId.isNullOrBlank()) return false
        val ownerId = trip.remoteOwnerId
        return when {
            ownerId != null -> ownerId == userId
            trip.remoteId == null -> true
            else -> false
        }
    }

    /** Returns true the first time this person is announced for this trip. */
    private fun markJoinAlerted(tripId: String, userId: String): Boolean {
        val key = "$tripId:$userId"
        if (!announcedJoins.add(key)) return false
        val prefs = appContext.getSharedPreferences(JOIN_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).commit()
        return true
    }

    private suspend fun reachedCopy(
        action: PostgresAction,
        record: NotifyRow,
        trip: TripEntity?,
    ): Pair<String, String>? {
        if (action !is PostgresAction.Update) return null
        if (record.status != "REACHED") return null
        val activeTrip = trip ?: return null
        if (!activeTrip.routeTrackingActive) return null
        val remoteId = record.id ?: return null
        val local = repository.getRouteStopByRemoteId(remoteId)
        if (local?.status == RouteStopStatus.REACHED) return null
        if (!reachedStopIds.add(remoteId)) return null
        val title = record.title ?: local?.title ?: "Stop"
        return "Stop reached" to "${activeTrip.name}: $title"
    }

    override fun onTripUpserted(localId: Long) = liveLaunch { upsertTrip(localId) }

    override fun onTripDeleted(remoteId: String?) = liveLaunch {
        deleteCoverObject(remoteId)
        deleteRemoteRow("trips", remoteId)
    }

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

    override fun onRouteStopUpserted(localId: Long) =
        liveLaunch(muteNotifications = false) { upsertRouteStop(localId) }

    override fun onRouteStopDeleted(remoteId: String?) =
        liveLaunch(muteNotifications = false) { deleteRemoteRow("route_stops", remoteId) }

    private fun liveLaunch(muteNotifications: Boolean = true, block: suspend () -> Unit) {
        if (muteNotifications) {
            muteNotificationsUntilMs = System.currentTimeMillis() + SELF_NOTIFY_MUTE_MS
        }
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
            val latest = repository.getTrip(localId) ?: return
            publishCover(supabase, latest, remote.id, isOwner = true)
        } else {
            supabase.from("trips").update(
                {
                    set("name", remote.name)
                    set("destination", remote.destination)
                    set("place_lat", remote.placeLat)
                    set("place_lng", remote.placeLng)
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

    private fun coverObjectKeyFor(remoteId: String) = "$remoteId/cover.jpg"

    private fun hasPendingCoverUpload(trip: TripEntity): Boolean {
        if (!trip.coverObjectKey.isNullOrBlank()) return false
        val path = trip.coverImagePath ?: return false
        return File(path).isFile
    }

    /**
     * Uploads or removes the owner's hero in the uploads bucket, then stores the object key
     * on the trip row. A photo that has not uploaded yet does not clear a cover already online.
     */
    private suspend fun publishCover(
        supabase: SupabaseClient,
        trip: TripEntity,
        remoteId: String,
        isOwner: Boolean,
    ) {
        if (!isOwner) return
        val desiredKey = coverObjectKeyFor(remoteId)
        val file = trip.coverImagePath?.let { File(it) }?.takeIf { it.isFile }
        val pathToStore: String? = if (file != null) {
            if (trip.coverObjectKey != desiredKey) {
                val uploaded = runCatching {
                    supabase.storage.from(COVER_BUCKET).upload(desiredKey, file.readBytes()) {
                        upsert = true
                        contentType = ContentType.Image.JPEG
                    }
                }
                if (uploaded.isFailure) {
                    Log.w(TAG, "Cover upload failed", uploaded.exceptionOrNull())
                    return
                }
                repository.putTripSynced(trip.copy(coverObjectKey = desiredKey))
            }
            desiredKey
        } else if (!trip.coverObjectKey.isNullOrBlank()) {
            val removed = runCatching {
                supabase.storage.from(COVER_BUCKET).delete(listOf(trip.coverObjectKey))
            }
            if (removed.isFailure) {
                Log.w(TAG, "Cover delete failed", removed.exceptionOrNull())
                return
            }
            repository.putTripSynced(trip.copy(coverImagePath = null, coverObjectKey = null))
            null
        } else {
            return
        }
        runCatching {
            supabase.from("trips").update({
                set("cover_path", pathToStore)
            }) {
                filter { eq("id", remoteId) }
            }
        }.onFailure { Log.w(TAG, "Cover path update failed", it) }
    }

    private suspend fun attachRemoteCover(
        supabase: SupabaseClient,
        trip: TripEntity,
        remoteId: String,
        remoteCoverPath: String?,
    ): TripEntity {
        if (hasPendingCoverUpload(trip)) return trip
        val localFile = trip.coverImagePath?.let { File(it) }?.takeIf { it.isFile }
        if (remoteCoverPath.isNullOrBlank()) {
            if (trip.coverObjectKey.isNullOrBlank()) return trip
            TripCoverStore.delete(trip.coverImagePath)
            return trip.copy(coverImagePath = null, coverObjectKey = null)
        }
        if (trip.coverObjectKey == remoteCoverPath && localFile != null) return trip
        val bytes = runCatching {
            supabase.storage.from(COVER_BUCKET).downloadAuthenticated(remoteCoverPath)
        }.getOrElse { err ->
            Log.w(TAG, "Cover download failed", err)
            return trip
        }
        val saved = TripCoverStore.saveDownloaded(appContext, remoteId, bytes) ?: return trip
        if (saved != trip.coverImagePath) TripCoverStore.delete(trip.coverImagePath)
        return trip.copy(coverImagePath = saved, coverObjectKey = remoteCoverPath)
    }

    private suspend fun deleteCoverObject(remoteId: String?) {
        if (remoteId.isNullOrBlank()) return
        val supabase = client ?: return
        runCatching {
            supabase.storage.from(COVER_BUCKET).delete(listOf(coverObjectKeyFor(remoteId)))
        }.onFailure { Log.w(TAG, "Cover delete failed", it) }
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
        private const val SELF_NOTIFY_MUTE_MS = 6_000L
        private const val COVER_BUCKET = "uploads"
        private const val JOIN_PREFS = "join_alerts"
    }
}

@Serializable
private data class TripAnnouncementRow(
    @SerialName("trip_id") val tripId: String,
    @SerialName("sender_id") val senderId: String,
    val title: String,
    val body: String,
)

@Serializable
private data class RemoteTripAccess(
    @SerialName("trip_id") val tripId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
private data class DeviceTokenRow(
    val token: String,
    @SerialName("user_id") val userId: String,
)

@Serializable
private data class NotifyRow(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val role: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val name: String? = null,
    val title: String? = null,
    val body: String? = null,
    val note: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    val status: String? = null,
    @SerialName("amount_paise") val amountPaise: Long? = null,
)
