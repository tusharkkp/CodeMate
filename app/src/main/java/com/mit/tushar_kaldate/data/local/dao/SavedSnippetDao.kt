package com.mit.tushar_kaldate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mit.tushar_kaldate.data.local.entity.SavedSnippetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSnippetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SavedSnippetEntity): Long

    @Update
    suspend fun updateSnippet(snippet: SavedSnippetEntity): Int

    @Query("SELECT * FROM saved_snippets WHERE id = :id LIMIT 1")
    suspend fun getSnippetById(id: Long): SavedSnippetEntity?

    @Query("SELECT * FROM saved_snippets WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeSnippetsForUser(userId: Long): Flow<List<SavedSnippetEntity>>

    @Query("SELECT * FROM saved_snippets WHERE userId = :userId AND language = :language ORDER BY createdAt DESC")
    fun observeSnippetsByLanguage(userId: Long, language: String): Flow<List<SavedSnippetEntity>>

    @Query("DELETE FROM saved_snippets WHERE id = :id")
    suspend fun deleteSnippetById(id: Long): Int

    @Query("DELETE FROM saved_snippets WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long): Int
}
