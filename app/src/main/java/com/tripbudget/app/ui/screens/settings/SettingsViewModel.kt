package com.tripbudget.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripbudget.app.cloud.AuthRepository
import com.tripbudget.app.cloud.AuthUser
import com.tripbudget.app.cloud.SyncEngine
import com.tripbudget.app.data.SyncPreferences
import com.tripbudget.app.data.HeaderWeatherPreference
import com.tripbudget.app.data.ThemeMode
import com.tripbudget.app.data.ThemePreferences
import com.tripbudget.app.data.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CloudUiState(
    val configured: Boolean = false,
    val user: AuthUser? = null,
    val emailInput: String = "",
    val passwordInput: String = "",
    val busy: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(
    private val themePreferences: ThemePreferences,
    private val authRepository: AuthRepository,
    private val syncEngine: SyncEngine,
    private val syncPreferences: SyncPreferences,
    private val tripRepository: TripRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val headerWeather: StateFlow<HeaderWeatherPreference> = themePreferences.headerWeather
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HeaderWeatherPreference.LIVE,
        )

    private val _cloud = MutableStateFlow(
        CloudUiState(configured = authRepository.isConfigured),
    )
    val cloud: StateFlow<CloudUiState> = _cloud.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _cloud.update { it.copy(user = user) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun setHeaderWeather(preference: HeaderWeatherPreference) {
        viewModelScope.launch {
            themePreferences.setHeaderWeather(preference)
        }
    }

    fun onEmailChange(value: String) {
        _cloud.update { it.copy(emailInput = value, message = null) }
    }

    fun onPasswordChange(value: String) {
        _cloud.update { it.copy(passwordInput = value, message = null) }
    }

    fun signUp() {
        val email = _cloud.value.emailInput
        val password = _cloud.value.passwordInput
        if (email.isBlank() || password.length < 6) {
            _cloud.update { it.copy(message = "Enter email and a password (min 6 characters)") }
            return
        }
        viewModelScope.launch {
            _cloud.update { it.copy(busy = true, message = null) }
            val result = authRepository.signUp(email, password)
            _cloud.update {
                it.copy(
                    busy = false,
                    passwordInput = if (result.isSuccess) "" else it.passwordInput,
                    message = result.fold(
                        onSuccess = {
                            "Account created. If email confirmation is on, check your inbox; otherwise sign in."
                        },
                        onFailure = { err -> err.message ?: "Sign up failed" },
                    ),
                )
            }
            if (result.isSuccess && authRepository.currentUserIdOrNull() != null) {
                syncAfterAuth()
            }
        }
    }

    fun signIn() {
        val email = _cloud.value.emailInput
        val password = _cloud.value.passwordInput
        if (email.isBlank() || password.isBlank()) {
            _cloud.update { it.copy(message = "Enter email and password") }
            return
        }
        viewModelScope.launch {
            _cloud.update { it.copy(busy = true, message = null) }
            val result = authRepository.signIn(email, password)
            _cloud.update {
                it.copy(
                    busy = false,
                    passwordInput = if (result.isSuccess) "" else it.passwordInput,
                    message = result.fold(
                        onSuccess = { "Signed in" },
                        onFailure = { err -> err.message ?: "Sign in failed" },
                    ),
                )
            }
            if (result.isSuccess) {
                syncAfterAuth()
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _cloud.update { it.copy(busy = true, message = null) }
            // Stop live sync first, then end session, then wipe cache (off main thread).
            syncEngine.stopRealtime()
            val result = authRepository.signOut()
            if (result.isSuccess) {
                tripRepository.clearAllLocalData()
                syncPreferences.clearLastSyncedAt()
            }
            _cloud.update {
                it.copy(
                    busy = false,
                    message = result.fold(
                        onSuccess = { null },
                        onFailure = { err -> err.message ?: "Sign out failed" },
                    ),
                )
            }
        }
    }

    private suspend fun syncAfterAuth() {
        _cloud.update { it.copy(busy = true, message = "Going live…") }
        syncEngine.startRealtime()
        val result = syncEngine.refresh(force = true)
        _cloud.update {
            it.copy(
                busy = false,
                message = result.fold(
                    onSuccess = { "Signed in — live and online" },
                    onFailure = { err -> "Signed in, but refresh failed: ${err.message}" },
                ),
            )
        }
    }

    companion object {
        fun factory(
            themePreferences: ThemePreferences,
            authRepository: AuthRepository,
            syncEngine: SyncEngine,
            syncPreferences: SyncPreferences,
            tripRepository: TripRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        themePreferences,
                        authRepository,
                        syncEngine,
                        syncPreferences,
                        tripRepository,
                    ) as T
                }
            }
    }
}
