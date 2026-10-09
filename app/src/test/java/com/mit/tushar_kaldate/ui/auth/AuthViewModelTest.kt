package com.mit.tushar_kaldate.ui.auth

import app.cash.turbine.test
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.data.local.entity.UserEntity
import com.mit.tushar_kaldate.data.repository.AuthRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    private class FakeAuthRepository : AuthRepository(mockk(relaxed = true), mockk(relaxed = true)) {
        var loginResult: Result<UserEntity> = Result.success(
            UserEntity(1L, "Default", "default@test.com", "h", "s")
        )
        var registerResult: Result<UserEntity> = Result.success(
            UserEntity(2L, "New", "new@test.com", "h", "s")
        )
        var guestResult: Result<UserEntity> = Result.success(
            UserEntity(3L, "Guest", "guest@test.local", "", "", isGuest = true)
        )
        var loginCalls = 0
        var registerCalls = 0
        var guestCalls = 0

        override suspend fun login(email: String, password: String): Result<UserEntity> {
            loginCalls++
            return loginResult
        }

        override suspend fun register(name: String, email: String, password: String): Result<UserEntity> {
            registerCalls++
            return registerResult
        }

        override suspend fun loginAsGuest(): Result<UserEntity> {
            guestCalls++
            return guestResult
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_invalidInputs_setsFieldErrorsWithoutCallingRepository() = runTest {
        viewModel.login("", "123")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertEquals(R.string.auth_error_email_empty, (state.emailError as UiText.StringResource).resId)
        assertEquals(R.string.auth_error_password_short, (state.passwordError as UiText.StringResource).resId)

        assertEquals(0, fakeAuthRepository.loginCalls)
    }

    @Test
    fun login_success_updatesStateAndEmitsNavigateToHome() = runTest {
        val user = UserEntity(
            id = 42L,
            name = "Test User",
            email = "user@codemate.app",
            passwordHash = "hash",
            salt = "salt"
        )
        fakeAuthRepository.loginResult = Result.success(user)

        viewModel.events.test {
            viewModel.login("user@codemate.app", "password123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isSuccess)
            assertNull(state.emailError)
            assertNull(state.passwordError)

            val event = awaitItem()
            assertTrue(event is AuthEvent.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, fakeAuthRepository.loginCalls)
    }

    @Test
    fun login_failure_setsGeneralError() = runTest {
        fakeAuthRepository.loginResult = Result.failure(IllegalArgumentException("Incorrect password"))

        viewModel.login("user@codemate.app", "wrongpassword")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertNotNull(state.generalError)
        assertTrue(state.generalError is UiText.DynamicString)
        assertEquals("Incorrect password", (state.generalError as UiText.DynamicString).value)
        assertEquals(1, fakeAuthRepository.loginCalls)
    }

    @Test
    fun register_validationFailure_setsFieldErrors() = runTest {
        viewModel.register("A", "invalid-email", "pass", "diffpass")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(R.string.auth_error_name_short, (state.nameError as UiText.StringResource).resId)
        assertEquals(R.string.auth_error_email_invalid, (state.emailError as UiText.StringResource).resId)
        assertEquals(R.string.auth_error_password_short, (state.passwordError as UiText.StringResource).resId)
        assertEquals(R.string.auth_error_password_mismatch, (state.confirmPasswordError as UiText.StringResource).resId)

        assertEquals(0, fakeAuthRepository.registerCalls)
    }

    @Test
    fun register_success_updatesStateAndEmitsNavigateToHome() = runTest {
        val user = UserEntity(
            id = 10L,
            name = "New Learner",
            email = "learner@codemate.app",
            passwordHash = "hash",
            salt = "salt"
        )
        fakeAuthRepository.registerResult = Result.success(user)

        viewModel.events.test {
            viewModel.register("New Learner", "learner@codemate.app", "secure123", "secure123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isSuccess)
            assertNull(state.generalError)

            val event = awaitItem()
            assertTrue(event is AuthEvent.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, fakeAuthRepository.registerCalls)
    }

    @Test
    fun loginAsGuest_success_updatesStateAndEmitsNavigateToHome() = runTest {
        val guest = UserEntity(
            id = 99L,
            name = "Guest Learner",
            email = "guest@codemate.local",
            passwordHash = "",
            salt = "",
            isGuest = true
        )
        fakeAuthRepository.guestResult = Result.success(guest)

        viewModel.events.test {
            viewModel.loginAsGuest()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isSuccess)

            val event = awaitItem()
            assertTrue(event is AuthEvent.NavigateToHome)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, fakeAuthRepository.guestCalls)
    }

    @Test
    fun clearErrors_resetsAllErrors() = runTest {
        viewModel.login("", "123")
        var state = viewModel.uiState.value
        assertNotNull(state.emailError)

        viewModel.clearErrors()
        state = viewModel.uiState.value
        assertNull(state.nameError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.confirmPasswordError)
        assertNull(state.generalError)
    }
}
