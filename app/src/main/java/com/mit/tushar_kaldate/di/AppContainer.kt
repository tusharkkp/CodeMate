package com.mit.tushar_kaldate.di

import android.content.Context
import com.mit.tushar_kaldate.data.curriculum.CurriculumLoader
import com.mit.tushar_kaldate.data.local.CodeMateDatabase
import com.mit.tushar_kaldate.data.prefs.SessionStore
import com.mit.tushar_kaldate.data.prefs.SettingsStore
import com.mit.tushar_kaldate.data.repository.AuthRepository
import com.mit.tushar_kaldate.data.repository.ChatRepository
import com.mit.tushar_kaldate.data.repository.ProgressRepository
import com.mit.tushar_kaldate.data.repository.QuizRepository
import com.mit.tushar_kaldate.data.repository.SearchRepository

/**
 * Manual Dependency Injection container for CodeMate.
 * Holds application-scoped dependencies without requiring Hilt.
 */
class AppContainer(private val applicationContext: Context) {
    val context: Context
        get() = applicationContext

    val database: CodeMateDatabase by lazy {
        CodeMateDatabase.buildDatabase(applicationContext)
    }

    val sessionStore: SessionStore by lazy {
        SessionStore(applicationContext)
    }

    val settingsStore: SettingsStore by lazy {
        SettingsStore(applicationContext)
    }

    val curriculumLoader: CurriculumLoader by lazy {
        CurriculumLoader(applicationContext)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(database.userDao(), sessionStore)
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(database.conversationDao(), database.messageDao())
    }

    val quizRepository: QuizRepository by lazy {
        QuizRepository(database.quizDao())
    }

    val progressRepository: ProgressRepository by lazy {
        ProgressRepository(database.topicProgressDao(), database.dailyActivityDao())
    }

    val searchRepository: SearchRepository by lazy {
        SearchRepository(database.messageDao(), database.conversationDao(), curriculumLoader)
    }

    val geminiCloudEngine: com.mit.tushar_kaldate.ai.cloud.GeminiCloudEngine by lazy {
        com.mit.tushar_kaldate.ai.cloud.GeminiCloudEngine()
    }

    val fakeLlmEngine: com.mit.tushar_kaldate.ai.engine.FakeLlmEngine by lazy {
        com.mit.tushar_kaldate.ai.engine.FakeLlmEngine()
    }

    val modelManager: com.mit.tushar_kaldate.ai.local.ModelManager by lazy {
        com.mit.tushar_kaldate.ai.local.ModelManager(applicationContext)
    }

    val liteRtLocalEngine: com.mit.tushar_kaldate.ai.local.LiteRtLocalEngine by lazy {
        com.mit.tushar_kaldate.ai.local.LiteRtLocalEngine(
            context = applicationContext,
            modelManager = modelManager
        )
    }

    val engineRouter: com.mit.tushar_kaldate.ai.engine.EngineRouter by lazy {
        com.mit.tushar_kaldate.ai.engine.EngineRouter(
            cloudEngine = geminiCloudEngine,
            localEngine = liteRtLocalEngine
        )
    }
}
