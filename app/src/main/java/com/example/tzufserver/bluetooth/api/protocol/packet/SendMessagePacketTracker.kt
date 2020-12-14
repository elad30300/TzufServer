package com.example.tzufserver.bluetooth.api.protocol.packet

import com.example.tzufserver.bluetooth.api.protocol.message.GeoMessage
import com.example.tzufserver.extension.longDivide
import com.example.tzufserver.utils.interfaces.LongInitialized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SendMessagePacketTracker(
    message: GeoMessage,
    val requestId: Int = 0,
    val size: Int,
    val shouldDivide: Boolean = true
) : LongInitialized {

    lateinit var packets: List<Packet>
    private var currentPart = 0
    override var onReadyCallback: (() -> Unit)? = null
    override var isReady: Boolean = false

    init {
        CoroutineScope(Dispatchers.Default).launch {
            val byteArray = message.toByteArray()
            val parts = if (shouldDivide) byteArray.longDivide(size - Packet.HEADER_SIZE) else listOf(byteArray)
            packets = parts.mapIndexed { part, data -> Packet(requestId, parts.size, part, data) }
            onReady()
        }
    }

    fun hasNext(): Boolean {
        return currentPart < packets.size
    }

    fun next(): Packet? {
        if (hasNext()) {
            return packets[currentPart++]
        }
        return null
    }

    companion object {


        fun createFollowingExistingTracker(
            tracker: SendMessagePacketTracker,
            message: GeoMessage,
            size: Int? = null
        ): SendMessagePacketTracker {
            val actualSize = size ?: tracker.size
            return SendMessagePacketTracker(message, tracker.requestId + 1, actualSize)
        }
    }

}