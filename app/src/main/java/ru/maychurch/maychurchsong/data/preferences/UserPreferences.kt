package ru.maychurch.maychurchsong.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.maychurch.maychurchsong.SongApplication

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences {
    private val dataStore = SongApplication.instance.dataStore
    
    // Ключи для преференсов
    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val USE_SYSTEM_THEME_KEY = booleanPreferencesKey("use_system_theme")
        private val FONT_SIZE_KEY = intPreferencesKey("font_size")
        private val INTERFACE_FONT_SIZE_KEY = intPreferencesKey("interface_font_size")
        private val AUTO_UPDATE_ENABLED_KEY = booleanPreferencesKey("auto_update_enabled")
        private val UPDATE_INTERVAL_HOURS_KEY = longPreferencesKey("update_interval_hours")
        private val LAST_UPDATE_TIME_KEY = longPreferencesKey("last_update_time")
        private val FIRST_LAUNCH_KEY = booleanPreferencesKey("is_first_launch")
        
        // Размеры шрифта
        const val FONT_SIZE_SMALL = 0
        const val FONT_SIZE_MEDIUM = 1
        const val FONT_SIZE_LARGE = 2
        
        // Интервалы обновления (в часах)
        const val UPDATE_INTERVAL_12_HOURS = 12L
        const val UPDATE_INTERVAL_24_HOURS = 24L
        const val UPDATE_INTERVAL_48_HOURS = 48L
        const val UPDATE_INTERVAL_WEEK = 168L // 7 дней
        
        // Значение по умолчанию для интервала обновления
        const val DEFAULT_UPDATE_INTERVAL = UPDATE_INTERVAL_WEEK
        
        // Singleton instance
        @Volatile
        private var INSTANCE: UserPreferences? = null
        
        fun getInstance(): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferences()
                INSTANCE = instance
                instance
            }
        }
    }
    
    // Получить настройку темы (true - темная, false - светлая)
    val isDarkTheme: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DARK_THEME_KEY] ?: false
    }
    
    // Использовать ли системную тему
    val useSystemTheme: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[USE_SYSTEM_THEME_KEY] ?: true
    }
    
    // Получить размер шрифта для текста песен
    val fontSize: Flow<Int> = dataStore.data.map { preferences ->
        preferences[FONT_SIZE_KEY] ?: FONT_SIZE_MEDIUM
    }
    
    // Получить размер шрифта для интерфейса
    val interfaceFontSize: Flow<Int> = dataStore.data.map { preferences ->
        preferences[INTERFACE_FONT_SIZE_KEY] ?: FONT_SIZE_MEDIUM
    }
    
    // Включено ли автоматическое обновление
    val isAutoUpdateEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_UPDATE_ENABLED_KEY] ?: true
    }
    
    // Интервал автоматического обновления (в часах)
    val updateIntervalHours: Flow<Long> = dataStore.data.map { preferences ->
        preferences[UPDATE_INTERVAL_HOURS_KEY] ?: DEFAULT_UPDATE_INTERVAL
    }
    
    // Время последнего обновления базы данных (в миллисекундах)
    val lastUpdateTime: Flow<Long> = dataStore.data.map { preferences ->
        preferences[LAST_UPDATE_TIME_KEY] ?: 0L
    }
    
    // Проверить, является ли это первым запуском приложения
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH_KEY] ?: true
    }
    
    // Установить тему
    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = isDark
        }
    }
    
    // Установить использование системной темы
    suspend fun setUseSystemTheme(useSystem: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_SYSTEM_THEME_KEY] = useSystem
        }
    }
    
    // Установить размер шрифта для текста песен
    suspend fun setFontSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = size
        }
    }
    
    // Установить размер шрифта для интерфейса
    suspend fun setInterfaceFontSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[INTERFACE_FONT_SIZE_KEY] = size
        }
    }
    
    // Включить/выключить автоматическое обновление
    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_UPDATE_ENABLED_KEY] = enabled
        }
    }
    
    // Установить интервал обновления
    suspend fun setUpdateIntervalHours(hours: Long) {
        dataStore.edit { preferences ->
            preferences[UPDATE_INTERVAL_HOURS_KEY] = hours
        }
    }
    
    // Установить время последнего обновления
    suspend fun setLastUpdateTime(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_UPDATE_TIME_KEY] = timestamp
        }
    }
    
    // Установить флаг первого запуска
    suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = false
        }
    }
} 