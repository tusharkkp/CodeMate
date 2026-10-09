package com.mit.tushar_kaldate.ui.auth

import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText

object AuthValidator {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateName(name: String): UiText? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> UiText.StringResource(R.string.auth_error_name_empty)
            trimmed.length < 2 -> UiText.StringResource(R.string.auth_error_name_short)
            else -> null
        }
    }

    fun validateEmail(email: String): UiText? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> UiText.StringResource(R.string.auth_error_email_empty)
            !EMAIL_REGEX.matches(trimmed) -> UiText.StringResource(R.string.auth_error_email_invalid)
            else -> null
        }
    }

    fun validatePassword(password: String): UiText? {
        return when {
            password.isEmpty() -> UiText.StringResource(R.string.auth_error_password_empty)
            password.length < 6 -> UiText.StringResource(R.string.auth_error_password_short)
            else -> null
        }
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): UiText? {
        return when {
            confirmPassword.isEmpty() -> UiText.StringResource(R.string.auth_error_password_empty)
            password != confirmPassword -> UiText.StringResource(R.string.auth_error_password_mismatch)
            else -> null
        }
    }
}
