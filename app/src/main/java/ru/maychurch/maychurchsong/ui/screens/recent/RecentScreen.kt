package ru.maychurch.maychurchsong.ui.screens.recent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
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
fun RecentScreen(
    navController: NavController,
    songViewModel: SongViewModel,
    fontSize: Int
) {
    val recentSongs by songViewModel.recentSongs.collectAsState()
    val isLoading by songViewModel.isLoading.collectAsState()
    val error by songViewModel.error.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Показываем ошибку, если она есть
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            songViewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Недавние песни") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (recentSongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет недавно просмотренных песен",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Недавно просмотренные",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    item {
                        Text(
                            text = "Всего: ${recentSongs.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    
                    items(recentSongs) { song ->
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
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
} 