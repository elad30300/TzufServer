package com.example.tzufserver.bluetooth.api.protocol.packet

import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.example.tzufserver.bluetooth.server.TzufGattServer
import com.example.tzufserver.extension.toByteArray
import com.example.tzufserver.extension.toInt

data class Packet(
    val requestId: Int,
    val totalNumberOfParts: Int,
    val part: Int,
    val data: ByteArray
) {

    fun toByteArray(): ByteArray {
        return ByteArray(0)
            .plus(requestId.toByte())
            .plus(totalNumberOfParts.toByteArray())
            .plus(part.toByteArray())
            .plus(data)
    }

    // recommended methods to generate accroding android studio in data class with ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Packet

        if (requestId != other.requestId) return false
        if (totalNumberOfParts != other.totalNumberOfParts) return false
        if (part != other.part) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = requestId
        result = 31 * result + totalNumberOfParts
        result = 31 * result + part
        result = 31 * result + data.contentHashCode()
        return result
    }

    companion object {
        const val REQUEST_ID_INDEX = 0
        const val TOTAL_NUM_OF_PARTS_INDEX = REQUEST_ID_INDEX + 1
        const val TOTAL_NUM_OF_PARTS_SIZE = 4 // todo: change to sizeof int
        const val PART_INDEX = TOTAL_NUM_OF_PARTS_INDEX + TOTAL_NUM_OF_PARTS_SIZE
        const val PART_SIZE = 4
        const val HEADER_SIZE = 1 + TOTAL_NUM_OF_PARTS_SIZE + PART_SIZE
        private const val DATA_START_INDEX = HEADER_SIZE

        fun fromByteArray(bytes: ByteArray) = Packet(
            bytes[REQUEST_ID_INDEX].toInt(),
            bytes.sliceArray(TOTAL_NUM_OF_PARTS_INDEX until (TOTAL_NUM_OF_PARTS_INDEX + TOTAL_NUM_OF_PARTS_SIZE)).toInt(),
            bytes.sliceArray(PART_INDEX until (PART_INDEX + PART_SIZE)).toInt(),
            bytes.sliceArray(DATA_START_INDEX until bytes.size)
        )
    }

}