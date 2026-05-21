package io.github.manhvu1212.tallyo.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import io.github.manhvu1212.tallyo.domain.Session
import io.github.manhvu1212.tallyo.domain.computePlayerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AiStatsService {

    fun generateInsights(
        apiKey: String,
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
                "Tóm tắt diễn biến trận đấu siêu ngắn gọn (tối đa 3 gạch đầu dòng ngắn, mỗi dòng dưới 15 từ): Ai thắng, ai về chót, và ván đấu bước ngoặt."
            } else {
                "Summarize the match extremely briefly (max 3 short bullet points, each under 15 words): Who won, who lost, and the turning point round."
            }
            "tactics" -> if (language == "vi") {
                "Phân tích chiến thuật siêu ngắn gọn (tối đa 3 gạch đầu dòng ngắn, mỗi dòng dưới 15 từ): Chỉ ra điểm cốt yếu của người chơi tốt nhất, người chót và lời khuyên cốt lõi."
            } else {
                "Analyze tactics extremely briefly (max 3 short bullet points, each under 15 words): Point out the key performance of the best/worst player and a core advice."
            }
            "roast" -> if (language == "vi") {
                "Cà khịa trận đấu hài hước nhưng cực kỳ ngắn gọn (tối đa 3 gạch đầu dòng ngắn, mỗi dòng dưới 15 từ), tập trung trêu chọc người thua và khen người thắng."
            } else {
                "Roast the match humorously but extremely briefly (max 3 short bullet points, each under 15 words), focusing on teasing the losers and praising the winner."
            }
            "poet" -> if (language == "vi") {
                "Làm một bài thơ ngắn vui nhộn (tối đa 4 câu thơ ngắn) kể về trận đấu, châm chọc người thua và ca ngợi người thắng."
            } else {
                "Write a short, funny rhyme or poem (max 4 short lines) about the match, teasing the loser and praising the winner."
            }
            "commentator" -> if (language == "vi") {
                "Đóng vai bình luận viên thể thao để tường thuật ngắn gọn trận đấu (tối đa 3 gạch đầu dòng ngắn), tạo không khí kịch tính như trận đấu chung kết."
            } else {
                "Act as a hyper-enthusiastic sports commentator summarizing the match (max 3 short bullet points) with high energy and drama."
            }
            "philosopher" -> if (language == "vi") {
                "Phân tích trận đấu dưới góc nhìn triết học sâu sắc nhưng hài hước, dí dỏm (tối đa 3 gạch đầu dòng ngắn), suy ngẫm về chiến thắng, thất bại và số phận."
            } else {
                "Analyze the match from a deep but humorous philosophical perspective (max 3 short bullet points), reflecting on victory, defeat, and fate."
            }
            else -> customQuery ?: (if (language == "vi") "Hãy phân tích trận đấu này ngắn gọn." else "Analyze this match briefly.")
        }

        val toneHeader = when (queryType) {
            "summary" -> if (language == "vi") "**📝 Tóm tắt nhanh:**" else "**📝 Quick Summary:**"
            "tactics" -> if (language == "vi") "**🧠 Phân tích chiến thuật:**" else "**🧠 Tactical Breakdown:**"
            "roast" -> if (language == "vi") "**🔥 Chế độ Cà khịa:**" else "**🔥 Roast Mode:**"
            "poet" -> if (language == "vi") "**✍️ Áng thơ bất hủ:**" else "**✍️ Legendary Rhymes:**"
            "commentator" -> if (language == "vi") "**🎙️ Bình luận viên:**" else "**🎙️ Live Commentator:**"
            "philosopher" -> if (language == "vi") "**🦉 Góc triết học:**" else "**🦉 Philosophical Corner:**"
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
                appendLine("- TRẢ LỜI CỰC KỲ NGẮN GỌN VÀ SÚC TÍCH. Tổng độ dài toàn bộ câu trả lời KHÔNG ĐƯỢC VƯỢT QUÁ 80 TỪ.")
                appendLine("- Đi thẳng vào vấn đề, không viết lời chào hỏi, giới thiệu hay kết luận dông dài.")
            } else {
                appendLine("- Please answer in English.")
                appendLine("- KEEP IT EXTREMELY BRIEF AND CONCISE. The total response length MUST NOT EXCEED 80 WORDS.")
                appendLine("- Go straight to the point, avoiding any introductory greetings, explanations, or conversational filler.")
            }
            appendLine("- Sử dụng Markdown để trình bày kết quả (in đậm, in nghiêng hoặc gạch đầu dòng) để hiển thị đẹp mắt.")
        }

        val systemInstruction = if (language == "vi") {
            """
                Bạn là một chuyên gia phân tích dữ liệu trò chơi thông minh, hóm hỉnh cho ứng dụng Tallyo.
                Nhiệm vụ của bạn là đưa ra nhận xét siêu ngắn gọn, súc tích và đi thẳng vào vấn đề (tối đa 3 gạch đầu dòng ngắn, tổng cộng dưới 80 từ).
                Tuyệt đối không viết dài dòng, không chào hỏi, không dông dài, không giải thích. Chỉ trả về trực tiếp kết quả phân tích theo phong cách được yêu cầu. Sử dụng Markdown chuẩn để hiển thị đẹp mắt.
            """.trimIndent()
        } else {
            """
                You are a smart, witty game data analyst for the Tallyo app.
                Your task is to provide extremely brief, concise, and direct observations (maximum 3 short bullet points, total under 80 words).
                Never write long paragraphs, greetings, explanations, or conversational filler. Only return the analysis result in the requested style. Use standard Markdown for beautiful rendering.
            """.trimIndent()
        }

        val config = generationConfig {
            maxOutputTokens = 300
            temperature = 0.7f
        }

        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
        )

        val models = listOf(
            "gemini-3.5-flash",
            "gemini-3-flash",
            "gemini-2.5-flash",
            "gemma-4-31b-it",
            "gemma-4-26b-a4b-it",
            "gemini-3.1-flash-lite",
            "gemini-2.5-flash-lite"
        )

        return flow {
            var success = false
            var lastException: Exception? = null

            for (modelName in models) {
                var receivedAnyText = false
                try {
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
                            receivedAnyText = true
                            emit(text)
                        }
                    }
                    success = true
                    break
                } catch (e: Exception) {
                    if (receivedAnyText) {
                        throw e
                    }
                    lastException = e
                }
            }
            if (!success) {
                throw lastException ?: Exception("All models failed")
            }
        }
    }
}
