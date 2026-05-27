package io.github.manhvu1212.tallyo.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.manhvu1212.tallyo.data.AiStatsService
import io.github.manhvu1212.tallyo.data.AiStreamEvent
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
    data class Loading(val aiName: String? = null) : AiUiState
    data class Success(val content: String, val aiName: String? = null) : AiUiState
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

    val selectedAiProvider: StateFlow<String> = preferencesManager.selectedAiProvider
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "gemini")

    val allApiKeys: StateFlow<Map<String, String>> = preferencesManager.allApiKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _aiUiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiUiState: StateFlow<AiUiState> = _aiUiState.asStateFlow()

    fun saveApiKey(provider: String, key: String) {
        viewModelScope.launch {
            preferencesManager.saveApiKey(provider, key)
        }
    }

    fun clearApiKey(provider: String) {
        viewModelScope.launch {
            preferencesManager.clearApiKey(provider)
            _aiUiState.value = AiUiState.Idle
        }
    }

    fun selectAiProvider(provider: String) {
        viewModelScope.launch {
            preferencesManager.saveSelectedAiProvider(provider)
            _aiUiState.value = AiUiState.Idle
        }
    }

    fun generateAiInsights(queryType: String, language: String = "vi") {
        val currentSession = session.value ?: return
        val keys = allApiKeys.value
        if (keys.isEmpty()) {
            val errorMsg = if (language.lowercase().startsWith("vi")) {
                "Mã khóa API chưa được cấu hình. Vui lòng cấu hình ít nhất một nhà cung cấp AI."
            } else {
                "API Key has not been configured. Please configure at least one AI provider."
            }
            _aiUiState.value = AiUiState.Error(errorMsg)
            return
        }

        // Randomize the tone/queryType if the requested type is "random"
        val targetQueryType = if (queryType == "random") {
            listOf(
                "summary", "tactics", "roast", "poet", "commentator", "philosopher",
                "conspiracy", "therapist", "statistician", "pirate", "cheerleader"
            ).random()
        } else {
            queryType
        }

        viewModelScope.launch {
            _aiUiState.value = AiUiState.Loading()
            try {
                var accumulated = ""
                var activeAiName: String? = null
                aiStatsService.generateInsights(
                    apiKeys = keys,
                    session = currentSession,
                    queryType = targetQueryType,
                    language = language
                ).collect { event ->
                    when (event) {
                        is AiStreamEvent.ProviderSelected -> {
                            activeAiName = "${event.providerName} (${event.modelName})"
                            // Reset accumulated text when we switch to a new model after a fallback.
                            accumulated = ""
                            _aiUiState.value = AiUiState.Loading(activeAiName)
                        }
                        is AiStreamEvent.TextChunk -> {
                            if (event.text.isNotEmpty()) {
                                accumulated += event.text
                                android.util.Log.d("StatsViewModel", "Raw accumulated text: $accumulated")
                                _aiUiState.value = AiUiState.Success(
                                    stripThinkingProcess(accumulated, isFinished = false),
                                    activeAiName
                                )
                            }
                        }
                    }
                }
                _aiUiState.value = AiUiState.Success(
                    stripThinkingProcess(accumulated, isFinished = true),
                    activeAiName
                )
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "Error generating AI insights", e)
                val rawMsg = e.localizedMessage ?: e.message ?: "Lỗi không xác định khi kết nối với AI"
                _aiUiState.value = AiUiState.Error(cleanErrorMessage(rawMsg, language))
            }
        }
    }

    private fun stripThinkingProcess(text: String, isFinished: Boolean): String {
        var result = text

        // 1. Handle standard reasoning tags: <think> ... </think>
        while (true) {
            val startIdx = result.indexOf("<think>")
            if (startIdx == -1) break
            val endIdx = result.indexOf("</think>", startIdx + 7)
            if (endIdx != -1) {
                result = result.removeRange(startIdx, endIdx + 8)
            } else {
                // Unclosed thinking block: remove everything from startIdx to the end
                result = result.substring(0, startIdx)
                break
            }
        }

        // 2. Handle models that output plain-text planning/thoughts before the required header
        result = stripPrecedingThoughts(result, isFinished)

        return result.trim()
    }

    private fun stripPrecedingThoughts(text: String, isFinished: Boolean): String {
        val headers = listOf(
            "📝 Tóm tắt nhanh:", "📝 Quick Summary:",
            "🧠 Phân tích chiến thuật:", "🧠 Tactical Breakdown:",
            "🔥 Chế độ Cà khịa:", "🔥 Roast Mode:",
            "✍️ Áng thơ bất hủ:", "✍️ Legendary Rhymes:",
            "🎙️ Bình luận viên:", "🎙️ Live Commentator:",
            "🦉 Góc triết học:", "🦉 Philosophical Corner:",
            "👽 Thuyết âm mưu:", "👽 Conspiracy Theory:",
            "🛋️ Bác sĩ tâm lý:", "🛋️ Therapist's Couch:",
            "📊 Nhà thống kê:", "📊 Statistician's Log:",
            "🏴‍☠️ Thuyền trưởng Hải tặc:", "🏴‍☠️ Pirate Captain:",
            "📣 Cổ động viên:", "📣 Cheerleader's Hype:"
        )

        var bestIndex = -1

        for (header in headers) {
            val idx = text.lastIndexOf(header)
            if (idx != -1) {
                var start = idx
                while (start > 0 && text[start - 1] == '*') {
                    start--
                }
                if (bestIndex == -1 || start > bestIndex) {
                    bestIndex = start
                }
            }
        }

        if (bestIndex != -1) {
            return text.substring(bestIndex)
        }

        // If the header is not found:
        // During generation, return empty to hide the raw thoughts.
        // Once finished, if we still haven't found the header, return the raw text as fallback.
        return if (isFinished) text else ""
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
