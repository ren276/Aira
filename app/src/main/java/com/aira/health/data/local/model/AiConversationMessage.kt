package com.aira.health.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_conversation_messages")
data class AiConversationMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val role: String, // "user"|"assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val contextSnapshotJson: String? = null
)
