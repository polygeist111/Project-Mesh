package com.greybox.projectmesh.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greybox.projectmesh.style.WhiteButton

@Composable
fun SendScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        WhiteButton(onClick = { /*TODO*/ },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            text = "Send File",
            enabled = true)
    }
}