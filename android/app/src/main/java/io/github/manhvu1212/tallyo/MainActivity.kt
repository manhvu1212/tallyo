package io.github.manhvu1212.tallyo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import io.github.manhvu1212.tallyo.ui.nav.TallyoNavHost
import io.github.manhvu1212.tallyo.ui.theme.TallyoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Restore the regular theme after the splash window background.
        setTheme(R.style.Theme_Tallyo)
        val container = (application as TallyoApp).container
        setContent {
            TallyoTheme {
                TallyoNavHost(container = container)
            }
        }
    }
}
