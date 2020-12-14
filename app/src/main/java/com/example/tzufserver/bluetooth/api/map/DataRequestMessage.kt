package com.example.tzufserver.bluetooth.api.map

import com.example.tzufserver.bluetooth.api.protocol.message.GeoMessage
import com.example.tzufserver.extension.toByteList
import com.example.tzufserver.extension.toDouble
import com.example.tzufserver.extension.toInt
import com.example.tzufserver.utils.map.Boundaries
import com.example.tzufserver.utils.map.ZoomType
import com.mapbox.mapboxsdk.geometry.LatLng
import java.lang.Exception
import kotlin.properties.Delegates

class DataRequestMessage :
    GeoMessage {
    var requestId by Delegates.notNull<Int>()
    private var fetchEntities by Delegates.notNull<Boolean>()
    private var fetchRasterTiles by Delegates.notNull<Boolean>()
    private lateinit var areaOfInterest: Boundaries
    private var zoom: ZoomType? = null

    constructor(bytes: ByteArray) : super(bytes) {
        requestId = bytes[REQUEST_ID_INDEX].toInt()
        setFetchEntitiesFromBytes(bytes)
        setFetchRasterTilesFromBytes(bytes)
        setAreaOfInterestFromBytes(bytes)
        if (fetchRasterTiles) {
            setZoomFromBytes(bytes)
        }
    }

    constructor(
        requestId: Int,
        fetchEntities: Boolean,
        fetchRasterTiles: Boolean,
        areaOfInterest: Boundaries,
        zoom: ZoomType?
    ) : super() {
        this.requestId = requestId
        this.fetchEntities = fetchEntities
        this.fetchRasterTiles = fetchRasterTiles
        this.areaOfInterest = areaOfInterest
        if (fetchRasterTiles) {
            zoom?.also {
                this.zoom = zoom
            } ?: throw Exception("zoom must be set if fetch raster tiles is enabled")
        }
    }

    private fun setFetchEntitiesFromBytes(bytes: ByteArray) {
        fetchEntities = getBooleanFromFieldValue(bytes[FETCH_ENTITIES_INDEX])
    }

    private fun setFetchRasterTilesFromBytes(bytes: ByteArray) {
        fetchRasterTiles =
            getBooleanFromFieldValue(bytes[FETCH_RASTER_TILES_INDEX])
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
            bytes.sliceArray(ZOOM_START_INDEX until (ZOOM_START_INDEX + TzufMapServerApi.ZOOM_SIZE)).toInt()
    }

    private fun getBooleanFromFieldValue(byte: Byte): Boolean {
        return when (byte.toInt()) {
            TzufMapServerApi.BOOLEAN_TRUE_VALUE -> true
            TzufMapServerApi.BOOLEAN_FALSE_VALUE -> false
            else -> throw Exception("Illegal value")
        }
    }

    override suspend fun toByteArray(): ByteArray {
        val bytes = mutableListOf<Byte>()

        bytes.add(requestId.toByte())

        bytes.add(TzufMapServerApi.booleanToMessageByte(fetchEntities))
        bytes.add(TzufMapServerApi.booleanToMessageByte(fetchRasterTiles))

        bytes.addAll(areaOfInterest.topLeft.latitude.toByteList())
        bytes.addAll(areaOfInterest.topLeft.longitude.toByteList())
        bytes.addAll(areaOfInterest.topRight.latitude.toByteList())
        bytes.addAll(areaOfInterest.topRight.longitude.toByteList())
        bytes.addAll(areaOfInterest.bottomLeft.latitude.toByteList())
        bytes.addAll(areaOfInterest.bottomLeft.longitude.toByteList())
        bytes.addAll(areaOfInterest.bottomRight.latitude.toByteList())
        bytes.addAll(areaOfInterest.bottomRight.longitude.toByteList())

        if (fetchRasterTiles) {
            zoom?.also {
                bytes.addAll(zoom!!.toByteList())
            } ?: throw Exception("zoom must be set if fetch raster tiles is enabled")
        }

        return bytes.toByteArray()
    }

    fun getAreaOfInterest() = areaOfInterest

    fun isFetchEntities() = fetchEntities

    fun isFetchRasterTiles() = fetchRasterTiles

    fun zoom() = zoom

    companion object {
        const val REQUEST_ID_INDEX = 0
        const val FETCH_ENTITIES_INDEX = REQUEST_ID_INDEX + 1
        const val FETCH_RASTER_TILES_INDEX = FETCH_ENTITIES_INDEX + 1
        const val AREA_OF_INTEREST_START_INDEX = FETCH_RASTER_TILES_INDEX + 1
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