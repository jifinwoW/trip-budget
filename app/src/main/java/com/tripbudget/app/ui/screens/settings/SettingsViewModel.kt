package com.tripbudget.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripbudget.app.BuildConfig
import com.tripbudget.app.cloud.AuthRepository
import com.tripbudget.app.cloud.AuthUser
import com.tripbudget.app.cloud.friendlyAuthError
import com.tripbudget.app.cloud.friendlyCloudError
import com.tripbudget.app.cloud.SyncEngine
import com.tripbudget.app.data.SyncPreferences
import com.tripbudget.app.data.HeaderWeatherPreference
import com.tripbudget.app.data.ThemeMode
import com.tripbudget.app.data.ThemePreferences
import com.tripbudget.app.data.TripRepository
import com.tripbudget.app.update.AppUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUpdateUiState(
    val versionName: String = BuildConfig.VERSION_NAME,
    val availableName: String? = null,
    val apkUrl: String? = null,
    val busy: Boolean = false,
    val progress: Float? = null,
    val message: String? = null,
)

data class CloudUiState(
    val configured: Boolean = false,
    val user: AuthUser? = null,
    val emailInput: String = "",
    val passwordInput: String = "",
    val busy: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(
    private val appContext: Context,
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

    private val _update = MutableStateFlow(AppUpdateUiState())
    val update: StateFlow<AppUpdateUiState> = _update.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _cloud.update { it.copy(user = user) }
            }
        }
        refreshAppUpdate()
    }

    fun refreshAppUpdate() {
        if (_update.value.busy) return
        viewModelScope.launch {
            val release = runCatching {
                AppUpdater.newerRelease(BuildConfig.VERSION_CODE)
            }.getOrNull()
            _update.update {
                it.copy(
                    availableName = release?.versionName,
                    apkUrl = release?.apkUrl,
                    message = if (release == null) null else it.message,
                )
            }
        }
    }

    fun installUpdate() {
        val apkUrl = _update.value.apkUrl
        if (_update.value.availableName == null || apkUrl.isNullOrBlank() || _update.value.busy) return
        if (!AppUpdater.canRequestInstall(appContext)) {
            AppUpdater.openInstallSettings(appContext)
            _update.update {
                it.copy(message = "Allow Chilavu to install updates, then tap Update again.")
            }
            return
        }
        viewModelScope.launch {
            _update.update { it.copy(busy = true, progress = null, message = null) }
            val downloaded = runCatching {
                AppUpdater.download(appContext, apkUrl) { fraction ->
                    _update.update { it.copy(progress = fraction) }
                }
            }
            downloaded.fold(
                onSuccess = { apk ->
                    _update.update { it.copy(busy = false, progress = null) }
                    runCatching { AppUpdater.openInstaller(appContext, apk) }
                        .onFailure {
                            _update.update { state ->
                                state.copy(message = "Couldn't open the installer. Try again.")
                            }
                        }
                },
                onFailure = {
                    _update.update { state ->
                        state.copy(busy = false, progress = null, message = "Couldn't download the update. Try again.")
                    }
                },
            )
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
                        onFailure = { err -> friendlyAuthError(err, signingUp = true) },
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
                        onFailure = { err -> friendlyAuthError(err, signingUp = false) },
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
            syncEngine.unregisterPushToken()
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
                        onFailure = { err -> friendlyCloudError(err, "Couldn't sign out. Try again.") },
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
                    onFailure = {
                        "Signed in, but the latest trips couldn’t be loaded. Pull down to refresh."
                    },
                ),
            )
        }
    }

    companion object {
        fun factory(
            appContext: Context,
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
                        appContext,
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
