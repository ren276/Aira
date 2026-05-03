package com.aira.health.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aira.health.ai.runtime.AiRuntimeGateway
import com.aira.health.ai.runtime.AiRuntimeRequest
import com.aira.health.data.local.dao.DailyMetricsDao
import com.aira.health.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class AssistantUiState(
    val messages: List<AssistantMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isStreaming: Boolean = false
)

@HiltViewModel
class AiraAssistantViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val aiRuntimeGateway: AiRuntimeGateway,
    private val dailyMetricsDao: DailyMetricsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        loadContinuityContext()
    }

    private fun loadContinuityContext() {
        viewModelScope.launch {
            try {
                // 3-second timeout prevents hanging on slow Firebase reads
                val summary = withTimeout(3_000L) { userRepository.getLatestLogoutSummary() }
                if (!summary.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            messages = listOf(
                                AssistantMessage(
                                    text = "Welcome back! Here's a quick recap from where we left off: $summary",
                                    role = MessageRole.ASSISTANT
                                )
                            )
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            messages = listOf(
                                AssistantMessage(
                                    text = "Hello! I'm Aira, your health intelligence assistant. How can I help you today?",
                                    role = MessageRole.ASSISTANT
                                )
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Timeout or DB error — show welcome anyway
                _uiState.update {
                    it.copy(
                        messages = listOf(
                            AssistantMessage(
                                text = "Hello! I'm Aira, your health intelligence assistant. How can I help you today?",
                                role = MessageRole.ASSISTANT
                            )
                        )
                    )
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isStreaming) return

        val userMessage = AssistantMessage(text = text, role = MessageRole.USER)
        _uiState.update { it.copy(messages = it.messages + userMessage) }

        viewModelScope.launch {
            _uiState.update { it.copy(isStreaming = true) }

            try {
                // Get last 14 days metrics for context
                val metrics = dailyMetricsDao.getLast14Days()
                val contextData = metrics.joinToString("\n") {
                    "Date: ${it.date}, Sleep: ${it.sleepScore}, Stress: ${it.stressScore}, Recovery: ${it.recoveryScore}"
                }

                val conversationHistory = _uiState.value.messages.takeLast(10).joinToString("\n") {
                    "${it.role}: ${it.text}"
                }

                val request = AiRuntimeRequest(
                    promptChunks = listOf(
                        "You are Aira, a privacy-first health intelligence OS assistant.",
                        "Provide explainable, accurate health advice based on the user's data.",
                        "Do not share raw biometric data if it were provided. Use scores (0-100).",
                        "User Context Data:\n$contextData",
                        "Conversation History:\n$conversationHistory",
                        "NEW USER MESSAGE: $text"
                    )
                )

                val assistantMessageId = AssistantMessage(text = "", role = MessageRole.ASSISTANT).id
                _uiState.update {
                    it.copy(messages = it.messages + AssistantMessage(id = assistantMessageId, text = "", role = MessageRole.ASSISTANT))
                }

                var fullResponse = ""
                aiRuntimeGateway.generate(request)
                    .catch { e ->
                        _uiState.update { it.copy(error = "Model error: ${e.message}") }
                    }
                    .collect { response ->
                        if (response.text.isNotEmpty()) {
                            fullResponse += response.text
                            updateLastAssistantMessage(assistantMessageId, fullResponse)
                        }
                    }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                // Guaranteed cleanup — stops the loading animation in ALL cases:
                // normal stream end, timeout, cancellation, or unhandled exception.
                _uiState.update { it.copy(isStreaming = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun updateLastAssistantMessage(id: String, newText: String) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { 
                if (it.id == id) it.copy(text = newText) else it
            }
            state.copy(messages = updatedMessages)
        }
    }
}
