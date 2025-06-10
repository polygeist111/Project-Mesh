package com.greybox.projectmesh.views

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.extension.getLocalIpFromDI
import com.greybox.projectmesh.viewModel.OnboardingViewModel
import org.kodein.di.compose.localDI

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    // Retrieve DI instance
    val di = localDI()
    // Get the local IP from DI using our helper function
    val localIp = getLocalIpFromDI(di)

    // Create a custom ViewModelProvider.Factory that passes the local IP to OnboardingViewModel
    val factory = remember {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return OnboardingViewModel(
                        userRepository = GlobalApp.GlobalUserRepo.userRepository,
                        prefs = GlobalApp.GlobalUserRepo.prefs,
                        localIp = localIp  // pass the local IP here
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    // Obtain the OnboardingViewModel using the custom factory
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = factory)
    val uiState by onboardingViewModel.uiState.collectAsState()

    // Build your UI
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Welcome to Project Mesh!")
        Spacer(modifier = Modifier.height(16.dp))
        Text("Please set your username:")
        TextField(
            value = uiState.username,
            onValueChange = { newValue -> onboardingViewModel.onUsernameChange(newValue) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            // Check if the username is null or blank
            if (uiState.username.isNullOrBlank()) {
                onboardingViewModel.onUsernameChange("John Doe")
            }

            // Retrieve the updated username from the ViewModel's state
//            val updatedUsername = onboardingViewModel.uiState.value.username ?: "John Doe"
//            Log.d("UsernameValue", "Username = $updatedUsername")

            onboardingViewModel.handleFirstTimeSetup { onComplete() }
        }) {
            Text("Next")
        }
    }
}
