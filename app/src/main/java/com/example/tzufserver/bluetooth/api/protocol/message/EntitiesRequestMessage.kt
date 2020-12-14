package com.example.tzufserver.bluetooth.api.protocol.message

import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.example.tzufserver.extension.toByteArray
import com.example.tzufserver.extension.toByteList
import com.example.tzufserver.extension.toDouble
import com.example.tzufserver.utils.map.Boundaries
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlin.properties.Delegates

class EntitiesRequestMessage : GeoMessage {
    var requestId by Delegates.notNull<Int>()
    private lateinit var areaOfInterest: Boundaries

    constructor(bytes: ByteArray) : super(bytes) {
        requestId = bytes[REQUEST_ID_INDEX].toInt()
        setAreaOfInterestFromBytes(bytes)
    }

    constructor(
        requestId: Int,
        areaOfInterest: Boundaries
    ) : super() {
        this.requestId = requestId
        this.areaOfInterest = areaOfInterest
    }

    private fun setAreaOfInterestFromBytes(bytes: ByteArray) {
        var currentIndex = TOP_LEFT_AREA_OF_INTEREST_START_INDEX
        var latitude =
            bytes.sliceArray(currentIndex until currentIndex + TzufMapServerApi.COORDINATE_SIZE)
                .toDouble()
        currentIndex += TzufMapServerApi.COORDINATE_SIZE
        var longitude =
            bytes.sliceArray(currentIndex until currentIndex + TzufMapServerApi.COORDINATE_SIZE)
                .toDouble()
        val topLeft = LatLng(latitude, longitude)

        currentIndex = TOP_RIGHT_AREA_OF_INTEREST_START_INDEX
        latitude =
            bytes.sliceArray(currentIndex until currentIndex + TzufMapServerApi.COORDINATE_SIZE)
                .toDouble()
        currentIndex += TzufMapServerApi.COORDINATE_SIZE
        longitude =
            bytes.sliceArray(currentIndex until currentIndex + TzufMapServerApi.COORDINATE_SIZE)
                .toDouble()
        val topRight = LatLng(latitude, longitude)

        currentIndex = BOTTOM_LEFT_AREA_OF_INTEREST_START_INDEX
        latitude =
            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                .toDouble()
        currentIndex += TzufMapServerApi.COORDINATE_SIZE
        longitude =
            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                .toDouble()
        val bottomLeft = LatLng(latitude, longitude)

        currentIndex = BOTTOM_RIGHT_AREA_OF_INTEREST_START_INDEX
        latitude =
            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                .toDouble()
        currentIndex += TzufMapServerApi.COORDINATE_SIZE
        longitude =
            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                .toDouble()
        val bottomRight = LatLng(latitude, longitude)

        areaOfInterest = Boundaries(topLeft, topRight, bottomLeft, bottomRight)
    }

    override suspend fun toByteArray(): ByteArray {
        return ByteArray(0)
            .plus(requestId.toByte())
            .plus(areaOfInterest.topLeft.latitude.toByteArray())
            .plus(areaOfInterest.topLeft.longitude.toByteArray())
            .plus(areaOfInterest.topRight.latitude.toByteArray())
            .plus(areaOfInterest.topRight.longitude.toByteArray())
            .plus(areaOfInterest.bottomLeft.latitude.toByteArray())
            .plus(areaOfInterest.bottomLeft.longitude.toByteArray())
            .plus(areaOfInterest.bottomRight.latitude.toByteArray())
            .plus(areaOfInterest.bottomRight.longitude.toByteArray())
    }

    fun getAreaOfInterest() = areaOfInterest

    override fun equals(other: Any?): Boolean {
        return (other as? EntitiesRequestMessage)?.let {
            this.requestId == other.requestId && this.areaOfInterest == other.areaOfInterest
        } ?: false
    }

    companion object {
        const val REQUEST_ID_INDEX = 0
        const val AREA_OF_INTEREST_START_INDEX = REQUEST_ID_INDEX + 1
        const val TOP_LEFT_AREA_OF_INTEREST_START_INDEX = AREA_OF_INTEREST_START_INDEX
        const val TOP_RIGHT_AREA_OF_INTEREST_START_INDEX =
            AREA_OF_INTEREST_START_INDEX + 2 * TzufMapServerApi.COORDINATE_SIZE
        const val BOTTOM_LEFT_AREA_OF_INTEREST_START_INDEX =
            AREA_OF_INTEREST_START_INDEX + 4 * TzufMapServerApi.COORDINATE_SIZE
        const val BOTTOM_RIGHT_AREA_OF_INTEREST_START_INDEX =
            AREA_OF_INTEREST_START_INDEX + 6 * TzufMapServerApi.COORDINATE_SIZE
    }
}