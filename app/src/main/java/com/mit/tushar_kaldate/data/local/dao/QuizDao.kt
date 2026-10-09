package com.mit.tushar_kaldate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mit.tushar_kaldate.data.local.entity.QuizAnswerEntity
import com.mit.tushar_kaldate.data.local.entity.QuizAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<QuizAnswerEntity>)

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAttemptsForUser(userId: Long): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE id = :attemptId LIMIT 1")
    suspend fun getAttemptById(attemptId: Long): QuizAttemptEntity?

    @Query("SELECT * FROM quiz_answers WHERE attemptId = :attemptId ORDER BY id ASC")
    suspend fun getAnswersForAttempt(attemptId: Long): List<QuizAnswerEntity>

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId AND language = :language AND topic = :topic ORDER BY createdAt DESC")
    suspend fun getAttemptsByTopic(userId: Long, language: String, topic: String): List<QuizAttemptEntity>

    @Query("DELETE FROM quiz_attempts WHERE id = :id")
    suspend fun deleteAttemptById(id: Long): Int

    @Query("DELETE FROM quiz_attempts WHERE userId = :userId")
    suspend fun deleteAllAttemptsForUser(userId: Long): Int
}
