package io.github.manhvu1212.tallyo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.manhvu1212.tallyo.data.AppContainer
import io.github.manhvu1212.tallyo.data.AppContainerImpl

class TallyoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Language follows the system; clear any per-app override left over
        // from prior versions that exposed an in-app language picker.
        if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
        container = AppContainerImpl(this)
    }
}
