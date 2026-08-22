package com.suisui.reminder.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.suisui.reminder.ui.Routes

@Composable
fun BottomNav(navController: NavHostController, current: String) {
    NavigationBar {
        NavItem(
            label = "首页",
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            selected = current == Routes.HOME,
            onClick = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME); launchSingleTop = true } }
        )
        NavItem(
            label = "瞬间",
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            selected = current == Routes.MOMENTS,
            onClick = { navController.navigate(Routes.MOMENTS) { popUpTo(Routes.HOME); launchSingleTop = true } }
        )
        NavItem(
            label = "设置",
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            selected = current == Routes.SETTINGS,
            onClick = { navController.navigate(Routes.SETTINGS) { popUpTo(Routes.HOME); launchSingleTop = true } }
        )
    }
}

@Composable
private fun RowScope.NavItem(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = { Text(label) }
    )
}
