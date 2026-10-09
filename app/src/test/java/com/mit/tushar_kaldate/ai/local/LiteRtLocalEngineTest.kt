package com.mit.tushar_kaldate.ai.local

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import com.mit.tushar_kaldate.ai.engine.EngineStatus
import com.mit.tushar_kaldate.ai.engine.LlmChunk
import com.mit.tushar_kaldate.ai.engine.LlmRequest
import com.mit.tushar_kaldate.data.model.EngineType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LiteRtLocalEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var modelManager: ModelManager
    private lateinit var activityManager: ActivityManager
    private lateinit var engine: LiteRtLocalEngine
    private lateinit var modelsDir: File

    @Before
    fun setUp() {
        modelsDir = tempFolder.newFolder("models")
        context = mockk(relaxed = true)
        activityManager = mockk(relaxed = true)

        val memoryInfo = ActivityManager.MemoryInfo().apply {
            totalMem = 8L * 1024 * 1024 * 1024
            availMem = 4L * 1024 * 1024 * 1024
        }
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.getMemoryInfo(any()) } answers {
            val outInfo = firstArg<ActivityManager.MemoryInfo>()
            outInfo.totalMem = memoryInfo.totalMem
            outInfo.availMem = memoryInfo.availMem
        }
        every { context.getExternalFilesDir("models") } returns modelsDir
        every { context.filesDir } returns tempFolder.root

        modelManager = ModelManager(context)
        engine = LiteRtLocalEngine(context, modelManager)
    }

    @Test
    fun `engine type is LOCAL`() {
        assertEquals(EngineType.LOCAL, engine.type)
    }

    @Test
    fun `isAvailable returns NotConfigured when no models installed`() = runBlocking {
        val status = engine.isAvailable()
        assertTrue(status is EngineStatus.NotConfigured)
    }

    @Test
    fun `isAvailable returns Ready when model is present`() = runBlocking {
        val modelFile = File(modelsDir, ModelCatalog.STANDARD_MODEL.filename)
        modelFile.writeText("model weights")

        val status = engine.isAvailable()
        assertTrue(status is EngineStatus.Ready)
    }

    @Test
    fun `generate emits Failure when model missing`() = runBlocking {
        val chunks = engine.generate(LlmRequest(prompt = "Hello")).toList()
        assertTrue(chunks.first() is LlmChunk.Failure)
    }

    @Test
    fun `generate streams tokens and completes with Done when model present`() = runBlocking {
        val modelFile = File(modelsDir, ModelCatalog.STANDARD_MODEL.filename)
        modelFile.writeText("model weights")

        val chunks = engine.generate(LlmRequest(prompt = "Explain binary search")).toList()
        val deltas = chunks.filterIsInstance<LlmChunk.Delta>()
        val done = chunks.filterIsInstance<LlmChunk.Done>()

        assertTrue("Expected streamed deltas", deltas.isNotEmpty())
        assertTrue("Expected Done chunk", done.isNotEmpty())
        assertTrue(engine.isLoaded())
    }

    @Test
    fun `generate can be cancelled early without failure`() = runBlocking {
        val modelFile = File(modelsDir, ModelCatalog.STANDARD_MODEL.filename)
        modelFile.writeText("model weights")

        // Take only the first 3 tokens
        val chunks = engine.generate(LlmRequest(prompt = "dijkstra")).take(3).toList()
        assertEquals(3, chunks.size)
        assertTrue(chunks.all { it is LlmChunk.Delta })
    }

    @Test
    fun `onTrimMemory unloads model session`() = runBlocking {
        val modelFile = File(modelsDir, ModelCatalog.STANDARD_MODEL.filename)
        modelFile.writeText("model weights")

        engine.generate(LlmRequest(prompt = "Hello")).toList()
        assertTrue(engine.isLoaded())

        engine.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        assertFalse(engine.isLoaded())
        assertNull(engine.getLoadedModel())
    }
}
