import Foundation

enum AiStreamEvent {
    case providerSelected(providerName: String, modelName: String)
    case textChunk(text: String)
}

protocol AiProvider {
    var id: String { get }
    var name: String { get }
    var models: [String] { get }
    func generateStream(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        fullPrompt: String
    ) -> AsyncThrowingStream<String, Error>
}

class GeminiProvider: AiProvider {
    let id = "gemini"
    let name = "Google Gemini"
    let models = [
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-1.5-flash"
    ]
    
    func generateStream(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        fullPrompt: String
    ) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            Task {
                do {
                    let urlString = "https://generativelanguage.googleapis.com/v1beta/models/\(modelName):streamGenerateContent?key=\(apiKey)"
                    guard let url = URL(string: urlString) else {
                        continuation.finish(throwing: URLError(.badURL))
                        return
                    }
                    
                    var request = URLRequest(url: url)
                    request.httpMethod = "POST"
                    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                    
                    let body: [String: Any] = [
                        "contents": [
                            ["parts": [["text": fullPrompt]]]
                        ],
                        "systemInstruction": [
                            "parts": [["text": systemInstruction]]
                        ],
                        "generationConfig": [
                            "maxOutputTokens": 2048,
                            "temperature": 0.7
                        ],
                        "safetySettings": [
                            ["category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"],
                            ["category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_NONE"],
                            ["category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"],
                            ["category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_NONE"]
                        ]
                    ]
                    
                    request.httpBody = try JSONSerialization.data(withJSONObject: body)
                    
                    let (resultBytes, response) = try await URLSession.shared.bytes(for: request)
                    guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                        let code = (response as? HTTPURLResponse)?.statusCode ?? -1
                        continuation.finish(throwing: NSError(domain: "GeminiError", code: code, userInfo: [NSLocalizedDescriptionKey: "HTTP Status: \(code)"]))
                        return
                    }
                    
                    for try await line in resultBytes.lines {
                        let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
                        if trimmed.isEmpty { continue }
                        
                        // Gemini stream chunks start with "[" or "," and end with "]" or ","
                        var jsonString = trimmed
                        if jsonString.hasPrefix("[") { jsonString.removeFirst() }
                        if jsonString.hasSuffix("]") { jsonString.removeLast() }
                        if jsonString.hasPrefix(",") { jsonString.removeFirst() }
                        if jsonString.hasSuffix(",") { jsonString.removeLast() }
                        
                        let cleanJson = jsonString.trimmingCharacters(in: .whitespacesAndNewlines)
                        if cleanJson.isEmpty { continue }
                        
                        guard let data = cleanJson.data(using: .utf8) else { continue }
                        if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                           let candidates = json["candidates"] as? [[String: Any]],
                           let first = candidates.first,
                           let content = first["content"] as? [String: Any],
                           let parts = content["parts"] as? [[String: Any]],
                           let part = parts.first,
                           let text = part["text"] as? String {
                            continuation.yield(text)
                        }
                    }
                    
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }
}

class GroqProvider: AiProvider {
    let id = "groq"
    let name = "Groq AI"
    let models = [
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768"
    ]
    
