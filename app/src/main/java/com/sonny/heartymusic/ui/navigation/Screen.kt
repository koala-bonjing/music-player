package com.sonny.heartymusic.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.sonny.heartymusic.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        titleRes = R.string.nav_home,
        icon = Icons.Rounded.Home
    )

    data object Search : Screen(
        route = "search",
        titleRes = R.string.nav_search,
        icon = Icons.Rounded.Search
    )

    data object Library : Screen(
        route = "library",
        titleRes = R.string.nav_library,
        icon = Icons.Rounded.FavoriteBorder
    )
}
