package com.alokchitri.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat
import com.alokchitri.app.ui.screens.AppLanguage
import com.alokchitri.app.ui.theme.AlokTheme

/** AppCompatActivity (not ComponentActivity) so AppCompatDelegate can switch the app language on any Android version. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            AlokTheme {
                AlokApp(
                    language = currentLanguage(),
                    onLanguageChange = { lang ->
                        // Recreates the activity with the new strings. Bangla is the default resource set.
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(if (lang == AppLanguage.EN) "en" else "bn"),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun currentLanguage(): AppLanguage =
    if (LocalConfiguration.current.locales[0]?.language == "en") AppLanguage.EN else AppLanguage.BN
