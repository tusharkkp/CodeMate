package com.mit.tushar_kaldate.ai.engine

import com.mit.tushar_kaldate.data.model.EngineType

/**
 * Routes LLM requests to either Cloud or On-Device engine based on
 * user preferences and real-time engine availability.
 */
class EngineRouter(
    val cloudEngine: LlmEngine,
    val localEngine: LlmEngine? = null
) {

    /**
     * Resolves the best available engine for the specified user preference.
     */
    suspend fun resolveEngine(preference: EngineType): LlmEngine {
        return when (preference) {
            EngineType.CLOUD -> cloudEngine
            EngineType.LOCAL -> localEngine ?: cloudEngine
            EngineType.AUTO -> {
                val cloudStatus = cloudEngine.isAvailable()
                if (cloudStatus is EngineStatus.Ready) {
                    cloudEngine
                } else {
                    val localStatus = localEngine?.isAvailable()
                    if (localStatus is EngineStatus.Ready) {
                        localEngine
                    } else {
                        // Default to cloud engine to surface clear error diagnostics
                        cloudEngine
                    }
                }
            }
        }
    }
}
