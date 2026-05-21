package io.github.manhvu1212.tallyo.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.manhvu1212.tallyo.data.AiStatsService
import io.github.manhvu1212.tallyo.data.PreferencesManager
import io.github.manhvu1212.tallyo.data.SessionRepository
import io.github.manhvu1212.tallyo.domain.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed interface AiUiState {
    object Idle : AiUiState
    object Loading : AiUiState
    data class Success(val content: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

class StatsViewModel(
    repository: SessionRepository,
    private val preferencesManager: PreferencesManager,
    private val aiStatsService: AiStatsService,
    sessionId: String,
) : ViewModel() {

    val session: StateFlow<Session?> = repository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val apiKey: StateFlow<String?> = preferencesManager.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _aiUiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiUiState: StateFlow<AiUiState> = _aiUiState.asStateFlow()

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            preferencesManager.saveGeminiApiKey(key)
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            preferencesManager.clearGeminiApiKey()
            _aiUiState.value = AiUiState.Idle
        }
    }

    fun generateAiInsights(queryType: String, language: String = "vi") {
        val currentSession = session.value ?: return
        val currentApiKey = apiKey.value
        if (currentApiKey.isNullOrBlank()) {
            val errorMsg = if (language.lowercase().startsWith("vi")) {
                "API Key chưa được cấu hình"
            } else {
                "API Key has not been configured"
            }
            _aiUiState.value = AiUiState.Error(errorMsg)
            return
        }

        // Randomize the tone/queryType for summary query to give a random style each time
        val targetQueryType = if (queryType == "summary") {
            listOf("summary", "tactics", "roast", "poet", "commentator", "philosopher").random()
        } else {
            queryType
        }

        viewModelScope.launch {
            _aiUiState.value = AiUiState.Loading
            try {
                var accumulated = ""
                aiStatsService.generateInsights(
                    apiKey = currentApiKey,
                    session = currentSession,
                    queryType = targetQueryType,
                    language = language
                ).collect { chunk ->
                    if (chunk.isNotEmpty()) {
                        accumulated += chunk
                        _aiUiState.value = AiUiState.Success(accumulated)
                    }
                }
            } catch (e: Exception) {
                val rawMsg = e.localizedMessage ?: e.message ?: "Lỗi không xác định khi kết nối với AI"
                _aiUiState.value = AiUiState.Error(cleanErrorMessage(rawMsg, language))
            }
        }
    }

    private fun cleanErrorMessage(rawError: String, language: String): String {
        // Try to extract the "message" field if the error is a JSON string using robust regex
        val jsonMessageRegex = "\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()
        val matchResult = jsonMessageRegex.find(rawError)
        var message = if (matchResult != null) {
            matchResult.groupValues[1]
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
        } else {
            rawError
        }

        // Clean common exception prefixes
        if (message.contains("ApiException:")) {
            message = message.substringAfter("ApiException:").trim()
        }
        if (message.contains("Exception:")) {
            message = message.substringAfter("Exception:").trim()
        }

        val msgLower = message.lowercase()
        val isVi = language.lowercase().startsWith("vi")

        // Map to user-friendly messages
        return when {
            msgLower.contains("api key not valid") || msgLower.contains("api_key_invalid") || msgLower.contains("invalid api key") || msgLower.contains("key not found") -> {
                if (isVi) "Mã API Key không hợp lệ. Vui lòng kiểm tra lại cấu hình."
                else "Invalid API Key. Please check your configuration."
            }
            msgLower.contains("quota exceeded") || msgLower.contains("exhausted") || msgLower.contains("429") || msgLower.contains("too many requests") -> {
                if (isVi) "Đã vượt quá giới hạn yêu cầu miễn phí (Rate Limit). Vui lòng thử lại sau vài giây."
                else "Free request quota exceeded. Please wait a few seconds and try again."
            }
            msgLower.contains("unknownhostexception") || msgLower.contains("unable to resolve host") || msgLower.contains("connectexception") || msgLower.contains("timed out") || msgLower.contains("timeout") -> {
                if (isVi) "Không thể kết nối với máy chủ AI. Vui lòng kiểm tra kết nối mạng."
                else "Cannot connect to AI server. Please check your internet connection."
            }
            msgLower.contains("blocked") || msgLower.contains("stopped") || msgLower.contains("safety") -> {
                if (isVi) "Yêu cầu bị từ chối hoặc dừng lại vì lý do an toàn của hệ thống AI."
                else "Request blocked or stopped due to AI safety policies."
            }
            msgLower.contains("model") && (msgLower.contains("not found") || msgLower.contains("not_found") || msgLower.contains("404")) -> {
                if (isVi) "Mẫu AI (Model) không tồn tại hoặc đã bị gỡ bỏ."
                else "The requested AI model was not found."
            }
            else -> {
                message.trim()
            }
        }
    }

    companion object {
        fun factory(
            repository: SessionRepository,
            preferencesManager: PreferencesManager,
            aiStatsService: AiStatsService,
            sessionId: String
        ) = viewModelFactory {
            initializer {
                StatsViewModel(
                    repository = repository,
                    preferencesManager = preferencesManager,
                    aiStatsService = aiStatsService,
                    sessionId = sessionId
                )
            }
        }
    }
}
