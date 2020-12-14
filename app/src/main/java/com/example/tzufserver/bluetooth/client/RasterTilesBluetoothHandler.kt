package com.example.tzufserver.bluetooth.client

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.example.tzufserver.bluetooth.api.protocol.message.RasterTilesNotificationMessage
import com.example.tzufserver.bluetooth.api.protocol.packet.Packet
import com.example.tzufserver.di.module.RasterTilesBleGeoProvider
import com.example.tzufserver.provider.BleGeoProvider
import com.example.tzufserver.provider.RasterTilesProvider
import com.example.tzufserver.provider.dispatchers.DefaultDispatcherProvider
import com.example.tzufserver.provider.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RasterTilesBluetoothHandler @Inject constructor(
    private val clientClassicBluetoothManager: ClientClassicBluetoothManager,
//    @RasterTilesBleGeoProvider private val rasterTilesProvider: RasterTilesProvider,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : ClientClassicBluetoothManager.BluetoothSocketListener {

    var server: Server? = null
    var rasterTilesProvider: RasterTilesProvider? = null

    fun listenForRasterTiles(requestId: Int) {
        /* TODO: should make it bullet-proof without too many scans in parallel - for example if the user moves the visible region
        * while there is already a request */
//        server?.setReceiveRequestId(RASTER_TILES_SOCKET_UUID, requestId)
        val scanOptions = ScanOptions().apply {
            serviceUuid = RASTER_TILES_SOCKET_UUID
        }
        if (!clientClassicBluetoothManager.scan(scanOptions) { onDeviceFound(it, requestId) }) {
            Log.e(TAG, "scan didn't start")
        }
    }

    private fun onDeviceFound(device: BluetoothDevice, requestId: Int) {
        server = Server(device).apply {
            setReceiveRequestId(RASTER_TILES_SOCKET_UUID, requestId)
        }
        clientClassicBluetoothManager.listenToUuidSocket(
            device,
            TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID,
            this
        )
    }

    override fun onSocketConnected(device: BluetoothDevice, uuid: UUID) {
        super.onSocketConnected(device, uuid)
        Log.i(TAG, "connected to device ${device.address} for socket with uuid $uuid")
    }

    override fun onSocketReady(device: BluetoothDevice, uuid: UUID) {
        super.onSocketReady(device, uuid)
        Log.i(TAG, "socket ready for ${device.address} for socket with uuid $uuid")

    }

    override fun onSocketError(device: BluetoothDevice, uuid: UUID, exception: Exception) {
        super.onSocketError(device, uuid, exception)
        Log.e(TAG, "error occurred device ${device.address} with uuid $uuid: $exception")
        Log.e(TAG, "${exception.stackTrace}")
    }

    override fun onSocketValueChanged(bytes: ByteArray, device: BluetoothDevice, uuid: UUID) {
        super.onSocketValueChanged(bytes, device, uuid)
        Log.i(TAG, "value changed for device ${device.address} with uuid $uuid")
        /* TODO: try to put the following logic in an independent compoenent -
            this logic is shared with BleGeoProvider onNotification thus making it harder to maintain
        *   */
        server?.also { server ->
            val packet = Packet.fromByteArray(bytes)
            if (server.doesReceivingPacketBelongToMessage(packet, uuid)) {
                if (server.isNeedToSetReceiveTracker(uuid)) {
                    server.createReceiveTracker(uuid, packet.totalNumberOfParts)
//                    server.setReceiveTracker(uuid, ReceiveMessagePacketTracker(packet.requestId, packet.totalNumberOfParts))
                }
                server.getReceiveTracker(uuid)?.also { tracker ->
                    try {
                        tracker.setPacket(packet)
//                        sendAcknowledgeForPacket(uuid, packet)
                        if (tracker.isDataComplete()) {
                            onReceiveTrackerCompleted(uuid)
                        }
                    } catch (ex: Exception) {
                        Log.e(
                            TAG,
                            "onSocketValueChanged - error occurred device ${device.address} with uuid $uuid: $ex"
                        )
                        Log.e(TAG, "${ex.stackTrace}")
                    }
                }
            } else {
                Log.d(
                    BleGeoProvider.TAG,
                    "got packet that doesn't belong to message woth requestId ${packet.requestId} for uuid $uuid"
                )
            }
        }
    }

    /* TODO: try to put the following logic in an independent compoenent -
            this logic is shared with BleGeoProvider onNotification thus making it harder to maintain
        *   */
    private fun onReceiveTrackerCompleted(uuid: UUID) {
        Log.d(BleGeoProvider.TAG, "tracker is done for uuid $uuid")
        server?.getReceiveTracker(uuid)?.also {
            server?.removeReceiveTracker(uuid)
            it.getData()?.also { data ->
                onRasterTileNotification(data)
                // TODO: refactor to notification callbacks in order to support clean code for multiple sockets
//                notificationsCallbacks[uuid]?.also { callback ->
//                    callback(data)
            }
        }
    }


    private fun onRasterTileNotification(bytes: ByteArray) {
        CoroutineScope(dispatcherProvider.default()).launch {
            val message = RasterTilesNotificationMessage.fromByteArray(bytes)
            rasterTilesProvider?.onRasterTilesFetched(message.getTiles())
        }
    }

    override fun onSocketTerminated(device: BluetoothDevice, uuid: UUID) {
        super.onSocketTerminated(device, uuid)
        Log.i(TAG, "socket terminated to device ${device.address} for socket with uuid $uuid")
        server = null
    }


    companion object {
        private const val TAG = "MapBtHandler"
        private val RASTER_TILES_SOCKET_UUID =
            TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID
    }

}