package com.mit.tushar_kaldate.ai.cloud

import com.mit.tushar_kaldate.BuildConfig
import com.mit.tushar_kaldate.ai.config.AiConfig
import com.mit.tushar_kaldate.ai.engine.EngineStatus
import com.mit.tushar_kaldate.ai.engine.LlmChunk
import com.mit.tushar_kaldate.ai.engine.LlmEngine
import com.mit.tushar_kaldate.ai.engine.LlmError
import com.mit.tushar_kaldate.ai.engine.LlmRequest
import com.mit.tushar_kaldate.ai.engine.TokenUsage
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.MessageRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Google Gemini Cloud Engine communicating via REST Server-Sent Events (SSE).
 *
 * Implements low-latency streaming token responses, typed error mapping,
 * safety filter inspection, and coroutine-linked network cancellation.
 */
class GeminiCloudEngine(
    private val apiKeyProvider: () -> String? = { BuildConfig.GEMINI_API_KEY },
    private val client: OkHttpClient = defaultOkHttpClient(),
    private val baseUrl: String = AiConfig.GEMINI_BASE_URL,
    private val model: String = AiConfig.DEFAULT_CLOUD_MODEL,
    private val json: Json = defaultJson()
) : LlmEngine {

    override val type: EngineType = EngineType.CLOUD

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(AiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        fun defaultJson(): Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
            coerceInputValues = true
        }
    }

    override suspend fun isAvailable(): EngineStatus {
        val key = apiKeyProvider()
        return if (key.isNullOrBlank()) {
            EngineStatus.NotConfigured("Gemini API key is not configured in local.properties")
        } else {
            EngineStatus.Ready
        }
    }

    override fun generate(request: LlmRequest): Flow<LlmChunk> = channelFlow {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            send(LlmChunk.Failure(LlmError.BadKey("Gemini API key is missing or blank")))
            return@channelFlow
        }

        // Build contents from history + current prompt
        val contents = mutableListOf<GeminiContent>()
        for (turn in request.history) {
            val role = when (turn.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "model"
                MessageRole.SYSTEM -> null
            }
            if (role != null) {
                contents.add(
                    GeminiContent(
                        role = role,
                        parts = listOf(GeminiPart(text = turn.content))
                    )
                )
            }
        }
        contents.add(
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = request.prompt))
            )
        )

        val systemInstruction = request.systemInstruction?.let { instruction ->
            GeminiContent(
                parts = listOf(GeminiPart(text = instruction))
            )
        }

        val generationConfig = GeminiGenerationConfig(
            temperature = request.temperature,
            maxOutputTokens = request.maxTokens
        )

        val geminiPayload = GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = generationConfig
        )

        val requestBodyJson = json.encodeToString(geminiPayload)
        val endpointUrl = "$baseUrl/models/$model:streamGenerateContent?alt=sse&key=$apiKey"

        val httpRequest = Request.Builder()
            .url(endpointUrl)
            .post(requestBodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val call = client.newCall(httpRequest)

        // Ensure cancelling the coroutine cancels the HTTP socket immediately (<1s)
        invokeOnClose {
            call.cancel()
        }

        try {
            withContext(Dispatchers.IO) {
                val response = call.execute()
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        val errorBody = resp.body?.string().orEmpty()
                        val error = mapHttpError(resp.code, errorBody)
                        send(LlmChunk.Failure(error))
                        return@withContext
                    }

                    val source = resp.body?.source()
                    if (source == null) {
                        send(LlmChunk.Failure(LlmError.Server(resp.code, "Empty response body")))
                        return@withContext
                    }

                    var tokenUsage: TokenUsage? = null

                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue

                        val jsonPayload = line.removePrefix("data:").trim()
                        if (jsonPayload.isEmpty()) continue

                        val streamResponse = try {
                            json.decodeFromString<GeminiStreamResponse>(jsonPayload)
                        } catch (e: Exception) {
                            null
                        }

                        if (streamResponse == null) continue

                        if (streamResponse.error != null) {
                            val code = streamResponse.error.code ?: 500
                            val msg = streamResponse.error.message ?: "Unknown API error"
                            send(LlmChunk.Failure(LlmError.Server(code, msg)))
                            return@withContext
                        }

                        if (streamResponse.promptFeedback?.blockReason != null) {
                            send(
                                LlmChunk.Failure(
                                    LlmError.SafetyBlocked(streamResponse.promptFeedback.blockReason)
                                )
                            )
                            return@withContext
                        }

                        streamResponse.candidates?.forEach { candidate ->
                            candidate.content?.parts?.forEach { part ->
                                if (part.text.isNotEmpty()) {
                                    send(LlmChunk.Delta(part.text))
                                }
                            }
                            if (candidate.finishReason == "SAFETY") {
                                send(LlmChunk.Failure(LlmError.SafetyBlocked("SAFETY")))
                                return@withContext
                            }
                        }

                        streamResponse.usageMetadata?.let { usage ->
                            tokenUsage = TokenUsage(
                                promptTokens = usage.promptTokenCount ?: 0,
                                completionTokens = usage.candidatesTokenCount ?: 0,
                                totalTokens = usage.totalTokenCount ?: 0
                            )
                        }
                    }

                    send(LlmChunk.Done(tokenUsage))
                }
            }
        } catch (e: CancellationException) {
            // Rethrow to allow clean coroutine cancellation
            throw e
        } catch (e: UnknownHostException) {
            send(LlmChunk.Failure(LlmError.NoNetwork(e.message ?: "Host unreachable")))
        } catch (e: ConnectException) {
            send(LlmChunk.Failure(LlmError.NoNetwork(e.message ?: "Connection failed")))
        } catch (e: SocketTimeoutException) {
            send(LlmChunk.Failure(LlmError.Timeout(e.message ?: "Socket timed out")))
        } catch (e: IOException) {
            if (!call.isCanceled()) {
                send(LlmChunk.Failure(LlmError.NoNetwork(e.message ?: "Network error")))
            }
        } catch (e: Exception) {
            if (!call.isCanceled()) {
                send(LlmChunk.Failure(LlmError.Unknown(e)))
            }
        }
    }

    private fun mapHttpError(code: Int, errorBody: String): LlmError {
        return when (code) {
            400 -> {
                if (errorBody.contains("API_KEY_INVALID", ignoreCase = true) ||
                    errorBody.contains("API key not valid", ignoreCase = true)
                ) {
                    LlmError.BadKey(errorBody)
                } else {
                    LlmError.Server(code, errorBody)
                }
            }
            401, 403 -> LlmError.BadKey("Unauthorized or invalid API key")
            429 -> LlmError.RateLimited("Rate limit exceeded")
            in 500..599 -> LlmError.Server(code, "Google Gemini service error: $code")
            else -> LlmError.Server(code, errorBody)
        }
    }
}
