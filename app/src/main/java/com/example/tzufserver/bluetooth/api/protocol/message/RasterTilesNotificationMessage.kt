package com.example.tzufserver.bluetooth.api.protocol.message

import android.graphics.Bitmap
import android.util.Log
import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.example.tzufserver.extension.toDouble
import com.example.tzufserver.extension.toInt
import com.example.tzufserver.provider.dispatchers.DefaultDispatcherProvider
import com.example.tzufserver.provider.dispatchers.DispatcherProvider
import com.example.tzufserver.utils.BitmapUtils
import com.example.tzufserver.utils.map.Tile
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RasterTilesNotificationMessage :
    GeoMessage {

    private var tiles = mutableListOf<Tile>()
//    override var isReady: Boolean = false
//    override var onReadyCallback: (() -> Unit)? = null

    constructor(
        tiles: List<Tile>,
        dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
    ) : super(dispatcherProvider) {
        this.tiles = tiles.toMutableList()
    }

//    private suspend fun setTilesListFromBytes(bytes: ByteArray) {
////        var listBytes = bytes.drop(TzufMapServerApi.RASTER_TILES_LIST_START_INDEX).toByteArray()
//        var listBytes = bytes
//        val parsingDeffereds = mutableListOf<Deferred<Tile>>()
//        val tilesMutex = Mutex()
////        val bytesMutex = Mutex()
//        while (listBytes.isNotEmpty()) {
//            with(listBytes) {
//                val deffered = CoroutineScope(currentCoroutineContext()).async {
//                    cropTileFromList(this@with)?.also { tile ->
////                    synchronized(tiles) {
//                        tilesMutex.withLock {
//                            tiles.add(tile)
//                        }
////                    }
//                        Log.d(TAG, "add tile to list, now size is ${tiles.size}")
//                    } ?: throw Exception("Tile Notification - problem in parsing message")
//                }
//                parsingDeffereds.add(deffered)
//            }
//            listBytes = listBytes.drop(findNumberOfBytesToDrop(listBytes)).toByteArray()
////            listBytes = listBytes.copyOfRange(findNumberOfBytesToDrop(listBytes), listBytes.size)
//        }
//        parsingDeffereds.forEach {
//            try {
//                it.await()
//            } catch (ex: java.lang.Exception) {
//                Log.d(TAG, "in excetion in await for parsing to tiles, ex: $ex")
//                throw ex
//            }
//        }
//    }
//
//    private fun cropTileFromList(bytes: ByteArray): Tile? {
//        Log.d(TAG, "start to crop tile")
//
//        var currentIndex =
//            TzufMapServerApi.RASTER_TILE_ZOOM_START_LOCAL_INDEX
//        val zoom =
//            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.ZOOM_SIZE)).toInt()
//
//        currentIndex =
//            TzufMapServerApi.TOP_LEFT_RASTER_TILE_START_LOCAL_INDEX
//        var latitude =
//            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
//                .toDouble()
//        currentIndex += TzufMapServerApi.COORDINATE_SIZE
//        var longitude =
//            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
//                .toDouble()
//        val topLeft = LatLng(latitude, longitude)
//
//        currentIndex =
//            TzufMapServerApi.TOP_RIGHT_RASTER_TILE_START_LOCAL_INDEX
//        latitude =
//            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
//                .toDouble()
//        currentIndex += TzufMapServerApi.COORDINATE_SIZE
//        longitude =
//            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
//                .toDouble()
//        val topRight = LatLng(latitude, longitude)
//
//        currentIndex =
//            TzufMapServerApi.BOTTOM_LEFT_RASTER_TILE_START_LOCAL_INDEX
//        latitude =
//            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
//                .toDouble()
//        currentIndex += TzufMapServerApi.COORDINATE_SIZE
//        longitude =
//            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
//                .toDouble()
//        val bottomLeft = LatLng(latitude, longitude)
//
//        currentIndex =
//            TzufMapServerApi.BOTTOM_RIGHT_RASTER_TILE_START_LOCAL_INDEX
//        latitude =
//            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
//                .toDouble()
//        currentIndex += TzufMapServerApi.COORDINATE_SIZE
//        longitude =
//            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
//                .toDouble()
//        val bottomRight = LatLng(latitude, longitude)
//
//        currentIndex =
//            TzufMapServerApi.BITMAP_LENGTH_RASTER_TILE_LOCAL_INDEX
//        val bitmapDataLength =
//            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.BITMAP_LENGTH_SIZE))
//                .toInt()
//
//        var bitmap: Bitmap? = null
//        if (bitmapDataLength > 0) {
//            currentIndex =
//                TzufMapServerApi.BITMAP_DATA_RASTER_TILE_LOCAL_INDEX
//            val bitmapData =
//                bytes.sliceArray(currentIndex until (currentIndex + bitmapDataLength))
//            bitmap = BitmapUtils.createFrom(bitmapData)
//            if (bitmap == null) {
//                Log.d(TAG, "couldn't decode bitmap of tile")
//            }
//        }
//
//        return Tile(topLeft, topRight, bottomLeft, bottomRight, bitmap, zoom)
//    }
//
//    private fun findNumberOfBytesToDrop(bytes: ByteArray): Int {
//        var currentIndex =
//            TzufMapServerApi.BITMAP_LENGTH_RASTER_TILE_LOCAL_INDEX
//        val bitmapDataLength =
//            bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.BITMAP_LENGTH_SIZE))
//                .toInt()
//        val bytesToDrop =
//            TzufMapServerApi.ZOOM_SIZE + 8 * TzufMapServerApi.COORDINATE_SIZE + TzufMapServerApi.BITMAP_LENGTH_SIZE + bitmapDataLength
//
//        return bytesToDrop
//    }

    override suspend fun toByteArray(): ByteArray {
        return withContext(dispatcherProvider.default()) {
            var bytes = super.toByteArray()

            val defferds = mutableListOf<Deferred<Unit>>()
            val mutex = Mutex()
            tiles.forEach { tile ->
                defferds.add(async {
                    mutex.withLock {
                        bytes = bytes.plus(tile.toByteArray())
                    }
                })
            }
            defferds.forEach {
                try {
                    it.await()
                } catch (ex: java.lang.Exception) {
                    Log.d(TAG, "in excetion in await for parsing to bytearray")
                    throw ex
                }
            }
            return@withContext bytes
        }
    }

    fun getTiles(): List<Tile> = tiles

    companion object {
        private const val TAG = "RasterTilesNotif"

        suspend fun fromByteArray(bytes: ByteArray, dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()): RasterTilesNotificationMessage {
//        var listBytes = bytes.drop(TzufMapServerApi.RASTER_TILES_LIST_START_INDEX).toByteArray()
            val tiles = mutableListOf<Tile>()
            var listBytes = bytes
            val parsingDeffereds = mutableListOf<Deferred<Tile>>()
            val tilesMutex = Mutex()
//        val bytesMutex = Mutex()
            while (listBytes.isNotEmpty()) {
                with(listBytes) {
                    val deffered = CoroutineScope(dispatcherProvider.default()).async {
                        cropTileFromList(this@with)?.also { tile ->
//                    synchronized(tiles) {
                            tilesMutex.withLock {
                                tiles.add(tile)
                            }
//                    }
                            Log.d(TAG, "add tile to list, now size is ${tiles.size}")
                        } ?: throw Exception("Tile Notification - problem in parsing message")
                    }
                    parsingDeffereds.add(deffered)
                }
                listBytes = listBytes.drop(findNumberOfBytesToDrop(listBytes)).toByteArray()
//            listBytes = listBytes.copyOfRange(findNumberOfBytesToDrop(listBytes), listBytes.size)
            }
            parsingDeffereds.forEach {
                try {
                    it.await()
                } catch (ex: java.lang.Exception) {
                    Log.d(TAG, "in excetion in await for parsing to tiles, ex: $ex")
                    throw ex
                }
            }

            return RasterTilesNotificationMessage(tiles)
        }

        private fun cropTileFromList(bytes: ByteArray): Tile? {
            Log.d(TAG, "start to crop tile")

            var currentIndex =
                TzufMapServerApi.RASTER_TILE_ZOOM_START_LOCAL_INDEX
            val zoom =
                bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.ZOOM_SIZE)).toInt()

            currentIndex =
                TzufMapServerApi.TOP_LEFT_RASTER_TILE_START_LOCAL_INDEX
            var latitude =
                bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                    .toDouble()
            currentIndex += TzufMapServerApi.COORDINATE_SIZE
            var longitude =
                bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                    .toDouble()
            val topLeft = LatLng(latitude, longitude)

            currentIndex =
                TzufMapServerApi.TOP_RIGHT_RASTER_TILE_START_LOCAL_INDEX
            latitude =
                bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                    .toDouble()
            currentIndex += TzufMapServerApi.COORDINATE_SIZE
            longitude =
                bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                    .toDouble()
            val topRight = LatLng(latitude, longitude)

            currentIndex =
                TzufMapServerApi.BOTTOM_LEFT_RASTER_TILE_START_LOCAL_INDEX
            latitude =
                bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                    .toDouble()
            currentIndex += TzufMapServerApi.COORDINATE_SIZE
            longitude =
                bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                    .toDouble()
            val bottomLeft = LatLng(latitude, longitude)

            currentIndex =
                TzufMapServerApi.BOTTOM_RIGHT_RASTER_TILE_START_LOCAL_INDEX
            latitude =
                bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                    .toDouble()
            currentIndex += TzufMapServerApi.COORDINATE_SIZE
            longitude =
                bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                    .toDouble()
            val bottomRight = LatLng(latitude, longitude)

            currentIndex =
                TzufMapServerApi.BITMAP_LENGTH_RASTER_TILE_LOCAL_INDEX
            val bitmapDataLength =
                bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.BITMAP_LENGTH_SIZE))
                    .toInt()

            var bitmap: Bitmap? = null
            if (bitmapDataLength > 0) {
                currentIndex =
                    TzufMapServerApi.BITMAP_DATA_RASTER_TILE_LOCAL_INDEX
                val bitmapData =
                    bytes.sliceArray(currentIndex until (currentIndex + bitmapDataLength))
                bitmap = BitmapUtils.createFrom(bitmapData)
                if (bitmap == null) {
                    Log.d(TAG, "couldn't decode bitmap of tile")
                }
            }

            return Tile(topLeft, topRight, bottomLeft, bottomRight, bitmap, zoom)
        }

        private fun findNumberOfBytesToDrop(bytes: ByteArray): Int {
            var currentIndex =
                TzufMapServerApi.BITMAP_LENGTH_RASTER_TILE_LOCAL_INDEX
            val bitmapDataLength =
                bytes.sliceArray(currentIndex until (currentIndex + TzufMapServerApi.BITMAP_LENGTH_SIZE))
                    .toInt()
            val bytesToDrop =
                TzufMapServerApi.ZOOM_SIZE + 8 * TzufMapServerApi.COORDINATE_SIZE + TzufMapServerApi.BITMAP_LENGTH_SIZE + bitmapDataLength

            return bytesToDrop
        }
    }
}