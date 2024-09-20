package com.greybox.projectmesh.model

import android.net.Uri

data class SendScreenModel(
    val fileUri: Uri? = null,
    val statusMsg: String? = null,
)