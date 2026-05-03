package com.aira.health.presentation.assistant

import java.util.UUID

data class AssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}
