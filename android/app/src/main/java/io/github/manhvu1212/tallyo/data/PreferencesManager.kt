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

    val geminiApiKey: Flow<String?> = appContext.dataStore.data.map { preferences ->
        preferences[GEMINI_API_KEY]
    }

    val groqApiKey: Flow<String?> = appContext.dataStore.data.map { preferences ->
        preferences[GROQ_API_KEY]
    }

    val selectedAiProvider: Flow<String> = appContext.dataStore.data.map { preferences ->
        preferences[SELECTED_AI_PROVIDER] ?: "gemini"
    }

    suspend fun saveGeminiApiKey(key: String) {
        appContext.dataStore.edit { preferences ->
            preferences[GEMINI_API_KEY] = key
        }
    }

    suspend fun clearGeminiApiKey() {
        appContext.dataStore.edit { preferences ->
            preferences.remove(GEMINI_API_KEY)
        }
    }

    suspend fun saveGroqApiKey(key: String) {
        appContext.dataStore.edit { preferences ->
            preferences[GROQ_API_KEY] = key
        }
    }

    suspend fun clearGroqApiKey() {
        appContext.dataStore.edit { preferences ->
            preferences.remove(GROQ_API_KEY)
        }
    }

    suspend fun saveSelectedAiProvider(provider: String) {
        appContext.dataStore.edit { preferences ->
            preferences[SELECTED_AI_PROVIDER] = provider
        }
    }
}
