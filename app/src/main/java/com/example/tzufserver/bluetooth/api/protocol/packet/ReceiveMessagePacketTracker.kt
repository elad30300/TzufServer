package com.example.tzufserver.bluetooth.api.protocol.packet

import java.lang.Exception

class ReceiveMessagePacketTracker(
    val requestId: Int,
    numberOfParts: Int
) {

    private val packetsData = MutableList<ByteArray?>(numberOfParts) { null }

    fun getData(): ByteArray? {
        if (isDataComplete()) {
            var data = ByteArray(0)
            packetsData.forEach { data = data.plus(it!!) }
            return data
        } else return null
    }

    fun setPacket(packet: Packet) {
        if (isPacketValidForSet(packet)) {
            packetsData[packet.part] = packet.data
        } else throw Exception("try to set packet of but set is non-valid in this case")
    }

    fun isDataComplete() = !(packetsData.any { it == null })

    fun isPacketSet(packetIndex: Int) = packetsData[packetIndex] != null

    fun isPacketNotSet(packetIndex: Int) = !isPacketSet(packetIndex)

    fun isPacketOfSameRequest(packet: Packet) = packet.requestId == this.requestId

    fun isPacketNotOfSameRequest(packet: Packet) = !isPacketOfSameRequest(packet)

    fun isPacketValidForSet(packet: Packet): Boolean =
        packet.part < packetsData.size && isPacketNotSet(packet.part)

}