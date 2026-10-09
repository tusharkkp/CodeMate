package com.mit.tushar_kaldate.ui.chat

import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.LearnerLevel
import com.mit.tushar_kaldate.data.model.TutorMode

data class ChatUiState(
    val conversationId: Long? = null,
    val title: String = "New Chat",
    val messages: List<ChatMessageUiModel> = emptyList(),
    val mode: TutorMode = TutorMode.CHAT,
    val selectedLanguage: String = "Python",
    val selectedLevel: LearnerLevel = LearnerLevel.BEGINNER,
    val selectedEngine: EngineType = EngineType.CLOUD,
    val isStreaming: Boolean = false,
    val errorMessage: UiText? = null,
    val isLoading: Boolean = false,
    val isEmpty: Boolean = true
)
