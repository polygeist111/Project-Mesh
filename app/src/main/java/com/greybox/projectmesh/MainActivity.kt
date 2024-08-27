package com.greybox.projectmesh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.greybox.projectmesh.debug.CrashHandler
import com.greybox.projectmesh.debug.CrashScreenActivity
import com.greybox.projectmesh.navigation.BottomNavItem
import com.greybox.projectmesh.navigation.BottomNavigationBar
import com.greybox.projectmesh.views.HomeScreen
import com.greybox.projectmesh.views.InfoScreen
import com.greybox.projectmesh.views.NetworkScreen
import com.greybox.projectmesh.views.ReceiveScreen
import com.greybox.projectmesh.views.SendScreen
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI

class MainActivity : ComponentActivity(), DIAware {
    override val di by closestDI()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BottomNavApp(di)
        }
        // crash screen
        CrashHandler.init(applicationContext,CrashScreenActivity::class.java)
    }
}

@Composable
fun BottomNavApp(di: DI){
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ){ innerPadding ->
        NavHost(navController, startDestination = BottomNavItem.Home.route, Modifier.padding(innerPadding))
        {
            composable(BottomNavItem.Home.route) { HomeScreen() }
            composable(BottomNavItem.Network.route) { NetworkScreen() }
            composable(BottomNavItem.Send.route) { SendScreen() }
            composable(BottomNavItem.Receive.route) { ReceiveScreen() }
            composable(BottomNavItem.Info.route) { InfoScreen() }
        }
    }
}
