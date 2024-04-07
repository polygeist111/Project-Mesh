package com.greybox.projectmesh

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.greybox.projectmesh.ui.theme.ProjectMeshTheme
import com.ustadmobile.meshrabiya.ext.bssidDataStore
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load layout
        setContentView(R.layout.activity_main)

        // Initialise Meshrabiya
        initMesh();
    }

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "meshr_settings")
    private fun initMesh()
    {
        // Create DataStore for network
        //val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "meshr_settings")
        val thisNode = AndroidVirtualNode(
            appContext = applicationContext,
            dataStore = applicationContext.dataStore
        )

    }
}