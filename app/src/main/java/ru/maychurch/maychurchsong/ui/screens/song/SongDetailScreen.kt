package ru.maychurch.maychurchsong.ui.screens.song

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.ui.theme.FontSizes
import ru.maychurch.maychurchsong.ui.theme.getSongTypography
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    songId: String,
    songViewModel: SongViewModel,
    fontSize: Int,
    onNavigateBack: () -> Unit
) {
    // Загружаем песню при открытии экрана
    LaunchedEffect(songId) {
        songViewModel.loadSong(songId)
    }
    
    val song by songViewModel.currentSong.collectAsState()
    val isLoading by songViewModel.isLoading.collectAsState()
    val error by songViewModel.error.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Показываем ошибку, если она есть
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            songViewModel.clearError()
        }
    }
    
    // Выбираем базовый размер шрифта в зависимости от настроек
    val baseTypography = getSongTypography(fontSize)
    
    // Состояние для хранения масштаба текста 
    // Начальное значение всегда 1.0f, так как базовый размер шрифта уже учитывает настройки пользователя
    var textScale by remember { mutableFloatStateOf(1.0f) }
    
    // Отслеживаем, происходит ли масштабирование в данный момент
    var isScaling by remember { mutableStateOf(false) }
    
    // Минимальный и максимальный масштаб текста
    val minScale = 0.8f
    val maxScale = 2.5f
    
    // Вычисляем актуальный размер шрифта на основе базового размера и масштаба
    val density = LocalDensity.current
    val baseTitleSize = with(density) { baseTypography.titleLarge.fontSize.toPx() }
    val baseBodySize = with(density) { baseTypography.bodyLarge.fontSize.toPx() }
    
    val scaledTitleSize = (baseTitleSize * textScale).sp
    val scaledBodySize = (baseBodySize * textScale).sp
    
    // Адаптивный межстрочный интервал
    val scaledLineHeight = 1.3.em
    
    // Состояние для LazyColumn
    val listState = rememberLazyListState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(song?.title ?: "Загрузка...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    // Кнопки для уменьшения и увеличения текста
                    IconButton(
                        onClick = { 
                            textScale = max(textScale - 0.1f, minScale)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Уменьшить текст"
                        )
                    }
                    
                    IconButton(
                        onClick = { 
                            textScale = min(textScale + 0.1f, maxScale) 
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Увеличить текст"
                        )
                    }
                    
                    IconButton(onClick = { song?.id?.let { songViewModel.toggleFavorite(it) } }) {
                        Icon(
                            imageVector = if (song?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (song?.isFavorite == true) "Удалить из избранного" else "Добавить в избранное"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // Применяем обработку жестов масштабирования на самом верхнем уровне
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        // Применяем масштабирование напрямую
                        textScale = (textScale * zoom).coerceIn(minScale, maxScale)
                        
                        // Если изменение масштаба существенно, считаем это масштабированием
                        if (abs(zoom - 1f) > 0.01f) {
                            isScaling = true
                            
                            // Запускаем короткий таймер для сброса состояния масштабирования
                            coroutineScope.launch {
                                delay(150)
                                isScaling = false
                            }
                        }
                    }
                }
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (song != null) {
                // Используем LazyColumn для более надежной прокрутки
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        // Добавляем отступ снизу для индикатора масштаба
                        .padding(bottom = 40.dp)
                        // Блокируем прокрутку во время масштабирования
                        .pointerInput(isScaling) {
                            // Этот модификатор поглощает жесты, когда isScaling = true,
                            // предотвращая конфликт с жестами прокрутки
                        }
                ) {
                    // Элемент заголовка
                    item {
                        Text(
                            text = song?.title ?: "",
                            style = baseTypography.titleLarge.copy(
                                fontSize = scaledTitleSize,
                                lineHeight = scaledLineHeight
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 20.dp)
                        )
                    }
                    
                    // Элемент текста песни
                    item {
                        Text(
                            text = song?.text ?: "",
                            style = baseTypography.bodyLarge.copy(
                                fontSize = scaledBodySize,
                                lineHeight = scaledLineHeight
                            ),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Отступ внизу
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
                
                // Индикатор масштаба (фиксированное положение внизу экрана)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Масштаб: ${(textScale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                Text(
                    text = "Песня не найдена",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }
    }
} 