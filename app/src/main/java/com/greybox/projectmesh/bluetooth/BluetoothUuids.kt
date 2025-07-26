package com.greybox.projectmesh.bluetooth
import com.ustadmobile.meshrabiya.util.uuidForMaskAndPort
import java.util.UUID

/*
* These are some UUIDs for our UUID Allocation Service to advertise. These UUIDS
* must be known to client and server
*/
object BluetoothUuids {
    // This UUID is calculated to work with the broken matchesMask logic
    val ALLOCATION_SERVICE_UUID: UUID =
        UUID.fromString("d8a8f0e8-1f62-4b1f-9c38-00007f5b5a1e")

    val ALLOCATION_CHAR_UUID: UUID =
        UUID.fromString("5a8dc9d3-c6b1-4eab-8f7d-3a2f6b0c9e4d")
}