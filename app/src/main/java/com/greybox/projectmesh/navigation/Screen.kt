package com.greybox.projectmesh.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector){
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Network : Screen("network", "Network", Icons.Default.Email)
    object Send : Screen("send", "Send", Icons.Default.Send)
    object Receive : Screen("receive", "Receive", Icons.Default.ArrowDropDown)
    object Info : Screen("info", "Info", Icons.Default.Info)
}