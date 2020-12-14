package com.example.tzufserver.bluetooth.api.protocol.packet

import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi

data class AckPacketMessage(
    val requestId: Int,
    val packet: Int,
    val ackValue: Boolean
) {

    fun toByteArray(): ByteArray {
        return ByteArray(3).apply {
            this[REQUEST_ID_INDEX] = requestId.toByte()
            this[PACKET_INDEX] = packet.toByte()
            this[ACK_VALUE_INDEX] = TzufMapServerApi.booleanToMessageByte(ackValue)
        }
    }

    companion object {

        fun fromByteArray(bytes: ByteArray) = AckPacketMessage(
            bytes[REQUEST_ID_INDEX].toInt(),
            bytes[PACKET_INDEX].toInt(),
            TzufMapServerApi.messageByteToBoolean(bytes[ACK_VALUE_INDEX])
        )

        fun fromPacket(packet: Packet, ackValue: Boolean = true) =
            AckPacketMessage(packet.requestId, packet.part, ackValue)

        const val REQUEST_ID_INDEX = 0
        const val PACKET_INDEX = 1
        const val ACK_VALUE_INDEX = 2
    }

}