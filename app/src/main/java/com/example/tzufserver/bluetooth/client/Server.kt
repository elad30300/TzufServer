package com.example.tzufserver.bluetooth.client

import android.bluetooth.BluetoothDevice
import com.example.tzufserver.bluetooth.api.protocol.packet.Packet
import com.example.tzufserver.bluetooth.api.protocol.packet.ReceiveMessagePacketTracker
import java.lang.Exception
import java.util.*

class Server(val device: BluetoothDevice) {
    private val receiveTrackerMap = mutableMapOf<UUID, ReceiveMessagePacketTracker>()
    private val sendRequestIds = mutableMapOf<UUID, Int>()
    private val receiveRequestIds = mutableMapOf<UUID, Int>()

    fun getReceiveTracker(uuid: UUID): ReceiveMessagePacketTracker? = receiveTrackerMap[uuid]

    fun hasReceiveTracker(uuid: UUID): Boolean = receiveTrackerMap.containsKey(uuid)

    private fun setReceiveTracker(uuid: UUID, tracker: ReceiveMessagePacketTracker) {
        receiveTrackerMap[uuid] = tracker
    }

    fun createReceiveTracker(uuid: UUID, numberOfParts: Int): ReceiveMessagePacketTracker {
        val tracker = ReceiveMessagePacketTracker(getReceiveRequestId(uuid), numberOfParts)
        setReceiveTracker(uuid, tracker)
        return tracker
    }

    fun removeReceiveTracker(uuid: UUID) {
        receiveTrackerMap.remove(uuid)
    }

    fun getReceiveRequestId(uuid: UUID) = receiveRequestIds[uuid]
        ?: throw Exception("try to get receive request id of un-tracked uuid $uuid")

    fun setReceiveRequestId(uuid: UUID, requestId: Int) {
        receiveRequestIds[uuid] = requestId
    }

    fun getSendRequestId(uuid: UUID) = sendRequestIds[uuid]
        ?: throw Exception("try to get send request id of un-tracked uuid $uuid")

    fun generateNextSendRequestId(uuid: UUID): Int {
        sendRequestIds[uuid] = (sendRequestIds[uuid] ?: 0) + 1
        return getSendRequestId(uuid)
    }

    fun doesReceivingPacketBelongToMessage(packet: Packet, uuid: UUID): Boolean {
        return try {
            getReceiveRequestId(uuid)?.let {
                it == packet.requestId
            }
        } catch (ex: Exception) {
            false
        }
    }

    fun isNeedToSetReceiveTracker(uuid: UUID): Boolean = getReceiveTracker(uuid) == null
}