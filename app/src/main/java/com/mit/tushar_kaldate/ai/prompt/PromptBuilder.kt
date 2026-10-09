package com.mit.tushar_kaldate.ai.prompt

import com.mit.tushar_kaldate.ai.config.AiConfig
import com.mit.tushar_kaldate.ai.engine.ChatTurn
import com.mit.tushar_kaldate.ai.engine.LlmRequest
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.LearnerLevel
import com.mit.tushar_kaldate.data.model.TutorMode

/**
 * Builds [LlmRequest] objects with mode-specific system prompts,
 * context budget trimming, and language/learner-level interpolation.
 */
object PromptBuilder {

    fun buildRequest(
        prompt: String,
        mode: TutorMode = TutorMode.CHAT,
        language: String = "Python",
        level: LearnerLevel = LearnerLevel.BEGINNER,
        history: List<ChatTurn> = emptyList(),
        engineType: EngineType = EngineType.CLOUD,
        temperature: Float = AiConfig.DEFAULT_TEMPERATURE,
        maxTokens: Int = AiConfig.DEFAULT_MAX_TOKENS
    ): LlmRequest {
        val isCompact = engineType == EngineType.LOCAL
        val historyLimit = if (isCompact) {
            AiConfig.LOCAL_HISTORY_TURNS_LIMIT
        } else {
            AiConfig.CLOUD_HISTORY_TURNS_LIMIT
        }

        val trimmedHistory = if (history.size > historyLimit) {
            history.takeLast(historyLimit)
        } else {
            history
        }

        val systemInstruction = PromptTemplates.buildSystemInstruction(
            mode = mode,
            language = language,
            level = level,
            isCompact = isCompact
        )

        return LlmRequest(
            prompt = prompt.trim(),
            systemInstruction = systemInstruction,
            history = trimmedHistory,
            temperature = temperature,
            maxTokens = maxTokens
        )
    }
}
