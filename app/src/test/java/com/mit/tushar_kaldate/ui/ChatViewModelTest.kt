package com.mit.tushar_kaldate.ui

import app.cash.turbine.test
import com.mit.tushar_kaldate.ai.engine.EngineRouter
import com.mit.tushar_kaldate.ai.engine.FakeLlmEngine
import com.mit.tushar_kaldate.ai.engine.LlmError
import com.mit.tushar_kaldate.data.local.entity.ConversationEntity
import com.mit.tushar_kaldate.data.local.entity.MessageEntity
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.LearnerLevel
import com.mit.tushar_kaldate.data.model.MessageRole
import com.mit.tushar_kaldate.data.model.TutorMode
import com.mit.tushar_kaldate.data.prefs.SessionStore
import com.mit.tushar_kaldate.data.prefs.SettingsStore
import com.mit.tushar_kaldate.data.repository.ChatRepository
import com.mit.tushar_kaldate.ui.chat.ChatViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val sessionStore: SessionStore = mockk(relaxed = true)
    private val settingsStore: SettingsStore = mockk(relaxed = true)

    private val fakeLlmEngine = FakeLlmEngine()
    private lateinit var engineRouter: EngineRouter
    private lateinit var viewModel: ChatViewModel

    private val messagesFlow = MutableStateFlow<List<MessageEntity>>(emptyList())
    private val defaultEngineFlow = MutableStateFlow(EngineType.CLOUD)
    private val userIdFlow = MutableStateFlow<Long?>(1L)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { settingsStore.defaultEngine } returns defaultEngineFlow
        every { sessionStore.userId } returns userIdFlow
        every { chatRepository.observeMessages(any()) } returns messagesFlow

        engineRouter = EngineRouter(cloudEngine = fakeLlmEngine)
        viewModel = ChatViewModel(chatRepository, engineRouter, sessionStore, settingsStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initConversation loads conversation and observes messages`() = runTest {
        val conv = ConversationEntity(
            id = 42L,
            userId = 1L,
            title = "Python Basics",
            mode = TutorMode.CHAT,
            language = "Python",
            level = LearnerLevel.INTERMEDIATE,
            engine = EngineType.CLOUD
        )
        coEvery { chatRepository.getConversation(42L) } returns conv

        val initialMessages = listOf(
            MessageEntity(
                id = 1L,
                conversationId = 42L,
                role = MessageRole.USER,
                content = "Hi"
            ),
            MessageEntity(
                id = 2L,
                conversationId = 42L,
                role = MessageRole.ASSISTANT,
                content = "Hello! How can I help?"
            )
        )
        messagesFlow.value = initialMessages

        viewModel.initConversation(42L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(42L, state.conversationId)
        assertEquals("Python Basics", state.title)
        assertEquals(LearnerLevel.INTERMEDIATE, state.selectedLevel)
        assertEquals(2, state.messages.size)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `sendMessage creates new conversation and persists assistant response`() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any(), any()) } returns 101L
        coEvery { chatRepository.getRecentMessages(any(), any()) } returns emptyList()
        coEvery { chatRepository.addMessage(any(), any(), any(), any(), any()) } returns 1L

        fakeLlmEngine.mode = FakeLlmEngine.Mode.Instant("Here is your explanation")

        viewModel.sendMessage("Explain recursion")
        advanceUntilIdle()

        coVerify {
            chatRepository.createConversation(
                userId = 1L,
                title = any(),
                mode = TutorMode.CHAT,
                language = "Python",
                level = LearnerLevel.BEGINNER,
                engine = EngineType.CLOUD
            )
        }

        coVerify {
            chatRepository.addMessage(
                conversationId = 101L,
                role = MessageRole.USER,
                content = "Explain recursion"
            )
        }

        coVerify {
            chatRepository.addMessage(
                conversationId = 101L,
                role = MessageRole.ASSISTANT,
                content = "Here is your explanation",
                engineUsed = EngineType.CLOUD
            )
        }

        assertFalse(viewModel.uiState.value.isStreaming)
    }

    @Test
    fun `stopGeneration cancels active generation and saves partial response`() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any(), any()) } returns 200L
        coEvery { chatRepository.getRecentMessages(any(), any()) } returns emptyList()
        coEvery { chatRepository.addMessage(any(), any(), any(), any(), any()) } returns 1L

        fakeLlmEngine.mode = FakeLlmEngine.Mode.Streaming(
            chunks = listOf("Partial", " code"),
            delayMs = 50L
        )

        viewModel.sendMessage("Long stream")
        testDispatcher.scheduler.advanceTimeBy(60L) // First chunk emitted

        viewModel.stopGeneration()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isStreaming)
        coVerify(atLeast = 1) {
            chatRepository.addMessage(
                conversationId = 200L,
                role = MessageRole.ASSISTANT,
                content = match { it.startsWith("Partial") },
                engineUsed = any()
            )
        }
    }

    @Test
    fun `error during generation sets errorMessage in uiState`() = runTest {
        coEvery { chatRepository.createConversation(any(), any(), any(), any(), any(), any()) } returns 300L
        coEvery { chatRepository.getRecentMessages(any(), any()) } returns emptyList()

        fakeLlmEngine.mode = FakeLlmEngine.Mode.Error(LlmError.RateLimited())

        viewModel.sendMessage("Will fail")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isStreaming)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `setLanguage, setLevel, setEngine mutate uiState`() = runTest {
        viewModel.setLanguage("Kotlin")
        assertEquals("Kotlin", viewModel.uiState.value.selectedLanguage)

        viewModel.setLevel(LearnerLevel.ADVANCED)
        assertEquals(LearnerLevel.ADVANCED, viewModel.uiState.value.selectedLevel)

        viewModel.setEngine(EngineType.LOCAL)
        assertEquals(EngineType.LOCAL, viewModel.uiState.value.selectedEngine)
    }
}
