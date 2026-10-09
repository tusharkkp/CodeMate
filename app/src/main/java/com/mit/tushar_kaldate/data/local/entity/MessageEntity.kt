package com.mit.tushar_kaldate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.MessageRole

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val role: MessageRole,
    val content: String,
    val engineUsed: EngineType? = null,
    val tokensEstimate: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
