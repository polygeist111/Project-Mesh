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
    private val _uiState = MutableStateFlow(SendScreenModel())
    val uiState: Flow<SendScreenModel> = _uiState.asStateFlow()
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

//    fun onFileChosen(uri: Uri?){
//        if (uri != null){
//            uiState.
//        }
//    }
}