    func generateStream(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        fullPrompt: String
    ) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            Task {
                do {
                    guard let url = URL(string: "https://api.groq.com/openai/v1/chat/completions") else {
                        continuation.finish(throwing: URLError(.badURL))
                        return
                    }
                    
                    var request = URLRequest(url: url)
                    request.httpMethod = "POST"
                    request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
                    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                    
                    let body: [String: Any] = [
                        "model": modelName,
                        "temperature": 0.7,
                        "max_tokens": 2048,
                        "stream": true,
                        "messages": [
                            ["role": "system", "content": systemInstruction],
                            ["role": "user", "content": fullPrompt]
                        ]
                    ]
                    
                    request.httpBody = try JSONSerialization.data(withJSONObject: body)
                    
                    let (resultBytes, response) = try await URLSession.shared.bytes(for: request)
                    guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                        let code = (response as? HTTPURLResponse)?.statusCode ?? -1
                        continuation.finish(throwing: NSError(domain: "GroqError", code: code, userInfo: [NSLocalizedDescriptionKey: "HTTP Status: \(code)"]))
                        return
                    }
                    
                    for try await line in resultBytes.lines {
                        let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
                        if trimmed.hasPrefix("data: ") {
                            let dataStr = trimmed.replacingOccurrences(of: "data: ", with: "").trimmingCharacters(in: .whitespacesAndNewlines)
                            if dataStr == "[DONE]" {
                                break
                            }
                            
                            guard let data = dataStr.data(using: .utf8) else { continue }
                            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                               let choices = json["choices"] as? [[String: Any]],
                               let first = choices.first,
                               let delta = first["delta"] as? [String: Any],
                               let content = delta["content"] as? String {
                                continuation.yield(content)
                            }
                        }
                    }
                    
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }
}

class OpenAiProvider: AiProvider {
    let id = "openai"
    let name = "OpenAI"
    let models = [
        "gpt-4o-mini",
        "gpt-4o"
    ]
    
    func generateStream(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        fullPrompt: String
    ) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            Task {
                do {
                    guard let url = URL(string: "https://api.openai.com/v1/chat/completions") else {
                        continuation.finish(throwing: URLError(.badURL))
                        return
                    }
                    
                    var request = URLRequest(url: url)
                    request.httpMethod = "POST"
                    request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
                    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                    
                    let body: [String: Any] = [
                        "model": modelName,
                        "temperature": 0.7,
                        "max_tokens": 2048,
                        "stream": true,
                        "messages": [
                            ["role": "system", "content": systemInstruction],
                            ["role": "user", "content": fullPrompt]
                        ]
                    ]
                    
                    request.httpBody = try JSONSerialization.data(withJSONObject: body)
                    
                    let (resultBytes, response) = try await URLSession.shared.bytes(for: request)
                    guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                        let code = (response as? HTTPURLResponse)?.statusCode ?? -1
                        continuation.finish(throwing: NSError(domain: "OpenAiError", code: code, userInfo: [NSLocalizedDescriptionKey: "HTTP Status: \(code)"]))
                        return
                    }
                    
                    for try await line in resultBytes.lines {
                        let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
                        if trimmed.hasPrefix("data: ") {
                            let dataStr = trimmed.replacingOccurrences(of: "data: ", with: "").trimmingCharacters(in: .whitespacesAndNewlines)
                            if dataStr == "[DONE]" {
                                break
                            }
                            
                            guard let data = dataStr.data(using: .utf8) else { continue }
                            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                               let choices = json["choices"] as? [[String: Any]],
                               let first = choices.first,
                               let delta = first["delta"] as? [String: Any],
                               let content = delta["content"] as? String {
                                continuation.yield(content)
                            }
                        }
                    }
                    
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }
}

class AiStatsService {
    private let providers: [AiProvider] = [
        GeminiProvider(),
        GroqProvider(),
        OpenAiProvider()
    ]
    
