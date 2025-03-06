package ru.maychurch.maychurchsong.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.maychurch.maychurchsong.data.preferences.UserPreferences
import ru.maychurch.maychurchsong.ui.navigation.NavigationGraph
import ru.maychurch.maychurchsong.ui.navigation.Screen

@Composable
fun MainLayout(
    userPreferences: UserPreferences
) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val isSongDetailOpen = currentDestination?.route?.startsWith("song/") ?: false
                
                Screen.bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            // Проверяем, открыта ли сейчас песня и пользователь нажал "Главная"
                            if (isSongDetailOpen && screen.route == Screen.Home.route) {
                                // Если так, возвращаемся назад к списку песен
                                navController.navigateUp()
                            } else {
                                // Стандартная навигация
                                navController.navigate(screen.route) {
                                    // Избегаем создания множества экземпляров экранов при повторной навигации
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Предотвращаем дублирование экранов в стеке навигации
                                    launchSingleTop = true
                                    // Сохраняем состояние при переключении между экранами
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            NavigationGraph(
                navController = navController,
                userPreferences = userPreferences
            )
        }
    }
} 