package com.tripbudget.app.cloud

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class AuthUser(
    val id: String,
    val email: String?,
)

class AuthRepository(
    private val auth: Auth?,
) {
    val isConfigured: Boolean = auth != null

    val sessionStatus: Flow<SessionStatus> =
        auth?.sessionStatus ?: flowOf(SessionStatus.NotAuthenticated(false))

    val currentUser: Flow<AuthUser?> =
        sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> status.session.user?.toAuthUser()
                // Token refresh in progress — keep the cached user so UI does not bounce to Sign in.
                is SessionStatus.RefreshFailure -> auth?.currentUserOrNull()?.toAuthUser()
                is SessionStatus.Initializing -> auth?.currentUserOrNull()?.toAuthUser()
                is SessionStatus.NotAuthenticated -> null
            }
        }

    /**
     * True while session is still loading or refreshing.
     * UI should keep the last screen (or a loader) — never wipe local data in this window.
     */
    val isAuthResolving: Flow<Boolean> =
        sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Initializing -> true
                is SessionStatus.RefreshFailure -> true
                is SessionStatus.Authenticated,
                is SessionStatus.NotAuthenticated,
                -> false
            }
        }

    fun currentUserIdOrNull(): String? =
        auth?.currentUserOrNull()?.id

    fun currentEmailOrNull(): String? =
        auth?.currentUserOrNull()?.email

    fun hasSession(): Boolean =
        currentUserIdOrNull() != null

    /** True when signed in with an email account (not anonymous). */
    fun hasEmailAccount(): Boolean =
        !currentEmailOrNull().isNullOrBlank()

    suspend fun signInAnonymously(): Result<Unit> = runCatching {
        val client = auth
            ?: error("Supabase is not configured.")
        client.signInAnonymously()
    }

    /** Ensures a session exists for join-by-code (anonymous if needed). */
    suspend fun ensureSessionForJoin(): Result<Unit> {
        if (currentUserIdOrNull() != null) return Result.success(Unit)
        return signInAnonymously()
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        val client = auth ?: error("Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties.")
        client.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        val client = auth ?: error("Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties.")
        client.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        val client = auth ?: return@runCatching
        client.signOut()
    }

    private fun UserInfo.toAuthUser(): AuthUser =
        AuthUser(id = id, email = email)
}
