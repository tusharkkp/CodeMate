package com.mit.tushar_kaldate.core

import android.content.Context
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class UiTextTest {

    private val context: Context = mockk()

    @Test
    fun dynamicStringReturnsDirectValue() {
        val uiText = UiText.DynamicString("Hello CodeMate")
        assertEquals("Hello CodeMate", uiText.asString(context))
    }

    @Test
    fun stringResourceCallsContextGetString() {
        every { context.getString(R.string.app_name) } returns "CodeMate"
        val uiText = UiText.StringResource(R.string.app_name)
        assertEquals("CodeMate", uiText.asString(context))
    }

    @Test
    fun stringResourceWithArgsCallsFormattedGetString() {
        every { context.getString(R.string.app_name, *anyVararg()) } returns "CodeMate 2.0"
        val uiText = UiText.StringResource(R.string.app_name, "2.0")
        assertEquals("CodeMate 2.0", uiText.asString(context))
    }

    @Test
    fun equalityWorksForIdenticalDynamicStrings() {
        val first = UiText.DynamicString("Sample")
        val second = UiText.DynamicString("Sample")
        assertEquals(first, second)
    }

    @Test
    fun equalityWorksForIdenticalStringResources() {
        val first = UiText.StringResource(R.string.app_name)
        val second = UiText.StringResource(R.string.app_name)
        assertEquals(first, second)
    }
}
