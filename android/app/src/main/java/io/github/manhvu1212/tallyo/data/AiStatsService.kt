package io.github.manhvu1212.tallyo.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.FinishReason
import io.github.manhvu1212.tallyo.domain.Session
import io.github.manhvu1212.tallyo.domain.computePlayerStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray

interface AiProvider {
    val id: String
    val name: String
    val models: List<String>
    fun generateStream(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        fullPrompt: String
    ): Flow<String>
}

class GeminiProvider : AiProvider {
    override val id: String = "gemini"
    override val name: String = "Google Gemini"
    override val models: List<String> = listOf(
        "gemini-3.5-flash",
        "gemini-3-flash",
        "gemini-2.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash-lite"
    )

    override fun generateStream(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        fullPrompt: String
    ): Flow<String> = flow {
        val config = generationConfig {
            maxOutputTokens = 2048
            temperature = 0.7f
        }
        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
        )
        val generativeModel = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = config,
            safetySettings = safetySettings,
            systemInstruction = content { text(systemInstruction) }
        )
        generativeModel.generateContentStream(fullPrompt).collect { response ->
            val text = response.text ?: ""
            if (text.isNotEmpty()) {
                emit(text)
            }
        }
    }
}

class GroqProvider : AiProvider {
    override val id: String = "groq"
    override val name: String = "Groq AI"
    override val models: List<String> = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768"
    )

    override fun generateStream(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        fullPrompt: String
    ): Flow<String> = flow {
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val requestBodyJson = JSONObject().apply {
            put("model", modelName)
            put("temperature", 0.7)
            put("max_tokens", 2048)
            put("stream", true)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", fullPrompt)
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .post(requestBodyJson.toString().toRequestBody(mediaType))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw Exception("HTTP Error: ${response.code} ${response.message}\n$errBody")
            }
            val source = response.body?.source() ?: throw Exception("Empty response body")
            
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.substring(6).trim()
                    if (data == "[DONE]") {
                        break
                    }
                    try {
                        val json = JSONObject(data)
                        val choices = json.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val delta = choice.optJSONObject("delta")
                            val content = delta?.optString("content") ?: ""
                            if (content.isNotEmpty()) {
                                emit(content)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore JSON parse errors on partial chunks
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}

class AiStatsService {

    private val providers = listOf(
        GeminiProvider(),
        GroqProvider()
    )

    fun generateInsights(
        apiKeys: Map<String, String>,
        session: Session,
        queryType: String, // "summary", "tactics", "roast", or "custom"
        customQuery: String? = null,
        language: String = "vi"
    ): Flow<String> {
        val stats = computePlayerStats(session)
        val rankedStats = stats.sortedByDescending { it.totalPoints }

        val gameInfo = buildString {
            appendLine("Trận đấu: ${session.name}")
            appendLine("Trò chơi: ${session.game}")
            appendLine("Số ván chơi: ${session.rounds.size}")
            appendLine("Luật Zero-sum (tổng điểm bằng 0): ${if (session.zeroSum) "Có" else "Không"}")
            appendLine()
            appendLine("Danh sách người chơi và thống kê tổng hợp:")
            rankedStats.forEach { p ->
                appendLine("- ${p.name}: Tổng điểm: ${p.totalPoints}, Số ván tham gia: ${p.roundsPlayed}, Số ván thắng: ${p.wins}, Số ván thua: ${p.losses}, Điểm ván tốt nhất: ${p.bestRound}, Điểm ván tệ nhất: ${p.worstRound}, Điểm trung bình mỗi ván: ${String.format(java.util.Locale.US, "%.1f", p.averagePerRound)}")
            }
            appendLine()
            appendLine("Chi tiết từng ván chơi (Round):")
            session.rounds.forEachIndexed { idx, round ->
                val roundScores = round.scores.joinToString(", ") { score ->
                    val playerName = session.players.firstOrNull { it.id == score.playerId }?.name ?: "Ẩn danh"
                    "$playerName: ${if (score.points > 0) "+" else ""}${score.points}"
                }
                appendLine("Ván ${idx + 1}${if (round.note != null) " (Ghi chú: ${round.note})" else ""} -> $roundScores")
            }
        }

        val promptInstruction = when (queryType) {
            "summary" -> if (language == "vi") {
                "Tóm tắt diễn biến trận đấu sinh động và đủ ý (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu): Ai thắng, ai thua đậm nhất, và ván đấu bước ngoặt."
            } else {
                "Summarize the match vividly and informatively (max 3 bullet points, each about 1-2 sentences): Who won, who lost the most, and the turning point round."
            }
            "tactics" -> if (language == "vi") {
                "Phân tích chiến thuật ngắn gọn nhưng sâu sắc (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu): Chỉ ra điểm cốt yếu giúp người thắng làm chủ cuộc chơi, lỗi của người về chót và lời khuyên thiết thực."
            } else {
                "Analyze tactics briefly but deeply (max 3 bullet points, each about 1-2 sentences): Point out the key moves that helped the winner, mistakes of the worst player, and practical advice."
            }
            "roast" -> if (language == "vi") {
                "Cà khịa trận đấu hài hước và xéo sắc (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu), châm chọc vui vẻ người thua cuộc và khen ngợi hóm hỉnh người chiến thắng."
            } else {
                "Roast the match humorously and sharply (max 3 bullet points, each about 1-2 sentences), teasing the losers and praising the winner."
            }
            "poet" -> if (language == "vi") {
                "Làm một bài thơ ngắn vui nhộn (tối đa 2 khổ thơ ngắn hoặc 8 câu thơ) kể về trận đấu, châm chọc người thua và ca ngợi người thắng."
            } else {
                "Write a short, funny poem (max 2 stanzas or 8 lines) about the match, teasing the loser and praising the winner."
            }
            "commentator" -> if (language == "vi") {
                "Đóng vai bình luận viên thể thao cuồng nhiệt tường thuật sinh động (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu), tạo không khí kịch tính như chung kết."
            } else {
                "Act as a hyper-enthusiastic sports commentator summarizing the match dynamically (max 3 bullet points, each about 1-2 sentences) with high energy."
            }
            "philosopher" -> if (language == "vi") {
                "Phân tích trận đấu dưới góc nhìn triết học sâu sắc nhưng dí dỏm (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu), suy ngẫm về chiến thắng, thất bại và nhân quả cuộc chơi."
            } else {
                "Analyze the match from a deep but witty philosophical perspective (max 3 bullet points, each about 1-2 sentences), reflecting on victory, defeat, and fate."
            }
            "conspiracy" -> if (language == "vi") {
                "Phân tích trận đấu dưới dạng thuyết âm mưu hài hước (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu): Nghi ngờ có sự dàn xếp ngầm, thông đồng giữa các người chơi hoặc vận may siêu nhiên kì lạ đứng sau kết quả."
            } else {
                "Analyze the match as a humorous conspiracy theorist (max 3 bullet points, each about 1-2 sentences): Suspect match-fixing, collusions, or supernatural luck."
            }
            "therapist" -> if (language == "vi") {
                "Đóng vai bác sĩ tâm lý để xoa dịu nỗi đau, chữa lành cho người thua và chúc mừng người thắng (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu) với giọng điệu cảm thông, ấm áp."
            } else {
                "Act as a friendly therapist offering comfort to the losers and congratulating the winner (max 3 bullet points, each about 1-2 sentences) with empathetic tone."
            }
            "statistician" -> if (language == "vi") {
                "Đóng vai nhà thống kê học để nhận xét khoa học phân tích số liệu trận đấu (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu) một cách chi tiết và chính xác."
            } else {
                "Act as a precise statistician giving scientific, observations based on the numbers (max 3 bullet points, each about 1-2 sentences) analyzing details."
            }
            "pirate" -> if (language == "vi") {
                "Đóng vai một thuyền trưởng hải tặc để nhận xét về trận đấu bằng ngôn ngữ cướp biển vui nhộn, hào sảng (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu)."
            } else {
                "Act as a funny pirate captain commenting on the match in pirate slang (max 3 bullet points, each about 1-2 sentences)."
            }
            "cheerleader" -> if (language == "vi") {
                "Đóng vai một cổ động viên cuồng nhiệt để cổ vũ và nâng cao tinh thần cho tất cả người chơi (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu) với giọng điệu sôi nổi."
            } else {
                "Act as an energetic cheerleader boosting everyone's spirits and hyping up all players (max 3 bullet points, each about 1-2 sentences)."
            }
            else -> customQuery ?: (if (language == "vi") "Hãy phân tích trận đấu này." else "Analyze this match.")
        }

        val toneHeader = when (queryType) {
            "summary" -> if (language == "vi") "**📝 Tóm tắt nhanh:**" else "**📝 Quick Summary:**"
            "tactics" -> if (language == "vi") "**🧠 Phân tích chiến thuật:**" else "**🧠 Tactical Breakdown:**"
            "roast" -> if (language == "vi") "**🔥 Chế độ Cà khịa:**" else "**🔥 Roast Mode:**"
            "poet" -> if (language == "vi") "**✍️ Áng thơ bất hủ:**" else "**✍️ Legendary Rhymes:**"
            "commentator" -> if (language == "vi") "**🎙️ Bình luận viên:**" else "**🎙️ Live Commentator:**"
            "philosopher" -> if (language == "vi") "**🦉 Góc triết học:**" else "**🦉 Philosophical Corner:**"
            "conspiracy" -> if (language == "vi") "**👽 Thuyết âm mưu:**" else "**👽 Conspiracy Theory:**"
            "therapist" -> if (language == "vi") "**🛋️ Bác sĩ tâm lý:**" else "**🛋️ Therapist's Couch:**"
            "statistician" -> if (language == "vi") "**📊 Nhà thống kê:**" else "**📊 Statistician's Log:**"
            "pirate" -> if (language == "vi") "**🏴‍☠️ Thuyền trưởng Hải tặc:**" else "**🏴‍☠️ Pirate Captain:**"
            "cheerleader" -> if (language == "vi") "**📣 Cổ động viên:**" else "**📣 Cheerleader's Hype:**"
            else -> ""
        }

        val fullPrompt = buildString {
            appendLine("Dưới đây là thông tin chi tiết về một trận đấu được ghi chép từ ứng dụng Tallyo:")
            appendLine()
            appendLine(gameInfo)
            appendLine()
            appendLine("Yêu cầu: $promptInstruction")
            appendLine()
            appendLine("Lưu ý bắt buộc:")
            if (toneHeader.isNotEmpty()) {
                if (language == "vi") {
                    appendLine("- Hãy bắt đầu câu trả lời bằng việc in ra chính xác dòng tiêu đề này: $toneHeader (sau đó xuống dòng và bắt đầu viết nội dung phân tích theo phong cách tương ứng).")
                } else {
                    appendLine("- Start your response by printing exactly this header line: $toneHeader (then insert a newline and start writing the analysis in that style).")
                }
            }
            if (language == "vi") {
                appendLine("- Hãy trả lời bằng tiếng Việt.")
                appendLine("- TUYỆT ĐỐI không sử dụng bất kỳ từ ngữ, chữ viết hoặc ký tự tiếng Trung (Trung Quốc/Hán tự) nào trong câu trả lời. Chỉ viết bằng tiếng Việt chuẩn ngữ pháp.")
                appendLine("- Bắt buộc gọi đúng tên của các người chơi xuất hiện trong dữ liệu trận đấu (ví dụ cụ thể tên người chơi, không nói chung chung 'người thắng', 'người thua cuộc'). Tập trung phân tích hành trình điểm số, sự bám đuổi và phong độ cụ thể của từng người chơi.")
                appendLine("- Nếu cần suy nghĩ, nháp hoặc lập luận, hãy bắt buộc đặt toàn bộ phần đó bên trong cặp thẻ <think>...</think>.")
                appendLine("- TRẢ LỜI NGẮN GỌN VÀ SÚC TÍCH. Tổng độ dài toàn bộ câu trả lời bên ngoài thẻ <think> KHÔNG ĐƯỢC VƯỢT QUÁ 150 TỪ.")
                appendLine("- Đi thẳng vào vấn đề, không viết lời chào hỏi, giới thiệu hay kết luận dông dài.")
            } else {
                appendLine("- Please answer in English.")
                appendLine("- You MUST use the players' actual names from the provided game data (e.g. refer to players by their names, do not use generic terms like 'the winner' or 'the loser'). Focus your analysis on individual player performances, score progression, and their specific rivalries.")
                appendLine("- If you need to think, draft, or reason, you MUST wrap all of it inside <think>...</think> tags.")
                appendLine("- KEEP IT BRIEF AND CONCISE. The total response length outside <think> tags MUST NOT EXCEED 150 WORDS.")
                appendLine("- Go straight to the point, avoiding any introductory greetings, explanations, or conversational filler.")
            }
            appendLine("- Sử dụng Markdown để trình bày kết quả (in đậm, in nghiêng hoặc gạch đầu dòng) để hiển thị đẹp mắt.")
        }

        val systemInstruction = if (language == "vi") {
            """
                Bạn là một chuyên gia phân tích dữ liệu trò chơi thông minh, hóm hỉnh cho ứng dụng Tallyo.
                Nhiệm vụ của bạn là đưa ra nhận xét ngắn gọn, súc tích và đi thẳng vào vấn đề (tối đa 3 gạch đầu dòng, tổng cộng dưới 150 từ).
                Bắt buộc gọi đúng tên của các người chơi xuất hiện trong dữ liệu trận đấu (không gọi chung chung là 'người thắng', 'người thua' hay 'người chơi'). Hãy tập trung phân tích sâu vào phong độ, điểm số và sự đối đầu của từng người chơi cụ thể.
                TUYỆT ĐỐI không sử dụng bất kỳ từ ngữ, chữ viết hoặc ký tự tiếng Trung (Trung Quốc/Hán tự) nào trong câu trả lời. Toàn bộ câu trả lời phải được viết bằng tiếng Việt chuẩn.
                Nếu bạn cần suy nghĩ, nháp hoặc lập luận trước khi trả lời, hãy bắt buộc đặt toàn bộ phần suy nghĩ/nháp đó bên trong cặp thẻ <think>...</think>.
                Tuyệt đối không viết suy nghĩ hay lập luận tự do bên ngoài thẻ <think>. Phần trả lời bên ngoài thẻ <think> phải đi thẳng vào vấn đề, không chào hỏi, không dông dài, và phải bắt đầu bằng tiêu đề được yêu cầu. Sử dụng Markdown chuẩn để hiển thị đẹp mắt.
            """.trimIndent()
        } else {
            """
                You are a smart, witty game data analyst for the Tallyo app.
                Your task is to provide brief, concise, and direct observations (maximum 3 bullet points, total under 150 words).
                You MUST use the players' actual names from the game data (never refer to them generically as 'the winner', 'the loser', or 'the player'). Focus your analysis deeply on the individual performance, scores, and rivalries of specific players.
                If you need to think, draft, or reason before answering, you MUST wrap all your thinking/drafting inside <think>...</think> tags.
                Never write free-form thoughts or reasoning outside the <think> tags. The official response outside <think> tags must go straight to the point, avoiding greetings or fluff, and must start with the requested header. Use standard Markdown for beautiful rendering.
            """.trimIndent()
        }

        data class ModelChoice(val provider: AiProvider, val modelName: String)

        return flow {
            // 1. Filter active providers that have non-blank keys
            val activeProviders = providers.filter { provider ->
                apiKeys[provider.id]?.isNotBlank() == true
            }

            if (activeProviders.isEmpty()) {
                throw Exception(if (language == "vi") "Không có AI provider nào được cấu hình." else "No AI provider is configured.")
            }

            // 2. Build list of all available choices
            val remainingChoices = mutableListOf<ModelChoice>()
            for (provider in activeProviders) {
                for (model in provider.models) {
                    remainingChoices.add(ModelChoice(provider, model))
                }
            }

            var success = false
            var lastException: Exception? = null

            // 3. Fallback/Ignored logic loop
            while (remainingChoices.isNotEmpty() && !success) {
                // Randomly select provider first from the remaining options
                val availableProviderIds = remainingChoices.map { it.provider.id }.distinct()
                val chosenProviderId = availableProviderIds.random()

                // Randomly select model from that provider's remaining options
                val providerChoices = remainingChoices.filter { it.provider.id == chosenProviderId }
                val chosenChoice = providerChoices.random()

                val provider = chosenChoice.provider
                val modelName = chosenChoice.modelName
                val apiKey = apiKeys[provider.id]!!

                android.util.Log.d("AiStatsService", "Attempting generation with provider=${provider.id}, model=$modelName")

                var receivedAnyText = false

                try {
                    provider.generateStream(
                        apiKey = apiKey,
                        modelName = modelName,
                        systemInstruction = systemInstruction,
                        fullPrompt = fullPrompt
                    ).collect { chunk ->
                        if (chunk.isNotEmpty()) {
                            receivedAnyText = true
                            emit(chunk)
                        }
                    }
                    success = true
                } catch (e: Exception) {
                    android.util.Log.e("AiStatsService", "Error with provider=${provider.id}, model=$modelName: ${e.message}", e)

                    if (receivedAnyText) {
                        val isSafetyOrRecitation = if (provider.id == "gemini" && e is ResponseStoppedException) {
                            val finishReason = e.response.candidates.firstOrNull()?.finishReason
                            finishReason == FinishReason.SAFETY || finishReason == FinishReason.RECITATION
                        } else {
                            false
                        }

                        if (isSafetyOrRecitation) {
                            throw e
                        } else {
                            // Non-safety exception at end of stream (e.g. SerializationException or normal stop)
                            success = true
                            break
                        }
                    }

                    // Remove failed model from remaining list (put it in ignore list)
                    remainingChoices.remove(chosenChoice)
                    lastException = e
                }
            }

            if (!success) {
                throw lastException ?: Exception("All configured AI models failed.")
            }
        }.flowOn(Dispatchers.IO)
    }
}
