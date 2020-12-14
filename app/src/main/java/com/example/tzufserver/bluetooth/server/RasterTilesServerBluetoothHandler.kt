package com.example.tzufserver.bluetooth.server

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.example.tzufserver.bluetooth.api.protocol.message.RasterTilesNotificationMessage
import com.example.tzufserver.bluetooth.api.protocol.packet.Packet
import com.example.tzufserver.di.module.RasterTilesBleGeoProvider
import com.example.tzufserver.di.module.RasterTilesGeopackageProvider
import com.example.tzufserver.provider.BleGeoProvider
import com.example.tzufserver.provider.RasterTilesProvider
import com.example.tzufserver.provider.dispatchers.DefaultDispatcherProvider
import com.example.tzufserver.provider.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import javax.inject.Inject

class RasterTilesServerBluetoothHandler @Inject constructor(
//    private val serverClassicBluetoothManager: ServerClassicBluetoothManager,
//    @RasterTilesGeopackageProvider private val rasterTilesProvider: RasterTilesProvider,
//    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : ServerClassicBluetoothManager.BluetoothSocketListener {

////    var server: Server? = null
//
//    init {
//        observeClassicBluetoothSocketsAvailability()
//    }
//
//    private fun observeClassicBluetoothSocketsAvailability() {
//        serverClassicBluetoothManager.canAcceptSockets().observeForever {
//            if (it) {
//                acceptConnectionsToRasterTilesSocket()
//            } else {
//                stopAcceptConnectionsToRasterTilesSocket()
//            }
//        }
//    }
//
//    private fun acceptConnectionsToRasterTilesSocket() {
//        acceptConnectionsToSocket(RASTER_TILES_SOCKET_UUID, "RASTER")
//    }
//
//    private fun stopAcceptConnectionsToRasterTilesSocket() {
//        stopAcceptingConnectionsToSocket(RASTER_TILES_SOCKET_UUID)
//    }
//
//    private fun acceptConnectionsToSocket(uuid: UUID, name: String) {
//        serverClassicBluetoothManager.startAcceptingConnectionsToSocket(uuid, name, this)
//    }
//
//    private fun stopAcceptingConnectionsToSocket(uuid: UUID) {
//        serverClassicBluetoothManager.stopAcceptingConnectionsToSocket(uuid)
//    }
//
//    private fun onRasterTilesSocketReady() {
//        rasterTilesProvider.getTilesForRegion()
//    }
//
//    private fun onUnknownSocketReady(uuid: UUID) {
//        Log.d(TAG, "unknown socket $uuid is ready")
//    }
//
//
//
////
////    fun listenForRasterTiles(requestId: Int) {
////        /* TODO: should make it bullet-proof without too many scans in parallel - for example if the user moves the visible region
////        * while there is already a request */
////        server?.setReceiveRequestId(RASTER_TILES_SOCKET_UUID, requestId)
////        val scanOptions = ScanOptions().apply {
////            serviceUuid = RASTER_TILES_SOCKET_UUID
////        }
////        if (serverClassicBluetoothManager.scan(scanOptions) { onDeviceFound(it) }) {
////            Log.e(TAG, "scan didn't start")
////        }
////    }
////
////    private fun onDeviceFound(device: BluetoothDevice) {
////        server = Server(device)
////        serverClassicBluetoothManager.listenToUuidSocket(
////            device,
////            TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID,
////            this
////        )
////    }
//
//    override fun onSocketConnected(device: BluetoothDevice, uuid: UUID) {
//        super.onSocketConnected(device, uuid)
//        Log.i(TAG, "connected to device ${device.address} for socket with uuid $uuid")
//    }
//
//    override fun onSocketReady(device: BluetoothDevice, uuid: UUID) {
//        super.onSocketReady(device, uuid)
//        Log.i(TAG, "socket ready for ${device.address} for socket with uuid $uuid")
//        when (uuid) {
//            RASTER_TILES_SOCKET_UUID -> onRasterTilesSocketReady()
//            else ->
//        }
//    }
//
//    override fun onSocketError(device: BluetoothDevice, uuid: UUID, exception: Exception) {
//        super.onSocketError(device, uuid, exception)
//        Log.e(TAG, "error occurred device ${device.address} with uuid $uuid: $exception")
//        Log.e(TAG, "${exception.stackTrace}")
//    }
//
//    /* TODO: try to put the following logic in an independent compoenent -
//            this logic is shared with BleGeoProvider onNotification thus making it harder to maintain
//        *   */
//    private fun onReceiveTrackerCompleted(uuid: UUID) {
//        Log.d(BleGeoProvider.TAG, "tracker is done for uuid $uuid")
//        server?.getReceiveTracker(uuid)?.also {
//            server?.removeReceiveTracker(uuid)
//            it.getData()?.also { data ->
//                onRasterTileNotification(data)
//                // TODO: refactor to notification callbacks in order to support clean code for multiple sockets
////                notificationsCallbacks[uuid]?.also { callback ->
////                    callback(data)
//            }
//        }
//    }
//
//
//    private fun onRasterTileNotification(bytes: ByteArray) {
//        CoroutineScope(dispatcherProvider.default()).launch {
//            val message = RasterTilesNotificationMessage.fromByteArray(bytes)
//            rasterTilesProvider.onRasterTilesFetched(message.getTiles())
//        }
//    }
//
//    override fun onSocketTerminated(device: BluetoothDevice, uuid: UUID) {
//        super.onSocketTerminated(device, uuid)
//        Log.i(TAG, "socket terminated to device ${device.address} for socket with uuid $uuid")
//        server = null
//    }
//
//
//    companion object {
//        private const val TAG = "SeMapBtHandler"
//        private val RASTER_TILES_SOCKET_UUID =
//            TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID
//    }

}