    func generateInsights(
        apiKeys: [String: String],
        session: Session,
        queryType: String,
        customQuery: String? = nil,
        language: String = "vi"
    ) -> AsyncThrowingStream<AiStreamEvent, Error> {
        let stats = computePlayerStats(session: session)
        let rankedStats = stats.sorted { $0.totalPoints > $1.totalPoints }
        
        let gameInfo = buildGameInfo(session: session, rankedStats: rankedStats)
        let promptInstruction = buildPromptInstruction(queryType: queryType, customQuery: customQuery, language: language)
        let toneHeader = buildToneHeader(queryType: queryType, language: language)
        let fullPrompt = buildFullPrompt(gameInfo: gameInfo, promptInstruction: promptInstruction, toneHeader: toneHeader, language: language)
        let systemInstruction = buildSystemInstruction(language: language)
        
        return AsyncThrowingStream { continuation in
            Task {
                // 1. Filter active providers that have non-blank keys
                let activeProviders = providers.filter { provider in
                    if let key = apiKeys[provider.id], !key.isEmpty {
                        return true
                    }
                    return false
                }
                
                if activeProviders.isEmpty {
                    let errMsg = language == "vi" ? "Không có AI provider nào được cấu hình." : "No AI provider is configured."
                    continuation.finish(throwing: NSError(domain: "AiService", code: 400, userInfo: [NSLocalizedDescriptionKey: errMsg]))
                    return
                }
                
                // 2. Build list of all available provider-model choices
                struct ModelChoice {
                    let provider: AiProvider
                    let modelName: String
                }
                
                var remainingChoices: [ModelChoice] = []
                for provider in activeProviders {
                    for model in provider.models {
                        remainingChoices.append(ModelChoice(provider: provider, modelName: model))
                    }
                }
                
                var success = false
                var lastError: Error? = nil
                
                // 3. Loop and try to generate with random fallback
                while !remainingChoices.isEmpty && !success {
                    // Group remaining by provider
                    let availableProviderIds = Array(Set(remainingChoices.map { $0.provider.id }))
                    guard let chosenProviderId = availableProviderIds.randomElement() else { break }
                    
                    let providerChoices = remainingChoices.filter { $0.provider.id == chosenProviderId }
                    guard let chosenChoice = providerChoices.randomElement() else { break }
                    
                    let provider = chosenChoice.provider
                    let modelName = chosenChoice.modelName
                    let apiKey = apiKeys[provider.id]!
                    
                    continuation.yield(.providerSelected(providerName: provider.name, modelName: modelName))
                    
                    do {
                        let stream = provider.generateStream(
                            apiKey: apiKey,
                            modelName: modelName,
                            systemInstruction: systemInstruction,
                            fullPrompt: fullPrompt
                        )
                        
                        for try await chunk in stream {
                            continuation.yield(.textChunk(text: chunk))
                        }
                        success = true
                    } catch {
                        lastError = error
                        // Remove this choice and retry
                        remainingChoices.removeAll { $0.provider.id == provider.id && $0.modelName == modelName }
                    }
                }
                
                if success {
                    continuation.finish()
                } else {
                    let finalErr = lastError ?? NSError(domain: "AiService", code: 500, userInfo: [NSLocalizedDescriptionKey: "All configured models failed"])
                    continuation.finish(throwing: finalErr)
                }
            }
        }
    }
    
    private func buildGameInfo(session: Session, rankedStats: [PlayerStats]) -> String {
        var str = ""
        str += "Trận đấu: \(session.name)\n"
        str += "Trò chơi: \(session.game)\n"
        str += "Số ván chơi: \(session.rounds.count)\n"
        str += "Luật Zero-sum (tổng điểm bằng 0): \(session.zeroSum ? "Có" : "Không")\n\n"
        str += "Danh sách người chơi và thống kê tổng hợp:\n"
        for p in rankedStats {
            str += "- \(p.name): Tổng điểm: \(p.totalPoints), Số ván tham gia: \(p.roundsPlayed), Số ván thắng: \(p.wins), Số ván thua: \(p.losses), Điểm ván tốt nhất: \(p.bestRound), Điểm ván tệ nhất: \(p.worstRound), Điểm trung bình mỗi ván: \(String(format: "%.1f", p.averagePerRound))\n"
        }
        str += "\nChi tiết từng ván chơi (Round):\n"
        
        let sortedRounds = session.rounds.sorted { $0.orderIndex < $1.orderIndex }
        for (idx, round) in sortedRounds.enumerated() {
            let roundScores = round.scores.map { score in
                let playerName = session.players.first(where: { $0.id == score.playerId })?.name ?? "Ẩn danh"
                return "\(playerName): \(score.points > 0 ? "+" : "")\(score.points)"
            }.joined(separator: ", ")
            str += "Ván \(idx + 1)\(round.note != nil ? " (Ghi chú: \(round.note!))" : "") -> \(roundScores)\n"
        }
        return str
    }
    
