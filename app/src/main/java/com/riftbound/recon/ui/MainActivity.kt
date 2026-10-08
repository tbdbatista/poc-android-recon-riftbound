package com.riftbound.recon.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.riftbound.recon.data.local.ThemeMode
import com.riftbound.recon.domain.model.AuthState
import com.riftbound.recon.ui.auth.AuthScreen
import com.riftbound.recon.ui.screens.*
import com.riftbound.recon.ui.theme.DarkOutlineVariant
import com.riftbound.recon.ui.theme.RiftboundTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }

            RiftboundTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RiftboundAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Collections : Screen("collections", "Coleções", Icons.Default.CollectionsBookmark)
    object Scan : Screen("scan", "Capturar", Icons.Default.CenterFocusStrong)
    object Search : Screen("search", "Buscar", Icons.Default.Search)
    object Compendium : Screen("compendium", "Compêndio", Icons.Default.AutoStories)
    object Settings : Screen("settings", "Configurações", Icons.Default.Settings)
}

@Composable
fun RiftboundAppScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarScreens = listOf(
        Screen.Collections,
        Screen.Scan,
        Screen.Search,
        Screen.Compendium
    )

    Scaffold(
        bottomBar = {
            // Only display bottom navigation on core tabs, hide on detail views
            if (currentRoute in listOf("collections", "scan", "search", "compendium")) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    val currentDestination = navBackStackEntry?.destination
                    bottomBarScreens.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                val authState by viewModel.authState.collectAsState()
                SplashScreen(
                    onAnimationComplete = {
                        val destination = if (authState is AuthState.Authenticated || authState is AuthState.Guest) {
                            "collections"
                        } else {
                            "auth"
                        }
                        navController.navigate(destination) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }
            composable("auth") {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate("collections") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }
            composable("collections") {
                CollectionsScreen(navController, viewModel)
            }
            composable("scan") {
                ScanScreen(navController, viewModel)
            }
            composable("search") {
                SearchScreen(navController, viewModel)
            }
            composable("compendium") {
                CompendiumScreen(navController, viewModel)
            }
            composable(
                route = "collection_detail/{collectionId}",
                arguments = listOf(navArgument("collectionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getLong("collectionId") ?: 0L
                CollectionDetailScreen(collectionId, navController, viewModel)
            }
            composable("settings") {
                SettingsScreen(navController, viewModel)
            }
        }
    }
}

