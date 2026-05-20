package io.github.manhvu1212.tallyo.prefs

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Reads and writes the per-app locale via the platform LocaleManager (API 33+)
 * with an AppCompat-style fallback for older devices.
 *
 * Supported locales: vi, en. Returns the current effective locale code.
 */
class LocaleStore(private val context: Context) {

    fun current(): String {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                .applicationLocales
                .takeIf { !it.isEmpty }
                ?.get(0)
                ?.language
        } else {
            null
        }
        return when (tag) {
            "vi" -> "vi"
            "en" -> "en"
            else -> detectFromSystem()
        }
    }

    fun set(locale: String) {
        val normalized = if (locale == "en") "en" else "vi"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                .applicationLocales = LocaleList.forLanguageTags(normalized)
        } else {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(normalized),
            )
        }
    }

    private fun detectFromSystem(): String {
        val sysLocales: List<Locale> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val list = context.resources.configuration.locales
            (0 until list.size()).map { list.get(it) }
        } else {
            @Suppress("DEPRECATION")
            listOf(context.resources.configuration.locale)
        }
        for (loc in sysLocales) {
            when (loc.language) {
                "vi" -> return "vi"
                "en" -> return "en"
            }
        }
        return "vi"
    }
}
