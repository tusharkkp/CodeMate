package com.mit.tushar_kaldate.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mit.tushar_kaldate.BuildConfig
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.prefs.SettingsStore
import com.mit.tushar_kaldate.data.prefs.ThemeMode
import com.mit.tushar_kaldate.data.repository.AuthRepository
import com.mit.tushar_kaldate.data.repository.ChatRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val settingsStore: SettingsStore,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val hasBuildConfigKey = BuildConfig.GEMINI_API_KEY.isNotBlank()

        viewModelScope.launch {
            combine(
                authRepository.currentUser,
                settingsStore.themeMode,
                settingsStore.defaultEngine,
                settingsStore.responseLength,
                settingsStore.customGeminiApiKey
            ) { user, theme, engine, respLength, customKey ->
                SettingsUiState(
                    currentUser = user,
                    isGuest = user?.isGuest == true,
                    themeMode = theme,
                    defaultEngine = engine,
                    isGeminiKeyConfigured = hasBuildConfigKey || !customKey.isNullOrBlank(),
                    responseLength = respLength
                )
            }.collect { combinedState ->
                _uiState.value = combinedState
            }
        }
    }

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch {
            settingsStore.setThemeMode(theme)
        }
    }

    fun setDefaultEngine(engine: EngineType) {
        viewModelScope.launch {
            settingsStore.setDefaultEngine(engine)
        }
    }

    fun clearChatData() {
        val userId = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            chatRepository.clearAllConversationsForUser(userId)
            _uiState.update { it.copy(isLoading = false) }
            _events.send(SettingsEvent.ShowMessage(UiText.StringResource(R.string.settings_clear_data_success)))
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _events.send(SettingsEvent.NavigateToLogin)
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val settingsStore: SettingsStore,
        private val chatRepository: ChatRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(authRepository, settingsStore, chatRepository) as T
        }
    }
}
