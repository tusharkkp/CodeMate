package com.mit.tushar_kaldate

import android.app.Application
import com.mit.tushar_kaldate.di.AppContainer

class CodeMateApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
