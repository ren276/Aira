package com.aira.health.data.local.dao

import androidx.room.*
import com.aira.health.data.local.model.AiConversationMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface AiConversationDao {
    @Insert
    suspend fun insert(message: AiConversationMessage): Long

    @Query("SELECT * FROM ai_conversation_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeSession(sessionId: String): Flow<List<AiConversationMessage>>

    @Query("SELECT * FROM ai_conversation_messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 10")
    suspend fun getLastTenTurns(sessionId: String): List<AiConversationMessage>

    @Query("DELETE FROM ai_conversation_messages WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)
}
