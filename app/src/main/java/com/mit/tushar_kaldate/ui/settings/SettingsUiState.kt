package com.mit.tushar_kaldate.ui.settings

import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.data.local.entity.UserEntity
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.prefs.ThemeMode

data class SettingsUiState(
    val currentUser: UserEntity? = null,
    val isGuest: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultEngine: EngineType = EngineType.AUTO,
    val isGeminiKeyConfigured: Boolean = false,
    val responseLength: Int = 512,
    val isLoading: Boolean = false,
    val message: UiText? = null
)

sealed class SettingsEvent {
    data object NavigateToLogin : SettingsEvent()
    data class ShowMessage(val message: UiText) : SettingsEvent()
}
