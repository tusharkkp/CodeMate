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
    private val conversationsFlow = MutableStateFlow<List<ConversationEntity>>(emptyList())
    private val defaultEngineFlow = MutableStateFlow(EngineType.CLOUD)
    private val userIdFlow = MutableStateFlow<Long?>(1L)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { settingsStore.defaultEngine } returns defaultEngineFlow
        every { sessionStore.userId } returns userIdFlow
        every { chatRepository.observeMessages(any()) } returns messagesFlow
        every { chatRepository.observeConversations(any()) } returns conversationsFlow

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

    @Test
    fun `initConversation with null restores latest conversation when available`() = runTest {
        val conv = ConversationEntity(
            id = 99L,
            userId = 1L,
            title = "Existing Chat",
            mode = TutorMode.CHAT,
            language = "Java",
            level = LearnerLevel.ADVANCED,
            engine = EngineType.CLOUD
        )
        conversationsFlow.value = listOf(conv)
        coEvery { chatRepository.getConversation(99L) } returns conv

        viewModel.initConversation(null)
        advanceUntilIdle()

        assertEquals(99L, viewModel.uiState.value.conversationId)
        assertEquals("Existing Chat", viewModel.uiState.value.title)
        assertEquals("Java", viewModel.uiState.value.selectedLanguage)
        assertEquals(LearnerLevel.ADVANCED, viewModel.uiState.value.selectedLevel)
    }

    @Test
    fun `initConversation retains active conversation without wiping when returning from tab switch`() = runTest {
        val conv = ConversationEntity(
            id = 55L,
            userId = 1L,
            title = "Active Chat",
            mode = TutorMode.CHAT,
            language = "Python",
            level = LearnerLevel.BEGINNER,
            engine = EngineType.CLOUD
        )
        coEvery { chatRepository.getConversation(55L) } returns conv
        viewModel.initConversation(55L)
        advanceUntilIdle()
        assertEquals(55L, viewModel.uiState.value.conversationId)

        // Simulate returning to Chat tab with null conversationId
        viewModel.initConversation(null)
        advanceUntilIdle()

        // Conversation 55L must still be preserved!
        assertEquals(55L, viewModel.uiState.value.conversationId)
        assertEquals("Active Chat", viewModel.uiState.value.title)
    }

    @Test
    fun `startNewConversation resets conversation to clean empty state`() = runTest {
        val conv = ConversationEntity(
            id = 77L,
            userId = 1L,
            title = "Chat to Clear",
            mode = TutorMode.CHAT,
            language = "Python",
            level = LearnerLevel.BEGINNER,
            engine = EngineType.CLOUD
        )
        coEvery { chatRepository.getConversation(77L) } returns conv
        viewModel.initConversation(77L)
        advanceUntilIdle()
        assertEquals(77L, viewModel.uiState.value.conversationId)

        // User taps New Chat
        viewModel.startNewConversation()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.conversationId)
        assertEquals("New Chat", viewModel.uiState.value.title)
        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(0, viewModel.uiState.value.messages.size)
    }
}
