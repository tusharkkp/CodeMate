package com.mit.tushar_kaldate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mit.tushar_kaldate.data.local.entity.ConversationEntity
import com.mit.tushar_kaldate.data.model.TutorMode
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity): Int

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun observeConversationById(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY pinned DESC, updatedAt DESC")
    fun observeConversationsByUser(userId: Long): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE userId = :userId AND pinned = 1 ORDER BY updatedAt DESC")
    fun observePinnedConversations(userId: Long): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE userId = :userId AND mode = :mode ORDER BY updatedAt DESC")
    fun observeConversationsByMode(userId: Long, mode: TutorMode): Flow<List<ConversationEntity>>

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long): Int

    @Query("DELETE FROM conversations WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long): Int

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("UPDATE conversations SET pinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Boolean): Int

    @Query("SELECT * FROM conversations WHERE userId = :userId AND title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchTitles(userId: Long, query: String): List<ConversationEntity>
}
