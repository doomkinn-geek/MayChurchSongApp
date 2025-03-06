package ru.maychurch.maychurchsong.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Главная",
        icon = Icons.Default.Home
    )
    
    data object Recent : Screen(
        route = "recent",
        title = "Недавние",
        icon = Icons.Default.History
    )
    
    data object Favorites : Screen(
        route = "favorites",
        title = "Избранное",
        icon = Icons.Default.Favorite
    )
    
    data object Settings : Screen(
        route = "settings",
        title = "Настройки",
        icon = Icons.Default.Settings
    )
    
    data object SongDetail : Screen(
        route = "song/{songId}",
        title = "Песня",
        icon = Icons.Default.Home
    ) {
        fun createRoute(songId: String): String {
            return "song/$songId"
        }
    }
    
    companion object {
        val bottomNavItems = listOf(Home, Recent, Favorites, Settings)
    }
} 