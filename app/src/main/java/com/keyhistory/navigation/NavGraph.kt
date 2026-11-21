package com.keyhistory.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.keyhistory.screens.AboutScreen
import com.keyhistory.screens.DashboardScreen
import com.keyhistory.screens.DetailScreen
import com.keyhistory.screens.ExportScreen
import com.keyhistory.screens.HistoryScreen
import com.keyhistory.screens.PrivacyScreen
import com.keyhistory.screens.SearchScreen
import com.keyhistory.screens.SettingsScreen
import com.keyhistory.screens.StorageScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val SEARCH = "search"
    const val DETAIL = "detail/{id}"
    const val EXPORT = "export"
    const val SETTINGS = "settings"
    const val STORAGE = "storage"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
    fun detail(id: Long) = "detail/$id"
}

@Composable
fun KeyHistoryNavHost() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }
    val open: (String) -> Unit = { nav.navigate(it) { launchSingleTop = true } }

    NavHost(
        navController = nav,
        startDestination = Routes.DASHBOARD,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeIn(tween(260)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(240)) + fadeOut(tween(240)) },
    ) {
        composable(Routes.DASHBOARD) { DashboardScreen(onNavigate = open) }
        composable(Routes.HISTORY) { HistoryScreen(onBack = back, onOpen = { open(Routes.detail(it)) }) }
        composable(Routes.SEARCH) { SearchScreen(onBack = back, onOpen = { open(Routes.detail(it)) }) }
        composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            DetailScreen(id = entry.arguments?.getLong("id") ?: 0L, onBack = back)
        }
        composable(Routes.EXPORT) { ExportScreen(onBack = back) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = back, onNavigate = open) }
        composable(Routes.STORAGE) { StorageScreen(onBack = back) }
        composable(Routes.PRIVACY) { PrivacyScreen(onBack = back) }
        composable(Routes.ABOUT) { AboutScreen(onBack = back) }
    }
}
