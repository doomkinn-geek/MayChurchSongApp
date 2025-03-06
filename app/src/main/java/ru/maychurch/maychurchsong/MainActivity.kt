package ru.maychurch.maychurchsong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.ui.MainLayout
import ru.maychurch.maychurchsong.ui.theme.MayChurchSongTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Получаем экземпляр UserPreferences
        val userPreferences = UserPreferences.getInstance()
        
        setContent {
            // Получаем настройки темы из UserPreferences
            val isDarkTheme by userPreferences.isDarkTheme.collectAsState(initial = false)
            val useSystemTheme by userPreferences.useSystemTheme.collectAsState(initial = true)
            
            // Определяем, какую тему использовать
            val darkTheme = if (useSystemTheme) isSystemInDarkTheme() else isDarkTheme
            
            MayChurchSongTheme(
                darkTheme = darkTheme
            ) {
                MainLayout(userPreferences = userPreferences)
            }
        }
    }
}