package com.mit.tushar_kaldate.ui.auth

import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun validateName_validName_returnsNull() {
        val result = AuthValidator.validateName("Tushar")
        assertNull(result)
    }

    @Test
    fun validateName_emptyOrBlank_returnsEmptyError() {
        val resultBlank = AuthValidator.validateName("   ")
        val resultEmpty = AuthValidator.validateName("")

        assertEquals(R.string.auth_error_name_empty, (resultBlank as UiText.StringResource).resId)
        assertEquals(R.string.auth_error_name_empty, (resultEmpty as UiText.StringResource).resId)
    }

    @Test
    fun validateName_singleChar_returnsShortError() {
        val result = AuthValidator.validateName("A")
        assertEquals(R.string.auth_error_name_short, (result as UiText.StringResource).resId)
    }

    @Test
    fun validateEmail_validEmail_returnsNull() {
        val result = AuthValidator.validateEmail("student@university.edu")
        assertNull(result)
    }

    @Test
    fun validateEmail_empty_returnsEmptyError() {
        val result = AuthValidator.validateEmail("")
        assertEquals(R.string.auth_error_email_empty, (result as UiText.StringResource).resId)
    }

    @Test
    fun validateEmail_invalidFormat_returnsInvalidError() {
        val resultNoAt = AuthValidator.validateEmail("studentuniversity.edu")
        val resultNoDomain = AuthValidator.validateEmail("student@")
        val resultNoDot = AuthValidator.validateEmail("student@domain")

        assertEquals(R.string.auth_error_email_invalid, (resultNoAt as UiText.StringResource).resId)
        assertEquals(R.string.auth_error_email_invalid, (resultNoDomain as UiText.StringResource).resId)
        assertEquals(R.string.auth_error_email_invalid, (resultNoDot as UiText.StringResource).resId)
    }

    @Test
    fun validatePassword_validPassword_returnsNull() {
        val result = AuthValidator.validatePassword("secret123")
        assertNull(result)
    }

    @Test
    fun validatePassword_empty_returnsEmptyError() {
        val result = AuthValidator.validatePassword("")
        assertEquals(R.string.auth_error_password_empty, (result as UiText.StringResource).resId)
    }

    @Test
    fun validatePassword_lessThan6Chars_returnsShortError() {
        val result = AuthValidator.validatePassword("12345")
        assertEquals(R.string.auth_error_password_short, (result as UiText.StringResource).resId)
    }

    @Test
    fun validateConfirmPassword_matching_returnsNull() {
        val result = AuthValidator.validateConfirmPassword("password123", "password123")
        assertNull(result)
    }

    @Test
    fun validateConfirmPassword_mismatch_returnsMismatchError() {
        val result = AuthValidator.validateConfirmPassword("password123", "password456")
        assertEquals(R.string.auth_error_password_mismatch, (result as UiText.StringResource).resId)
    }
}
