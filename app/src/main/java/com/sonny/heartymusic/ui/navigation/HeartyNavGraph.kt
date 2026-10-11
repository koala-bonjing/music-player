package com.sonny.heartymusic.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sonny.heartymusic.ui.screens.home.HomeScreen
import com.sonny.heartymusic.ui.screens.library.LibraryScreen
import com.sonny.heartymusic.ui.screens.search.SearchScreen
import com.sonny.heartymusic.ui.theme.HeartyBackground
import com.sonny.heartymusic.ui.theme.HeartyPrimary
import com.sonny.heartymusic.ui.theme.HeartyPrimaryContainer
import com.sonny.heartymusic.ui.theme.HeartySurface
import com.sonny.heartymusic.ui.theme.HeartyText
import com.sonny.heartymusic.ui.theme.HeartyTextSecondary

private val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Library
)

@Composable
fun HeartyMainScreen(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = HeartyBackground,
        bottomBar = {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Search.route) {
                SearchScreen()
            }
            composable(Screen.Library.route) {
                LibraryScreen()
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
        containerColor = HeartySurface,
        contentColor = HeartyText
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
                    indicatorColor = HeartyPrimaryContainer,
                    selectedIconColor = HeartyPrimary,
                    selectedTextColor = HeartyText,
                    unselectedIconColor = HeartyTextSecondary,
                    unselectedTextColor = HeartyTextSecondary
                )
            )
        }
    }
}
