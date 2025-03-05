package com.greybox.projectmesh.testing

import android.util.Log
import com.greybox.projectmesh.GlobalApp
import com.greybox.projectmesh.GlobalApp.GlobalUserRepo.userRepository
import com.greybox.projectmesh.messaging.data.entities.Message
import kotlinx.coroutines.runBlocking
import java.net.InetAddress

class TestDeviceService {
    companion object {
        const val TEST_DEVICE_IP = "192.168.0.99"
        const val TEST_DEVICE_NAME = "Test Echo Device"

        private var isInitialized = false

        fun initialize() {
            try {
                if (!isInitialized) {
                    runBlocking {
                        val existingUser = userRepository.getUserByIp(TEST_DEVICE_IP)
                        if (existingUser == null) {
                            // If there's no user with this IP, insert one with a "temp" UUID
                            val pseudoUuid = "temp-$TEST_DEVICE_IP"
                            userRepository.insertOrUpdateUser(
                                uuid = pseudoUuid,
                                name = TEST_DEVICE_NAME,
                                address = TEST_DEVICE_IP
                            )
                        } else {
                            // If a user with this IP already exists, just update the name
                            // (keeping the same uuid and address)
                            userRepository.insertOrUpdateUser(
                                uuid = existingUser.uuid,
                                name = TEST_DEVICE_NAME,
                                address = existingUser.address
                            )
                        }
                    }
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