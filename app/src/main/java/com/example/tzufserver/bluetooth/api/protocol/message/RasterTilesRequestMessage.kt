package com.example.tzufserver.bluetooth.api.protocol.message

import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.example.tzufserver.extension.toByteArray
import com.example.tzufserver.extension.toByteList
import com.example.tzufserver.extension.toDouble
import com.example.tzufserver.extension.toInt
import com.example.tzufserver.utils.map.Boundaries
import com.example.tzufserver.utils.map.ZoomType
import com.mapbox.mapboxsdk.geometry.LatLng
import java.lang.Exception
import kotlin.properties.Delegates

class RasterTilesRequestMessage : GeoMessage {
    var requestId by Delegates.notNull<Int>()
    private lateinit var areaOfInterest: Boundaries
    private var zoom by Delegates.notNull<ZoomType>()

    constructor(bytes: ByteArray) : super(bytes) {
        requestId = bytes[REQUEST_ID_INDEX].toInt()
        setAreaOfInterestFromBytes(bytes)
        setZoomFromBytes(bytes)
    }

    constructor(
        requestId: Int,
        areaOfInterest: Boundaries,
        zoom: ZoomType
    ) : super() {
        this.requestId = requestId
        this.areaOfInterest = areaOfInterest
        this.zoom = zoom
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

    private fun setZoomFromBytes(bytes: ByteArray) {
        zoom =
            bytes.sliceArray(ZOOM_START_INDEX until (ZOOM_START_INDEX + TzufMapServerApi.ZOOM_SIZE))
                .toInt()
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
            .plus(zoom.toByteArray())
    }

    fun getAreaOfInterest() = areaOfInterest

    override fun equals(other: Any?): Boolean {
        return (other as? RasterTilesRequestMessage)?.let {
            this.requestId == other.requestId && this.areaOfInterest == other.areaOfInterest && this.zoom == other.zoom
        } ?: false
    }

    fun zoom() = zoom

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
        const val ZOOM_START_INDEX =
            BOTTOM_RIGHT_AREA_OF_INTEREST_START_INDEX + 2 * TzufMapServerApi.COORDINATE_SIZE
    }
}