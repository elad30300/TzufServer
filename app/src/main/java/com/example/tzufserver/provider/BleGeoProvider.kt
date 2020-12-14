package com.example.tzufserver.provider

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.tzufserver.ApplicationSettings
import com.example.tzufserver.bluetooth.api.map.*
import com.example.tzufserver.bluetooth.api.protocol.message.EntitiesNotification
import com.example.tzufserver.bluetooth.api.protocol.message.EntitiesRequestMessage
import com.example.tzufserver.bluetooth.api.protocol.message.RasterTilesNotificationMessage
import com.example.tzufserver.bluetooth.api.protocol.message.RasterTilesRequestMessage
import com.example.tzufserver.bluetooth.api.protocol.packet.AckPacketMessage
import com.example.tzufserver.bluetooth.api.protocol.packet.Packet
import com.example.tzufserver.bluetooth.client.*
import com.example.tzufserver.provider.dispatchers.DefaultDispatcherProvider
import com.example.tzufserver.provider.dispatchers.DispatcherProvider
import com.example.tzufserver.utils.map.Boundaries
import com.example.tzufserver.utils.map.ZoomType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleGeoProvider @Inject constructor(
    private val bleManager: BleManager,
    private val clientClassicBluetoothManager: ClientClassicBluetoothManager,
    applicationSettings: ApplicationSettings,
    private val rasterTilesBluetoothHandler: RasterTilesBluetoothHandler,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : EntitiesProvider, RasterTilesProvider, ClientClassicBluetoothManager.BluetoothSocketListener {

    override val entitiesProviderAvailable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(false)
    }
    override val rasterTilesProviderAvailable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(false)
    }

    override var rasterTilesDelegate: RasterTilesProvider.Delegate? = null
    override var entitiesProviderDelegate: EntitiesProvider.Delegate? = null

    private var server: Server? = null
    private val notificationsCallbacks = mapOf<UUID, (ByteArray) -> Unit>(
        TzufMapServerApi.ENTITIES_NOTIFICATIONS_CHARACTERISTIC_UUID to { bytes ->
            onEntitiesNotification(bytes)
        },
        TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID to { bytes ->
            onRasterTileNotification(bytes)
        }
    )

    init {
        if (applicationSettings.gattType == applicationSettings.GATT_TYPE_CLIENT) {
            applicationSettings.bluetoothEnabled.observeForever {
                onBluetoothStatusChanged(it)
            }
            rasterTilesBluetoothHandler.rasterTilesProvider = this
        }
    }

    private fun onBluetoothStatusChanged(enabled: Boolean) {
        if (enabled) {
            onBluetoothTurnedOn()
        } else {
            onBluetoothTurnedOff()
        }
    }

    private fun onBluetoothTurnedOn() {
        scanServer()
    }

    private fun onBluetoothTurnedOff() {
        entitiesProviderAvailable.postValue(false)
        rasterTilesProviderAvailable.postValue(false)
//        scanServer()
    }

    ////////////////////////////////////////
    //             BLE ACTIONS            //
    ////////////////////////////////////////

    private fun scanServer() {
        bleManager.scan(
            ScanOptions()
                .withRemoteName("TZUF"),
//                .withServiceUuid(TzufMapServerApi.GEO_SERVICE_UUID),
            {
                onScanServerCompleted(it)
            },
            {
                onScanFailed()
            },
            {
                onFailure(it)
            }
        )
    }

    private fun connectToServer(device: BluetoothDevice) {
        bleManager.connect(
            device.address,
            {
                onConnectedToServer(device)
            },
            {
                Log.i(TAG, "disconnected from device ${device.address}")
                connectToServer(device) //  TODO: change this to check for intentional discconections
            },
            {
                onConnectFailed()
            },
            {
                onFailure(it)
            }
        )
    }

    private fun setupEntitiesNotifications() {
        server?.device?.apply {
            bleManager.registerCharacteristicNotifications(
                TzufMapServerApi.ENTITIES_NOTIFICATIONS_CHARACTERISTIC_UUID,
                address,
                {
//                    onEntitiesNotification(it)
                    onNotification(it, TzufMapServerApi.ENTITIES_NOTIFICATIONS_CHARACTERISTIC_UUID)
                },
                {
                    onFailure(it)
                },
                {
                    onSetupEntitiesNotifications()
                }
            )
        }
    }

    private fun setupRasterTilesNotifications() {
        server?.device?.apply {
            bleManager.registerCharacteristicNotifications(
                TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID,
                address,
                {
                    onNotification(
                        it,
                        TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID
                    )
                },
                {
                    onFailure(it)
                },
                {
                    onSetupRasterTilesNotifications()
                }
            )
        }
    }

    private fun writeMessage(uuid: UUID, data: ByteArray) {
        server?.device?.also { device ->
            bleManager.writeToCharacteristic(
                uuid,
                device.address,
                data,
                { _, _ ->
                    onMessageWrittenSuccessfully(uuid, data)
                },
                {
                    onFailure(it)
                }
            )
        } ?: Log.e(TAG, "try to write message but server is null")
    }

    private fun askEntities(region: Boundaries) {
        CoroutineScope(Dispatchers.Default).launch {
            server?.also {
                val requestId =
                    it.generateNextSendRequestId(TzufMapServerApi.ENTITIES_REQUEST_CHARACTERISTIC_UUID)
                val message = EntitiesRequestMessage(requestId, region)
                writeMessage(
                    TzufMapServerApi.ENTITIES_REQUEST_CHARACTERISTIC_UUID,
                    message.toByteArray()
                )
            }
        }
    }

    private fun askRasterTiles(region: Boundaries, zoom: ZoomType) {
        CoroutineScope(Dispatchers.Default).launch {
            server?.also {
                val requestId =
                    it.generateNextSendRequestId(TzufMapServerApi.RASTER_TILES_REQUEST_CHARACTERISTIC_UUID)
                val message = RasterTilesRequestMessage(requestId, region, zoom)
                writeMessage(
                    TzufMapServerApi.RASTER_TILES_REQUEST_CHARACTERISTIC_UUID,
                    message.toByteArray()
                )
            }
        }
    }

    private fun sendAcknowledgeForPacket(
        uuidToBeAcked: UUID,
        packet: Packet,
        ackValue: Boolean = true
    ) {
        val message = AckPacketMessage.fromPacket(packet, ackValue)
        TzufMapServerApi.ackCharacteristics[uuidToBeAcked]?.also { uuid ->
            writeMessage(uuid, message.toByteArray())
        } ?: Log.d(
            TAG,
            "try to send ack for uuid that isn't registered as reply in api $uuidToBeAcked"
        )
    }

    ////////////////////////////////////////
    //             CALLBACKS              //
    ////////////////////////////////////////

    private fun onScanServerCompleted(device: BluetoothDevice) {
        connectToServer(device)
    }

    private fun onScanFailed() {
        Log.e(TAG, "onScanFailed")
    }

    private fun onFailure(throwable: Throwable?) {
        Log.e(TAG, "failure: $throwable")
        throwable?.printStackTrace()
    }

    private fun onConnectedToServer(device: BluetoothDevice) {
        this.server = Server(device)
        // reason - to avoid exceptions of characteristic not found
        Thread.sleep(500) // refactor - try something like await to when service and characteristics are found to
        setupEntitiesNotifications()
        setupRasterTilesNotifications() // TODO: enable this for raster tiles notiifications after entities test!!
    }

    private fun onDisconnectedFromServer(device: BluetoothDevice) {
        // TODO: send appripriate message to ble manager so it can dispose any existing disposables!!
        this.server = null
        rasterTilesProviderAvailable.postValue(false)
        entitiesProviderAvailable.postValue(false)
    }

    private fun onConnectFailed() {
        Log.e(TAG, "onConnectFailed")
    }

    private fun onSetupEntitiesNotifications() {
        // TODO: ask data request for entities
        Log.d(TAG, "setup entities notifications")
        entitiesProviderAvailable.postValue(true)
    }

    private fun onSetupRasterTilesNotifications() {
        // TODO: ask data request for entities
        Log.d(TAG, "setup raster tiles notifications")
        rasterTilesProviderAvailable.postValue(true)
    }

    private fun onReceiveTrackerCompleted(uuid: UUID) {
        Log.d(TAG, "tracker is done for uuid $uuid")
        server?.getReceiveTracker(uuid)?.also {
            server?.removeReceiveTracker(uuid)
            it.getData()?.also { data ->
                notificationsCallbacks[uuid]?.also { callback ->
                    callback(data)
                }
            }
        }
    }

    private fun onNotification(bytes: ByteArray, uuid: UUID) {
        Log.d(TAG, "onNotification for uuid $uuid, bytes size is ${bytes.size}")
        server?.also { server ->
            val packet = Packet.fromByteArray(bytes)
            if (server.doesReceivingPacketBelongToMessage(packet, uuid)) {
                Log.d(TAG, "got packet for uuid $uuid - ${packet.part} / ${packet.totalNumberOfParts}")
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
                        onFailure(ex)
                    }
                }
            } else {
                Log.d(
                    TAG,
                    "got packet that doesn't belong to message woth requestId ${packet.requestId} for uuid $uuid"
                )
            }
        }
    }

    private fun onEntitiesNotification(bytes: ByteArray) {
        CoroutineScope(dispatcherProvider.default()).launch {
            val entitiesNotification = EntitiesNotification.fromByteArray(bytes)
            onEntitiesFetched(entitiesNotification.getEntities())
        }
    }

    private fun onRasterTileNotification(bytes: ByteArray) {
        CoroutineScope(dispatcherProvider.default()).launch {
            val message = RasterTilesNotificationMessage.fromByteArray(bytes)
            onRasterTilesFetched(message.getTiles())
        }
    }

    private fun onMessageWrittenSuccessfully(uuid: UUID, data: ByteArray) {
        when (uuid) {
            TzufMapServerApi.ENTITIES_REQUEST_CHARACTERISTIC_UUID -> onAskEntitiesSuccessfully()
            TzufMapServerApi.RASTER_TILES_REQUEST_CHARACTERISTIC_UUID -> onAskRasterTilesSuccessfully()
            TzufMapServerApi.ENTITIES_NOTIFICATIONS_ACK_CHARACTERISTIC_UUID -> onSentEntitiesAckSuccessfully()
            TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_ACK_CHARACTERISTIC_UUID -> onSentRasterTilesAckSuccessfully()
            else -> Log.i(TAG, "success in write message to characteristic $uuid")
        }
    }

    private fun onAskEntitiesSuccessfully() {
        Log.d(TAG, "asked entities successfully")
        server?.getSendRequestId(TzufMapServerApi.ENTITIES_REQUEST_CHARACTERISTIC_UUID)?.also {
            server?.setReceiveRequestId(
                TzufMapServerApi.ENTITIES_NOTIFICATIONS_CHARACTERISTIC_UUID,
                it
            )
        }
    }

    private fun onAskRasterTilesSuccessfully() {
        Log.d(TAG, "asked raster tiles successfully")
        server?.getSendRequestId(TzufMapServerApi.RASTER_TILES_REQUEST_CHARACTERISTIC_UUID)?.also {
            server?.setReceiveRequestId(
                TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID,
                it
            )
            listenForRasterTiles()
//            rasterTilesBluetoothHandler.listenForRasterTiles(it)
        }
    }

    private fun onSentEntitiesAckSuccessfully() {
        Log.d(TAG, "success in send ack for entities to server")
    }

    private fun onSentRasterTilesAckSuccessfully() {
        Log.d(TAG, "success in send ack for raster tiles to server")
    }


    override fun getTilesForRegion(boundaries: Boundaries, zoom: ZoomType) {
        askRasterTiles(boundaries, zoom)
    }

    override fun getEntitiesForRegion(boundaries: Boundaries) {
        askEntities(boundaries)
    }

    // classic bluetooth

    fun listenForRasterTiles() {
        /* TODO: should make it bullet-proof without too many scans in parallel - for example if the user moves the visible region
        * while there is already a request */
//        server?.setReceiveRequestId(RASTER_TILES_SOCKET_UUID, requestId)
        val scanOptions = ScanOptions().apply {
            serviceUuid = TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID
        }
        if (!clientClassicBluetoothManager.scan(scanOptions) {
                onDeviceFound(it)
            }) {
            Log.e(TAG, "scan didn't start")
        }
    }

    private fun onDeviceFound(device: BluetoothDevice) {
        Log.i(TAG, "on device found ${device.address}")
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
        Log.e(TAG, "error occurred device ${device.address} with uuid $uuid: ", exception)
    }

    override fun onSocketValueChanged(bytes: ByteArray, device: BluetoothDevice, uuid: UUID) {
        super.onSocketValueChanged(bytes, device, uuid)
        Log.i(TAG, "value changed for device ${device.address} with uuid $uuid")
        onNotification(bytes, uuid)
    }

    override fun onSocketTerminated(device: BluetoothDevice, uuid: UUID) {
        super.onSocketTerminated(device, uuid)
        Log.i(TAG, "socket terminated to device ${device.address} for socket with uuid $uuid")
    }

    companion object {
        const val TAG = "BleMapProvider"
    }

}