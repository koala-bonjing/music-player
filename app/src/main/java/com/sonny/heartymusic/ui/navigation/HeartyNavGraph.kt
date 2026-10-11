package com.sonny.heartymusic.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sonny.heartymusic.HeartyApp
import com.sonny.heartymusic.ui.components.MiniPlayerBar
import com.sonny.heartymusic.ui.screens.home.HomeScreen
import com.sonny.heartymusic.ui.screens.library.LibraryScreen
import com.sonny.heartymusic.ui.screens.player.PlayerScreen
import com.sonny.heartymusic.ui.screens.search.SearchScreen
import com.sonny.heartymusic.ui.screens.search.SearchViewModel
import com.sonny.heartymusic.ui.theme.CozyAccent
import com.sonny.heartymusic.ui.theme.CozyAccentSoft
import com.sonny.heartymusic.ui.theme.CozyBg
import com.sonny.heartymusic.ui.theme.CozyMuted
import com.sonny.heartymusic.ui.theme.CozyNavBg
import com.sonny.heartymusic.ui.theme.CozyText

private val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Library
)

@Composable
fun HeartyMainScreen(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val app = context.applicationContext as HeartyApp

    val searchViewModel: SearchViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(
                    app.container.musicRepository,
                    app.container.musicPlayerManager
                ) as T
            }
        }
    )

    val playerState by app.container.musicPlayerManager.playerState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    var isPlayerExpanded by remember { mutableStateOf(false) }
    val currentSong = playerState.currentSong

    BackHandler(enabled = isPlayerExpanded) {
        isPlayerExpanded = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = CozyBg,
            bottomBar = {
                Column {
                    MiniPlayerBar(
                        playerState = playerState,
                        onTogglePlayPause = { app.container.musicPlayerManager.togglePlayPause() },
                        onExpandPlayer = { isPlayerExpanded = true }
                    )
                    HeartyBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToSearchWithQuery = { query ->
                            searchViewModel.onQueryChange(query)
                            searchViewModel.search()
                            navController.navigate(Screen.Search.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        currentPlayingSong = playerState.currentSong,
                        isPlaying = playerState.isPlaying
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(viewModel = searchViewModel)
                }
                composable(Screen.Library.route) {
                    LibraryScreen()
                }
            }
        }

        AnimatedVisibility(
            visible = isPlayerExpanded && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (currentSong != null) {
                PlayerScreen(
                    song = currentSong,
                    playerState = playerState,
                    onTogglePlayPause = { app.container.musicPlayerManager.togglePlayPause() },
                    onSeekTo = { posMs -> app.container.musicPlayerManager.seekTo(posMs) },
                    onDismiss = { isPlayerExpanded = false }
                )
            }
        }
    }
}

@Composable
private fun HeartyBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = CozyNavBg,
        contentColor = CozyText
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(screen.route) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = stringResource(id = screen.titleRes)
                    )
                },
                label = {
                    Text(text = stringResource(id = screen.titleRes))
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = CozyAccentSoft,
                    selectedIconColor = CozyAccent,
                    selectedTextColor = CozyAccent,
                    unselectedIconColor = CozyMuted,
                    unselectedTextColor = CozyMuted
                )
            )
        }
    }
}
