package com.greybox.projectmesh.extension

import com.ustadmobile.meshrabiya.vnet.AndroidVirtualNode
import org.kodein.di.DI
import org.kodein.di.instance

fun getLocalIpFromDI(di: DI): String {
    // Retrieve the AndroidVirtualNode from DI and return its IP address
    val node: AndroidVirtualNode by di.instance()
    return node.address.hostAddress
}