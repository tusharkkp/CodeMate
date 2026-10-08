package com.mit.tushar_kaldate.di

import android.content.Context

/**
 * Manual Dependency Injection container for CodeMate.
 * Holds application-scoped dependencies without requiring Hilt.
 */
class AppContainer(private val applicationContext: Context) {
    val context: Context
        get() = applicationContext

    // Repositories and engines will be lazily initialized here in upcoming phases
}
