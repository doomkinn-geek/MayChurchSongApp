package ru.maychurch.maychurchsong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.ui.MainLayout
import ru.maychurch.maychurchsong.ui.theme.MayChurchSongTheme

class MainActivity : ComponentActivity() {
    
    private val userPreferences by lazy { UserPreferences.getInstance() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // Получаем настройки для темы
            val isDarkTheme by userPreferences.isDarkTheme.collectAsState(initial = false)
            val useSystemTheme by userPreferences.useSystemTheme.collectAsState(initial = true)
            
            // Применяем темную тему в зависимости от настроек
            val darkTheme = if (useSystemTheme) {
                androidx.compose.foundation.isSystemInDarkTheme()
            } else {
                isDarkTheme
            }
            
            // Используем тему с передачей userPreferences для корректного применения размера шрифта интерфейса
            MayChurchSongTheme(
                darkTheme = darkTheme,
                userPreferences = userPreferences
            ) {
                // Поверхность на весь экран
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainLayout(userPreferences = userPreferences)
                }
            }
        }
    }
}