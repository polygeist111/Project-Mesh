package com.greybox.projectmesh.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedUriViewModel: ViewModel() {
    private val _uris = MutableStateFlow<List<Uri>>(emptyList())
    val uris: StateFlow<List<Uri>> get() = _uris

    fun setUris(uriList: List<Uri>) {
        _uris.value = uriList
    }
}