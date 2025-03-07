package ru.maychurch.maychurchsong.ui.screens.song

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import ru.maychurch.maychurchsong.data.model.Song
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.data.repository.SongRepository
import ru.maychurch.maychurchsong.utils.SongUpdater
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SongViewModel : ViewModel() {
    
    private val repository = SongRepository.getInstance()
    private val userPreferences = UserPreferences.getInstance()
    
    // Состояние загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Состояние ошибки
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Состояние инициализации базы данных
    private val _isDatabaseInitialized = MutableStateFlow(false)
    val isDatabaseInitialized: StateFlow<Boolean> = _isDatabaseInitialized.asStateFlow()
    
    // Все песни
    val allSongs = repository.getAllSongs()
        .catch { e ->
            Log.e("SongViewModel", "Error loading songs", e)
            _error.value = "Ошибка загрузки песен: ${e.message}"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Избранные песни
    val favoriteSongs = repository.getFavoriteSongs()
        .catch { e ->
            Log.e("SongViewModel", "Error loading favorite songs", e)
            _error.value = "Ошибка загрузки избранных песен: ${e.message}"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Недавние песни
    private val _forceRecentSongsRefresh = MutableStateFlow(0)
    val recentSongs = _forceRecentSongsRefresh
        .flatMapLatest { repository.getRecentSongs() }
        .catch { e ->
            Log.e("SongViewModel", "Error loading recent songs", e)
            _error.value = "Ошибка загрузки недавних песен: ${e.message}"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Текущая песня
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    // Результаты поиска
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Результаты поиска
    val searchResults = MutableStateFlow<List<Song>>(emptyList())
    
    // Информация о последнем обновлении
    private val _lastUpdateInfo = MutableStateFlow<String?>(null)
    val lastUpdateInfo: StateFlow<String?> = _lastUpdateInfo.asStateFlow()
    
    // Дата последнего обновления
    private val _lastUpdateDate = MutableStateFlow<Long>(0)
    val lastUpdateDate: StateFlow<Long> = _lastUpdateDate.asStateFlow()
    
    // Форматтер для даты
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    
    init {
        // При инициализации обновляем данные с сайта
        initializeDatabase()
        
        // Загружаем дату последнего обновления
        viewModelScope.launch {
            try {
                val lastUpdateTime = userPreferences.lastUpdateTime.first()
                _lastUpdateDate.value = lastUpdateTime
                if (lastUpdateTime > 0) {
                    val formattedDate = dateFormatter.format(Date(lastUpdateTime))
                    _lastUpdateInfo.value = "Последнее обновление: $formattedDate"
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "Error loading last update time", e)
            }
        }
        
        // Проверяем, является ли это первым запуском приложения
        viewModelScope.launch {
            try {
                val isFirstLaunch = userPreferences.isFirstLaunch.first()
                if (isFirstLaunch) {
                    Log.d("SongViewModel", "Первый запуск приложения, настраиваем параметры по умолчанию")
                    
                    // При первой установке список недавних песен и так пустой, нет нужды в очистке
                    // repository.clearRecentSongs()
                    
                    // Устанавливаем средний размер шрифта
                    userPreferences.setFontSize(UserPreferences.FONT_SIZE_MEDIUM)
                    Log.d("SongViewModel", "Установлен средний размер шрифта для песен")
                    
                    // Устанавливаем средний размер шрифта интерфейса
                    userPreferences.setInterfaceFontSize(UserPreferences.FONT_SIZE_MEDIUM)
                    Log.d("SongViewModel", "Установлен средний размер шрифта для интерфейса")
                    
                    // Устанавливаем еженедельное обновление
                    userPreferences.setUpdateIntervalHours(UserPreferences.UPDATE_INTERVAL_WEEK)
                    Log.d("SongViewModel", "Установлен период обновления: 1 раз в неделю")
                    
                    // Отмечаем, что первый запуск завершен
                    userPreferences.setFirstLaunchCompleted()
                    Log.d("SongViewModel", "Первоначальная настройка завершена")
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "Ошибка при проверке первого запуска: ${e.message}", e)
            }
        }
    }
    
    // Инициализация базы данных
    private fun initializeDatabase() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = repository.initializeDatabaseIfNeeded()
                if (result.isSuccess) {
                    _isDatabaseInitialized.value = true
                    if (result.getOrDefault(false)) {
                        _lastUpdateInfo.value = "База данных создана и заполнена"
                    } else {
                        _lastUpdateInfo.value = "База данных уже инициализирована"
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    if (exception != null) {
                        Log.e("SongViewModel", "Ошибка инициализации: ${exception.message}", exception)
                        if (exception is IOException && exception.message?.contains("интернету") == true) {
                            _lastUpdateInfo.value = "Работаем в офлайн режиме"
                            _isDatabaseInitialized.value = true
                        } else {
                            _error.value = "Ошибка инициализации базы данных: ${exception.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "Error initializing database", e)
                _error.value = "Ошибка инициализации базы данных: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Обновить данные с сайта
    fun refreshSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _lastUpdateInfo.value = null
            
            try {
                val result = repository.refreshSongsFromWebsite()
                
                if (result.isSuccess) {
                    val updateInfo = result.getOrNull()
                    
                    // Сохраняем время обновления
                    val currentTime = System.currentTimeMillis()
                    userPreferences.setLastUpdateTime(currentTime)
                    _lastUpdateDate.value = currentTime
                    
                    // Форматируем время для отображения
                    val formattedDate = dateFormatter.format(Date(currentTime))
                    
                    _lastUpdateInfo.value = if (updateInfo != null) {
                        when {
                            updateInfo.newSongsCount > 0 -> "Добавлено ${updateInfo.newSongsCount} новых песен.\nПоследнее обновление: $formattedDate"
                            updateInfo.updatedSongsCount > 0 -> "Обновлено ${updateInfo.updatedSongsCount} песен.\nПоследнее обновление: $formattedDate"
                            else -> "Обновлений не найдено.\nПоследнее обновление: $formattedDate"
                        }
                    } else {
                        "База данных обновлена.\nПоследнее обновление: $formattedDate"
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    if (exception != null) {
                        if (exception is IOException && exception.message?.contains("интернету") == true) {
                            _lastUpdateInfo.value = "Невозможно обновить песни: нет подключения к интернету"
                        } else {
                            _error.value = "Ошибка обновления песен: ${exception.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "Error refreshing songs", e)
                _error.value = "Ошибка обновления песен: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Загрузить песню по ID
    fun loadSong(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val song = repository.getSongById(id)
                _currentSong.value = song
                
                // Обновляем время последнего доступа
                if (song != null) {
                    repository.updateLastAccessed(id)
                    // Обновляем список недавно просмотренных песен
                    _forceRecentSongsRefresh.value = _forceRecentSongsRefresh.value + 1
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "Error loading song $id", e)
                _error.value = "Ошибка загрузки песни: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Обновить статус избранного
    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            try {
                val song = repository.getSongById(id)
                if (song != null) {
                    repository.updateFavoriteStatus(id, !song.isFavorite)
                    
                    // Обновляем текущую песню, если она открыта
                    if (_currentSong.value?.id == id) {
                        _currentSong.value = _currentSong.value?.copy(isFavorite = !song.isFavorite)
                    }
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "Error toggling favorite for song $id", e)
                _error.value = "Ошибка обновления статуса избранного: ${e.message}"
            }
        }
    }
    
    // Поиск песен
    fun search(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                searchResults.value = emptyList()
                return@launch
            }
            
            try {
                // Используем расширенный поиск с приоритизацией результатов
                repository.advancedSearchSongs(query).collect {
                    searchResults.value = it
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "Error searching songs", e)
                _error.value = "Ошибка поиска песен: ${e.message}"
            }
        }
    }
    
    // Очистить ошибку
    fun clearError() {
        _error.value = null
    }
    
    // Очистить сообщение об обновлении
    fun clearUpdateInfo() {
        _lastUpdateInfo.value = null
    }
    
    // Очистить список недавно просмотренных песен
    fun clearRecentSongs() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.clearRecentSongs()
                // Принудительное обновление списка недавно просмотренных песен
                _forceRecentSongsRefresh.value = _forceRecentSongsRefresh.value + 1
                Log.d("SongViewModel", "Список недавно просмотренных песен очищен")
                
                // Уведомление пользователя
                _error.value = "Список недавно просмотренных песен очищен"
            } catch (e: Exception) {
                Log.e("SongViewModel", "Ошибка при очистке списка недавно просмотренных песен: ${e.message}", e)
                _error.value = "Ошибка при очистке списка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Получить форматированную дату последнего обновления
    fun getFormattedLastUpdateDate(): String {
        val lastUpdate = _lastUpdateDate.value
        return if (lastUpdate > 0) {
            dateFormatter.format(Date(lastUpdate))
        } else {
            "Никогда"
        }
    }
} 