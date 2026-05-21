package io.github.manhvu1212.tallyo.data

import android.content.Context
import io.github.manhvu1212.tallyo.data.db.TallyoDatabase

interface AppContainer {
    val repository: SessionRepository
    val preferencesManager: PreferencesManager
    val aiStatsService: AiStatsService
}

class AppContainerImpl(context: Context) : AppContainer {
    private val db = TallyoDatabase.get(context)
    override val repository: SessionRepository = SessionRepository(db.sessionDao())
    override val preferencesManager: PreferencesManager = PreferencesManager(context)
    override val aiStatsService: AiStatsService = AiStatsService()
}
