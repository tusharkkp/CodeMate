package com.mit.tushar_kaldate.ai.config

/**
 * Global AI engine configuration parameters, model identifiers,
 * timeouts, and token/history limits.
 */
object AiConfig {
    /** Recommended low-latency Google Gemini cloud model */
    const val DEFAULT_CLOUD_MODEL = "gemini-2.5-flash"

    /** Gemini v1beta REST base endpoint */
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

    /** HTTP timeouts */
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 15L

    /** Generation parameters */
    const val DEFAULT_TEMPERATURE = 0.7f
    const val DEFAULT_MAX_TOKENS = 2048

    /** Context window turn limits */
    const val CLOUD_HISTORY_TURNS_LIMIT = 10
    const val LOCAL_HISTORY_TURNS_LIMIT = 4
}
