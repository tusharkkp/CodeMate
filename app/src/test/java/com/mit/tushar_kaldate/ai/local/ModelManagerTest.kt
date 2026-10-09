package com.mit.tushar_kaldate.ai.local

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class ModelManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var modelsDir: File
    private lateinit var modelManager: ModelManager

    @Before
    fun setUp() {
        modelsDir = tempFolder.newFolder("models")
        context = mockk(relaxed = true)
        every { context.getExternalFilesDir("models") } returns modelsDir
        every { context.filesDir } returns tempFolder.root
        modelManager = ModelManager(context)
    }

    @Test
    fun `getModelsDirectory returns existing directory`() {
        val dir = modelManager.getModelsDirectory()
        assertTrue(dir.exists())
        assertEquals(modelsDir.absolutePath, dir.absolutePath)
    }

    @Test
    fun `isModelInstalled returns false when file absent or empty`() {
        val model = ModelCatalog.STANDARD_MODEL
        assertFalse(modelManager.isModelInstalled(model))

        val emptyFile = File(modelsDir, model.filename)
        emptyFile.createNewFile()
        assertFalse(modelManager.isModelInstalled(model))
    }

    @Test
    fun `isModelInstalled returns true when file exists with content`() {
        val model = ModelCatalog.STANDARD_MODEL
        val file = File(modelsDir, model.filename)
        file.writeText("sample model bytes")

        assertTrue(modelManager.isModelInstalled(model))
        assertTrue(modelManager.isModelInstalled(model.id))
    }

    @Test
    fun `deleteModel removes file from directory`() {
        val model = ModelCatalog.LITE_MODEL
        val file = File(modelsDir, model.filename)
        file.writeText("test lite model")

        assertTrue(modelManager.isModelInstalled(model))
        val deleted = modelManager.deleteModel(model)

        assertTrue(deleted)
        assertFalse(file.exists())
        assertFalse(modelManager.isModelInstalled(model))
    }

    @Test
    fun `getActiveModelFile returns highest installed tier`() {
        assertNull(modelManager.getActiveModelFile())

        // Install Lite tier
        val liteFile = File(modelsDir, ModelCatalog.LITE_MODEL.filename)
        liteFile.writeText("lite weights")
        assertEquals(liteFile.absolutePath, modelManager.getActiveModelFile()?.absolutePath)

        // Install Quality tier (should now be preferred over Lite)
        val qualityFile = File(modelsDir, ModelCatalog.QUALITY_MODEL.filename)
        qualityFile.writeText("quality weights")
        assertEquals(qualityFile.absolutePath, modelManager.getActiveModelFile()?.absolutePath)
    }

    @Test
    fun `verifyChecksum returns true for valid SHA256`() {
        val testFile = File(modelsDir, "test.bin")
        val content = "codemate model checksum test"
        testFile.writeText(content)

        val digest = MessageDigest.getInstance("SHA-256")
        val expectedHash = digest.digest(content.toByteArray()).joinToString("") { "%02x".format(it) }

        assertTrue(modelManager.verifyChecksum(testFile, expectedHash))
        assertFalse(modelManager.verifyChecksum(testFile, "invalidhash12345"))
    }
}
