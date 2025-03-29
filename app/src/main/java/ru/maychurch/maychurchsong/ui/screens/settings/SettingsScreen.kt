package ru.maychurch.maychurchsong.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.workers.WorkManagerHelper
import ru.maychurch.maychurchsong.data.repository.SongRepository
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    onExitAppRequest: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Получаем настройки из UserPreferences
    val isDarkTheme by userPreferences.isDarkTheme.collectAsState(initial = false)
    val useSystemTheme by userPreferences.useSystemTheme.collectAsState(initial = true)
    val fontSize by userPreferences.fontSize.collectAsState(initial = UserPreferences.FONT_SIZE_MEDIUM)
    val interfaceFontSize by userPreferences.interfaceFontSize.collectAsState(initial = UserPreferences.FONT_SIZE_MEDIUM)
    val isAutoUpdateEnabled by userPreferences.isAutoUpdateEnabled.collectAsState(initial = true)
    val updateIntervalHours by userPreferences.updateIntervalHours.collectAsState(initial = UserPreferences.DEFAULT_UPDATE_INTERVAL)
    
    // Прокрутка для длинного контента
    val scrollState = rememberScrollState()
    
    // Состояние для отслеживания процесса обновления
    var isUpdating by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<String?>(null) }
    var updateError by remember { mutableStateOf<String?>(null) }
    
    // Отдельная область для корутин обновления, чтобы они могли быть отменены при уничтожении экрана
    val updateScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Секция темы
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Тема приложения",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Использовать системную тему",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = useSystemTheme,
                            onCheckedChange = {
                                scope.launch {
                                    userPreferences.setUseSystemTheme(it)
                                }
                            }
                        )
                    }
                    
                    if (!useSystemTheme) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Темная тема",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = {
                                    scope.launch {
                                        userPreferences.setDarkTheme(it)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Секция размера шрифта песен
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .selectableGroup()
                ) {
                    Text(
                        text = "Размер шрифта песен",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FontSizeOption(
                        text = "Маленький",
                        selected = fontSize == UserPreferences.FONT_SIZE_SMALL,
                        onClick = {
                            scope.launch {
                                userPreferences.setFontSize(UserPreferences.FONT_SIZE_SMALL)
                            }
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    FontSizeOption(
                        text = "Средний",
                        selected = fontSize == UserPreferences.FONT_SIZE_MEDIUM,
                        onClick = {
                            scope.launch {
                                userPreferences.setFontSize(UserPreferences.FONT_SIZE_MEDIUM)
                            }
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    FontSizeOption(
                        text = "Большой",
                        selected = fontSize == UserPreferences.FONT_SIZE_LARGE,
                        onClick = {
                            scope.launch {
                                userPreferences.setFontSize(UserPreferences.FONT_SIZE_LARGE)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Секция размера шрифта интерфейса
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .selectableGroup()
                ) {
                    Text(
                        text = "Размер шрифта интерфейса",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FontSizeOption(
                        text = "Маленький",
                        selected = interfaceFontSize == UserPreferences.FONT_SIZE_SMALL,
                        onClick = {
                            scope.launch {
                                userPreferences.setInterfaceFontSize(UserPreferences.FONT_SIZE_SMALL)
                            }
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    FontSizeOption(
                        text = "Средний",
                        selected = interfaceFontSize == UserPreferences.FONT_SIZE_MEDIUM,
                        onClick = {
                            scope.launch {
                                userPreferences.setInterfaceFontSize(UserPreferences.FONT_SIZE_MEDIUM)
                            }
                        }
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    FontSizeOption(
                        text = "Большой",
                        selected = interfaceFontSize == UserPreferences.FONT_SIZE_LARGE,
                        onClick = {
                            scope.launch {
                                userPreferences.setInterfaceFontSize(UserPreferences.FONT_SIZE_LARGE)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Секция настроек обновления базы данных
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Обновление базы данных",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Автоматическое обновление",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isAutoUpdateEnabled,
                            onCheckedChange = {
                                scope.launch {
                                    userPreferences.setAutoUpdateEnabled(it)
                                    // Обновляем расписание
                                    if (it) {
                                        WorkManagerHelper.schedulePeriodicSongUpdate(
                                            context, 
                                            updateIntervalHours
                                        )
                                    } else {
                                        WorkManagerHelper.cancelPeriodicSongUpdate(context)
                                    }
                                }
                            }
                        )
                    }
                    
                    if (isAutoUpdateEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Интервал обновления",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectableGroup()
                        ) {
                            UpdateIntervalOption(
                                text = "Каждые 12 часов",
                                selected = updateIntervalHours == UserPreferences.UPDATE_INTERVAL_12_HOURS,
                                onClick = {
                                    scope.launch {
                                        userPreferences.setUpdateIntervalHours(UserPreferences.UPDATE_INTERVAL_12_HOURS)
                                        WorkManagerHelper.schedulePeriodicSongUpdate(
                                            context, 
                                            UserPreferences.UPDATE_INTERVAL_12_HOURS
                                        )
                                    }
                                }
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            UpdateIntervalOption(
                                text = "Каждые 24 часа (раз в день)",
                                selected = updateIntervalHours == UserPreferences.UPDATE_INTERVAL_24_HOURS,
                                onClick = {
                                    scope.launch {
                                        userPreferences.setUpdateIntervalHours(UserPreferences.UPDATE_INTERVAL_24_HOURS)
                                        WorkManagerHelper.schedulePeriodicSongUpdate(
                                            context, 
                                            UserPreferences.UPDATE_INTERVAL_24_HOURS
                                        )
                                    }
                                }
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            UpdateIntervalOption(
                                text = "Каждые 48 часов (раз в 2 дня)",
                                selected = updateIntervalHours == UserPreferences.UPDATE_INTERVAL_48_HOURS,
                                onClick = {
                                    scope.launch {
                                        userPreferences.setUpdateIntervalHours(UserPreferences.UPDATE_INTERVAL_48_HOURS)
                                        WorkManagerHelper.schedulePeriodicSongUpdate(
                                            context, 
                                            UserPreferences.UPDATE_INTERVAL_48_HOURS
                                        )
                                    }
                                }
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            UpdateIntervalOption(
                                text = "Раз в неделю",
                                selected = updateIntervalHours == UserPreferences.UPDATE_INTERVAL_WEEK,
                                onClick = {
                                    scope.launch {
                                        userPreferences.setUpdateIntervalHours(UserPreferences.UPDATE_INTERVAL_WEEK)
                                        WorkManagerHelper.schedulePeriodicSongUpdate(
                                            context, 
                                            UserPreferences.UPDATE_INTERVAL_WEEK
                                        )
                                    }
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Проверка обновлений
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Проверка обновлений",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Найти новые песни на сайте",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Button(
                            onClick = {
                                isUpdating = true
                                updateResult = null
                                updateError = null
                                
                                updateScope.launch {
                                    try {
                                        val repo = SongRepository.getInstance()
                                        val result = repo.refreshSongsFromWebsite(forceUpdate = false)
                                        
                                        if (result.isSuccess) {
                                            val updateInfo = result.getOrNull()
                                            if (updateInfo != null) {
                                                if (updateInfo.newSongsCount > 0) {
                                                    updateResult = "Добавлено ${updateInfo.newSongsCount} новых песен. " +
                                                            "Всего в базе: ${updateInfo.totalSongsCount} песен."
                                                } else {
                                                    updateResult = "Новых песен не найдено. В базе данных уже есть " +
                                                            "${updateInfo.totalSongsCount} песен."
                                                }
                                            } else {
                                                updateResult = "Проверка завершена, но результат неизвестен."
                                            }
                                        } else {
                                            updateError = "Ошибка: ${result.exceptionOrNull()?.message ?: "неизвестная ошибка"}"
                                        }
                                    } catch (e: Exception) {
                                        updateError = "Ошибка: ${e.message ?: "неизвестная ошибка"}"
                                    } finally {
                                        isUpdating = false
                                    }
                                }
                            },
                            enabled = !isUpdating
                        ) {
                            Text("Проверить")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Принудительное обновление
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Принудительное обновление",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Обновить все песни (длительная операция)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Button(
                            onClick = {
                                isUpdating = true
                                updateResult = null
                                updateError = null
                                
                                updateScope.launch {
                                    try {
                                        val repo = SongRepository.getInstance()
                                        val result = repo.refreshSongsFromWebsite(forceUpdate = true)
                                        
                                        if (result.isSuccess) {
                                            val updateInfo = result.getOrNull()
                                            if (updateInfo != null) {
                                                updateResult = "Обновление завершено. Добавлено ${updateInfo.newSongsCount} новых песен, " +
                                                        "обновлено ${updateInfo.updatedSongsCount} существующих песен. " +
                                                        "Всего в базе: ${updateInfo.totalSongsCount} песен."
                                            } else {
                                                updateResult = "Обновление завершено, но результат неизвестен."
                                            }
                                        } else {
                                            updateError = "Ошибка: ${result.exceptionOrNull()?.message ?: "неизвестная ошибка"}"
                                        }
                                    } catch (e: Exception) {
                                        updateError = "Ошибка: ${e.message ?: "неизвестная ошибка"}"
                                    } finally {
                                        isUpdating = false
                                    }
                                }
                            },
                            enabled = !isUpdating
                        ) {
                            Text("Обновить")
                        }
                    }
                    
                    // Индикатор загрузки
                    if (isUpdating) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "Выполняется обновление...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // Результат обновления
                    if (updateResult != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = updateResult!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Ошибка обновления
                    if (updateError != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = updateError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Информация о приложении
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "О приложении",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Духовные песни: сборник текстов песен Майской Церкви",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Версия 1.3.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Источник данных: https://maychurch.ru/songs/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun FontSizeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // null because we're handling the click on the row
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun UpdateIntervalOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // null because we're handling the click on the row
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
} 