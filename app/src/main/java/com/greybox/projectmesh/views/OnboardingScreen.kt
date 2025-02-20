package com.greybox.projectmesh.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.viewModel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    // 1) Create a custom factory that uses the global singletons
    val factory = remember {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return OnboardingViewModel(
                        userRepository = GlobalApp.GlobalUserRepo.userRepository,
                        prefs = GlobalApp.GlobalUserRepo.prefs
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    // 2) Pass that factory to viewModel(...)
    val onboardingViewModel: OnboardingViewModel = viewModel(factory = factory)

    // 3) Use it as normal
    val uiState by onboardingViewModel.uiState.collectAsState()

    Column {
        Text("Welcome to Project Mesh!")
        Spacer(modifier = Modifier.height(16.dp))

        Text("Please set your username:")
        TextField(
            value = uiState.username,
            onValueChange = { newValue ->
                onboardingViewModel.onUsernameChange(newValue)
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            onboardingViewModel.handleFirstTimeSetup {
                onComplete()
            }
        }) {
            Text("Next")
        }
    }
}