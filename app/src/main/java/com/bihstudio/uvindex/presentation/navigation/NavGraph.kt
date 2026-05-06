package com.bihstudio.uvindex.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bihstudio.uvindex.presentation.screens.language.LanguageScreen
import com.bihstudio.uvindex.presentation.screens.location.LocationSearchScreen
import com.bihstudio.uvindex.presentation.screens.permission.PermissionScreen
import com.bihstudio.uvindex.presentation.screens.splash.SplashScreen
import com.bihstudio.uvindex.presentation.screens.uvindex.UVIndexScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Splash.route) {
            SplashScreen(onFinished = {
                navController.navigate(Screen.Language.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Language.route) {
            LanguageScreen(onLanguageSelected = {
                navController.navigate(Screen.Permission.route) {
                    popUpTo(Screen.Language.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Permission.route) {
            PermissionScreen(onPermissionsHandled = {
                navController.navigate(Screen.UVIndex.route) {
                    popUpTo(Screen.Permission.route) { inclusive = true }
                }
            })
        }

        composable(Screen.UVIndex.route) {
            UVIndexScreen(onNavigateToLocation = {
                navController.navigate(Screen.Location.route)
            })
        }

        composable(Screen.Location.route) {
            LocationSearchScreen(onBack = {
                navController.popBackStack()
            })
        }
    }
}
