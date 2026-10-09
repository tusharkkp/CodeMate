package com.mit.tushar_kaldate.ai.engine

import androidx.annotation.StringRes
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.ai.config.AiConfig
import com.mit.tushar_kaldate.data.model.MessageRole

data class ChatTurn(
    val role: MessageRole,
    val content: String
)

data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

data class LlmRequest(
    val prompt: String,
    val systemInstruction: String? = null,
    val history: List<ChatTurn> = emptyList(),
    val temperature: Float = AiConfig.DEFAULT_TEMPERATURE,
    val maxTokens: Int = AiConfig.DEFAULT_MAX_TOKENS
)

sealed class LlmChunk {
    data class Delta(val text: String) : LlmChunk()
    data class Done(val usage: TokenUsage? = null) : LlmChunk()
    data class Failure(val error: LlmError) : LlmChunk()
}

sealed class LlmError(open val rawMessage: String) {
    data class NoNetwork(
        override val rawMessage: String = "No network connection"
    ) : LlmError(rawMessage)

    data class BadKey(
        override val rawMessage: String = "Invalid API key"
    ) : LlmError(rawMessage)

    data class RateLimited(
        override val rawMessage: String = "Rate limited"
    ) : LlmError(rawMessage)

    data class Server(
        val code: Int,
        val detail: String
    ) : LlmError("Server error $code: $detail")

    data class Timeout(
        override val rawMessage: String = "Request timed out"
    ) : LlmError(rawMessage)

    data class ModelMissing(
        override val rawMessage: String = "Model file missing"
    ) : LlmError(rawMessage)

    data class SafetyBlocked(
        val reason: String
    ) : LlmError("Safety blocked: $reason")

    data class Unknown(
        val cause: Throwable? = null,
        override val rawMessage: String = cause?.message ?: "Unknown error"
    ) : LlmError(rawMessage)

    @StringRes
    fun getStringResId(): Int = when (this) {
        is NoNetwork -> R.string.error_no_network
        is BadKey -> R.string.error_bad_key
        is RateLimited -> R.string.error_rate_limited
        is Server -> R.string.error_server
        is Timeout -> R.string.error_timeout
        is ModelMissing -> R.string.error_model_missing
        is SafetyBlocked -> R.string.error_safety_blocked
        is Unknown -> R.string.error_unknown
    }
}

sealed class EngineStatus {
    data object Ready : EngineStatus()
    data class NotConfigured(val reason: String) : EngineStatus()
    data class Unavailable(val reason: String) : EngineStatus()
}
