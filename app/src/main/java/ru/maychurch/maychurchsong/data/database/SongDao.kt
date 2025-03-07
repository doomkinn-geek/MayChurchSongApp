package ru.maychurch.maychurchsong.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.maychurch.maychurchsong.data.model.Song

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)
    
    @Update
    suspend fun updateSong(song: Song)
    
    @Update
    suspend fun updateSongs(songs: List<Song>)
    
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): Song?
    
    @Query("SELECT * FROM songs ORDER BY id ASC")
    fun getAllSongs(): Flow<List<Song>>
    
    @Query("SELECT * FROM songs ORDER BY id ASC")
    suspend fun getAllSongsAsList(): List<Song>
    
    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongsCount(): Int
    
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY id ASC")
    fun getFavoriteSongs(): Flow<List<Song>>
    
    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR text LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<Song>>
    
    /**
     * Расширенный поиск песен с поддержкой поиска по ID и сортировкой результатов
     * Возвращает песни, где ID совпадает с запросом точно,
     * затем песни с запросом в заголовке, затем песни с запросом в тексте
     * Учитывает поиск по числовому ID без ведущих нулей
     */
    @Query("""
        SELECT *, 
        CASE
            WHEN id = :query THEN 0                 -- Наивысший приоритет для точного совпадения по ID
            WHEN CAST(CAST(id AS INTEGER) AS TEXT) = CAST(:query AS TEXT) THEN 0  -- Также высший приоритет при совпадении без ведущих нулей
            WHEN id LIKE '0' || :query THEN 0       -- Высший приоритет если ID = 0 + введенный номер (для 2-3 значных чисел)
            WHEN id LIKE '00' || :query THEN 0      -- Высший приоритет если ID = 00 + введенный номер (для 1-2 значных чисел)
            WHEN title LIKE :query || '%' THEN 1    -- Высокий приоритет для совпадения с началом заголовка
            WHEN title LIKE '%' || :query || '%' THEN 2  -- Средний приоритет для совпадения с заголовком
            WHEN text LIKE '%' || :query || '%' THEN 3   -- Низкий приоритет для совпадения с текстом
            ELSE 4                                  -- Наименьший приоритет для остальных
        END as priorityOrder
        FROM songs
        WHERE 
            id = :query 
            OR CAST(CAST(id AS INTEGER) AS TEXT) = CAST(:query AS TEXT)
            OR id LIKE '0' || :query 
            OR id LIKE '00' || :query
            OR title LIKE '%' || :query || '%' 
            OR text LIKE '%' || :query || '%'
        ORDER BY priorityOrder ASC, id ASC
    """)
    fun advancedSearchSongs(query: String): Flow<List<Song>>
    
    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)
    
    @Query("UPDATE songs SET lastAccessed = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: String, timestamp: Long)
    
    @Query("UPDATE songs SET lastAccessed = 0")
    suspend fun clearAllLastAccessed()
    
    @Query("SELECT * FROM songs WHERE lastAccessed > 0 ORDER BY lastAccessed DESC LIMIT 10")
    fun getRecentSongs(): Flow<List<Song>>
} 