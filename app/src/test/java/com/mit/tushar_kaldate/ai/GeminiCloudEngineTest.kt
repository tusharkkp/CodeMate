package com.mit.tushar_kaldate.ai

import app.cash.turbine.test
import com.mit.tushar_kaldate.ai.cloud.GeminiCloudEngine
import com.mit.tushar_kaldate.ai.engine.EngineStatus
import com.mit.tushar_kaldate.ai.engine.LlmChunk
import com.mit.tushar_kaldate.ai.engine.LlmError
import com.mit.tushar_kaldate.ai.engine.LlmRequest
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class GeminiCloudEngineTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `isAvailable returns Ready when API key is present`() = runTest {
        val engine = GeminiCloudEngine(
            apiKeyProvider = { "valid-api-key" },
            client = okHttpClient,
            baseUrl = mockWebServer.url("/v1beta").toString().removeSuffix("/")
        )

        val status = engine.isAvailable()
        assertTrue(status is EngineStatus.Ready)
    }

    @Test
    fun `isAvailable returns NotConfigured when API key is blank`() = runTest {
        val engine = GeminiCloudEngine(
            apiKeyProvider = { "" },
            client = okHttpClient,
            baseUrl = mockWebServer.url("/v1beta").toString().removeSuffix("/")
        )

        val status = engine.isAvailable()
        assertTrue(status is EngineStatus.NotConfigured)
    }

    @Test
    fun `generate streams SSE deltas and completes with usage`() = runTest {
        val sseBody = """
            data: {"candidates":[{"content":{"parts":[{"text":"Hello"}]}}]}
            
            data: {"candidates":[{"content":{"parts":[{"text":" world!"}]}}],"usageMetadata":{"promptTokenCount":5,"candidatesTokenCount":7,"totalTokenCount":12}}
            
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody)
        )

        val engine = GeminiCloudEngine(
            apiKeyProvider = { "test-api-key" },
            client = okHttpClient,
            baseUrl = mockWebServer.url("/v1beta").toString().removeSuffix("/")
        )

        engine.generate(LlmRequest(prompt = "Say hello")).test {
            val chunk1 = awaitItem() as LlmChunk.Delta
            assertEquals("Hello", chunk1.text)

            val chunk2 = awaitItem() as LlmChunk.Delta
            assertEquals(" world!", chunk2.text)

            val done = awaitItem() as LlmChunk.Done
            assertEquals(12, done.usage?.totalTokens)
            awaitComplete()
        }

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path?.contains("/v1beta/models/gemini-flash-latest:streamGenerateContent") == true)
        assertTrue(recordedRequest.path?.contains("alt=sse") == true)
        assertTrue(recordedRequest.path?.contains("key=test-api-key") == true)
    }

    @Test
    fun `generate emits BadKey on 401 Unauthorized`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"code":401,"message":"API key not valid"}}""")
        )

        val engine = GeminiCloudEngine(
            apiKeyProvider = { "invalid-key" },
            client = okHttpClient,
            baseUrl = mockWebServer.url("/v1beta").toString().removeSuffix("/")
        )

        engine.generate(LlmRequest(prompt = "Test")).test {
            val failure = awaitItem() as LlmChunk.Failure
            assertTrue(failure.error is LlmError.BadKey)
            awaitComplete()
        }
    }

    @Test
    fun `generate emits RateLimited on 429 Too Many Requests`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"error":{"code":429,"message":"Resource exhausted"}}""")
        )

        val engine = GeminiCloudEngine(
            apiKeyProvider = { "rate-limited-key" },
            client = okHttpClient,
            baseUrl = mockWebServer.url("/v1beta").toString().removeSuffix("/")
        )

        engine.generate(LlmRequest(prompt = "Test")).test {
            val failure = awaitItem() as LlmChunk.Failure
            assertTrue(failure.error is LlmError.RateLimited)
            awaitComplete()
        }
    }

    @Test
    fun `generate emits Server on 503 Service Unavailable`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("Service Unavailable")
        )

        val engine = GeminiCloudEngine(
            apiKeyProvider = { "valid-key" },
            client = okHttpClient,
            baseUrl = mockWebServer.url("/v1beta").toString().removeSuffix("/")
        )

        engine.generate(LlmRequest(prompt = "Test")).test {
            val failure = awaitItem() as LlmChunk.Failure
            assertTrue(failure.error is LlmError.Server)
            assertEquals(503, (failure.error as LlmError.Server).code)
            awaitComplete()
        }
    }

    @Test
    fun `generate emits SafetyBlocked when candidate finishReason is SAFETY`() = runTest {
        val sseBody = """
            data: {"candidates":[{"content":{"parts":[{"text":"I cannot..."}]},"finishReason":"SAFETY"}]}
            
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseBody)
        )

        val engine = GeminiCloudEngine(
            apiKeyProvider = { "valid-key" },
            client = okHttpClient,
            baseUrl = mockWebServer.url("/v1beta").toString().removeSuffix("/")
        )

        engine.generate(LlmRequest(prompt = "Unsafe request")).test {
            val delta = awaitItem() as LlmChunk.Delta
            assertEquals("I cannot...", delta.text)

            val failure = awaitItem() as LlmChunk.Failure
            assertTrue(failure.error is LlmError.SafetyBlocked)
            awaitComplete()
        }
    }
}
