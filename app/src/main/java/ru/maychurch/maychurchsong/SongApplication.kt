package ru.maychurch.maychurchsong

import android.app.Application
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.maychurch.maychurchsong.data.database.SongDatabase
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.utils.DatabaseGenerator
import ru.maychurch.maychurchsong.workers.WorkManagerHelper

class SongApplication : Application() {

    // Клиент для HTTP запросов
    val httpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    // Область корутин приложения
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // База данных
    val database by lazy {
        // Проверяем наличие предзаполненной базы данных
        try {
            if (SongDatabase.prepopulatedDatabaseExists(applicationContext)) {
                Log.d(TAG, "Using prepopulated database from assets: файл найден")
                // Проверяем размер файла
                try {
                    val assetManager = applicationContext.assets
                    val inputStream = assetManager.open("database/prepopulated_songs.db")
                    val size = inputStream.available()
                    inputStream.close()
                    Log.d(TAG, "Размер файла предзаполненной базы: $size байт")
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при проверке размера базы: ${e.message}")
                }
                
                val result = SongDatabase.copyPrepopulatedDatabase(applicationContext)
                if (result) {
                    Log.d(TAG, "Копирование базы из assets успешно выполнено")
                } else {
                    Log.e(TAG, "Ошибка при копировании базы из assets")
                }
                
                SongDatabase.buildDatabase(applicationContext, applicationScope)
            } else {
                Log.e(TAG, "Prepopulated database not found, building empty database. Path: ${SongDatabase.getDatabasePath(applicationContext)}")
                // Если предзаполненной базы нет, используем обычную инициализацию
                SongDatabase.buildDatabase(applicationContext, applicationScope)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при инициализации базы данных: ${e.message}", e)
            SongDatabase.buildDatabase(applicationContext, applicationScope)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Инициализация базы данных
        initializeDatabase()
        
        // Настраиваем периодическое обновление базы данных
        setupPeriodicDatabaseUpdates()
    }
    
    private fun initializeDatabase() {
        applicationScope.launch {
            try {
                // Проверяем возможность создания базы данных для разработки
                if (isDebugMode() && GENERATE_DATABASE_FOR_DEVELOPMENT) {
                    Log.d(TAG, "Development mode: Starting database generation")
                    val result = DatabaseGenerator.generateAndSaveToAssets(applicationContext)
                    if (result) {
                        Log.d(TAG, "Database generation completed successfully")
                    } else {
                        Log.e(TAG, "Database generation failed")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in database initialization: ${e.message}")
            }
        }
    }
    
    /**
     * Настраивает периодическое обновление базы данных песен
     */
    private fun setupPeriodicDatabaseUpdates() {
        // Используем корутину для доступа к Flow
        applicationScope.launch {
            try {
                // Получаем настройки пользователя
                val userPreferences = UserPreferences.getInstance()
                val isAutoUpdateEnabled = userPreferences.isAutoUpdateEnabled.first()
                val updateIntervalHours = userPreferences.updateIntervalHours.first()
                
                if (isAutoUpdateEnabled) {
                    WorkManagerHelper.schedulePeriodicSongUpdate(
                        applicationContext,
                        updateIntervalHours
                    )
                    Log.d(TAG, "Периодические обновления базы данных настроены с интервалом $updateIntervalHours часов")
                } else {
                    Log.d(TAG, "Автоматические обновления отключены в настройках")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при настройке периодических обновлений: ${e.message}", e)
                // В случае ошибки используем значения по умолчанию
                WorkManagerHelper.schedulePeriodicSongUpdate(applicationContext)
            }
        }
    }
    
    // Проверка режима отладки
    private fun isDebugMode(): Boolean {
        return try {
            // Проверяем режим отладки
            val appInfo = applicationContext.packageManager.getApplicationInfo(applicationContext.packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            // В случае ошибки возвращаем false
            false
        }
    }

    companion object {
        private const val TAG = "SongApplication"
        private const val GENERATE_DATABASE_FOR_DEVELOPMENT = true // Включаем генерацию базы
        
        lateinit var instance: SongApplication
            private set
    }
} 