package io.github.manhvu1212.tallyo.ui

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.manhvu1212.tallyo.data.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
