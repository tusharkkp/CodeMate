package com.mit.tushar_kaldate.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.ai.engine.ChatTurn
import com.mit.tushar_kaldate.ai.engine.EngineRouter
import com.mit.tushar_kaldate.ai.engine.LlmChunk
import com.mit.tushar_kaldate.ai.prompt.PromptBuilder
import com.mit.tushar_kaldate.data.local.entity.MessageEntity
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.LearnerLevel
import com.mit.tushar_kaldate.data.model.MessageRole
import com.mit.tushar_kaldate.data.model.TutorMode
import com.mit.tushar_kaldate.data.prefs.SessionStore
import com.mit.tushar_kaldate.data.prefs.SettingsStore
import com.mit.tushar_kaldate.data.repository.ChatRepository
import com.mit.tushar_kaldate.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing Chat state, token streaming, AI engine selection,
 * prompt construction, and Room conversation persistence.
 */
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val engineRouter: EngineRouter,
    private val sessionStore: SessionStore,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null
    private var generationJob: Job? = null
    private val currentStreamingText = java.lang.StringBuilder()
    private var isPartialSaved = false
    private var lastPersistedEntities = listOf<MessageEntity>()

    init {
        // Initialize default preferences
        viewModelScope.launch {
            val preferredEngine = settingsStore.defaultEngine.first()
            _uiState.update { it.copy(selectedEngine = preferredEngine) }
        }
    }

    fun initConversation(conversationId: Long? = null) {
        if (conversationId != null) {
            viewModelScope.launch {
                val conv = chatRepository.getConversation(conversationId)
                if (conv != null) {
                    _uiState.update {
                        it.copy(
                            conversationId = conv.id,
                            title = conv.title,
                            selectedLanguage = conv.language,
                            selectedLevel = conv.level,
                            selectedEngine = conv.engine,
                            mode = conv.mode
                        )
                    }
                    observeMessagesFor(conv.id)
                }
            }
        } else {
            messagesJob?.cancel()
            lastPersistedEntities = emptyList()
            _uiState.update {
                it.copy(
                    conversationId = null,
                    title = "New Chat",
                    messages = emptyList(),
                    isEmpty = true
                )
            }
        }
    }

    fun setLanguage(language: String) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun setLevel(level: LearnerLevel) {
        _uiState.update { it.copy(selectedLevel = level) }
    }

    fun setEngine(engine: EngineType) {
        _uiState.update { it.copy(selectedEngine = engine) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun sendMessage(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isEmpty() || _uiState.value.isStreaming) return

        viewModelScope.launch {
            try {
                var convId = _uiState.value.conversationId
                if (convId == null) {
                    val userId = sessionStore.userId.first() ?: 0L
                    val title = if (trimmed.length > 30) {
                        trimmed.take(30) + "…"
                    } else {
                        trimmed
                    }

                    convId = chatRepository.createConversation(
                        userId = userId,
                        title = title,
                        mode = _uiState.value.mode,
                        language = _uiState.value.selectedLanguage,
                        level = _uiState.value.selectedLevel,
                        engine = _uiState.value.selectedEngine
                    )
                    _uiState.update { it.copy(conversationId = convId, title = title) }
                    observeMessagesFor(convId)
                }

                // Persist user message to Room
                chatRepository.addMessage(
                    conversationId = convId,
                    role = MessageRole.USER,
                    content = trimmed
                )

                // Start AI generation
                generateResponse(convId, trimmed)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = UiText.DynamicString(e.message ?: "Failed to send message")
                    )
                }
            }
        }
    }

    private suspend fun generateResponse(conversationId: Long, currentPrompt: String) {
        val currentState = _uiState.value
        val recentMessages = chatRepository.getRecentMessages(conversationId, 12)
        // History excluding the just-sent prompt
        val historyTurns = recentMessages
            .dropLast(1)
            .map { ChatTurn(role = it.role, content = it.content) }

        val request = PromptBuilder.buildRequest(
            prompt = currentPrompt,
            mode = currentState.mode,
            language = currentState.selectedLanguage,
            level = currentState.selectedLevel,
            history = historyTurns,
            engineType = currentState.selectedEngine
        )

        val resolvedEngine = engineRouter.resolveEngine(currentState.selectedEngine)

        currentStreamingText.clear()
        isPartialSaved = false
        _uiState.update { it.copy(isStreaming = true, errorMessage = null) }

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            try {
                resolvedEngine.generate(request).collect { chunk ->
                    when (chunk) {
                        is LlmChunk.Delta -> {
                            currentStreamingText.append(chunk.text)
                            updateUiWithTransientMessage(
                                currentStreamingText.toString(),
                                resolvedEngine.type
                            )
                        }
                        is LlmChunk.Done -> {
                            saveAssistantMessage(
                                conversationId,
                                currentStreamingText.toString(),
                                resolvedEngine.type
                            )
                        }
                        is LlmChunk.Failure -> {
                            if (currentStreamingText.isNotEmpty()) {
                                saveAssistantMessage(
                                    conversationId,
                                    currentStreamingText.toString(),
                                    resolvedEngine.type
                                )
                            }
                            _uiState.update {
                                it.copy(
                                    isStreaming = false,
                                    errorMessage = UiText.StringResource(chunk.error.getStringResId())
                                )
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                if (!isPartialSaved && currentStreamingText.isNotEmpty()) {
                    saveAssistantMessage(
                        conversationId,
                        currentStreamingText.toString(),
                        resolvedEngine.type
                    )
                }
                _uiState.update { it.copy(isStreaming = false) }
                throw e
            } catch (e: Exception) {
                if (!isPartialSaved && currentStreamingText.isNotEmpty()) {
                    saveAssistantMessage(
                        conversationId,
                        currentStreamingText.toString(),
                        resolvedEngine.type
                    )
                }
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        errorMessage = UiText.DynamicString(e.message ?: "AI response failed")
                    )
                }
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
    }

    fun regenerateLastMessage() {
        val convId = _uiState.value.conversationId ?: return
        if (_uiState.value.isStreaming) return

        viewModelScope.launch {
            val lastUserMsg = lastPersistedEntities.lastOrNull { it.role == MessageRole.USER }
            if (lastUserMsg != null) {
                generateResponse(convId, lastUserMsg.content)
            }
        }
    }

    private suspend fun saveAssistantMessage(
        conversationId: Long,
        content: String,
        engineType: EngineType
    ) {
        if (isPartialSaved || content.isBlank()) return
        isPartialSaved = true
        chatRepository.addMessage(
            conversationId = conversationId,
            role = MessageRole.ASSISTANT,
            content = content,
            engineUsed = engineType
        )
        _uiState.update { it.copy(isStreaming = false) }
    }

    private fun observeMessagesFor(conversationId: Long) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { entities ->
                lastPersistedEntities = entities
                rebuildUiMessages(entities)
            }
        }
    }

    private fun updateUiWithTransientMessage(streamingContent: String, engineType: EngineType) {
        rebuildUiMessages(lastPersistedEntities, streamingContent, engineType)
    }

    private fun rebuildUiMessages(
        persisted: List<MessageEntity>,
        streamingContent: String? = null,
        engineType: EngineType = EngineType.CLOUD
    ) {
        val uiList = mutableListOf<ChatMessageUiModel>()

        for ((index, entity) in persisted.withIndex()) {
            val isLast = (index == persisted.size - 1) && (streamingContent == null)
            uiList.add(
                ChatMessageUiModel(
                    id = entity.id,
                    role = entity.role,
                    content = entity.content,
                    engineUsed = entity.engineUsed,
                    isStreaming = false,
                    isLastMessage = isLast
                )
            )
        }

        if (streamingContent != null) {
            uiList.add(
                ChatMessageUiModel(
                    id = -1L,
                    role = MessageRole.ASSISTANT,
                    content = streamingContent,
                    engineUsed = engineType,
                    isStreaming = true,
                    isLastMessage = true
                )
            )
        }

        _uiState.update {
            it.copy(
                messages = uiList,
                isEmpty = uiList.isEmpty()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
        messagesJob?.cancel()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(
                    chatRepository = container.chatRepository,
                    engineRouter = container.engineRouter,
                    sessionStore = container.sessionStore,
                    settingsStore = container.settingsStore
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
