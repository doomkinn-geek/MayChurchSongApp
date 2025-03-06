package ru.maychurch.maychurchsong.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.maychurch.maychurchsong.ui.components.SongItem
import ru.maychurch.maychurchsong.ui.navigation.Screen
import ru.maychurch.maychurchsong.ui.screens.song.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    songViewModel: SongViewModel,
    fontSize: Int
) {
    val searchQuery by songViewModel.searchQuery.collectAsState()
    val searchResults by songViewModel.searchResults.collectAsState()
    val isLoading by songViewModel.isLoading.collectAsState()
    val error by songViewModel.error.collectAsState()
    
    var query by remember { mutableStateOf(searchQuery) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Показываем ошибку, если она есть
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            songViewModel.clearError()
        }
    }
    
    // Обновляем поиск при изменении запроса
    LaunchedEffect(query) {
        songViewModel.search(query)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поиск песен") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = { Text("Поиск по названию или тексту") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Поиск"
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистить"
                            )
                        }
                    }
                },
                singleLine = true
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (query.isBlank()) {
                    Text(
                        text = "Введите текст для поиска",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (searchResults.isEmpty()) {
                    Text(
                        text = "Ничего не найдено",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults) { song ->
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
                    }
                }
            }
        }
    }
} 