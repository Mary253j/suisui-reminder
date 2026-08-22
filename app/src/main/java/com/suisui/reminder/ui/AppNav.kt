package com.suisui.reminder.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.suisui.reminder.ui.screens.AddEditEventScreen
import com.suisui.reminder.ui.screens.EventDetailScreen
import com.suisui.reminder.ui.screens.HomeScreen
import com.suisui.reminder.ui.screens.ImportScreen
import com.suisui.reminder.ui.screens.MomentsScreen
import com.suisui.reminder.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val MOMENTS = "moments"
    const val SETTINGS = "settings"
    const val IMPORT = "import"
    const val ADD_EVENT = "add_event?eventId={eventId}"
    const val DETAIL = "detail/{eventId}"

    fun addEvent(eventId: Long? = null): String =
        if (eventId == null) "add_event" else "add_event?eventId=$eventId"

    fun detail(eventId: Long): String = "detail/$eventId"
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(navController)
        }
        composable(
            route = Routes.ADD_EVENT,
            arguments = listOf(navArgument("eventId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: -1L
            AddEditEventScreen(navController, eventId)
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: -1L
            EventDetailScreen(navController, eventId)
        }
        composable(Routes.MOMENTS) {
            MomentsScreen(navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController)
        }
        composable(Routes.IMPORT) {
            ImportScreen(navController)
        }
    }
}
