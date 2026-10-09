package com.mit.tushar_kaldate.ui.auth

import com.mit.tushar_kaldate.core.ui.UiText

data class AuthUiState(
    val isLoading: Boolean = false,
    val nameError: UiText? = null,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmPasswordError: UiText? = null,
    val generalError: UiText? = null,
    val isSuccess: Boolean = false
)

sealed class AuthEvent {
    data object NavigateToHome : AuthEvent()
    data class ShowSnackbar(val message: UiText) : AuthEvent()
}
