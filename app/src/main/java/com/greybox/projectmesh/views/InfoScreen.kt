package com.greybox.projectmesh.views

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun InfoScreen() {
    Column (modifier = Modifier.fillMaxSize().padding(18.dp)){
        Text("Project Mesh -- Version 1.0")
        Spacer(modifier = Modifier.height(10.dp))
        Text("Device Name: ${Build.MANUFACTURER} ${Build.MODEL}")
        Spacer(modifier = Modifier.height(10.dp))
        Text("Android Version: ${Build.VERSION.RELEASE}")
        Spacer(modifier = Modifier.height(10.dp))
        Text("SDK Version: ${Build.VERSION.SDK_INT}")
        Spacer(modifier = Modifier.height(10.dp))
    }

}