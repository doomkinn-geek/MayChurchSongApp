#!/usr/bin/env kotlin

import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.sql.DriverManager
import java.util.regex.Pattern

/**
 * Скрипт для генерации базы данных SQLite с песнями с сайта maychurch.ru
 * 
 * Использование:
 * 1. Убедитесь, что у вас установлен Kotlin и SQLite
 * 2. Запустите скрипт: kotlinc -script GenerateDatabase.kts
 * 3. Скопируйте созданный файл prepopulated_songs.db в директорию assets/database/ вашего проекта
 */

val baseUrl = "https://maychurch.ru/songs/"
val mainUrl = "${baseUrl}0000.htm"
val dbFile = File("prepopulated_songs.db")

// Удаляем старую базу данных, если она существует
if (dbFile.exists()) {
    dbFile.delete()
    println("Удалена старая база данных")
}

// Создаем соединение с базой данных SQLite
val connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
val statement = connection.createStatement()

// Создаем таблицу для песен
statement.executeUpdate("""
    CREATE TABLE songs (
        id TEXT PRIMARY KEY NOT NULL,
        title TEXT,
        text TEXT,
        url TEXT,
        isFavorite INTEGER NOT NULL DEFAULT 0,
        lastAccessed INTEGER NOT NULL DEFAULT 0
    )
""")

println("Создана таблица в базе данных")

// Функция для загрузки HTML страницы
fun loadHtml(url: String): String {
    return URL(url).openStream().bufferedReader().use { it.readText() }
}

// Загружаем главную страницу со списком песен
val mainPageHtml = loadHtml(mainUrl)

// Находим все ссылки на песни с помощью регулярного выражения
val pattern = Pattern.compile("href=\"(\\d{4}\\.htm)\"")
val matcher = pattern.matcher(mainPageHtml)

val songLinks = mutableListOf<String>()
while (matcher.find()) {
    val songId = matcher.group(1).replace(".htm", "")
    
    // Пропускаем файл 0000.htm, так как это оглавление, а не песня
    if (songId == "0000") {
        println("Пропускаем файл 0000.htm (оглавление)")
        continue
    }
    
    songLinks.add(matcher.group(1))
}

println("Найдено ${songLinks.size} песен на сайте")

// Подготавливаем SQL запрос для вставки
val insertStatement = connection.prepareStatement(
    "INSERT INTO songs (id, title, text, url, isFavorite, lastAccessed) VALUES (?, ?, ?, ?, 0, 0)"
)

// Загружаем каждую песню и добавляем в базу данных
var successCount = 0
var errorCount = 0

songLinks.forEachIndexed { index, link ->
    val songId = link.replace(".htm", "")
    val songUrl = baseUrl + link
    
    try {
        val songHtml = loadHtml(songUrl)
        
        // Извлекаем заголовок и текст песни
        val titlePattern = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL)
        val titleMatcher = titlePattern.matcher(songHtml)
        val title = if (titleMatcher.find()) titleMatcher.group(1).trim() else "Песня $songId"
        
        val bodyPattern = Pattern.compile("<body[^>]*>(.*?)</body>", Pattern.DOTALL)
        val bodyMatcher = bodyPattern.matcher(songHtml)
        val bodyText = if (bodyMatcher.find()) bodyMatcher.group(1) else ""
        
        // Удаляем HTML теги для получения чистого текста
        val textWithoutTags = bodyText.replace("<[^>]*>".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
        
        // Вставляем песню в базу данных
        insertStatement.setString(1, songId)
        insertStatement.setString(2, title)
        insertStatement.setString(3, textWithoutTags)
        insertStatement.setString(4, songUrl)
        insertStatement.executeUpdate()
        
        successCount++
        
        // Выводим прогресс каждые 10 песен
        if (index % 10 == 0 || index == songLinks.size - 1) {
            println("Обработано ${index + 1}/${songLinks.size} песен")
        }
    } catch (e: Exception) {
        println("Ошибка при обработке песни $songId: ${e.message}")
        errorCount++
    }
}

// Закрываем соединение с базой данных
insertStatement.close()
statement.close()
connection.close()

println("Готово! Создана база данных с $successCount песнями ($errorCount ошибок)")
println("Файл базы данных: ${dbFile.absolutePath}")
println("Скопируйте этот файл в директорию assets/database/ вашего проекта") 