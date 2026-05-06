package com.bihstudio.uvindex.presentation.navigation

sealed class Screen(val route: String) {
    object Splash      : Screen("splash")
    object Language    : Screen("language")
    object Permission  : Screen("permission")
    object UVIndex     : Screen("uv_index")
    object Location    : Screen("location")
}
