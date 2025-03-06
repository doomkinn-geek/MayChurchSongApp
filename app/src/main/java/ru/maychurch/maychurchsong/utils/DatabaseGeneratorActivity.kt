package ru.maychurch.maychurchsong.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.maychurch.maychurchsong.ui.theme.MayChurchSongTheme
import java.io.File
import java.io.IOException
import android.content.ContentValues
import android.provider.MediaStore

/**
 * Активность для генерации базы данных.
 * Эта активность используется только во время разработки!
 * После генерации базы данных, нужно скопировать файл в assets/database/ проекта.
 */
class DatabaseGeneratorActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "DatabaseGeneratorActivity"
    }
    
    private var generationStatus = mutableStateOf("Нажмите кнопку для генерации базы данных")
    private var isGenerating = mutableStateOf(false)
    private var outputFilePath = mutableStateOf<String?>(null)
    private var cacheFilePath = mutableStateOf<String?>(null)
    
    // Создаем собственную область корутин для активности
    private val activityScope = CoroutineScope(Dispatchers.Main)
    
    // Запрос разрешений для Android 10+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Разрешение получено")
            startDatabaseGeneration()
        } else {
            Log.e(TAG, "Разрешение не получено")
            generationStatus.value = "Ошибка: нет разрешения на запись файлов"
            Toast.makeText(
                this,
                "Необходимо разрешение на запись файлов",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Дополнительный обработчик для экспорта (для разделения логики)
    private val exportPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Разрешение для экспорта получено")
            // Продолжаем экспорт с прямым доступом, теперь с разрешениями
            val sourceFiles = listOf(
                File(cacheDir, "prepopulated_songs.db"),
                File(getExternalFilesDir(null), "database/prepopulated_songs.db")
            )
            val sourceFile = sourceFiles.firstOrNull { it.exists() }
            if (sourceFile != null) {
                exportUsingDirectAccess(sourceFile)
            } else {
                Toast.makeText(this, "Файл базы данных не найден", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e(TAG, "Разрешение для экспорта не получено")
            Toast.makeText(
                this,
                "Необходимо разрешение для экспорта файла",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Проверка доступности интернета
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    // Проверка доступности сайта
    private suspend fun isSiteAvailable(url: String = "https://maychurch.ru"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "HEAD"
                val responseCode = connection.responseCode
                Log.d(TAG, "Код ответа от сайта $url: $responseCode")
                return@withContext responseCode == 200
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при проверке доступности сайта $url", e)
                return@withContext false
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "DatabaseGeneratorActivity создана")
        
        setContent {
            MayChurchSongTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DatabaseGeneratorScreen(
                        isGenerating = isGenerating.value,
                        generationStatus = generationStatus.value,
                        outputFilePath = outputFilePath.value,
                        cacheFilePath = cacheFilePath.value,
                        onGenerateClick = { checkPermissionsAndGenerate() },
                        onExportClick = { exportDatabaseToDownloads() }
                    )
                }
            }
        }
    }
    
    private fun checkPermissionsAndGenerate() {
        Log.d(TAG, "Проверка разрешений и запуск генерации")
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Для Android 10 (Q) и ниже, запрашиваем стандартное разрешение
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Разрешение уже получено")
                startDatabaseGeneration()
            } else {
                Log.d(TAG, "Запрашиваем разрешение")
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            // Для Android 11+ (R) используем getExternalFilesDir, который не требует специальных разрешений
            Log.d(TAG, "Android 11+: Используем приватную директорию приложения")
            startDatabaseGeneration()
        }
    }
    
    private fun startDatabaseGeneration() {
        isGenerating.value = true
        generationStatus.value = "Генерация базы данных..."
        Log.d(TAG, "Начинаем генерацию базы данных")
        
        // Проверяем наличие интернета
        if (!isNetworkAvailable()) {
            isGenerating.value = false
            generationStatus.value = "Ошибка: отсутствует подключение к интернету"
            Log.e(TAG, "Нет подключения к интернету")
            Toast.makeText(
                this,
                "Требуется подключение к интернету для загрузки песен",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        // Проверяем доступность сайта
        activityScope.launch {
            if (!isSiteAvailable()) {
                isGenerating.value = false
                generationStatus.value = "Ошибка: сайт с песнями недоступен"
                Log.e(TAG, "Сайт maychurch.ru недоступен")
                Toast.makeText(
                    this@DatabaseGeneratorActivity,
                    "Сайт maychurch.ru недоступен. Проверьте соединение или попробуйте позже.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            
            // Продолжаем генерацию базы данных
            startDatabaseGenerationProcess()
        }
    }
    
    // Основной процесс генерации базы данных
    private fun startDatabaseGenerationProcess() {
        // Запускаем генерацию в корутине
        activityScope.launch {
            try {
                // Проверяем доступность директории для записи
                // Используем внутреннее хранилище приложения, не требующее специальных разрешений
                val outputDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Для Android 11+, используем приватную директорию без запроса разрешений
                    File(getExternalFilesDir(null), "database")
                } else {
                    // Для более старых версий
                    File(getExternalFilesDir(null), "database")
                }
                
                if (!outputDir.exists()) {
                    val created = outputDir.mkdirs()
                    Log.d(TAG, "Создание директории ${outputDir.absolutePath}: $created")
                    if (!created && !outputDir.exists()) {
                        throw IOException("Не удалось создать директорию ${outputDir.absolutePath}")
                    }
                }
                
                val outputFile = File(outputDir, "prepopulated_songs.db")
                Log.d(TAG, "Файл для базы данных: ${outputFile.absolutePath}")
                
                // Выполняем генерацию базы данных в IO потоке
                Log.d(TAG, "Вызываем DatabaseGenerator.generateDatabase")
                val songCount = withContext(Dispatchers.IO) {
                    DatabaseGenerator.generateDatabase(
                        applicationContext,
                        outputFile
                    )
                }
                Log.d(TAG, "Генерация завершена, получено песен: $songCount")
                
                // Обрабатываем результат (на UI потоке)
                if (songCount > 0) {
                    // Также копируем файл в кэш приложения для гарантированного доступа
                    try {
                        val cacheFile = File(cacheDir, "prepopulated_songs.db")
                        outputFile.copyTo(cacheFile, overwrite = true)
                        Log.d(TAG, "Копия базы данных также сохранена в: ${cacheFile.absolutePath}")
                        cacheFilePath.value = cacheFile.absolutePath
                        
                        // Копируем файл в публичную директорию Downloads для легкого доступа
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Для Android 10+ используем MediaStore
                            exportToDownloadsUsingMediaStore(outputFile)
                        } else {
                            // Для старых версий Android используем прямую запись
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val publicFile = File(downloadsDir, "maychurch_songs.db")
                            outputFile.copyTo(publicFile, overwrite = true)
                            
                            Toast.makeText(
                                this@DatabaseGeneratorActivity,
                                "База данных скопирована в Downloads/maychurch_songs.db",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            Log.d(TAG, "Копия сохранена в общедоступной директории: ${publicFile.absolutePath}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при копировании в публичную директорию", e)
                    }
                    
                    generationStatus.value = "База данных успешно создана: $songCount песен"
                    outputFilePath.value = outputFile.absolutePath
                    Toast.makeText(
                        this@DatabaseGeneratorActivity,
                        "База данных создана в: ${outputFile.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    generationStatus.value = "Ошибка создания базы данных: получено 0 песен"
                    Log.e(TAG, "Получено 0 песен при генерации")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при генерации базы данных", e)
                generationStatus.value = "Ошибка: ${e.javaClass.simpleName}: ${e.message}"
            } finally {
                isGenerating.value = false
            }
        }
    }
    
    // Метод для копирования файла в директорию Downloads через MediaStore на Android 10+
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun exportToDownloadsUsingMediaStore(sourceFile: File) {
        try {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "maychurch_songs.db")
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }
            
            val resolver = contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                
                val publicPath = "/storage/emulated/0/Download/maychurch_songs.db"
                Toast.makeText(
                    this,
                    "База данных экспортирована в Downloads/maychurch_songs.db",
                    Toast.LENGTH_LONG
                ).show()
                
                Log.d(TAG, "Копия сохранена через MediaStore: $publicPath")
            } else {
                Log.e(TAG, "Не удалось создать файл через MediaStore API")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при экспорте через MediaStore: ${e.message}", e)
        }
    }
    
    private fun exportDatabaseToDownloads() {
        // Проверяем наличие файла в кэше
        val sourceFiles = listOf(
            File(cacheDir, "prepopulated_songs.db"),
            File(getExternalFilesDir(null), "database/prepopulated_songs.db")
        )
        
        val sourceFile = sourceFiles.firstOrNull { it.exists() }
        
        if (sourceFile == null) {
            Toast.makeText(this, "Файл базы данных не найден. Сначала создайте базу данных.", Toast.LENGTH_LONG).show()
            return
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Для Android 10+ используем MediaStore API
                exportUsingMediaStore(sourceFile)
            } else {
                // Для Android 9 и ниже используем прямой доступ (требуется разрешение)
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Запрашиваем разрешение через новый launcher
                    exportPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    return
                }
                
                exportUsingDirectAccess(sourceFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при экспорте базы данных", e)
            Toast.makeText(this, "Ошибка при экспорте: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    // Экспорт через MediaStore API (Android 10+)
    private fun exportUsingMediaStore(sourceFile: File) {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "prepopulated_songs.db")
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        
        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                
                generationStatus.value = "База данных скопирована в загрузки"
                
                Toast.makeText(
                    this,
                    "База данных экспортирована в загрузки (Download/prepopulated_songs.db)",
                    Toast.LENGTH_LONG
                ).show()
                
                Log.d(TAG, "База данных успешно экспортирована через MediaStore")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при записи через MediaStore", e)
                throw e
            }
        } else {
            Toast.makeText(this, "Не удалось создать файл в загрузках", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Не удалось получить URI для записи через MediaStore")
            throw IOException("Не удалось получить URI для записи")
        }
    }
    
    // Экспорт через прямой доступ к файловой системе (Android 9 и ниже)
    private fun exportUsingDirectAccess(sourceFile: File) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destinationFile = File(downloadsDir, "prepopulated_songs.db")
        
        sourceFile.copyTo(destinationFile, overwrite = true)
        
        generationStatus.value = "База данных скопирована в загрузки: ${destinationFile.absolutePath}"
        
        Toast.makeText(
            this,
            "База данных скопирована в загрузки:\n${destinationFile.absolutePath}",
            Toast.LENGTH_LONG
        ).show()
        
        Log.d(TAG, "База данных успешно экспортирована: ${destinationFile.absolutePath}")
    }
}

@Composable
fun DatabaseGeneratorScreen(
    isGenerating: Boolean,
    generationStatus: String,
    outputFilePath: String?,
    cacheFilePath: String?,
    onGenerateClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Утилита для создания базы данных",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = generationStatus,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isGenerating) {
            CircularProgressIndicator()
        } else {
            Button(onClick = onGenerateClick) {
                Text("Создать базу данных")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(onClick = onExportClick) {
                Text("Экспорт в загрузки (Downloads)")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        outputFilePath?.let {
            Text(
                text = "Путь к файлу базы данных: $it",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Скопируйте этот файл в assets/database/ вашего проекта",
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        cacheFilePath?.let {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Кэш-файл базы данных: $it",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Скопируйте этот файл в assets/database/ вашего проекта",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
} 