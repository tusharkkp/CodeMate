package com.mit.tushar_kaldate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mit.tushar_kaldate.data.model.Difficulty

@Entity(
    tableName = "quiz_attempts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val language: String,
    val topic: String,
    val difficulty: Difficulty = Difficulty.EASY,
    val total: Int,
    val correct: Int,
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
