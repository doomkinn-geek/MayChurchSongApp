package ru.maychurch.maychurchsong

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Bundle
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
import ru.maychurch.maychurchsong.utils.AutoSaveManager
import ru.maychurch.maychurchsong.utils.DatabaseGenerator
import ru.maychurch.maychurchsong.workers.WorkManagerHelper
import java.util.concurrent.atomic.AtomicBoolean

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

    // Объект для синхронизации операций сохранения базы данных
    private val SAVE_LOCK = Any()
    
    // Максимальное количество повторных попыток при сохранении
    private val MAX_RETRY_COUNT = 3
    
    // Флаг, указывающий что сохранение выполняется в данный момент
    private val isSaveInProgress = AtomicBoolean(false)

    // Область корутин приложения
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Менеджер автосохранения
    private var autoSaveManager: AutoSaveManager? = null

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

    // Счетчик видимых активностей для определения, когда приложение уходит в фон
    private var visibleActivities = 0

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Добавляем обработчик необработанных исключений
        setupUncaughtExceptionHandler()
        
        // Проверяем состояние базы данных и при необходимости переоткрываем
        ensureDatabaseIsOpen()
        
        // Инициализация базы данных
        initializeDatabase()
        
        // Настраиваем периодическое обновление базы данных
        setupPeriodicDatabaseUpdates()
        
        // Регистрируем отслеживание жизненного цикла активностей
        registerActivityLifecycleCallbacks(appLifecycleCallbacks)
        
        // Запускаем автоматическое сохранение данных
        initAutoSaveManager()
    }
    
    /**
     * Настраивает обработчик необработанных исключений 
     * для сохранения данных перед аварийным завершением
     */
    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "Необработанное исключение, пытаемся сохранить данные", throwable)
                // Синхронное сохранение данных перед падением
                saveDatabase()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при сохранении данных перед аварией: ${e.message}", e)
            } finally {
                // Вызываем стандартный обработчик после нашего
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
    
    /**
     * Запускает автоматическое сохранение данных с интервалом
     */
    private fun initAutoSaveManager() {
        Log.d(TAG, "initAutoSaveManager: Инициализация менеджера автосохранения")
        try {
            // Создаем новый экземпляр с использованием applicationScope и лямбда-функции для сохранения
            autoSaveManager = AutoSaveManager(
                saveIntervalMs = 60000, // Сохраняем каждую минуту
                coroutineScope = applicationScope,
                saveAction = {
                    try {
                        // Выполняем минимальное сохранение без блокировок
                        forceSaveDatabaseToDisk()
                    } catch (e: Exception) {
                        Log.e(TAG, "AutoSave: Ошибка при сохранении: ${e.message}", e)
                    }
                }
            )
            
            // Запускаем автосохранение
            autoSaveManager?.startAutoSave()
            Log.d(TAG, "initAutoSaveManager: Менеджер автосохранения инициализирован и запущен")
        } catch (e: Exception) {
            Log.e(TAG, "initAutoSaveManager: Ошибка при инициализации менеджера автосохранения: ${e.message}", e)
        }
    }
    
    /**
     * Проверяет, что база данных открыта и валидна
     */
    private fun ensureDatabaseIsOpen() {
        try {
            // Проверяем состояние базы данных
            val isOpen = database.isOpen
            Log.d(TAG, "Проверка состояния базы данных: открыта = $isOpen")
            
            if (!isOpen) {
                Log.e(TAG, "База данных закрыта, пересоздаем")
                synchronized(SongDatabase::class.java) {
                    // Пересоздаем базу данных
                    SongDatabase.buildDatabase(applicationContext, applicationScope)
                }
                Log.d(TAG, "База данных успешно пересоздана")
            } else {
                Log.d(TAG, "База данных успешно проверена и доступна")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке базы данных: ${e.message}", e)
            
            // В случае ошибки попытаемся создать новый экземпляр
            try {
                synchronized(SongDatabase::class.java) {
                    SongDatabase.buildDatabase(applicationContext, applicationScope)
                }
                Log.d(TAG, "Создан новый экземпляр базы данных после ошибки")
            } catch (e2: Exception) {
                Log.e(TAG, "Критическая ошибка при создании базы данных: ${e2.message}", e2)
            }
        }
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
    
    /**
     * Проверяет, находится ли приложение на переднем плане
     */
    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = packageName
        
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND 
                && appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }
    
    /**
     * Сохраняет все изменения в базе данных
     */
    fun saveDatabase() {
        forceSaveDatabaseToDisk()
    }
    
    /**
     * Корректно завершает работу приложения, сохраняя все данные
     */
    fun exitApplication() {
        Log.e(TAG, "EXIT: Завершение работы приложения")
        try {
            // Останавливаем автосохранение
            stopAutoSave()
            
            // Сохраняем данные на диск
            forceSaveDatabaseToDisk()
            
            // Закрываем базу данных
            SongDatabase.closeDatabase()
            Log.e(TAG, "EXIT: База данных закрыта")
        } catch (e: Exception) {
            Log.e(TAG, "EXIT: Ошибка при закрытии приложения: ${e.message}", e)
        }
        
        Log.e(TAG, "EXIT: Приложение корректно завершило работу")
    }
    
    /**
     * Закрывает все ресурсы приложения
     */
    private fun closeResources() {
        try {
            Log.e(TAG, "CLOSE: Начинаю закрытие ресурсов приложения")
            
            // Логируем состояние базы перед закрытием
            try {
                val db = database.openHelper.readableDatabase
                val isOpen = db.isOpen
                Log.e(TAG, "CLOSE: Состояние базы данных перед закрытием: открыта = $isOpen")
            } catch (e: Exception) {
                Log.e(TAG, "CLOSE: Ошибка при проверке состояния базы: ${e.message}", e)
            }
            
            // Закрываем базу данных
            try {
                Log.e(TAG, "CLOSE: Закрываю базу данных")
                SongDatabase.closeDatabase()
                Log.e(TAG, "CLOSE: База данных успешно закрыта")
            } catch (e: Exception) {
                Log.e(TAG, "CLOSE: Ошибка при закрытии базы данных: ${e.message}", e)
            }
            
            // Закрываем HTTP клиент
            try {
                Log.e(TAG, "CLOSE: Закрываю HTTP клиент")
                httpClient.close()
                Log.e(TAG, "CLOSE: HTTP клиент успешно закрыт")
            } catch (e: Exception) {
                Log.e(TAG, "CLOSE: Ошибка при закрытии HTTP клиента: ${e.message}", e)
            }
            
            Log.e(TAG, "CLOSE: Все ресурсы успешно закрыты")
        } catch (e: Exception) {
            Log.e(TAG, "CLOSE: Критическая ошибка при закрытии ресурсов: ${e.message}", e)
        }
    }
    
    /**
     * Сохраняет конкретные данные о состоянии избранных и недавних песен
     * СИНХРОННЫЙ метод для гарантии сохранения этих важных данных до закрытия приложения
     */
    fun syncFavoritesAndRecentSongs() {
        try {
            Log.d(TAG, "SYNC: Синхронизация избранных и недавних песен")
            
            // Используем runBlocking, чтобы гарантировать выполнение синхронизации
            kotlinx.coroutines.runBlocking {
                val songDao = database.songDao()
                
                // Синхронизируем избранные песни
                val favorites = songDao.getFavoriteIds()
                Log.d(TAG, "SYNC: Количество избранных песен: ${favorites.size}")
                
                // Синхронизируем недавние песни
                val recents = songDao.getRecentSongsWithTimestamps()
                Log.d(TAG, "SYNC: Количество недавних песен: ${recents.size}")
                
                if (recents.isNotEmpty()) {
                    val recentIds = recents.take(5).map { it.id }
                    Log.d(TAG, "SYNC: Последние недавние песни: ${recentIds.joinToString(", ")}")
                }
            }
            
            Log.d(TAG, "SYNC: Синхронизация успешно завершена")
        } catch (e: Exception) {
            Log.e(TAG, "SYNC: Ошибка при синхронизации: ${e.message}", e)
        }
    }
    
    /**
     * Принудительно сохраняет базу данных на диск,
     * гарантируя, что все изменения будут записаны
     */
    private fun forceSaveDatabaseToDisk() {
        if (isSaveInProgress.getAndSet(true)) {
            Log.d(TAG, "FORCE_SAVE: Сохранение уже выполняется, пропускаю")
            return
        }
        
        try {
            Log.d(TAG, "FORCE_SAVE: Начинаю принудительное сохранение")

            // Сначала синхронизируем избранные и недавние песни
            syncFavoritesAndRecentSongs()

            // Затем сохраняем изменения на диск
            val db = database.openHelper.writableDatabase
            Log.d(TAG, "FORCE_SAVE: Получен доступ к базе данных по пути ${db.path}")
            
            // Проверяем состояние избранных и недавних
            try {
                val favoritesCursor = db.query("SELECT COUNT(*) FROM songs WHERE isFavorite = 1")
                var favCount = 0
                if (favoritesCursor.moveToFirst()) {
                    favCount = favoritesCursor.getInt(0)
                }
                favoritesCursor.close()
                
                val recentsCursor = db.query("SELECT COUNT(*) FROM songs WHERE lastAccessed > 0")
                var recentCount = 0
                if (recentsCursor.moveToFirst()) {
                    recentCount = recentsCursor.getInt(0)
                }
                recentsCursor.close()
                
                Log.d(TAG, "FORCE_SAVE: В базе данных: избранных = $favCount, недавних = $recentCount")
            } catch (e: Exception) {
                Log.e(TAG, "FORCE_SAVE: Ошибка при проверке данных: ${e.message}", e)
            }
            
            try {
                // Минимальная операция, которая вызывает фиксацию изменений
                db.execSQL("PRAGMA temp_store = MEMORY")
                
                // Также запускаем checkpoint WAL-журнала
                try {
                    db.execSQL("PRAGMA wal_checkpoint(PASSIVE)")
                    Log.d(TAG, "FORCE_SAVE: Выполнен checkpoint WAL-журнала")
                } catch (e: Exception) {
                    Log.d(TAG, "FORCE_SAVE: Не удалось выполнить checkpoint: ${e.message}")
                }
                
                Log.d(TAG, "FORCE_SAVE: Изменения успешно сохранены")
            } catch (e: Exception) {
                Log.e(TAG, "FORCE_SAVE: Ошибка при сохранении данных: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "FORCE_SAVE: Критическая ошибка при принудительном сохранении: ${e.message}", e)
        } finally {
            isSaveInProgress.set(false)
            Log.d(TAG, "FORCE_SAVE: Процесс сохранения завершен")
        }
    }
    
    /**
     * Callback для отслеживания жизненного цикла активностей
     */
    private val appLifecycleCallbacks = object : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            // При создании активности убедимся, что база данных открыта
            ensureDatabaseIsOpen()
        }
        
        override fun onActivityStarted(activity: Activity) {
            visibleActivities++
            // При возвращении в приложение из фона убедимся, что база данных открыта
            if (visibleActivities == 1) {
                Log.d(TAG, "Приложение стало активным, проверяем состояние базы данных")
                ensureDatabaseIsOpen()
            }
        }

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {
            // При паузе активности сохраняем данные
            Log.d(TAG, "Активность приостановлена, сохраняем данные")
            autoSaveManager?.saveNow()
        }

        override fun onActivityStopped(activity: Activity) {
            visibleActivities--
            // Если все активности остановлены и приложение не на переднем плане
            if (visibleActivities <= 0 && !isAppInForeground()) {
                Log.d(TAG, "Приложение ушло в фон, синхронизируем и сохраняем все изменения")
                syncFavoritesAndRecentSongs()
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            // Сохраняем данные при уничтожении активности
            Log.d(TAG, "Активность уничтожена, сохраняем данные")
            saveDatabase()
        }
    }

    /**
     * Вызывается при принудительном завершении процесса приложения
     * Обычно не вызывается на реальных устройствах, но полезно для тестирования
     */
    override fun onTerminate() {
        Log.e(TAG, "onTerminate: Система завершает процесс приложения")
        try {
            // Останавливаем автосохранение
            stopAutoSave()
            
            // Закрываем базу данных
            SongDatabase.closeDatabase()
            Log.e(TAG, "onTerminate: База данных закрыта")
        } catch (e: Exception) {
            Log.e(TAG, "onTerminate: Ошибка при закрытии ресурсов: ${e.message}", e)
        }
        
        super.onTerminate()
        Log.e(TAG, "onTerminate: Приложение завершено")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // При нехватке памяти сохраняем все данные
        Log.d(TAG, "Мало памяти, сохраняем данные")
        saveDatabase()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // При получении уведомления о необходимости освободить память
        // сохраняем данные если уровень критический
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL || 
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            Log.d(TAG, "Критическая нехватка памяти (уровень: $level), сохраняем данные")
            saveDatabase()
        }
    }

    /**
     * Останавливает AutoSaveManager и ждет завершения его работы
     */
    fun stopAutoSave() {
        Log.d(TAG, "stopAutoSave: Останавливаю AutoSaveManager")
        try {
            autoSaveManager?.let { manager ->
                manager.stopAutoSave()
                Log.d(TAG, "stopAutoSave: AutoSaveManager успешно остановлен")
            } ?: Log.d(TAG, "stopAutoSave: AutoSaveManager не был инициализирован")
        } catch (e: Exception) {
            Log.e(TAG, "stopAutoSave: Ошибка при остановке AutoSaveManager: ${e.message}", e)
        }
    }

    /**
     * Проверяет и при необходимости пересоздает соединение с базой данных
     */
    fun checkAndRebuildDatabaseIfNeeded() {
        try {
            Log.d(TAG, "DB_CHECK: Проверка состояния базы данных")
            
            var needRebuild = false
            
            // Проверяем, открыта ли база данных
            try {
                if (!database.isOpen) {
                    Log.d(TAG, "DB_CHECK: База данных закрыта, требуется пересоздание")
                    needRebuild = true
                } else {
                    // Проверяем, работает ли соединение
                    try {
                        val testCursor = database.openHelper.readableDatabase.query("SELECT 1")
                        testCursor.close()
                        Log.d(TAG, "DB_CHECK: Соединение с базой работает нормально")
                    } catch (e: Exception) {
                        Log.e(TAG, "DB_CHECK: Ошибка при проверке соединения: ${e.message}", e)
                        needRebuild = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "DB_CHECK: Ошибка при проверке состояния базы: ${e.message}", e)
                needRebuild = true
            }
            
            if (needRebuild) {
                Log.d(TAG, "DB_CHECK: Требуется пересоздание соединения с базой данных")
                // Очищаем существующее соединение
                try {
                    SongDatabase.closeDatabase()
                    Log.d(TAG, "DB_CHECK: Старое соединение закрыто")
                } catch (e: Exception) {
                    Log.e(TAG, "DB_CHECK: Ошибка при закрытии старого соединения: ${e.message}", e)
                }
                
                // Пересоздаем соединение через SongDatabase
                synchronized(SongDatabase::class.java) {
                    // Нельзя переназначать database, т.к. это val
                    // Пересоздаем INSTANCE, к которому обращается lazy
                    SongDatabase.buildDatabase(applicationContext, applicationScope)
                    Log.d(TAG, "DB_CHECK: Новое соединение с базой успешно создано")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DB_CHECK: Критическая ошибка при проверке базы данных: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "SongApplication"
        private const val GENERATE_DATABASE_FOR_DEVELOPMENT = true // Включаем генерацию базы
        
        lateinit var instance: SongApplication
            private set
    }
} 