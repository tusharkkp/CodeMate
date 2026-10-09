package com.mit.tushar_kaldate.core.ui

import android.content.Context
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin

/**
 * Utility for rendering Markdown formatting, tables, and fenced code blocks
 * inside TextView widgets across the application.
 */
class MarkdownRenderer private constructor(private val markwon: Markwon) {

    fun render(textView: TextView, markdown: String) {
        markwon.setMarkdown(textView, markdown)
    }

    companion object {
        @Volatile
        private var instance: MarkdownRenderer? = null

        fun get(context: Context): MarkdownRenderer {
            return instance ?: synchronized(this) {
                instance ?: MarkdownRenderer(
                    Markwon.builder(context.applicationContext)
                        .usePlugin(TablePlugin.create(context.applicationContext))
                        .build()
                ).also { instance = it }
            }
        }
    }
}
