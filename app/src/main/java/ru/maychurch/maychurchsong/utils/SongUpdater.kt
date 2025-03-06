package ru.maychurch.maychurchsong.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import ru.maychurch.maychurchsong.data.model.Song
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.data.repository.SongRepository
import java.io.IOException
import java.util.regex.Pattern

/**
 * Утилита для обновления базы данных песен
 */
class SongUpdater(
    private val context: Context,
    private val repository: SongRepository,
    private val userPreferences: UserPreferences
) {
    companion object {
        private const val TAG = "SongUpdater"
        private const val BASE_URL = "https://maychurch.ru/songs"
        
        /**
         * Класс для хранения информации о песне
         */
        data class SongInfo(
            val id: String,
            val title: String,
            val url: String
        )
    }
    
    /**
     * Подготавливает Jsoup к работе с HTTPS
     */
    private fun setupJsoup() {
        try {
            // Настраиваем безопасные подключения
            val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
            sslContext.init(null, null, java.security.SecureRandom())
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            
            // Игнорируем ошибки валидации сертификатов
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {}
            })
            
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup SSL context", e)
        }
    }
    
    /**
     * Обновляет базу данных песен. Если forceUpdate=true, обновляет все песни,
     * иначе проверяет только наличие новых.
     * @return Результат с информацией о количестве новых и обновленных песен
     */
    suspend fun updateSongs(forceUpdate: Boolean = false): Result<UpdateResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Начинаем обновление песен. Принудительное обновление: $forceUpdate")
            
            // Настраиваем Jsoup для работы с HTTPS
            setupJsoup()
            
            // Получаем список всех песен с оглавлений
            val allSongsInfo = getAllSongsFromIndexPages()
            
            if (allSongsInfo.isEmpty()) {
                Log.e(TAG, "Не удалось получить список песен с сайта")
                return@withContext Result.failure(IOException("Не удалось получить список песен с сайта"))
            }
            
            Log.d(TAG, "Найдено ${allSongsInfo.size} песен в оглавлении")
            
            // Получаем существующие песни из базы данных
            val existingSongs = repository.getAllSongs().first()
            val existingSongIds = existingSongs.map { it.id }.toSet()
            
            // Определяем, какие песни новые
            val newSongInfos = allSongsInfo.filter { it.id !in existingSongIds }
            
            // Также можем определить песни, которые нужно обновить (если их id есть в базе)
            val songsToUpdate = if (forceUpdate) {
                allSongsInfo.filter { it.id in existingSongIds }
            } else {
                emptyList() // При обычном обновлении не обновляем существующие песни
            }
            
            val newSongs = mutableListOf<Song>()
            val updatedSongs = mutableListOf<Song>()
            
            // Обрабатываем новые песни
            if (newSongInfos.isNotEmpty()) {
                Log.d(TAG, "Найдено ${newSongInfos.size} новых песен для добавления")
                
                for ((index, songInfo) in newSongInfos.withIndex()) {
                    try {
                        Log.d(TAG, "Загрузка новой песни ${index + 1}/${newSongInfos.size}: ${songInfo.id}")
                        
                        // Загружаем песню
                        val song = downloadSong(songInfo)
                        if (song != null) {
                            newSongs.add(song)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при загрузке песни ${songInfo.id}: ${e.message}")
                    }
                }
                
                // Сохраняем новые песни в базу данных
                if (newSongs.isNotEmpty()) {
                    repository.insertSongs(newSongs)
                    Log.d(TAG, "Добавлено ${newSongs.size} новых песен в базу данных")
                }
            } else {
                Log.d(TAG, "Новых песен не найдено")
            }
            
            // Обрабатываем песни для обновления
            if (songsToUpdate.isNotEmpty()) {
                Log.d(TAG, "Найдено ${songsToUpdate.size} песен для обновления")
                
                for ((index, songInfo) in songsToUpdate.withIndex()) {
                    try {
                        Log.d(TAG, "Обновление песни ${index + 1}/${songsToUpdate.size}: ${songInfo.id}")
                        
                        // Загружаем песню
                        val song = downloadSong(songInfo)
                        if (song != null) {
                            // Сохраняем статус избранного из существующей песни
                            val existingSong = existingSongs.find { it.id == songInfo.id }
                            if (existingSong != null) {
                                val updatedSong = song.copy(isFavorite = existingSong.isFavorite)
                                updatedSongs.add(updatedSong)
                            } else {
                                updatedSongs.add(song)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при обновлении песни ${songInfo.id}: ${e.message}")
                    }
                }
                
                // Обновляем существующие песни
                if (updatedSongs.isNotEmpty()) {
                    repository.updateSongs(updatedSongs)
                    Log.d(TAG, "Обновлено ${updatedSongs.size} существующих песен")
                }
            }
            
            // Сохраняем время последнего обновления
            userPreferences.setLastUpdateTime(System.currentTimeMillis())
            
            // Формируем результат
            val result = UpdateResult(
                newSongsCount = newSongs.size,
                updatedSongsCount = updatedSongs.size,
                totalSongsCount = existingSongs.size + newSongs.size
            )
            
            return@withContext Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении песен: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Загружает песню по её информации
     */
    private suspend fun downloadSong(songInfo: SongInfo): Song? = withContext(Dispatchers.IO) {
        try {
            // Загружаем страницу песни
            val songDoc = Jsoup.connect(songInfo.url)
                .timeout(15000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()
            
            // Извлекаем содержимое блока <div class="song">
            val songContent = parseSongContent(songDoc.html())
            
            // Очищаем контент от HTML-тегов
            val cleanContent = cleanupSongHtml(songContent)
            
            if (cleanContent.isEmpty()) {
                Log.w(TAG, "Пустой текст песни: ${songInfo.id}")
                return@withContext null
            }
            
            return@withContext Song(
                id = songInfo.id,
                title = songInfo.title,
                text = cleanContent,
                url = songInfo.url,
                isFavorite = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке песни ${songInfo.id}: ${e.message}")
            return@withContext null
        }
    }
    
    /**
     * Получает список песен из оглавлений p01.htm - p28.htm
     */
    private suspend fun getAllSongsFromIndexPages(): List<SongInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<SongInfo>()
        val existingIds = mutableSetOf<String>()
        
        // Обрабатываем страницы оглавления p01.htm - p28.htm
        for (i in 1..28) {
            val indexPageName = "p%02d.htm".format(i)
            val indexPageUrl = "$BASE_URL/$indexPageName"
            
            try {
                Log.d(TAG, "Загрузка страницы оглавления: $indexPageUrl")
                
                val response = Jsoup.connect(indexPageUrl)
                    .timeout(20000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get()
                
                val pageContent = response.html()
                
                // Ищем <a href="0039.htm">...</a> блоки
                val linkPattern = Pattern.compile(
                    "<a\\s+href\\s*=\\s*\"(\\d{4})\\.htm\"(.*?)</a>",
                    Pattern.CASE_INSENSITIVE or Pattern.DOTALL
                )
                val matcher = linkPattern.matcher(pageContent)
                
                var countOnPage = 0
                while (matcher.find()) {
                    if (matcher.groupCount() < 2) continue
                    
                    // "0039"
                    val songId = matcher.group(1)
                    
                    // Пропускаем файл 0000.html, так как это оглавление, а не песня
                    if (songId == "0000") {
                        Log.d(TAG, "Пропускаем файл 0000.html (оглавление)")
                        continue
                    }
                    
                    // Содержимое внутри <a>...</a>
                    val anchorContent = matcher.group(2)
                    
                    // Извлекаем текст заголовка
                    val titleText = parseSongTitleFromAnchor(anchorContent)
                        ?: continue // Пропускаем, если не удалось получить заголовок
                    
                    // Удаляем завершающие 4 цифры (номер) в конце строки
                    val cleanTitle = removeTrailingNumber(titleText)
                    
                    // Формируем URL песни
                    val songUrl = "$BASE_URL/$songId.htm"
                    
                    // Избегаем дубликатов
                    if (songId != null && !existingIds.contains(songId)) {
                        existingIds.add(songId)
                        result.add(SongInfo(songId, cleanTitle, songUrl))
                        countOnPage++
                    }
                }
                
                Log.d(TAG, "Найдено $countOnPage песен на странице $indexPageName")
                
                // Небольшая задержка между загрузкой страниц оглавления
                kotlinx.coroutines.delay(500)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке страницы оглавления $indexPageUrl: ${e.message}")
            }
        }
        
        Log.d(TAG, "Всего найдено песен в оглавлениях: ${result.size}")
        return@withContext result
    }
    
    /**
     * Извлекает заголовок песни из содержимого тега <a>
     */
    private fun parseSongTitleFromAnchor(anchorContent: String): String? {
        try {
            // Пример: <div class="list-grid-item-text"> Благо есть славить Господа 0039 </div>
            val pattern = Pattern.compile(
                "<div\\s+class\\s*=\\s*\"list-grid-item-text\"[^>]*>(.*?)</div>",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            )
            val matcher = pattern.matcher(anchorContent)
            
            if (!matcher.find() || matcher.groupCount() < 1) {
                return null
            }
            
            val rawTitle = matcher.group(1) ?: return null
            
            // Используем простую очистку HTML
            var cleanTitle = rawTitle
                .replace("<[^>]*>".toRegex(), "") // Удаляем HTML-теги
                .trim()
            
            // Заменяем HTML-сущности
            cleanTitle = cleanTitle
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
            
            return cleanTitle
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при разборе заголовка: ${e.message}")
            return null
        }
    }
    
    /**
     * Удаляет завершающий номер из строки (пробел + 4 цифры)
     */
    private fun removeTrailingNumber(input: String): String {
        val pattern = Pattern.compile("^(.*?)\\s+(\\d{4})\\s*$")
        val matcher = pattern.matcher(input)
        
        if (matcher.find() && matcher.groupCount() >= 2) {
            return matcher.group(1)?.trim() ?: input.trim()
        }
        
        return input.trim()
    }
    
    /**
     * Извлекает блок <div class="song"> из HTML-страницы песни
     */
    private fun parseSongContent(htmlContent: String): String {
        // Резервный метод, не использующий Jsoup.text()
        val startDiv = "<div class=\"song\">"
        val endDiv = "</div>"
        
        val startIndex = htmlContent.indexOf(startDiv, ignoreCase = true)
        if (startIndex < 0) {
            return "Нет текста песни"
        }
        
        val contentStart = startIndex + startDiv.length
        val endIndex = htmlContent.indexOf(endDiv, contentStart, ignoreCase = true)
        if (endIndex < 0) {
            return "Нет текста песни"
        }
        
        return htmlContent.substring(contentStart, endIndex).trim()
    }
    
    /**
     * Очищает HTML-контент песни от тегов, преобразуя теги <p> в одинарные переводы строк,
     * а теги <br> - в двойные переводы строк для разделения блоков
     */
    private fun cleanupSongHtml(rawSongHtml: String): String {
        try {
            // Упрощенный подход к разбору HTML-песни
            
            // Заменяем HTML-сущности
            var content = rawSongHtml
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
            
            // Заменяем теги <br> на специальный маркер для разделения блоков
            content = content.replace("<br\\s*/*>".toRegex(RegexOption.IGNORE_CASE), "[[BLOCK_SEPARATOR]]")
            
            // Теперь обрабатываем <p> теги
            content = content.replace("<p[^>]*>".toRegex(RegexOption.IGNORE_CASE), "")
            content = content.replace("</p>".toRegex(RegexOption.IGNORE_CASE), "\n")
            
            // Удаляем все остальные HTML-теги
            content = content.replace("<[^>]*>".toRegex(), "")
            
            // Разбиваем на блоки и обрабатываем каждый блок
            val blocks = content.split("[[BLOCK_SEPARATOR]]")
            val result = StringBuilder()
            
            for (i in blocks.indices) {
                val block = blocks[i].trim()
                if (block.isEmpty()) continue
                
                // Разбиваем блок на строки, удаляем лишние пробелы и пустые строки
                val lines = block.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                // Добавляем строки блока с одинарными переводами
                if (lines.isNotEmpty()) {
                    // Добавляем блок в результат
                    if (result.isNotEmpty()) {
                        // Если это не первый блок, добавляем двойной перевод строки перед ним
                        result.append("\n\n")
                    }
                    
                    // Добавляем строки с одинарными переводами
                    result.append(lines.joinToString("\n"))
                }
            }
            
            return result.toString().trim()
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обработки HTML: ${e.message}", e)
            // Резервный вариант очистки при ошибке
            return rawSongHtml
                .replace("<p[^>]*>".toRegex(RegexOption.IGNORE_CASE), "")
                .replace("</p>".toRegex(RegexOption.IGNORE_CASE), "\n")
                .replace("<br\\s*/*>".toRegex(RegexOption.IGNORE_CASE), "\n\n")
                .replace("<[^>]*>".toRegex(), "")
                .replace("&nbsp;", " ")
                .replace("\n{3,}".toRegex(), "\n\n")
                .trim()
        }
    }
    
    /**
     * Класс для хранения результатов обновления
     */
    data class UpdateResult(
        val newSongsCount: Int,
        val updatedSongsCount: Int,
        val totalSongsCount: Int
    )
} 