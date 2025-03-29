package ru.maychurch.maychurchsong.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.maychurch.maychurchsong.data.model.Song
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Database(entities = [Song::class], version = 1, exportSchema = false)
abstract class SongDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    
    companion object {
        private const val DATABASE_NAME = "songs_database"
        private var INSTANCE: SongDatabase? = null
        
        /**
         * Создает базу данных в памяти для генерации предзаполненной базы
         */
        fun createInMemory(context: Context): SongDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                SongDatabase::class.java
            ).build()
        }
        
        /**
         * Создает базу данных на диске с указанным путем к файлу
         */
        fun createDatabaseWithName(context: Context, databasePath: String): SongDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SongDatabase::class.java,
                databasePath
            ).build()
        }
        
        /**
         * Создает базу данных с предзаполнением из assets
         */
        fun buildDatabase(context: Context, scope: CoroutineScope): SongDatabase {
            // Если у нас уже есть экземпляр, возвращаем его, предварительно проверив
            // не закрыт ли он
            synchronized(SongDatabase::class.java) {
                var instance = INSTANCE
                if (instance != null) {
                    // Проверяем, открыта ли БД и валидна ли
                    if (instance.isOpen && isValidInstance(instance)) {
                        return instance
                    }
                }
                
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    SongDatabase::class.java,
                    DATABASE_NAME
                )
                
                try {
                    // Проверяем, существует ли файл в assets
                    if (prepopulatedDatabaseExists(context)) {
                        // Пытаемся использовать предзаполненную базу из assets
                        builder.createFromAsset("database/prepopulated_songs.db")
                        android.util.Log.d("SongDatabase", "Настроено создание базы из ассетов: database/prepopulated_songs.db")
                    } else {
                        android.util.Log.e("SongDatabase", "Файл базы в ассетах не найден!")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SongDatabase", "Ошибка при настройке создания из ассетов: ${e.message}")
                }
                
                // Настройка миграции и callback
                builder.fallbackToDestructiveMigration()
                    // Установка режима записи БД, чтобы изменения сохранялись немедленно
                    // WAL (Write-Ahead Logging) обеспечивает более надежную фиксацию изменений
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    // Разрешаем операции с базой данных в основном потоке для гарантии синхронизации
                    // и предотвращения потери данных при закрытии приложения
                    .allowMainThreadQueries()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            android.util.Log.d("SongDatabase", "onCreate callback запущен")
                            // Если предзаполненная база не найдена, запустится этот callback
                            scope.launch(Dispatchers.IO) {
                                // При необходимости здесь можно добавить дополнительную инициализацию базы
                            }
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            android.util.Log.d("SongDatabase", "onOpen callback запущен")
                            // Проверим количество записей в базе данных
                            try {
                                val cursor = db.query("SELECT COUNT(*) FROM songs")
                                if (cursor.moveToFirst()) {
                                    val count = cursor.getLong(0)
                                    android.util.Log.d("SongDatabase", "Количество песен в базе: $count")
                                }
                                cursor.close()
                            } catch (e: Exception) {
                                android.util.Log.e("SongDatabase", "Ошибка при чтении количества записей: ${e.message}")
                            }
                        }
                    })
                
                instance = builder.build()
                INSTANCE = instance
                return instance
            }
        }
        
        /**
         * Проверяет, валиден ли экземпляр базы данных
         */
        private fun isValidInstance(db: SongDatabase): Boolean {
            return try {
                // Пробуем выполнить простой запрос для проверки работоспособности базы
                db.openHelper.readableDatabase.query("SELECT 1").close()
                true
            } catch (e: Exception) {
                android.util.Log.e("SongDatabase", "База данных невалидна: ${e.message}")
                false
            }
        }
        
        /**
         * Проверяет, существует ли подготовленная база данных в assets
         */
        fun prepopulatedDatabaseExists(context: Context): Boolean {
            return try {
                context.assets.open("database/prepopulated_songs.db").use { true }
            } catch (e: IOException) {
                false
            }
        }
        
        /**
         * Копирует базу данных из assets во внутреннее хранилище приложения
         */
        fun copyPrepopulatedDatabase(context: Context): Boolean {
            return try {
                val dbPath = context.getDatabasePath(DATABASE_NAME)
                if (dbPath.exists()) {
                    return true // База уже существует
                }
                
                // Создаем директорию, если ее нет
                dbPath.parentFile?.mkdirs()
                
                // Копируем файл из assets
                context.assets.open("database/prepopulated_songs.db").use { input ->
                    FileOutputStream(dbPath).use { output ->
                        input.copyTo(output)
                    }
                }
                
                true
            } catch (e: IOException) {
                false
            }
        }
        
        /**
         * Получает путь к файлу базы данных
         */
        fun getDatabasePath(context: Context): String {
            return context.getDatabasePath(DATABASE_NAME).absolutePath
        }

        /**
         * Принудительно сохраняет все изменения в базе данных
         * Используется при выходе из приложения для гарантии сохранения данных
         */
        fun closeDatabase() {
            synchronized(SongDatabase::class.java) {
                val instance = INSTANCE
                if (instance != null && instance.isOpen) {
                    try {
                        android.util.Log.e("SongDatabase", "DB_CLOSE: Принудительное сохранение и закрытие базы данных")
                        
                        // Используем прямой доступ к базе данных
                        val db = instance.openHelper.writableDatabase
                        android.util.Log.e("SongDatabase", "DB_CLOSE: Получен доступ к базе данных по пути ${db.path}")
                        
                        // Проверим, сколько записей сохранено перед закрытием
                        try {
                            // Проверяем данные перед закрытием
                            val favoritesCursor = db.query("SELECT COUNT(*) FROM songs WHERE isFavorite = 1")
                            var favCount = 0
                            if (favoritesCursor.moveToFirst()) {
                                favCount = favoritesCursor.getInt(0)
                            }
                            favoritesCursor.close()
                            
                            val recentsCursor = db.query("SELECT COUNT(*) FROM songs WHERE lastAccessed > 0 ORDER BY lastAccessed DESC LIMIT 10")
                            var recentCount = 0
                            val recentIds = mutableListOf<String>()
                            
                            if (recentsCursor.moveToFirst()) {
                                do {
                                    recentCount++
                                    try {
                                        val idIndex = recentsCursor.getColumnIndex("id")
                                        if (idIndex >= 0) {
                                            recentIds.add(recentsCursor.getString(idIndex))
                                        }
                                    } catch (e: Exception) {
                                        // Игнорируем ошибки индексации
                                    }
                                } while (recentsCursor.moveToNext())
                            }
                            recentsCursor.close()
                            
                            android.util.Log.e("SongDatabase", "DB_CLOSE: Перед закрытием: избранных песен = $favCount, недавних песен = $recentCount")
                            if (recentIds.isNotEmpty()) {
                                android.util.Log.e("SongDatabase", "DB_CLOSE: Недавние песни: ${recentIds.joinToString(",")}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SongDatabase", "DB_CLOSE: Ошибка при проверке состояния базы: ${e.message}", e)
                        }
                        
                        // Выполняем простые команды без транзакций для фиксации изменений
                        try {
                            // Устанавливаем простую прагму, которая приводит к фиксации изменений
                            db.execSQL("PRAGMA temp_store = MEMORY")
                            android.util.Log.e("SongDatabase", "DB_CLOSE: Выполнена фиксация изменений")
                        } catch (e: Exception) {
                            android.util.Log.e("SongDatabase", "DB_CLOSE: Ошибка при фиксации изменений: ${e.message}", e)
                        }
                        
                        // Закрываем соединения в пуле перед закрытием базы данных
                        try {
                            android.util.Log.e("SongDatabase", "DB_CLOSE: Освобождаю соединения в пуле")
                            db.query("PRAGMA optimize").close()
                        } catch (e: Exception) {
                            android.util.Log.e("SongDatabase", "DB_CLOSE: Ошибка при оптимизации базы: ${e.message}", e)
                        }
                        
                        // Закрываем базу данных
                        try {
                            android.util.Log.e("SongDatabase", "DB_CLOSE: Закрываю базу данных")
                            instance.close()
                            android.util.Log.e("SongDatabase", "DB_CLOSE: База данных успешно закрыта")
                        } catch (e: Exception) {
                            android.util.Log.e("SongDatabase", "DB_CLOSE: Ошибка при закрытии базы: ${e.message}", e)
                        }
                        
                        // Очищаем экземпляр
                        INSTANCE = null
                        android.util.Log.e("SongDatabase", "DB_CLOSE: Ссылка на экземпляр базы обнулена")
                        
                        // Запрашиваем сборку мусора для освобождения ресурсов
                        try {
                            System.gc()
                            android.util.Log.e("SongDatabase", "DB_CLOSE: Запрошена сборка мусора")
                        } catch (e: Exception) {
                            android.util.Log.e("SongDatabase", "DB_CLOSE: Ошибка при запросе сборки мусора: ${e.message}", e)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SongDatabase", "DB_CLOSE: Критическая ошибка при закрытии базы данных: ${e.message}", e)
                        
                        try {
                            // В случае ошибки все равно пытаемся закрыть базу и обнулить экземпляр
                            instance.close()
                            INSTANCE = null
                            android.util.Log.e("SongDatabase", "DB_CLOSE: База данных закрыта после ошибки")
                        } catch (e2: Exception) {
                            android.util.Log.e("SongDatabase", "DB_CLOSE: Не удалось закрыть базу после ошибки: ${e2.message}", e2)
                        }
                    }
                } else if (instance != null) {
                    android.util.Log.e("SongDatabase", "DB_CLOSE: База данных уже закрыта")
                    INSTANCE = null
                } else {
                    android.util.Log.e("SongDatabase", "DB_CLOSE: Экземпляр базы данных не инициализирован")
                }
            }
        }

        /**
         * Получает экземпляр базы данных
         */
        fun getInstance(context: Context): SongDatabase {
            return INSTANCE ?: synchronized(SongDatabase::class.java) {
                INSTANCE ?: buildDatabase(context, CoroutineScope(SupervisorJob() + Dispatchers.Default))
            }
        }
    }
} 