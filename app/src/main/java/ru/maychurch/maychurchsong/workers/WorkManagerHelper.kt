package ru.maychurch.maychurchsong.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Класс-помощник для работы с WorkManager.
 * Управляет расписанием и выполнением фоновых задач.
 */
object WorkManagerHelper {
    private const val TAG = "WorkManagerHelper"
    
    // Интервал обновления базы данных песен (в часах)
    private const val DEFAULT_UPDATE_INTERVAL_HOURS = 24L
    
    /**
     * Планирует периодическое обновление базы данных песен
     * @param context контекст приложения
     * @param intervalHours интервал обновления в часах (по умолчанию - 24 часа)
     */
    fun schedulePeriodicSongUpdate(
        context: Context,
        intervalHours: Long = DEFAULT_UPDATE_INTERVAL_HOURS
    ) {
        Log.d(TAG, "Планирование периодического обновления песен с интервалом $intervalHours часов")
        
        // Создаем ограничения: обновлять только при наличии сети
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Создаем периодический запрос
        val updateRequest = PeriodicWorkRequestBuilder<SongUpdateWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        
        // Планируем работу с заменой существующей
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SongUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Обновит существующее расписание
            updateRequest
        )
        
        Log.d(TAG, "Периодическое обновление песен запланировано")
    }
    
    /**
     * Отменяет периодическое обновление базы данных песен
     * @param context контекст приложения
     */
    fun cancelPeriodicSongUpdate(context: Context) {
        Log.d(TAG, "Отмена периодического обновления песен")
        
        WorkManager.getInstance(context).cancelUniqueWork(SongUpdateWorker.WORK_NAME)
        
        Log.d(TAG, "Периодическое обновление песен отменено")
    }
    
    /**
     * Запускает обновление базы данных песен однократно
     * @param context контекст приложения
     */
    fun runOneTimeSongUpdate(context: Context) {
        Log.d(TAG, "Запуск однократного обновления песен")
        
        // Создаем ограничения: обновлять только при наличии сети
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Создаем однократный запрос
        val updateRequest = androidx.work.OneTimeWorkRequestBuilder<SongUpdateWorker>()
            .setConstraints(constraints)
            .build()
        
        // Планируем работу
        WorkManager.getInstance(context).enqueue(updateRequest)
        
        Log.d(TAG, "Однократное обновление песен запланировано")
    }
} 