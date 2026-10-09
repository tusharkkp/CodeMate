package com.mit.tushar_kaldate.ui.settings

import app.cash.turbine.test
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.data.local.entity.UserEntity
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.prefs.SettingsStore
import com.mit.tushar_kaldate.data.prefs.ThemeMode
import com.mit.tushar_kaldate.data.repository.AuthRepository
import com.mit.tushar_kaldate.data.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val settingsStore: SettingsStore = mockk(relaxed = true)
    private val chatRepository: ChatRepository = mockk(relaxed = true)

    private val userFlow = MutableStateFlow<UserEntity?>(null)
    private val themeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val engineFlow = MutableStateFlow(EngineType.AUTO)
    private val responseLengthFlow = MutableStateFlow(512)
    private val customKeyFlow = MutableStateFlow<String?>(null)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { authRepository.currentUser } returns userFlow
        every { settingsStore.themeMode } returns themeFlow
        every { settingsStore.defaultEngine } returns engineFlow
        every { settingsStore.responseLength } returns responseLengthFlow
        every { settingsStore.customGeminiApiKey } returns customKeyFlow

        viewModel = SettingsViewModel(authRepository, settingsStore, chatRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_observesUserAndPreferences() = runTest {
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(EngineType.AUTO, state.defaultEngine)

        val user = UserEntity(
            id = 5L,
            name = "Dev User",
            email = "dev@codemate.app",
            passwordHash = "hash",
            salt = "salt",
            isGuest = false
        )
        userFlow.value = user
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Dev User", state.currentUser?.name)
        assertFalse(state.isGuest)
    }

    @Test
    fun setTheme_delegatesToSettingsStore() = runTest {
        viewModel.setTheme(ThemeMode.DARK)
        advanceUntilIdle()

        coVerify { settingsStore.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun setDefaultEngine_delegatesToSettingsStore() = runTest {
        viewModel.setDefaultEngine(EngineType.LOCAL)
        advanceUntilIdle()

        coVerify { settingsStore.setDefaultEngine(EngineType.LOCAL) }
    }

    @Test
    fun clearChatData_callsRepositoryAndEmitsMessage() = runTest {
        val user = UserEntity(
            id = 7L,
            name = "Test",
            email = "test@codemate.app",
            passwordHash = "h",
            salt = "s"
        )
        userFlow.value = user
        advanceUntilIdle()

        coEvery { chatRepository.clearAllConversationsForUser(7L) } returns 3

        viewModel.events.test {
            viewModel.clearChatData()
            advanceUntilIdle()

            coVerify { chatRepository.clearAllConversationsForUser(7L) }

            val event = awaitItem()
            assertTrue(event is SettingsEvent.ShowMessage)
            assertEquals(
                R.string.settings_clear_data_success,
                ((event as SettingsEvent.ShowMessage).message as UiText.StringResource).resId
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun logout_callsAuthRepositoryAndEmitsNavigateToLogin() = runTest {
        viewModel.events.test {
            viewModel.logout()
            advanceUntilIdle()

            coVerify { authRepository.logout() }

            val event = awaitItem()
            assertTrue(event is SettingsEvent.NavigateToLogin)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
