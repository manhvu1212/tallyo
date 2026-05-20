package io.github.manhvu1212.tallyo.data

import android.content.Context
import io.github.manhvu1212.tallyo.data.db.TallyoDatabase
import io.github.manhvu1212.tallyo.prefs.LocaleStore

interface AppContainer {
    val repository: SessionRepository
    val localeStore: LocaleStore
}

class AppContainerImpl(context: Context) : AppContainer {
    private val db = TallyoDatabase.get(context)
    override val repository: SessionRepository = SessionRepository(db.sessionDao())
    override val localeStore: LocaleStore = LocaleStore(context.applicationContext)
}
