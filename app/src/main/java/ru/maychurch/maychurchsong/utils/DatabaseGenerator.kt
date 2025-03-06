package ru.maychurch.maychurchsong.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import ru.maychurch.maychurchsong.data.database.SongDatabase
import ru.maychurch.maychurchsong.data.model.Song
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.regex.Pattern

/**
 * Утилита для создания предварительно заполненной базы данных
 */
class DatabaseGenerator {
    companion object {
        private const val TAG = "DatabaseGenerator"
        private const val BASE_URL = "https://maychurch.ru/songs"
        
        /**
         * Класс для хранения информации о песне (аналог SongInfo из C#-кода)
         */
        private data class SongInfo(
            val id: String,
            val title: String
        )
        
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
         * Генерирует базу данных с песнями и сохраняет ее во внешний файл
         * @param context Контекст приложения
         * @param outputFile Файл для сохранения базы данных
         * @return Количество загруженных песен или -1 в случае ошибки
         */
        suspend fun generateDatabase(context: Context, outputFile: File): Int = withContext(Dispatchers.IO) {
            var db: android.database.sqlite.SQLiteDatabase? = null
            
            try {
                Log.d(TAG, "Starting database generation to ${outputFile.absolutePath}")
                
                // Настраиваем Jsoup для работы с HTTPS
                setupJsoup()
                
                // Убедимся, что директория существует
                outputFile.parentFile?.mkdirs()
                
                // Удалим файл, если он уже существует
                if (outputFile.exists()) {
                    outputFile.delete()
                    Log.d(TAG, "Deleted existing database file")
                }
                
                // Создаем и открываем SQLite базу данных напрямую
                db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(outputFile, null)
                Log.d(TAG, "Created new SQLite database")
                
                // Создаем таблицу для песен
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS songs (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT,
                        text TEXT,
                        url TEXT,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        lastAccessed INTEGER NOT NULL DEFAULT 0
                    )
                """)
                Log.d(TAG, "Created songs table")
                
                // Получаем список песен из оглавлений
                val allSongs = getAllSongsFromIndexPages()
                Log.d(TAG, "Found ${allSongs.size} songs in indexes")
                
                if (allSongs.isEmpty()) {
                    Log.e(TAG, "No songs found in index pages")
                    db.close()
                    return@withContext 0
                }
                
                // Подготавливаем SQL запрос для вставки песен
                val insertStmt = db.compileStatement(
                    "INSERT OR REPLACE INTO songs (id, title, text, url, isFavorite, lastAccessed) VALUES (?, ?, ?, ?, 0, 0)"
                )
                
                // Начинаем транзакцию для быстрой вставки
                db.beginTransaction()
                var successCount = 0
                
                try {
                    for ((index, songInfo) in allSongs.withIndex()) {
                        val songId = songInfo.id
                        val songUrl = "$BASE_URL/$songId.htm"
                        
                        try {
                            Log.d(TAG, "Downloading song $songId (${index+1}/${allSongs.size}): $songUrl")
                            
                            // Загружаем страницу песни
                            val songDoc = Jsoup.connect(songUrl)
                                .timeout(15000)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                                .get()
                            
                            // Извлекаем содержимое блока <div class="song">
                            val songContent = parseSongContent(songDoc.html())
                            
                            // Очищаем контент от HTML-тегов
                            val cleanContent = cleanupSongHtml(songContent)
                            
                            // Отладочная информация
                            if (songId == "0162" || songId == "0039") { // Песни из примера
                                Log.d(TAG, "======== Песня $songId ========")
                                Log.d(TAG, "Исходный HTML: ${songContent.take(100)}...")
                                
                                // Визуализируем переводы строк для четкого понимания
                                val formattedContent = cleanContent
                                    .replace("\n\n", "[ДВОЙНОЙ_ПЕРЕВОД_СТРОКИ]\n")
                                    .replace("\n", "[ПЕРЕВОД_СТРОКИ]\n")
                                
                                Log.d(TAG, "Обработанный текст с визуализацией:\n$formattedContent")
                                Log.d(TAG, "Объем обработанного текста: ${cleanContent.length} символов")
                                
                                // Подсчет и вывод числа блоков и переводов строк
                                val blockCount = "\\n\\n".toRegex().findAll(cleanContent).count() + 1
                                val lineBreakCount = "\\n".toRegex().findAll(cleanContent).count()
                                Log.d(TAG, "Количество блоков: $blockCount, переводов строк: $lineBreakCount")
                            }
                            
                            Log.d(TAG, "Downloaded song: ${songInfo.title} (ID: $songId)")
                            
                            if (cleanContent.isNotEmpty()) {
                                // Вставляем песню в базу данных
                                insertStmt.bindString(1, songId)
                                insertStmt.bindString(2, songInfo.title)
                                insertStmt.bindString(3, cleanContent)
                                insertStmt.bindString(4, songUrl)
                                insertStmt.executeInsert()
                                
                                successCount++
                            } else {
                                Log.w(TAG, "Empty song content: $songId")
                            }
                            
                            // Логируем прогресс
                            if ((index + 1) % 10 == 0 || index == allSongs.size - 1) {
                                Log.d(TAG, "Processed ${index+1}/${allSongs.size} songs, added $successCount")
                            }
                            
                            // Небольшая задержка, чтобы не перегружать сервер
                            delay(300)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error downloading song $songId: ${e.message}")
                        }
                    }
                    
                    // Завершаем транзакцию
                    db.setTransactionSuccessful()
                    Log.d(TAG, "Transaction successful")
                } finally {
                    // Закрываем транзакцию
                    try {
                        db.endTransaction()
                        Log.d(TAG, "Transaction ended")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error ending transaction", e)
                    }
                }
                
                // Завершаем запрос
                insertStmt.close()
                
                // Закрываем базу данных
                db.close()
                
                Log.d(TAG, "Successfully created database with $successCount songs")
                return@withContext successCount
            } catch (e: Exception) {
                Log.e(TAG, "Error generating database", e)
                try {
                    db?.close()
                } catch (closeEx: Exception) {
                    Log.e(TAG, "Error closing database", closeEx)
                }
                return@withContext -1
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
                    Log.d(TAG, "Downloading index page: $indexPageUrl")
                    
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
                            Log.d(TAG, "Skipping file 0000.html (index page)")
                            continue
                        }
                        
                        // Содержимое внутри <a>...</a>
                        val anchorContent = matcher.group(2)
                        
                        // Извлекаем текст заголовка
                        val titleText = parseSongTitleFromAnchor(anchorContent)
                            ?: continue // Пропускаем, если не удалось получить заголовок
                        
                        // Удаляем завершающие 4 цифры (номер) в конце строки
                        val cleanTitle = removeTrailingNumber(titleText)
                        
                        // Избегаем дубликатов
                        if (songId != null && !existingIds.contains(songId)) {
                            existingIds.add(songId)
                            result.add(SongInfo(songId, cleanTitle))
                            countOnPage++
                        }
                    }
                    
                    Log.d(TAG, "Found $countOnPage songs on page $indexPageName")
                    
                    // Небольшая задержка между загрузкой страниц оглавления
                    delay(500)
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading index page $indexPageUrl: ${e.message}")
                }
            }
            
            Log.d(TAG, "Total songs found in indexes: ${result.size}")
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
                
                // Используем простую очистку HTML вместо Jsoup.text()
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
                Log.e(TAG, "Error parsing title: ${e.message}")
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
                Log.e(TAG, "Error processing HTML: ${e.message}", e)
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
         * Запускает процесс генерации базы данных и сохраняет ее в директории assets
         */
        suspend fun generateAndSaveToAssets(context: Context): Boolean = withContext(Dispatchers.IO) {
            try {
                // Создаем файл в кэше
                val outputFile = File(context.cacheDir, "prepopulated_songs.db")
                
                // Убедимся, что директория существует
                outputFile.parentFile?.mkdirs()
                
                Log.d(TAG, "Starting database generation for assets")
                
                // Генерируем базу данных
                val songCount = generateDatabase(context, outputFile)
                
                if (songCount > 0) {
                    Log.d(TAG, "Generated database with $songCount songs")
                    Log.d(TAG, "Copy this file manually to your project: ${outputFile.absolutePath}")
                    Log.d(TAG, "Target location: app/src/main/assets/database/prepopulated_songs.db")
                    
                    // Пытаемся также сохранить во внешнем хранилище для удобства
                    try {
                        val externalDir = context.getExternalFilesDir(null)
                        if (externalDir != null) {
                            val externalFile = File(externalDir, "prepopulated_songs.db")
                            outputFile.copyTo(externalFile, overwrite = true)
                            Log.d(TAG, "Additional copy saved to external storage: ${externalFile.absolutePath}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not save additional copy to external storage", e)
                    }
                    
                    return@withContext true
                } else {
                    Log.e(TAG, "Failed to generate database, no songs loaded")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in generateAndSaveToAssets", e)
                return@withContext false
            }
        }
    }
} 