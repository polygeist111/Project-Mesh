package com.greybox.projectmesh.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector


sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector){
    data object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    data object Network : BottomNavItem("network", "Network", Icons.Default.Email)
    data object Send : BottomNavItem("send", "Send", Icons.Default.Send)
    data object Receive : BottomNavItem("receive", "Receive", Icons.Default.ArrowDropDown)
    data object Info : BottomNavItem("info", "Info", Icons.Default.Info)
}