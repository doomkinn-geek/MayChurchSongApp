package ru.maychurch.maychurchsong.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.ui.screens.favorites.FavoritesScreen
import ru.maychurch.maychurchsong.ui.screens.home.HomeScreen
import ru.maychurch.maychurchsong.ui.screens.recent.RecentScreen
import ru.maychurch.maychurchsong.ui.screens.settings.SettingsScreen
import ru.maychurch.maychurchsong.ui.screens.song.SongDetailScreen
import ru.maychurch.maychurchsong.ui.screens.song.SongViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    // Получаем общую ViewModel для песен
    val songViewModel: SongViewModel = viewModel()
    
    // Получаем текущий размер шрифта
    val fontSize by userPreferences.fontSize.collectAsState(initial = UserPreferences.FONT_SIZE_MEDIUM)
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                songViewModel = songViewModel,
                fontSize = fontSize
            )
        }
        
        composable(Screen.Recent.route) {
            RecentScreen(
                navController = navController,
                songViewModel = songViewModel,
                fontSize = fontSize
            )
        }
        
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                navController = navController,
                songViewModel = songViewModel,
                fontSize = fontSize
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                userPreferences = userPreferences
            )
        }
        
        composable(Screen.SongDetail.route) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: ""
            SongDetailScreen(
                songId = songId,
                songViewModel = songViewModel,
                fontSize = fontSize,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
} 