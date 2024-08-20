package com.greybox.projectmesh.components

import android.content.Context
import com.greybox.projectmesh.navigation.Screen
import com.greybox.projectmesh.views.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
//import com.greybox.projectmesh.model.HomeScreenModel
//import com.greybox.projectmesh.viewModel.HomeScreenViewModel
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode


@Composable
fun BottomNavApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ){ innerPadding ->
        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding))
        {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Network.route) { NetworkScreen() }
            composable(Screen.Send.route) { SendScreen() }
            composable(Screen.Receive.route) { ReceiveScreen() }
            composable(Screen.Info.route) { InfoScreen() }
        }
    }
}