    private func buildPromptInstruction(queryType: String, customQuery: String?, language: String) -> String {
        let isVi = language == "vi"
        switch queryType {
        case "summary":
            return isVi ? "Tóm tắt diễn biến trận đấu sinh động và đủ ý (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu): Ai thắng, ai thua đậm nhất, và ván đấu bước ngoặt."
                        : "Summarize the match vividly and informatively (max 3 bullet points, each about 1-2 sentences): Who won, who lost the most, and the turning point round."
        case "tactics":
            return isVi ? "Phân tích chiến thuật ngắn gọn nhưng sâu sắc (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu): Chỉ ra điểm cốt yếu giúp người thắng làm chủ cuộc chơi, lỗi của người về chót và lời khuyên thiết thực."
                        : "Analyze tactics briefly but deeply (max 3 bullet points, each about 1-2 sentences): Point out the key moves that helped the winner, mistakes of the worst player, and practical advice."
        case "roast":
            return isVi ? "Cà khịa trận đấu hài hước và xéo sắc (tối đa 3 gạch đầu dòng, mỗi dòng khoảng 1-2 câu), châm chọc vui vẻ người thua cuộc và khen ngợi hóm hỉnh người chiến thắng."
                        : "Roast the match humorously and sharply (max 3 bullet points, each about 1-2 sentences), teasing the losers and praising the winner."
        default:
            return customQuery ?? (isVi ? "Hãy phân tích trận đấu này." : "Analyze this match.")
        }
    }
    
    private func buildToneHeader(queryType: String, language: String) -> String {
        let isVi = language == "vi"
        switch queryType {
        case "summary":
            return isVi ? "**📝 Tóm tắt nhanh:**" : "**📝 Quick Summary:**"
        case "tactics":
            return isVi ? "**🧠 Phân tích chiến thuật:**" : "**🧠 Tactical Breakdown:**"
        case "roast":
            return isVi ? "**🔥 Chế độ Cà khịa:**" : "**🔥 Roast Mode:**"
        default:
            return ""
        }
    }
    
    private func buildFullPrompt(gameInfo: String, promptInstruction: String, toneHeader: String, language: String) -> String {
        let isVi = language == "vi"
        var str = ""
        str += "Dưới đây là thông tin chi tiết về một trận đấu được ghi chép từ ứng dụng Tallyo:\n\n"
        str += gameInfo
        str += "\n\nYêu cầu: \(promptInstruction)\n\n"
        str += "Lưu ý bắt buộc:\n"
        if !toneHeader.isEmpty {
            str += isVi ? "- Hãy bắt đầu câu trả lời bằng việc in ra chính xác dòng tiêu đề này: \(toneHeader) (sau đó xuống dòng và bắt đầu viết nội dung phân tích theo phong cách tương ứng).\n"
                        : "- Start your response by printing exactly this header line: \(toneHeader) (then insert a newline and start writing the analysis in that style).\n"
        }
        if isVi {
            str += "- Hãy trả lời bằng tiếng Việt.\n"
            str += "- TUYỆT ĐỐI không sử dụng bất kỳ từ ngữ, chữ viết hoặc ký tự tiếng Trung (Trung Quốc/Hán tự) nào trong câu trả lời. Chỉ viết bằng tiếng Việt chuẩn ngữ pháp.\n"
            str += "- Bắt buộc gọi đúng tên của các người chơi xuất hiện trong dữ liệu trận đấu (ví dụ cụ thể tên người chơi, không nói chung chung 'người thắng', 'người thua cuộc'). Tập trung phân tích hành trình điểm số, sự bám đuổi và phong độ cụ thể của từng người chơi.\n"
            str += "- Nếu cần suy nghĩ, nháp hoặc lập luận, hãy bắt buộc đặt toàn bộ phần đó bên trong cặp thẻ <think>...</think>.\n"
            str += "- TRẢ LỜI NGẮN GỌN VÀ SÚC TÍCH. Tổng độ dài toàn bộ câu trả lời bên ngoài thẻ <think> KHÔNG ĐƯỢC VƯỢT QUÁ 150 TỪ.\n"
            str += "- Đi thẳng vào vấn đề, không viết lời chào hỏi, giới thiệu hay kết luận dông dài.\n"
        } else {
            str += "- Please answer in English.\n"
            str += "- You MUST use the players' actual names from the provided game data (e.g. refer to players by their names, do not use generic terms like 'the winner' or 'the loser'). Focus your analysis on individual player performances, score progression, and their specific rivalries.\n"
            str += "- If you need to think, draft, or reason, you MUST wrap all of it inside <think>...</think> tags.\n"
            str += "- KEEP IT BRIEF AND CONCISE. The total response length outside <think> tags MUST NOT EXCEED 150 WORDS.\n"
            str += "- Go straight to the point, avoiding any introductory greetings, explanations, or conversational filler.\n"
        }
        str += "- Sử dụng Markdown để trình bày kết quả (in đậm, in nghiêng hoặc gạch đầu dòng) để hiển thị đẹp mắt."
        return str
    }
    
