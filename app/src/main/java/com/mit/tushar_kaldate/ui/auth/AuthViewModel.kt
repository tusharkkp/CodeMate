package com.mit.tushar_kaldate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.data.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun login(email: String, password: String) {
        val emailErr = AuthValidator.validateEmail(email)
        val passwordErr = AuthValidator.validatePassword(password)

        if (emailErr != null || passwordErr != null) {
            _uiState.update {
                it.copy(
                    emailError = emailErr,
                    passwordError = passwordErr,
                    generalError = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                emailError = null,
                passwordError = null,
                generalError = null
            )
        }

        viewModelScope.launch {
            val result = authRepository.login(email, password)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                _events.send(AuthEvent.NavigateToHome)
            }.onFailure { ex ->
                val errorMsg = ex.message ?: "Authentication failed"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = UiText.DynamicString(errorMsg)
                    )
                }
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        val nameErr = AuthValidator.validateName(name)
        val emailErr = AuthValidator.validateEmail(email)
        val passwordErr = AuthValidator.validatePassword(password)
        val confirmPasswordErr = AuthValidator.validateConfirmPassword(password, confirmPassword)

        if (nameErr != null || emailErr != null || passwordErr != null || confirmPasswordErr != null) {
            _uiState.update {
                it.copy(
                    nameError = nameErr,
                    emailError = emailErr,
                    passwordError = passwordErr,
                    confirmPasswordError = confirmPasswordErr,
                    generalError = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                nameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                generalError = null
            )
        }

        viewModelScope.launch {
            val result = authRepository.register(name, email, password)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                _events.send(AuthEvent.NavigateToHome)
            }.onFailure { ex ->
                val errorMsg = ex.message ?: "Registration failed"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = UiText.DynamicString(errorMsg)
                    )
                }
            }
        }
    }

    fun loginAsGuest() {
        _uiState.update {
            it.copy(
                isLoading = true,
                nameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                generalError = null
            )
        }

        viewModelScope.launch {
            val result = authRepository.loginAsGuest()
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                _events.send(AuthEvent.NavigateToHome)
            }.onFailure { ex ->
                val errorMsg = ex.message ?: "Guest login failed"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = UiText.DynamicString(errorMsg)
                    )
                }
            }
        }
    }

    fun clearErrors() {
        _uiState.update {
            it.copy(
                nameError = null,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                generalError = null
            )
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
