package com.tripbudget.app.cloud

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.RestException
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

/** Short copy for the sign-in sheet. Supabase exceptions include the request URL. */
fun friendlyAuthError(error: Throwable, signingUp: Boolean): String {
    val auth = generateSequence(error) { it.cause }
        .filterIsInstance<AuthRestException>()
        .firstOrNull()
    if (auth != null) {
        return when (auth.errorCode) {
            AuthErrorCode.InvalidCredentials -> "Email or password is incorrect."
            AuthErrorCode.EmailNotConfirmed -> "Confirm your email, then sign in."
            AuthErrorCode.UserAlreadyExists,
            AuthErrorCode.EmailExists,
            -> "An account with this email already exists. Sign in instead."
            AuthErrorCode.WeakPassword -> "Choose a stronger password."
            AuthErrorCode.ValidationFailed -> validationMessage(auth.errorDescription, signingUp)
            AuthErrorCode.SignupDisabled,
            AuthErrorCode.EmailProviderDisabled,
            -> "Creating an account isn’t available right now."
            AuthErrorCode.OverRequestRateLimit,
            AuthErrorCode.OverEmailSendRateLimit,
            -> "Too many attempts. Wait a moment and try again."
            AuthErrorCode.AnonymousProviderDisabled ->
                "Joining without an account isn’t available right now."
            AuthErrorCode.UserBanned -> "This account can’t sign in."
            AuthErrorCode.RequestTimeout -> "The server took too long. Try again."
            else -> defaultAuthError(signingUp)
        }
    }
    val text = error.message.orEmpty()
    if (
        text.contains("unable to resolve host", ignoreCase = true) ||
        text.contains("failed to connect", ignoreCase = true) ||
        text.contains("timeout", ignoreCase = true)
    ) {
        return "Couldn't reach the server. Check your connection and try again."
    }
    return defaultAuthError(signingUp)
}

private fun validationMessage(description: String, signingUp: Boolean): String {
    val text = description.lowercase()
    return when {
        "email" in text -> "Enter a valid email address."
        "password" in text -> "Password must be at least 6 characters."
        else -> defaultAuthError(signingUp)
    }
}

private fun defaultAuthError(signingUp: Boolean): String =
    if (signingUp) "Couldn't create the account. Try again." else "Sign in failed. Try again."

/**
 * User-facing cloud error. Supabase exceptions include the request URL and headers,
 * so those never go on screen. Short messages thrown by this app are kept.
 */
fun friendlyCloudError(error: Throwable, fallback: String): String {
    if (generateSequence(error) { it.cause }.any { it is AuthRestException }) {
        return friendlyAuthError(error, signingUp = false)
    }
    if (isOffline(error)) {
        return "Couldn't reach the server. Check your connection and try again."
    }
    val rest = generateSequence(error) { it.cause }.filterIsInstance<RestException>().firstOrNull()
    if (rest != null) {
        val sentence = rest.error.trim()
        if (isSafeUserMessage(sentence)) return sentence
        return when (rest.statusCode) {
            401, 403 -> "You don’t have access to do that."
            404 -> "That couldn’t be found."
            408, 504 -> "The server took too long. Try again."
            429 -> "Too many attempts. Wait a moment and try again."
            else -> fallback
        }
    }
    val text = error.message?.trim().orEmpty()
    if (isSafeUserMessage(text)) return text
    return fallback
}

private fun isOffline(error: Throwable): Boolean {
    val text = generateSequence(error) { it.cause }
        .mapNotNull { it.message }
        .joinToString(" ")
    return text.contains("unable to resolve host", ignoreCase = true) ||
        text.contains("failed to connect", ignoreCase = true) ||
        text.contains("timeout", ignoreCase = true) ||
        text.contains("network", ignoreCase = true)
}

private fun isSafeUserMessage(text: String): Boolean {
    if (text.isBlank() || text.length > 160 || text.contains('\n')) return false
    val lower = text.lowercase()
    val technical = listOf(
        "url:",
        "headers:",
        "http",
        "pgrst",
        "jwt",
        "exception",
        "supabase",
        "local.properties",
        "violates",
        "duplicate key",
        "stack",
    )
    return technical.none { it in lower }
}
