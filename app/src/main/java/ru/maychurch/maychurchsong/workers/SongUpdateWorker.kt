package ru.maychurch.maychurchsong.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maychurch.maychurchsong.data.repository.SongRepository

/**
 * Worker для периодического обновления базы данных песен.
 * Выполняет запрос на сайт для проверки наличия новых песен и добавляет их в базу данных.
 */
class SongUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SongUpdateWorker"
        
        // Имя уникальной работы для планировщика
        const val WORK_NAME = "song_update_worker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Начинаем периодическое обновление базы данных песен")
        
        try {
            // Получаем репозиторий
            val repository = SongRepository.getInstance()
            
            // Обновляем базу данных - только проверяем наличие новых песен (forceUpdate = false)
            val result = repository.refreshSongsFromWebsite(forceUpdate = false)
            
            return@withContext if (result.isSuccess) {
                val updateResult = result.getOrNull()
                if (updateResult != null) {
                    Log.d(TAG, "Обновление завершено успешно. Добавлено новых песен: ${updateResult.newSongsCount}, " +
                            "обновлено существующих: ${updateResult.updatedSongsCount}, " +
                            "всего песен в базе: ${updateResult.totalSongsCount}")
                    Result.success()
                } else {
                    Log.d(TAG, "Обновление завершено, но результат пустой")
                    Result.success()
                }
            } else {
                val exception = result.exceptionOrNull()
                Log.e(TAG, "Ошибка при обновлении песен: ${exception?.message}", exception)
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Необработанная ошибка при обновлении песен: ${e.message}", e)
            Result.failure()
        }
    }
} 