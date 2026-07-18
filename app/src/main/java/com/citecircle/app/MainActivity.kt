package com.citecircle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citecircle.app.core.data.AppTheme
import com.citecircle.app.core.data.ThemeRepository
import com.citecircle.app.core.designsystem.BrightScholarTheme
import com.citecircle.app.core.navigation.CiteCircleNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themeRepository: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by themeRepository.getTheme().collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
            val isSystemDark = isSystemInDarkTheme()

            val isDark = when (theme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> isSystemDark
            }

            var isDarkTheme by remember { mutableStateOf(isDark) }

            LaunchedEffect(isDark) {
                isDarkTheme = isDark
            }

            BrightScholarTheme(darkTheme = isDarkTheme) {
                CiteCircleNavHost(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { newDark ->
                        isDarkTheme = newDark
                    }
                )
            }
        }
    }
}
