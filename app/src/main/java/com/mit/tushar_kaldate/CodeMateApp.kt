package com.mit.tushar_kaldate

import android.app.Application
import com.mit.tushar_kaldate.di.AppContainer

class CodeMateApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    val container: AppContainer
        get() = appContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
