package com.mit.tushar_kaldate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mit.tushar_kaldate.data.local.entity.TopicProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: TopicProgressEntity)

    @Update
    suspend fun updateProgress(progress: TopicProgressEntity): Int

    @Query("SELECT * FROM topic_progress WHERE userId = :userId AND language = :language AND topic = :topic LIMIT 1")
    suspend fun getProgress(userId: Long, language: String, topic: String): TopicProgressEntity?

    @Query("SELECT * FROM topic_progress WHERE userId = :userId AND language = :language AND topic = :topic LIMIT 1")
    fun observeProgress(userId: Long, language: String, topic: String): Flow<TopicProgressEntity?>

    @Query("SELECT * FROM topic_progress WHERE userId = :userId ORDER BY lastStudiedAt DESC")
    fun observeAllForUser(userId: Long): Flow<List<TopicProgressEntity>>

    @Query("SELECT * FROM topic_progress WHERE userId = :userId AND language = :language ORDER BY mastery ASC")
    fun observeByLanguage(userId: Long, language: String): Flow<List<TopicProgressEntity>>

    @Query("SELECT AVG(mastery) FROM topic_progress WHERE userId = :userId AND language = :language")
    suspend fun getAverageMastery(userId: Long, language: String): Double?

    @Query("DELETE FROM topic_progress WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long): Int
}
