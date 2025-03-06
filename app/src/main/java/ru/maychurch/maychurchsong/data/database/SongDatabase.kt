package ru.maychurch.maychurchsong.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
            
            return builder.build()
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
    }
} 