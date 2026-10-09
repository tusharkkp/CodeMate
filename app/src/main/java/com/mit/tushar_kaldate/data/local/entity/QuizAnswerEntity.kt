package com.mit.tushar_kaldate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_answers",
    foreignKeys = [
        ForeignKey(
            entity = QuizAttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["attemptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["attemptId"])]
)
data class QuizAnswerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val attemptId: Long,
    val question: String,
    val optionsJson: String,
    val correctIndex: Int,
    val selectedIndex: Int,
    val explanation: String,
    val concept: String
)
