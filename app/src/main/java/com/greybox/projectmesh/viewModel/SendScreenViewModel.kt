package com.greybox.projectmesh.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.model.SendScreenModel
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

class SendScreenViewModel(di: DI): ViewModel() {
    // _uiState will be updated whenever there is a change in the UI state
    private val _uiState = MutableStateFlow(SendScreenModel())
    // uiState is a read-only property that shows the current UI state
    val uiState: Flow<SendScreenModel> = _uiState.asStateFlow()
    // di is used to get the AndroidVirtualNode instance
    private val node: AndroidVirtualNode by di.instance()

    init {
        viewModelScope.launch{
            node.state.collect{
                _uiState.update { prev ->
                    prev.copy(

                    )
                }

            }
        }
    }

    fun onFileChosen(uri: Uri?){
        if (uri != null){
            _uiState.value = _uiState.value.copy(fileUri = uri)
        }
    }
}