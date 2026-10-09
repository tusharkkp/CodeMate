package com.mit.tushar_kaldate.ai.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun `ModelCatalog contains all 3 hardware tiers`() {
        assertEquals(3, ModelCatalog.allModels.size)

        val quality = ModelCatalog.QUALITY_MODEL
        assertEquals(ModelTier.QUALITY, quality.tier)
        assertTrue(quality.sizeBytes > 2_000_000_000L)
        assertTrue(quality.minRamBytes >= 6L * 1024 * 1024 * 1024)
        assertTrue(quality.isGated)

        val standard = ModelCatalog.STANDARD_MODEL
        assertEquals(ModelTier.STANDARD, standard.tier)
        assertFalse(standard.isGated)
        assertEquals("Apache-2.0", standard.license)

        val lite = ModelCatalog.LITE_MODEL
        assertEquals(ModelTier.LITE, lite.tier)
        assertFalse(lite.isGated)
        assertTrue(lite.sizeBytes < 1_000_000_000L)
    }

    @Test
    fun `getModelByTier returns correct model`() {
        assertEquals(ModelCatalog.QUALITY_MODEL, ModelCatalog.getModelByTier(ModelTier.QUALITY))
        assertEquals(ModelCatalog.STANDARD_MODEL, ModelCatalog.getModelByTier(ModelTier.STANDARD))
        assertEquals(ModelCatalog.LITE_MODEL, ModelCatalog.getModelByTier(ModelTier.LITE))
    }

    @Test
    fun `getModelById and getModelByFilename find models correctly`() {
        assertEquals(ModelCatalog.STANDARD_MODEL, ModelCatalog.getModelById("qwen2.5-1.5b-instruct"))
        assertNull(ModelCatalog.getModelById("non-existent"))

        assertEquals(
            ModelCatalog.STANDARD_MODEL,
            ModelCatalog.getModelByFilename("qwen2.5-1.5b-instruct.litertlm")
        )
        assertNull(ModelCatalog.getModelByFilename("unknown.litertlm"))
    }
}
