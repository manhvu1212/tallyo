package io.github.manhvu1212.tallyo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tallyo_settings")

class PreferencesManager(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        private val SELECTED_AI_PROVIDER = stringPreferencesKey("selected_ai_provider")
    }

    private fun getPrefKeyForProvider(providerId: String): Preferences.Key<String> {
        return when (providerId) {
            "gemini" -> GEMINI_API_KEY
            "groq" -> GROQ_API_KEY
            else -> stringPreferencesKey("api_key_$providerId")
        }
    }

    val allApiKeys: Flow<Map<String, String>> = appContext.dataStore.data.map { preferences ->
        val keysMap = mutableMapOf<String, String>()
        
        // Check legacy keys
        preferences[GEMINI_API_KEY]?.let { if (it.isNotBlank()) keysMap["gemini"] = it }
        preferences[GROQ_API_KEY]?.let { if (it.isNotBlank()) keysMap["groq"] = it }
        
        // Scan dynamic keys
        preferences.asMap().forEach { (key, value) ->
            if (key.name.startsWith("api_key_") && value is String && value.isNotBlank()) {
                val providerId = key.name.removePrefix("api_key_")
                keysMap[providerId] = value
            }
        }
        
        keysMap
    }

    fun getApiKey(providerId: String): Flow<String?> = appContext.dataStore.data.map { preferences ->
        preferences[getPrefKeyForProvider(providerId)]
    }

    suspend fun saveApiKey(providerId: String, key: String) {
        appContext.dataStore.edit { preferences ->
            preferences[getPrefKeyForProvider(providerId)] = key
        }
    }

    suspend fun clearApiKey(providerId: String) {
        appContext.dataStore.edit { preferences ->
            preferences.remove(getPrefKeyForProvider(providerId))
        }
    }

    val geminiApiKey: Flow<String?> = getApiKey("gemini")
    val groqApiKey: Flow<String?> = getApiKey("groq")

    val selectedAiProvider: Flow<String> = appContext.dataStore.data.map { preferences ->
        preferences[SELECTED_AI_PROVIDER] ?: "gemini"
    }

    suspend fun saveGeminiApiKey(key: String) = saveApiKey("gemini", key)
    suspend fun clearGeminiApiKey() = clearApiKey("gemini")
    suspend fun saveGroqApiKey(key: String) = saveApiKey("groq", key)
    suspend fun clearGroqApiKey() = clearApiKey("groq")

    suspend fun saveSelectedAiProvider(provider: String) {
        appContext.dataStore.edit { preferences ->
            preferences[SELECTED_AI_PROVIDER] = provider
        }
    }
}
