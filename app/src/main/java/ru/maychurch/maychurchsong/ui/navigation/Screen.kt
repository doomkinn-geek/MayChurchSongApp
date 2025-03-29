package ru.maychurch.maychurchsong.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Экраны приложения
 */
sealed class Screen(
    val route: String, 
    val title: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector
) {
    // Главный экран со списком всех песен
    object Home : Screen(
        route = "home", 
        title = "Все песни",
        icon = Icons.Filled.Home,
        outlinedIcon = Icons.Outlined.Home
    )
    
    // Экран с недавно просмотренными песнями
    object Recent : Screen(
        route = "recent", 
        title = "Недавние",
        icon = Icons.Filled.History,
        outlinedIcon = Icons.Outlined.History
    )
    
    // Экран с избранными песнями
    object Favorites : Screen(
        route = "favorites", 
        title = "Избранное",
        icon = Icons.Filled.Favorite,
        outlinedIcon = Icons.Outlined.Favorite
    )
    
    // Экран настроек
    object Settings : Screen(
        route = "settings", 
        title = "Настройки",
        icon = Icons.Filled.Settings,
        outlinedIcon = Icons.Outlined.Settings
    )
    
    // Экран с детальной информацией о песне
    object SongDetail : Screen(
        route = "song/{songId}", 
        title = "Песня",
        icon = Icons.Filled.Home, // Иконки не используются для этого экрана, так как он не в меню
        outlinedIcon = Icons.Outlined.Home
    ) {
        // Функция для создания маршрута с ID песни
        fun createRoute(songId: String): String = "song/$songId"
        
        // Проверка, является ли маршрут экраном деталей песни
        fun matches(route: String?): Boolean = route?.startsWith("song/") ?: false
    }
    
    companion object {
        // Элементы для нижней навигационной панели
        val bottomNavItems = listOf(Home, Recent, Favorites, Settings)
    }
} 