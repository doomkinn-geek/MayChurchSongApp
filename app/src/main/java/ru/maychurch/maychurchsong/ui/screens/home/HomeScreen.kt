package ru.maychurch.maychurchsong.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.maychurch.maychurchsong.ui.components.SongItem
import ru.maychurch.maychurchsong.ui.navigation.Screen
import ru.maychurch.maychurchsong.ui.screens.song.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    songViewModel: SongViewModel,
    fontSize: Int
) {
    val songs by songViewModel.allSongs.collectAsState()
    val isLoading by songViewModel.isLoading.collectAsState()
    val error by songViewModel.error.collectAsState()
    val lastUpdateInfo by songViewModel.lastUpdateInfo.collectAsState()
    val isDatabaseInitialized by songViewModel.isDatabaseInitialized.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by songViewModel.searchResults.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Получаем отфильтрованные/найденные песни в зависимости от наличия поискового запроса
    val displayedSongs = remember(searchQuery, songs, searchResults) {
        if (searchQuery.isBlank()) {
            songs.sortedBy { it.title } // Сортировка по алфавиту когда нет поиска
        } else {
            searchResults // Используем результаты поиска
        }
    }
    
    // Обновляем результаты поиска при изменении searchQuery
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            songViewModel.search("") // Очищаем результаты поиска
        } else {
            // Добавляем небольшую задержку, чтобы не запускать поиск после каждого нажатия клавиши
            delay(300) // 300 мс задержка
            songViewModel.search(searchQuery)
        }
    }
    
    // Показываем ошибку, если она есть
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            songViewModel.clearError()
        }
    }
    
    // Автоматически скрываем информацию об обновлении через 5 секунд
    LaunchedEffect(lastUpdateInfo) {
        if (lastUpdateInfo != null) {
            delay(5000)
            songViewModel.clearUpdateInfo()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Духовные песни. Сборник Майской Церкви") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Поле поиска
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth(),
                            placeholder = { Text("Введите номер песни или текст для поиска...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Поиск"
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Очистить"
                                        )
                                    }
                                }
                            },
                            singleLine = true
                        )
                        
                        // Подсказка о поиске
                        /*Text(
                            text = "Поиск работает по номеру (с ведущими нулями или без них), заголовку и тексту песни",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp, end = 4.dp)
                        )*/
                    }
                }
                
                // Показываем информацию об обновлении базы данных
                /*lastUpdateInfo?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }*/
                
                if (isLoading && songs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Загрузка песен...")
                        }
                    }
                } else if (songs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет доступных песен. Перейдите в настройки и нажмите обновить, чтобы загрузить песни.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else if (displayedSongs.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "По запросу \"$searchQuery\" ничего не найдено",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Информация о количестве найденных песен
                        item {
                            val isNumericSearch = searchQuery.trim().matches(Regex("\\d+"))
                            
                            Text(
                                text = if (searchQuery.isBlank()) 
                                    "Всего песен: ${displayedSongs.size}" 
                                else {
                                    if (isNumericSearch) {
                                        // Форматируем номер с ведущими нулями, если нужно
                                        val formattedNumber = try {
                                            val num = searchQuery.trim().toInt()
                                            when {
                                                num < 10 -> "000$num" // Для однозначных чисел
                                                num < 100 -> "00$num" // Для двузначных чисел
                                                num < 1000 -> "0$num" // Для трехзначных чисел
                                                else -> num.toString()
                                            }
                                        } catch (e: Exception) {
                                            searchQuery.trim()
                                        }
                                        
                                        if (displayedSongs.isEmpty()) {
                                            "Песни с номером \"$formattedNumber\" не найдено"
                                        } else if (displayedSongs.size == 1) {
                                            "Найдена песня с номером \"${displayedSongs[0].id}\""
                                        } else {
                                            "Найдено песен по запросу \"$formattedNumber\": ${displayedSongs.size}"
                                        }
                                    } else {
                                        "Найдено песен по запросу \"$searchQuery\": ${displayedSongs.size}"
                                    }
                                },
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        // Список песен
                        items(displayedSongs) { song ->
                            SongItem(
                                song = song,
                                onClick = {
                                    navController.navigate(Screen.SongDetail.createRoute(song.id))
                                },
                                onFavoriteClick = {
                                    songViewModel.toggleFavorite(song.id)
                                },
                                fontSize = fontSize
                            )
                        }
                        
                        // Дополнительное пространство внизу для лучшего UX
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
            
            // Индикатор загрузки
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }
    }
} 