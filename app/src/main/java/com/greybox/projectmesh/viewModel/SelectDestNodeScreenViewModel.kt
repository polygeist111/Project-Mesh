package com.greybox.projectmesh.viewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greybox.projectmesh.model.SelectDestNodeScreenModel
import com.greybox.projectmesh.server.AppServer
import com.ustadmobile.meshrabiya.ext.addressToByteArray
import com.ustadmobile.meshrabiya.ext.addressToDotNotation
import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.instance
import java.net.InetAddress

class SelectDestNodeScreenViewModel(
    di: DI,
    private val sendUri: String,
    private val popBackWhenDone: () -> Unit
): ViewModel(){

    private val _uiState = MutableStateFlow(SelectDestNodeScreenModel())
    val uiState: Flow<SelectDestNodeScreenModel> = _uiState.asStateFlow()
    private val appServer: AppServer by di.instance()
    private val node: AndroidVirtualNode by di.instance()

    init {
        _uiState.update { prev ->
            prev.copy(
                uri = sendUri,
            )
        }

        viewModelScope.launch {
            node.state.collect{
                _uiState.update { prev ->
                    prev.copy(
                        allNodes = it.originatorMessages
                    )
                }
            }
        }
    }

    fun onClickReceiver(address: Int){
        // convert the ip address to byte array, then convert it to InetAddress object
        // which can be used to perform network operation
        val inetAddress = InetAddress.getByAddress(address.addressToByteArray())
        _uiState.update { prev ->
            prev.copy(
                contactingInProgressDevice = address.addressToDotNotation()
            )
        }
        viewModelScope.launch {
            val transfer = withContext(Dispatchers.IO){
                try{
                    appServer.addOutgoingTransfer(
                        uri = Uri.parse(sendUri),
                        toNode = inetAddress,
                    )
                }
                catch (e: Exception){
                    Log.e("AppServer", "Exception attempting to send to destination", e)
                    null
                }
            }
            // if transfer is successful, pop back to previous screen
            if (transfer != null)
                popBackWhenDone()
        }
    }
}