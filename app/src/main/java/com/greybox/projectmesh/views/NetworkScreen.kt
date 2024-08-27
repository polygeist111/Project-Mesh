package com.greybox.projectmesh.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NetworkScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)){
        Text(text = "Network Screen")
    }
}