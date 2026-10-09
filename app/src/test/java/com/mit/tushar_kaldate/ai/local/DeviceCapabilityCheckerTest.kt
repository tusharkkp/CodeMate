package com.mit.tushar_kaldate.ai.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCapabilityCheckerTest {

    private val gb = 1024L * 1024 * 1024

    @Test
    fun `evaluate recommends QUALITY tier for 8 GB RAM and ample storage`() {
        val capability = DeviceCapabilityChecker.evaluate(
            totalRam = 8L * gb,
            availRam = 4L * gb,
            freeStorage = 20L * gb
        )

        assertEquals(ModelTier.QUALITY, capability.recommendedTier)
        assertTrue(capability.canRunOfflineLlm)
        assertNull(capability.warningMessage)
        assertEquals(8.0, capability.totalRamGb, 0.01)
        assertEquals(20.0, capability.freeStorageGb, 0.01)
    }

    @Test
    fun `evaluate recommends STANDARD tier for 4 GB RAM`() {
        val capability = DeviceCapabilityChecker.evaluate(
            totalRam = 4L * gb,
            availRam = 2L * gb,
            freeStorage = 10L * gb
        )

        assertEquals(ModelTier.STANDARD, capability.recommendedTier)
        assertTrue(capability.canRunOfflineLlm)
        assertNull(capability.warningMessage)
    }

    @Test
    fun `evaluate recommends LITE tier for 3 GB RAM`() {
        val capability = DeviceCapabilityChecker.evaluate(
            totalRam = 3L * gb,
            availRam = 1L * gb,
            freeStorage = 5L * gb
        )

        assertEquals(ModelTier.LITE, capability.recommendedTier)
        assertTrue(capability.canRunOfflineLlm)
        assertNull(capability.warningMessage)
    }

    @Test
    fun `evaluate flags warning and disables running for low RAM below 2_5 GB`() {
        val capability = DeviceCapabilityChecker.evaluate(
            totalRam = 2L * gb,
            availRam = 500L * 1024 * 1024,
            freeStorage = 5L * gb
        )

        assertFalse(capability.canRunOfflineLlm)
        assertNotNull(capability.warningMessage)
    }

    @Test
    fun `evaluate flags warning and disables running for low storage below 800 MB`() {
        val capability = DeviceCapabilityChecker.evaluate(
            totalRam = 6L * gb,
            availRam = 3L * gb,
            freeStorage = 500L * 1024 * 1024
        )

        assertFalse(capability.canRunOfflineLlm)
        assertNotNull(capability.warningMessage)
    }
}
