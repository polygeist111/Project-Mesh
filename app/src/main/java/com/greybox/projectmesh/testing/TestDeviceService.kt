package com.greybox.projectmesh.testing

import android.util.Log
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.messaging.data.entities.Message
import java.net.InetAddress

class TestDeviceService {
    companion object {
        const val TEST_DEVICE_IP = "192.168.0.99"
        const val TEST_DEVICE_NAME = "Test Echo Device"

        private var isInitialized = false

        fun initialize() {
            try {
                if (!isInitialized) {
                    // Register our test device with the DeviceInfoManager
                    GlobalApp.DeviceInfoManager.addDevice(TEST_DEVICE_IP, TEST_DEVICE_NAME)
                    isInitialized = true
                    Log.d("TestDeviceService", "Test device initialized successfully with IP: $TEST_DEVICE_IP")
                }
            } catch (e: Exception) {
                Log.e("TestDeviceService", "Failed to initialize test device", e)
            }
        }

        fun getTestDeviceAddress(): InetAddress {
            return InetAddress.getByName(TEST_DEVICE_IP)
        }

        fun isTestDevice(address: InetAddress): Boolean {
            return address.hostAddress == TEST_DEVICE_IP
        }

        fun createEchoResponse(originalMessage: Message): Message {
            return Message(
                id = 0,
                dateReceived = System.currentTimeMillis(),
                content = "Echo: ${originalMessage.content}",
                sender = TEST_DEVICE_NAME,
                chat = originalMessage.chat
            )
        }
    }
}