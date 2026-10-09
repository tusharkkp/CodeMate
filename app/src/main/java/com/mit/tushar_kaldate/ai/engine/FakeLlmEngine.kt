package com.mit.tushar_kaldate.ai.engine

import com.mit.tushar_kaldate.data.model.EngineType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Deterministic fake implementation of [LlmEngine] for unit testing,
 * UI development, and offline mock execution.
 */
class FakeLlmEngine(
    override val type: EngineType = EngineType.CLOUD,
    var mode: Mode = Mode.Instant("Hello! I am CodeMate, your programming tutor. How can I help you today?")
) : LlmEngine {

    sealed class Mode {
        data class Instant(val response: String) : Mode()
        data class Streaming(val chunks: List<String>, val delayMs: Long = 15L) : Mode()
        data class Error(val error: LlmError) : Mode()
    }

    var availableStatus: EngineStatus = EngineStatus.Ready
    val recordedRequests = mutableListOf<LlmRequest>()

    override suspend fun isAvailable(): EngineStatus = availableStatus

    override fun generate(request: LlmRequest): Flow<LlmChunk> = flow {
        recordedRequests.add(request)
        when (val currentMode = mode) {
            is Mode.Instant -> {
                emit(LlmChunk.Delta(currentMode.response))
                emit(LlmChunk.Done(TokenUsage(promptTokens = 10, completionTokens = 20, totalTokens = 30)))
            }
            is Mode.Streaming -> {
                for (chunk in currentMode.chunks) {
                    if (currentMode.delayMs > 0) {
                        delay(currentMode.delayMs)
                    }
                    emit(LlmChunk.Delta(chunk))
                }
                emit(LlmChunk.Done(TokenUsage(promptTokens = 10, completionTokens = 20, totalTokens = 30)))
            }
            is Mode.Error -> {
                emit(LlmChunk.Failure(currentMode.error))
            }
        }
    }
}