    private func buildSystemInstruction(language: String) -> String {
        if language == "vi" {
            return """
                Bạn là một chuyên gia phân tích dữ liệu trò chơi thông minh, hóm hỉnh cho ứng dụng Tallyo.
                Nhiệm vụ của bạn là đưa ra nhận xét ngắn gọn, súc tích và đi thẳng vào vấn đề (tối đa 3 gạch đầu dòng, tổng cộng dưới 150 từ).
                Bắt buộc gọi đúng tên của các người chơi xuất hiện trong dữ liệu trận đấu (không gọi chung chung là 'người thắng', 'người thua' hay 'người chơi'). Hãy tập trung phân tích sâu vào phong độ, điểm số và sự đối đầu của từng người chơi cụ thể.
                TUYỆT ĐỐI không sử dụng bất kỳ từ ngữ, chữ viết hoặc ký tự tiếng Trung (Trung Quốc/Hán tự) nào trong câu trả lời. Toàn bộ câu trả lời phải được viết bằng tiếng Việt chuẩn.
                Nếu bạn cần suy nghĩ, nháp hoặc lập luận trước khi trả lời, hãy bắt buộc đặt toàn bộ phần suy nghĩ/nháp đó bên trong cặp thẻ <think>...</think>.
                Tuyệt đối không viết suy nghĩ hay lập luận tự do bên ngoài thẻ <think>. Phần trả lời bên ngoài thẻ <think> phải đi thẳng vào vấn đề, không chào hỏi, không dông dài, và phải bắt đầu bằng tiêu đề được yêu cầu. Sử dụng Markdown chuẩn để hiển thị đẹp mắt.
                """
        } else {
            return """
                You are a smart, witty game data analyst for the Tallyo app.
                Your task is to provide brief, concise, and direct observations (maximum 3 bullet points, total under 150 words).
                You MUST use the players' actual names from the game data (never refer to them generically as 'the winner', 'the loser', or 'the player'). Focus your analysis deeply on the individual performance, scores, and rivalries of specific players.
                If you need to think, draft, or reason before answering, you MUST wrap all your thinking/drafting inside <think>...</think> tags.
                Never write free-form thoughts or reasoning outside the <think> tags. The official response outside <think> tags must go straight to the point, avoiding greetings or fluff, and must start with the requested header. Use standard Markdown for beautiful rendering.
                """
        }
    }
}
