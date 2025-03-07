package ru.maychurch.maychurchsong.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.jsoup.Jsoup
import ru.maychurch.maychurchsong.SongApplication
import ru.maychurch.maychurchsong.data.database.SongDao
import ru.maychurch.maychurchsong.data.database.SongDatabase
import ru.maychurch.maychurchsong.data.model.Song
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.utils.SongUpdater
import java.io.IOException

class SongRepository(private val songDao: SongDao) {
    
    private val updater by lazy {
        SongUpdater(
            context = SongApplication.instance,
            repository = this,
            userPreferences = UserPreferences.getInstance()
        )
    }
    
    // Получить все песни из базы данных
    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()
    
    // Получить избранные песни
    fun getFavoriteSongs(): Flow<List<Song>> = songDao.getFavoriteSongs()
    
    // Поиск песен (устаревший метод)
    fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)
    
    // Расширенный поиск песен с приоритизацией результатов
    fun advancedSearchSongs(query: String): Flow<List<Song>> = songDao.advancedSearchSongs(query)
    
    // Получить недавно просмотренные песни
    fun getRecentSongs(): Flow<List<Song>> = songDao.getRecentSongs()
    
    // Получить песню по ID
    suspend fun getSongById(id: String): Song? = songDao.getSongById(id)
    
    // Обновить статус избранного
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean) {
        songDao.updateFavoriteStatus(id, isFavorite)
    }
    
    // Обновить время последнего доступа
    suspend fun updateLastAccessed(id: String) {
        songDao.updateLastAccessed(id, System.currentTimeMillis())
    }
    
    // Очистить список недавно просмотренных песен (сбросить все lastAccessed значения)
    suspend fun clearRecentSongs() {
        try {
            // Используем прямой SQL-запрос для эффективной очистки всех записей
            songDao.clearAllLastAccessed()
            Log.d(TAG, "Список недавно просмотренных песен очищен успешно")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при очистке списка недавно просмотренных песен: ${e.message}", e)
            throw e // Пробрасываем исключение для обработки в ViewModel
        }
    }
    
    // Вставка новых песен в базу данных
    suspend fun insertSongs(songs: List<Song>) {
        songDao.insertSongs(songs)
    }
    
    // Обновление существующих песен
    suspend fun updateSongs(songs: List<Song>) {
        songDao.updateSongs(songs)
    }
    
    // Проверить, есть ли подключение к интернету
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
    
    // Загружает все песни с сайта и обновляет базу данных
    suspend fun refreshSongsFromWebsite(forceUpdate: Boolean = false): Result<SongUpdater.UpdateResult> {
        // Проверяем наличие интернета
        if (!isNetworkAvailable(SongApplication.instance)) {
            Log.d(TAG, "No internet connection available, skipping refresh")
            return Result.failure(IOException("Отсутствует подключение к интернету"))
        }
        
        return try {
            updater.updateSongs(forceUpdate)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing songs: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Инициализирует базу данных, если она пуста
    suspend fun initializeDatabaseIfNeeded(): Result<Boolean> {
        try {
            // Проверяем, есть ли уже песни в базе данных
            val songsCount = songDao.getSongsCount()
            Log.d(TAG, "Количество песен в базе данных: $songsCount")
            
            if (songsCount == 0) {
                Log.d(TAG, "База данных пуста, начинаем инициализацию...")
                
                // Пробуем использовать предзаполненную базу
                val prepopulatedExists = SongDatabase.prepopulatedDatabaseExists(SongApplication.instance)
                Log.d(TAG, "Предзаполненная база в ассетах ${if(prepopulatedExists) "найдена" else "не найдена"}")
                
                // Если предзаполненная база есть, используем ее, иначе загружаем с сайта
                if (prepopulatedExists) {
                    Log.d(TAG, "Используем предзаполненную базу. Запустим проверку количества записей через 2 секунды...")
                    kotlinx.coroutines.delay(2000) // Дадим время на инициализацию
                    val countAfterDelay = songDao.getSongsCount()
                    Log.d(TAG, "После инициализации предзаполненной базы количество песен: $countAfterDelay")
                    
                    if (countAfterDelay == 0) {
                        Log.e(TAG, "Ошибка: база данных всё ещё пуста после инициализации из ассетов!")
                        // Попробуем загрузить с сайта если в базе все равно ничего нет
                        val refreshResult = refreshSongsFromWebsite(true)
                        return if (refreshResult.isSuccess) {
                            val newCount = songDao.getSongsCount()
                            Log.d(TAG, "После загрузки с сайта в базе $newCount песен")
                            Result.success(true)
                        } else {
                            Log.e(TAG, "Не удалось загрузить песни с сайта: ${refreshResult.exceptionOrNull()?.message}")
                            Result.failure(refreshResult.exceptionOrNull() ?: Exception("Неизвестная ошибка"))
                        }
                    }
                    
                    return Result.success(true)
                } else {
                    // База данных пуста, обновляем с сайта
                    Log.d(TAG, "Предзаполненная база не найдена, загружаем песни с сайта")
                    val refreshResult = refreshSongsFromWebsite(true)
                    return if (refreshResult.isSuccess) {
                        val newCount = songDao.getSongsCount()
                        Log.d(TAG, "После загрузки с сайта в базе $newCount песен")
                        Result.success(true)
                    } else {
                        Log.e(TAG, "Не удалось загрузить песни с сайта: ${refreshResult.exceptionOrNull()?.message}")
                        Result.failure(refreshResult.exceptionOrNull() ?: Exception("Неизвестная ошибка"))
                    }
                }
            }
            
            Log.d(TAG, "База данных уже инициализирована (содержит $songsCount песен)")
            return Result.success(false) // База данных уже инициализирована
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при инициализации базы данных: ${e.message}", e)
            return Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "SongRepository"
        
        // Singleton instance
        @Volatile
        private var INSTANCE: SongRepository? = null
        
        fun getInstance(): SongRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SongRepository(
                    SongApplication.instance.database.songDao()
                )
                INSTANCE = instance
                instance
            }
        }
    }
} 