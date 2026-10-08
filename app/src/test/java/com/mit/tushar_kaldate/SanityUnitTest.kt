package com.mit.tushar_kaldate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SanityUnitTest {

    @Test
    fun applicationPackageMatchesSpec() {
        assertEquals("com.mit.tushar_kaldate", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun buildConfigFieldsAreDefined() {
        assertNotNull(BuildConfig.GEMINI_API_KEY)
    }

    @Test
    fun simpleMathSanityCheck() {
        assertEquals(4, 2 + 2)
    }
}
