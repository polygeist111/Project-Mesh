package com.greybox.projectmesh.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.greybox.projectmesh.R

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem(BottomNavItem.Home.route, stringResource(id = R.string.home), BottomNavItem.Home.icon),
        NavigationItem(BottomNavItem.Network.route, stringResource(id = R.string.network), BottomNavItem.Network.icon),
        NavigationItem(BottomNavItem.Send.route, stringResource(id = R.string.send), BottomNavItem.Send.icon),
        NavigationItem(BottomNavItem.Receive.route, stringResource(id = R.string.receive), BottomNavItem.Receive.icon),
        NavigationItem(BottomNavItem.Settings.route, stringResource(id = R.string.settings), BottomNavItem.Settings.icon)
    )
    NavigationBar {
        val currentRoute = navController.currentDestination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Avoid multiple copies of the same destination when reselecting the same item
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                        // Avoid multiple copies of the same destination when reselecting the same item
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
