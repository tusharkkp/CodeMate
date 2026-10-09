package com.mit.tushar_kaldate.ai.engine

import com.mit.tushar_kaldate.data.model.EngineType
import kotlinx.coroutines.flow.Flow

/**
 * Universal abstraction for AI language model execution.
 * Backed by GeminiCloudEngine (Cloud), LiteRtLmEngine (On-Device),
 * or FakeLlmEngine (Tests & Offline Simulation).
 */
interface LlmEngine {
    val type: EngineType

    suspend fun isAvailable(): EngineStatus

    fun generate(request: LlmRequest): Flow<LlmChunk>
}
