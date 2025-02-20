package com.greybox.projectmesh.testing

import com.ustadmobile.meshrabiya.vnet.Protocol
import com.ustadmobile.meshrabiya.vnet.VirtualPacket
import com.ustadmobile.meshrabiya.vnet.VirtualRouter
import com.ustadmobile.meshrabiya.vnet.socket.ChainSocketNextHop
import java.net.DatagramPacket
import java.net.InetAddress

class TestVirtualRouter : VirtualRouter {
    override val address: InetAddress = InetAddress.getByName(TestDeviceService.TEST_DEVICE_IP)
    override val localDatagramPort: Int = 4242
    override val networkPrefixLength: Int = 16

    override fun route(packet: VirtualPacket, datagramPacket: DatagramPacket?, virtualNodeDatagramSocket: com.ustadmobile.meshrabiya.vnet.VirtualNodeDatagramSocket?) {
        // no-op for test implementation
    }

    override fun allocateUdpPortOrThrow(virtualDatagramSocketImpl: com.ustadmobile.meshrabiya.vnet.datagram.VirtualDatagramSocketImpl, portNum: Int): Int {
        return portNum
    }

    override fun deallocatePort(protocol: Protocol, portNum: Int) {
        // no-op for test implementation
    }

    override fun lookupNextHopForChainSocket(address: InetAddress, port: Int): ChainSocketNextHop {
        //Return dummy next hop for testing with all required parameters
        return ChainSocketNextHop(
            address = address,
            port = port,
            isFinalDest = true,
            network = null //For testing purposes, we can pass null for the network
        )
    }

    override fun nextMmcpMessageId(): Int = 1
}