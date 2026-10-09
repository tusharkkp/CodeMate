package com.mit.tushar_kaldate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mit.tushar_kaldate.data.local.dao.ConversationDao
import com.mit.tushar_kaldate.data.local.dao.DailyActivityDao
import com.mit.tushar_kaldate.data.local.dao.MessageDao
import com.mit.tushar_kaldate.data.local.dao.QuizDao
import com.mit.tushar_kaldate.data.local.dao.SavedSnippetDao
import com.mit.tushar_kaldate.data.local.dao.TopicProgressDao
import com.mit.tushar_kaldate.data.local.dao.UserDao
import com.mit.tushar_kaldate.data.local.entity.ConversationEntity
import com.mit.tushar_kaldate.data.local.entity.DailyActivityEntity
import com.mit.tushar_kaldate.data.local.entity.MessageEntity
import com.mit.tushar_kaldate.data.local.entity.MessageFtsEntity
import com.mit.tushar_kaldate.data.local.entity.QuizAnswerEntity
import com.mit.tushar_kaldate.data.local.entity.QuizAttemptEntity
import com.mit.tushar_kaldate.data.local.entity.SavedSnippetEntity
import com.mit.tushar_kaldate.data.local.entity.TopicProgressEntity
import com.mit.tushar_kaldate.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        MessageFtsEntity::class,
        QuizAttemptEntity::class,
        QuizAnswerEntity::class,
        TopicProgressEntity::class,
        DailyActivityEntity::class,
        SavedSnippetEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CodeMateDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun quizDao(): QuizDao
    abstract fun topicProgressDao(): TopicProgressDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun savedSnippetDao(): SavedSnippetDao

    companion object {
        const val DATABASE_NAME = "codemate.db"

        fun buildDatabase(context: Context): CodeMateDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CodeMateDatabase::class.java,
                DATABASE_NAME
            ).fallbackToDestructiveMigration(false)
                .build()
        }

        fun buildInMemory(context: Context): CodeMateDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                CodeMateDatabase::class.java
            ).allowMainThreadQueries()
                .build()
        }
    }
}
