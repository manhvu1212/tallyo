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
            "conspiracy" -> if (language == "vi") {
                "Phân tích trận đấu dưới dạng thuyết âm mưu hài hước (tối đa 3 gạch đầu dòng ngắn, mỗi dòng dưới 15 từ): Nghi ngờ có sự dàn xếp, thông đồng hoặc vận may siêu nhiên đứng sau kết quả."
            } else {
                "Analyze the match as a humorous conspiracy theorist (max 3 short bullet points, each under 15 words): Suspect match-fixing, collusions, or supernatural luck behind the results."
            }
            "therapist" -> if (language == "vi") {
                "Đóng vai bác sĩ tâm lý để an ủi, tư vấn tâm lý cho người thua và chúc mừng người thắng (tối đa 3 gạch đầu dòng ngắn, mỗi dòng dưới 15 từ) với giọng điệu cảm thông, ấm áp."
            } else {
                "Act as a friendly therapist offering counseling and comfort to the losers and congratulating the winner (max 3 short bullet points, each under 15 words) with empathetic, warm tone."
            }
            "statistician" -> if (language == "vi") {
                "Đóng vai nhà thống kê học khô khan nhưng chính xác để đưa ra nhận xét khoa học (tối đa 3 gạch đầu dòng ngắn, mỗi dòng dưới 15 từ) dựa trên các con số."
            } else {
                "Act as a dry, precise statistician giving scientific, numbers-based observations (max 3 short bullet points, each under 15 words)."
            }
            "pirate" -> if (language == "vi") {
                "Đóng vai một thuyền trưởng hải tặc để nhận xét về trận đấu bằng ngôn ngữ cướp biển vui nhộn (tối đa 3 gạch đầu dòng ngắn, mỗi dòng dưới 15 từ)."
            } else {
                "Act as a funny pirate captain commenting on the match in pirate slang (max 3 short bullet points, each under 15 words)."
            }
            "cheerleader" -> if (language == "vi") {
                "Đóng vai một cổ động viên cuồng nhiệt, dùng giọng điệu cực kỳ sôi nổi để cổ vũ và nâng cao tinh thần cho tất cả người chơi (tối đa 3 gạch đầu dòng ngắn, mỗi dòng dưới 15 từ)."
            } else {
                "Act as an energetic cheerleader boosting everyone's spirits and hyping up all players (max 3 short bullet points, each under 15 words)."
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
                appendLine("- Nếu cần suy nghĩ, nháp hoặc lập luận, hãy bắt buộc đặt toàn bộ phần đó bên trong cặp thẻ <think>...</think>.")
                appendLine("- TRẢ LỜI CỰC KỲ NGẮN GỌN VÀ SÚC TÍCH. Tổng độ dài toàn bộ câu trả lời bên ngoài thẻ <think> KHÔNG ĐƯỢC VƯỢT QUÁ 80 TỪ.")
                appendLine("- Đi thẳng vào vấn đề, không viết lời chào hỏi, giới thiệu hay kết luận dông dài.")
            } else {
                appendLine("- Please answer in English.")
                appendLine("- If you need to think, draft, or reason, you MUST wrap all of it inside <think>...</think> tags.")
                appendLine("- KEEP IT EXTREMELY BRIEF AND CONCISE. The total response length outside <think> tags MUST NOT EXCEED 80 WORDS.")
                appendLine("- Go straight to the point, avoiding any introductory greetings, explanations, or conversational filler.")
            }
            appendLine("- Sử dụng Markdown để trình bày kết quả (in đậm, in nghiêng hoặc gạch đầu dòng) để hiển thị đẹp mắt.")
        }

        val systemInstruction = if (language == "vi") {
            """
                Bạn là một chuyên gia phân tích dữ liệu trò chơi thông minh, hóm hỉnh cho ứng dụng Tallyo.
                Nhiệm vụ của bạn là đưa ra nhận xét siêu ngắn gọn, súc tích và đi thẳng vào vấn đề (tối đa 3 gạch đầu dòng ngắn, tổng cộng dưới 80 từ).
                Nếu bạn cần suy nghĩ, nháp hoặc lập luận trước khi trả lời, hãy bắt buộc đặt toàn bộ phần suy nghĩ/nháp đó bên trong cặp thẻ <think>...</think>.
                Tuyệt đối không viết suy nghĩ hay lập luận tự do bên ngoài thẻ <think>. Phần trả lời bên ngoài thẻ <think> phải đi thẳng vào vấn đề, không chào hỏi, không dông dài, và phải bắt đầu bằng tiêu đề được yêu cầu. Sử dụng Markdown chuẩn để hiển thị đẹp mắt.
            """.trimIndent()
        } else {
            """
                You are a smart, witty game data analyst for the Tallyo app.
                Your task is to provide extremely brief, concise, and direct observations (maximum 3 short bullet points, total under 80 words).
                If you need to think, draft, or reason before answering, you MUST wrap all your thinking/drafting inside <think>...</think> tags.
                Never write free-form thoughts or reasoning outside the <think> tags. The official response outside <think> tags must go straight to the point, avoiding greetings or fluff, and must start with the requested header. Use standard Markdown for beautiful rendering.
            """.trimIndent()
        }

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

        val models = listOf(
            "gemini-3.5-flash",
            "gemini-3-flash",
            "gemini-2.5-flash",
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
                    android.util.Log.e("AiStatsService", "Exception caught during generation for model $modelName: ${e.message}", e)

                    if (receivedAnyText) {
                        val isSafetyOrRecitation = if (e is ResponseStoppedException) {
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
                    lastException = e
                }
            }
            if (!success) {
                throw lastException ?: Exception("All models failed")
            }
        }
    }
}
