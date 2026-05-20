package io.github.manhvu1212.tallyo

import android.app.Application
import io.github.manhvu1212.tallyo.data.AppContainer
import io.github.manhvu1212.tallyo.data.AppContainerImpl

class TallyoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
    }
}
