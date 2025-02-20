package com.greybox.projectmesh.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import com.greybox.projectmesh.user.UserRepository
import android.content.SharedPreferences

/**
 * - Generates/stores UUID if needed
 * - Updates local DB with the new user record
 */
class OnboardingViewModel(
    private val userRepository: UserRepository,  // or a DAO if you prefer
    private val prefs: SharedPreferences
) : ViewModel() {

    // UI state exposed as a Flow/StateFlow
    private val _uiState = MutableStateFlow(OnboardingUiState(username = ""))
    val uiState = _uiState.asStateFlow()

    /**
     * Called when the user types into the username TextField.
     */
    fun onUsernameChange(newUsername: String) {
        _uiState.value = _uiState.value.copy(username = newUsername)
    }

    /**
     * Save the username and do any first-time setup, e.g. generate UUID, insert DB record, etc.
     * Called when user presses "Next" in OnboardingScreen.
     */
    fun handleFirstTimeSetup(onComplete: () -> Unit) {
        viewModelScope.launch {
            // 1) If no UUID in SharedPreferences, generate one
            var uuid = prefs.getString("UUID", null)
            if (uuid.isNullOrEmpty()) {
                uuid = UUID.randomUUID().toString()
                prefs.edit().putString("UUID", uuid).apply()
            }

            // 2) Insert or update the user in your DB
            //    This might differ based on your data model
            userRepository.insertOrUpdateUser(
                uuid = uuid,
                name = _uiState.value.username
            )
            prefs.edit().putString("device_name", _uiState.value.username).apply()

            // 3) Mark onboarding as complete (e.g., set hasRunBefore = true)
            prefs.edit().putBoolean("hasRunBefore", true).apply()

            // 4) Callback to navigate away from onboarding
            onComplete()
        }
    }
}

/**
 * Simple UI state data class for the Onboarding screen.
 */
data class OnboardingUiState(
    val username: String
)