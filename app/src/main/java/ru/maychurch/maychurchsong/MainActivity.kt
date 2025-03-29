package ru.maychurch.maychurchsong

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.maychurch.maychurchsong.data.database.SongDatabase
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.ui.MainLayout
import ru.maychurch.maychurchsong.ui.navigation.Screen
import ru.maychurch.maychurchsong.ui.theme.MayChurchSongTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    
    private val userPreferences by lazy { UserPreferences.getInstance() }
    private val TAG = "MainActivity"
    
    // Для обработки двойного нажатия "назад"
    private var backPressedTime: Long = 0
    private val backToExitPeriod: Long = 2000 // 2 секунды для повторного нажатия
    private var currentRoute: String = Screen.Home.route
    private var navController: NavHostController? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Настраиваем обработчик кнопки "Назад" для корректного выхода из приложения
        setupBackPressHandler()
        
        // Убедимся, что база данных открыта и валидна
        ensureDatabaseIsOpen()
        
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
            
            // Запоминаем NavController для отслеживания текущего экрана
            val navController = rememberNavController()
            this.navController = navController
            
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
                    MainLayout(
                        userPreferences = userPreferences,
                        onExitAppRequest = { exitApplication() },
                        onCurrentRouteChange = { route -> 
                            currentRoute = route
                            Log.d(TAG, "Текущий маршрут: $route")
                        },
                        navController = navController
                    )
                }
            }
        }
    }
    
    /**
     * Убеждается, что база данных открыта и валидна
     */
    private fun ensureDatabaseIsOpen() {
        try {
            val app = SongApplication.instance
            // Вместо вызова проблемного метода reopenIfClosed создаем базу заново
            if (!app.database.isOpen) {
                Log.e(TAG, "База данных закрыта, пересоздаем экземпляр")
                SongDatabase.buildDatabase(applicationContext, app.applicationScope)
            } else {
                Log.d(TAG, "База данных успешно проверена и доступна")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке базы данных: ${e.message}", e)
            // В случае ошибки попытаемся создать новый экземпляр базы
            try {
                val app = SongApplication.instance
                SongDatabase.buildDatabase(applicationContext, app.applicationScope)
                Log.d(TAG, "Создан новый экземпляр базы данных")
            } catch (e2: Exception) {
                Log.e(TAG, "Критическая ошибка при создании базы данных: ${e2.message}", e2)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // При возвращении в активность проверяем состояние базы данных
        ensureDatabaseIsOpen()
    }
    
    /**
     * Настраивает обработчик кнопки "Назад"
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Если мы находимся на главном экране (Home)
                if (currentRoute == Screen.Home.route) {
                    // Обрабатываем двойное нажатие для выхода
                    if (backPressedTime + backToExitPeriod > System.currentTimeMillis()) {
                        // Второе нажатие в течение периода - выходим
                        exitApplication()
                    } else {
                        // Первое нажатие - показываем сообщение
                        Toast.makeText(
                            this@MainActivity,
                            "Нажмите ещё раз для выхода",
                            Toast.LENGTH_SHORT
                        ).show()
                        backPressedTime = System.currentTimeMillis()
                    }
                } else {
                    // На других экранах - просто возвращаемся назад
                    navController?.navigateUp()
                    
                    // Если навигация не удалась, выполняем стандартное действие
                    if (navController?.currentDestination?.route == currentRoute) {
                        saveAndFinish()
                    }
                }
            }
        })
    }
    
    /**
     * Сохраняет все изменения и завершает работу приложения
     */
    private fun saveAndFinish() {
        try {
            // Сохраняем данные о избранных и недавних песнях перед выходом
            Log.d(TAG, "Выход из приложения по кнопке 'Назад', сохраняем данные")
            val app = SongApplication.instance
            
            // Используем единый метод сохранения базы данных
            app.saveDatabase()
            
            Log.d(TAG, "Данные успешно сохранены, завершаем приложение")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сохранении данных: ${e.message}", e)
        } finally {
            // Завершаем активность
            finish()
        }
    }
    
    /**
     * Принудительно завершает работу приложения с сохранением всех данных
     */
    fun exitApplication() {
        Log.e(TAG, "exitApplication: Запрошен выход из приложения")
        
        try {
            val app = application as SongApplication
            
            // Останавливаем AutoSaveManager
            app.stopAutoSave()
            Log.e(TAG, "exitApplication: AutoSaveManager остановлен")
            
            // Проверяем состояние базы данных перед выходом
            app.checkAndRebuildDatabaseIfNeeded()
            
            // Сохраняем данные в отдельном потоке с таймаутом
            Thread {
                try {
                    // Принудительно сохраняем данные
                    app.saveDatabase()
                    Log.e(TAG, "exitApplication: Данные сохранены")
                    
                    // Затем закрываем базу данных
                    SongDatabase.closeDatabase()
                    Log.e(TAG, "exitApplication: База данных успешно закрыта")
                } catch (e: Exception) {
                    Log.e(TAG, "exitApplication: Ошибка при закрытии базы данных: ${e.message}", e)
                }
            }.apply {
                isDaemon = false // Не демон, чтобы гарантировать выполнение
                start()
                try {
                    join(5000) // Ждем максимум 5 секунд
                    Log.e(TAG, "exitApplication: Поток сохранения завершен")
                } catch (e: InterruptedException) {
                    Log.e(TAG, "exitApplication: Превышено время ожидания закрытия базы данных", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "exitApplication: Ошибка при подготовке к выходу: ${e.message}", e)
        }
        
        Log.e(TAG, "exitApplication: Завершаю активность и приложение")
        finish()
        System.exit(0)
    }

    override fun onPause() {
        super.onPause()
        try {
            // Принудительно сохраняем данные при сворачивании приложения
            Log.d(TAG, "Активность приостановлена, сохраняем данные")
            val app = SongApplication.instance
            app.saveDatabase()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сохранении данных: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy: Активность уничтожается")
        
        // Убедимся, что сохранили все изменения
        try {
            val app = application as SongApplication
            
            // Останавливаем AutoSaveManager
            app.stopAutoSave()
            Log.e(TAG, "onDestroy: AutoSaveManager остановлен")
            
            // Проверяем состояние базы данных
            app.checkAndRebuildDatabaseIfNeeded()
            
            // Принудительно сохраняем изменения
            Thread {
                try {
                    // Принудительно сохраняем данные
                    app.saveDatabase()
                    Log.e(TAG, "onDestroy: Данные сохранены")
                    
                    // Если это final onDestroy (активность не пересоздается), закрываем базу данных
                    if (isFinishing) {
                        SongDatabase.closeDatabase()
                        Log.e(TAG, "onDestroy: База данных успешно закрыта")
                    } else {
                        Log.e(TAG, "onDestroy: Активность пересоздается, база данных не закрывается")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onDestroy: Ошибка при сохранении данных: ${e.message}", e)
                }
            }.apply {
                isDaemon = false // Не демон, чтобы гарантировать выполнение
                start()
                try {
                    if (isFinishing) {
                        join(3000) // Ждем максимум 3 секунды при завершении
                    } else {
                        join(1000) // Ждем меньше при пересоздании
                    }
                    Log.e(TAG, "onDestroy: Поток сохранения завершен")
                } catch (e: InterruptedException) {
                    Log.e(TAG, "onDestroy: Превышено время ожидания сохранения данных", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy: Ошибка при сохранении изменений: ${e.message}", e)
        }
        
        Log.e(TAG, "onDestroy: Активность полностью уничтожена")
    }
}