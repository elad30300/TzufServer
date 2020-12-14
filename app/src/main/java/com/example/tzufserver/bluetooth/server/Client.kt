package com.example.tzufserver.bluetooth.server

import android.bluetooth.BluetoothDevice
import com.example.tzufserver.bluetooth.api.protocol.message.GeoMessage
import com.example.tzufserver.bluetooth.api.protocol.packet.SendMessagePacketTracker
import java.util.*

class Client(val device: BluetoothDevice) {
    var classicBluetoothDevice: BluetoothDevice? = null

    var registeredToEntitiesNotifications = false
    var registeredToRasterTilessNotifications = false
    //        var currentEntitiesRequestId by Delegates.notNull<Int>()
//        var currentRasterTilesRequestId by Delegates.notNull<Int>()
    private val uuidSendRequestIdsMap = mutableMapOf<UUID, Int>()
    private val sendTrackersMap = mutableMapOf<UUID, SendMessagePacketTracker>()

    // TODO: test those functions
    fun getSendTracker(uuid: UUID): SendMessagePacketTracker? = sendTrackersMap[uuid]

    fun hasSendTracker(uuid: UUID): Boolean = sendTrackersMap.containsKey(uuid)

    private fun setSendTracker(uuid: UUID, tracker: SendMessagePacketTracker) {
        sendTrackersMap[uuid] = tracker
    }

    fun removeSendTracker(uuid: UUID) {
        sendTrackersMap.remove(uuid)
    }

    fun createSendTracker(uuid: UUID, message: GeoMessage, size: Int, shouldDivide: Boolean = true): SendMessagePacketTracker {
        val tracker = SendMessagePacketTracker(message, getSendRequestId(uuid), size, shouldDivide)
        this.setSendTracker(uuid, tracker)
        return tracker
    }

    fun setSendRequestId(uuid: UUID, requestId: Int) {
        uuidSendRequestIdsMap[uuid] = requestId
    }

    fun getSendRequestId(uuid: UUID): Int =
        uuidSendRequestIdsMap[uuid]
            ?: throw Exception("get request id of un-tracked uuid $uuid")

}