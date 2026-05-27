import Foundation
import Combine

class PreferencesManager: ObservableObject {
    static let shared = PreferencesManager()
    
    private let defaults = UserDefaults.standard
    
    private let geminiKey = "gemini_api_key"
    private let groqKey = "groq_api_key"
    private let selectedProviderKey = "selected_ai_provider"
    
    @Published var selectedAiProvider: String {
        didSet {
            defaults.set(selectedAiProvider, forKey: selectedProviderKey)
        }
    }
    
    init() {
        self.selectedAiProvider = defaults.string(forKey: selectedProviderKey) ?? "gemini"
    }
    
    func getApiKey(providerId: String) -> String? {
        switch providerId {
        case "gemini":
            return defaults.string(forKey: geminiKey)
        case "groq":
            return defaults.string(forKey: groqKey)
        default:
            return defaults.string(forKey: "api_key_\(providerId)")
        }
    }
    
    func saveApiKey(providerId: String, key: String) {
        let trimmed = key.trimmingCharacters(in: .whitespacesAndNewlines)
        switch providerId {
        case "gemini":
            defaults.set(trimmed, forKey: geminiKey)
        case "groq":
            defaults.set(trimmed, forKey: groqKey)
        default:
            defaults.set(trimmed, forKey: "api_key_\(providerId)")
        }
        objectWillChange.send()
    }
    
    func clearApiKey(providerId: String) {
        switch providerId {
        case "gemini":
            defaults.removeObject(forKey: geminiKey)
        case "groq":
            defaults.removeObject(forKey: groqKey)
        default:
            defaults.removeObject(forKey: "api_key_\(providerId)")
        }
        objectWillChange.send()
    }
    
    var allApiKeys: [String: String] {
        var keys: [String: String] = [:]
        if let gemini = getApiKey(providerId: "gemini"), !gemini.isEmpty {
            keys["gemini"] = gemini
        }
        if let groq = getApiKey(providerId: "groq"), !groq.isEmpty {
            keys["groq"] = groq
        }
        if let openai = getApiKey(providerId: "openai"), !openai.isEmpty {
            keys["openai"] = openai
        }
        return keys
    }
}
