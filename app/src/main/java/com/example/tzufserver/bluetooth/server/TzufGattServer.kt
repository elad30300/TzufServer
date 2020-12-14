package com.example.tzufserver.bluetooth.server

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.util.Log
import com.example.tzufserver.ApplicationSettings
import com.example.tzufserver.bluetooth.api.map.DataRequestMessage
import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.example.tzufserver.bluetooth.api.protocol.message.*
import com.example.tzufserver.bluetooth.api.protocol.packet.AckPacketMessage
import com.example.tzufserver.bluetooth.api.protocol.packet.Packet
import com.example.tzufserver.bluetooth.api.protocol.packet.SendMessagePacketTracker
import com.example.tzufserver.data.model.BaseEntity
import com.example.tzufserver.di.module.EntitiesExecutor
import com.example.tzufserver.di.module.EntitiesTempProvider
import com.example.tzufserver.di.module.RasterTilesExecutor
import com.example.tzufserver.di.module.RasterTilesGeopackageProvider
import com.example.tzufserver.provider.EntitiesProvider
import com.example.tzufserver.provider.RasterTilesProvider
import com.example.tzufserver.provider.dispatchers.DefaultDispatcherProvider
import com.example.tzufserver.provider.dispatchers.DispatcherProvider
import com.example.tzufserver.utils.map.Tile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TzufGattServer @Inject constructor(
    @ApplicationContext private val context: Context,
    @EntitiesTempProvider private val entitiesProvider: EntitiesProvider,
    @RasterTilesGeopackageProvider private val rasterTilesProvider: RasterTilesProvider,
    private val serverClassicBluetoothManager: ServerClassicBluetoothManager,
    @EntitiesExecutor private val entitiesExecutor: Executor,
    @RasterTilesExecutor private val rasterTilesExecutor: Executor,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    applicationSettings: ApplicationSettings
) :
    BluetoothGattServerCallback(), RasterTilesProvider.Delegate, EntitiesProvider.Delegate,
    ServerClassicBluetoothManager.BluetoothSocketListener {

    private val bluetoothManager =
        context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var gattServer: BluetoothGattServer? = null
    private val clients = mutableListOf<Client>()
    private var mtu = 10

    init {
        rasterTilesProvider.rasterTilesDelegate = this
        entitiesProvider.entitiesProviderDelegate = this
        if (applicationSettings.gattType == applicationSettings.GATT_TYPE_SERVER) {
            applicationSettings.bluetoothEnabled.observeForever {
                onBluetoothStatusChanged(it)
            }
            observeClassicBluetoothSocketsAvailability()
        }
    }

    private fun observeClassicBluetoothSocketsAvailability() {
        serverClassicBluetoothManager.canAcceptSockets().observeForever {
//            if (it) {
//                acceptConnectionsToRasterTilesSocket()
//            } else {
//                stopAcceptConnectionsToRasterTilesSocket()
//            }
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
        startServer(context)
        startAdvertising()
    }

    private fun onBluetoothTurnedOff() {
        closeServer()
    }

    private fun findClient(device: BluetoothDevice) =
        clients.find { it.device.address == device.address }

    private fun findClientByClassicBluetooth(device: BluetoothDevice) =
        clients.find { it.classicBluetoothDevice?.address == device.address }

    private fun getPartSize() = 500
//    private fun getPartSize() = this.mtu - Packet.HEADER_SIZE

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "LE Advertise Failed: $errorCode")
            startAdvertising()
        }
    }

    private fun startServer(context: Context) {
        gattServer = bluetoothManager.openGattServer(context, this).run {
            addService(GeoService())
            this
        }
    }

    private fun closeServer() {
        gattServer?.close()
    }

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private fun startAdvertising() {
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothManager.adapter.bluetoothLeAdvertiser

        bluetoothLeAdvertiser?.let {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
//                .addServiceUuid(ParcelUuid(UUID.fromString(SampleService.SERVICE_UUID)))
                .build()

            it.startAdvertising(settings, data, advertiseCallback)
        } ?: Log.w(TAG, "Failed to create advertiser")
    }

    /**
     * Stop Bluetooth advertisements.
     */
    fun stopAdvertising() {
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothManager.adapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.let {
            it.stopAdvertising(advertiseCallback)
        } ?: Log.w(TAG, "Failed to create advertiser")
    }

    //////////////////////////////////
    // GATT SERVER CALLBACKS        //
    /////////////////////////////////

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
        Log.d(
            TAG,
            "onCharacteristicReadRequest, mac ${device?.address} requestId = $requestId, offset = $offset, characteristic ${characteristic?.uuid}"
        )
    }

    override fun onDescriptorReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        descriptor: BluetoothGattDescriptor?
    ) {
        super.onDescriptorReadRequest(device, requestId, offset, descriptor)
        Log.d(
            TAG,
            "onDescriptorReadRequest, mac ${device?.address} requestId = $requestId, offset = $offset, characteristic ${descriptor?.uuid}"
        )
    }

    override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
        super.onNotificationSent(device, status)
        Log.d(TAG, "onNotificationSent, mac ${device?.address} status = $status")
    }

    override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
        super.onMtuChanged(device, mtu)
        Log.d(TAG, "onMtuChanged, mac ${device?.address} mtu = $mtu")
        this.mtu = mtu
    }

    override fun onPhyUpdate(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
        super.onPhyUpdate(device, txPhy, rxPhy, status)
        Log.d(
            TAG,
            "onPhyUpdate, mac ${device?.address} txPhy = $txPhy rxPhy = $rxPhy status = $status"
        )
    }

    override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
        super.onExecuteWrite(device, requestId, execute)
        Log.d(
            TAG,
            "onExecuteWrite, mac ${device?.address} requestId = $requestId execute = $execute"
        )
    }

    override fun onCharacteristicWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ) {
        super.onCharacteristicWriteRequest(
            device,
            requestId,
            characteristic,
            preparedWrite,
            responseNeeded,
            offset,
            value
        )
        val valueDescription = if (value == null) "" else String(value)
        Log.d(
            TAG,
            "onCharacteristicWriteRequest, mac ${device?.address} requestId = $requestId, preparedWrite = $preparedWrite, characteristic ${characteristic?.uuid} " +
                    "responseNeeded = $responseNeeded, offset = $offset, value = $valueDescription"
        )
        when (characteristic?.uuid) {
            TzufMapServerApi.ENTITIES_REQUEST_CHARACTERISTIC_UUID -> {
                Log.i(TAG, "got entities request message")
                if (responseNeeded) {
                    sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset)
                }
                value?.also { message ->
                    onEntitiesRequestMessage(device!!, message)
                }
            }
            TzufMapServerApi.RASTER_TILES_REQUEST_CHARACTERISTIC_UUID -> {
                Log.i(TAG, "got raster tiles request message")
                if (responseNeeded) {
                    sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset)
                }
                value?.also { message ->
                    onRasterTilesRequestMessage(device!!, message)
                }
            }
            in TzufMapServerApi.ackCharacteristics.values -> {
                Log.i(TAG, "got ack")
                if (responseNeeded) {
                    sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset)
                }
                value?.also { message ->
                    characteristic?.uuid?.also { uuid -> onAcknowledge(device!!, uuid, message) }
                }
            }
            else -> {
                if (responseNeeded) {
                    sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset)
                }
            }
        }
    }

    private fun updateCharacteristicValue(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        characteristic.apply {
            setValue(value)
            gattServer?.notifyCharacteristicChanged(device, this, true)
        }
    }

    override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
        super.onConnectionStateChange(device, status, newState)
        var stateDescription = ""
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                stateDescription = "connected"
                onClientConnected(device)
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                stateDescription = "disconnected"
                onClientDisconnected(device)
            }
            BluetoothProfile.STATE_CONNECTING -> stateDescription = "connecting"
            BluetoothProfile.STATE_DISCONNECTING -> {
                stateDescription = "disconnecting"
            }
            else -> stateDescription = "unknown"
        }
        Log.d(
            TAG,
            "onConnectionStateChange, mac ${device?.address} newState = $stateDescription, status = $status"
        )
    }

    private fun onClientConnected(device: BluetoothDevice?) {
        stopAdvertising()
        device?.apply { clients.add(Client(this)) }
    }

    private fun onClientDisconnected(device: BluetoothDevice?) {
        clients?.removeIf { it.device.address == device?.address }
        startAdvertising()
    }

    override fun onPhyRead(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
        super.onPhyRead(device, txPhy, rxPhy, status)
        Log.d(
            TAG,
            "onPhyRead, mac ${device?.address} txPhy = $txPhy rxPhy = $rxPhy status = $status"
        )
    }

    override fun onDescriptorWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        descriptor: BluetoothGattDescriptor?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ) {
        super.onDescriptorWriteRequest(
            device,
            requestId,
            descriptor,
            preparedWrite,
            responseNeeded,
            offset,
            value
        )
        Log.d(
            TAG,
            "onDescriptorWriteRequest, mac ${device?.address} requestId = $requestId, preparedWrite = $preparedWrite, descriptor ${descriptor?.uuid} " +
                    "responseNeeded = $responseNeeded, offset = $offset, value = $value, characteristic = ${descriptor?.characteristic?.uuid}"
        )
        descriptor?.also {
            if (responseNeeded) {
                sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset)
            }
            if (descriptor.uuid == TzufMapServerApi.CLIENT_CONFIG_DESCRIPTOR_UUID) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(
                        TAG,
                        "Subscribe device to notifications: $device, uuid ${descriptor?.characteristic?.uuid}"
                    )
                    onRegisteredToNotifications(device!!, descriptor.characteristic)
                } else if (Arrays.equals(
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE,
                        value
                    )
                ) {
                    Log.d(
                        TAG,
                        "Unsubscribe device from notifications: $device, uuid ${descriptor?.characteristic?.uuid}"
                    )
                    onUnregisteredToNotifications(device!!, descriptor.characteristic)
                }
            } else if (responseNeeded) {
                sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset)
            }
        } ?: sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset)
    }

    override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
        super.onServiceAdded(status, service)
        Log.d(TAG, "onServiceAdded, service ${service?.uuid} status = $status")
    }

    private fun onRegisteredToNotifications(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic?
    ) {
        when (characteristic?.uuid) {
            TzufMapServerApi.ENTITIES_NOTIFICATIONS_CHARACTERISTIC_UUID -> onRegisteredToEntitiesNotifications(
                device
            )
            TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID -> onRegisteredToRasterTilesNotifications(
                device
            )
        }
    }

    private fun onUnregisteredToNotifications(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic?
    ) {
        Log.i(TAG, "unregisterd from notifications for characteristic ${characteristic?.uuid}")
        when (characteristic?.uuid) {
            TzufMapServerApi.ENTITIES_NOTIFICATIONS_CHARACTERISTIC_UUID -> onUnregisteredFromEntitiesNotifications(
                device
            )
            TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID -> onUnregisteredFromRasterTilesNotifications(
                device
            )
        }
    }

    private fun onAcknowledge(device: BluetoothDevice, uuid: UUID, bytes: ByteArray) {
        TzufMapServerApi.getUuidToBeAckedByAckUuid(uuid)?.also { uuidToBeAcked ->
            val ackPacketMessage = AckPacketMessage.fromByteArray(bytes)
            Log.d(TAG, "on acknowledge for uuid $uuidToBeAcked with ack message $ackPacketMessage")
            if (ackPacketMessage.ackValue) {
                findClient(device)?.also { client -> sendNextPart(client, uuidToBeAcked) }
            }
        } ?: Log.e(TAG, "got ack in uuuid $uuid but there is no corressponding uuid for that")
    }

    private fun onSendPartsDone(client: Client, uuid: UUID) {
        Log.i(TAG, "finished to send parts to client ${client.device.address} for uuid $uuid")
        client.removeSendTracker(uuid)
    }

    private fun sendNextPart(client: Client, uuid: UUID) {
        client.getSendTracker(uuid)?.also { tracker ->
            tracker.next()?.also { packet ->
                notifyCharacteristic(client.device, packet.toByteArray(), uuid)
            } ?: onSendPartsDone(client, uuid)
        }
    }

    private fun sendMessage(message: GeoMessage, client: Client, uuid: UUID) {
        try {
            client.createSendTracker(uuid, message, getPartSize()).also {
                it.setNewOnReadyCallback {
                    while (it.hasNext()) {
                        sendNextPart(client, uuid)
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "exception in sendMessage, ex $ex\n${ex.stackTrace}")
        }
    }

    private fun onRegisteredToEntitiesNotifications(device: BluetoothDevice) {
        findClient(device)?.registeredToEntitiesNotifications =
            true
    }

    private fun onRegisteredToRasterTilesNotifications(device: BluetoothDevice) {
        findClient(device)?.registeredToRasterTilessNotifications =
            true
    }

    private fun onUnregisteredFromEntitiesNotifications(device: BluetoothDevice) {
        findClient(device)?.registeredToEntitiesNotifications =
            false
    }

    private fun onUnregisteredFromRasterTilesNotifications(device: BluetoothDevice) {
        findClient(device)?.registeredToRasterTilessNotifications =
            false
    }

    private fun notifyEntitiesAllDevices(entities: List<BaseEntity>) {
        clients.forEach {
            notifyEntitiesToDevice(it.device, entities)
        }
    }

    private fun notifyRasterTilesAllDevices(tiles: List<Tile>) {
        clients.forEach {
            notifyRasterTilesToDevice(it.device, tiles)
        }
    }

    private fun notifyEntitiesToDevice(device: BluetoothDevice, entities: List<BaseEntity>) {
        findClient(device)?.also { client ->
            sendMessage(
                EntitiesNotification(entities),
                client,
                TzufMapServerApi.ENTITIES_NOTIFICATIONS_CHARACTERISTIC_UUID
            )
        }
    }

    private fun notifyRasterTilesToDevice(device: BluetoothDevice, tiles: List<Tile>) {
        findClient(device)?.also { client ->
            sendMessage(
                RasterTilesNotificationMessage(tiles),
                client,
                TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID
            )
        }
    }

    private fun notifyCharacteristic(
        device: BluetoothDevice,
        value: ByteArray,
        characteristicUuid: UUID
    ) {
        findCharacteristic(characteristicUuid)?.also {
            updateCharacteristicValue(device, it, value)
        } ?: Log.e(TAG, "characteristic $characteristicUuid not found")
    }

    // TODO: test this function!!
    private fun findCharacteristic(characteristicUuid: UUID): BluetoothGattCharacteristic? {
        gattServer?.services?.forEach {
            val characteristic = it.getCharacteristic(characteristicUuid)
            if (characteristic != null) {
                return characteristic
            }
        }
        return null
    }

    private fun sendResponse(
        device: BluetoothDevice?,
        requestId: Int,
        responseValue: Int,
        offset: Int
    ) {
        gattServer?.also {
            if (device == null) {
                throw Exception("try to send response to null device")
            }
            if (!it.sendResponse(
                    device,
                    requestId,
                    responseValue,
                    offset,
                    null
                )
            ) {
                Log.e(
                    TAG,
                    "send response with value $responseValue to device ${device?.address} failed"
                )
            }
        }
    }

    private fun onEntitiesRequestMessage(device: BluetoothDevice, bytes: ByteArray) {
        findClient(device)?.also { client ->
            val message = EntitiesRequestMessage(bytes)
            client.setSendRequestId(
                TzufMapServerApi.ENTITIES_NOTIFICATIONS_CHARACTERISTIC_UUID,
                message.requestId
            )
            entitiesExecutor.execute {
                entitiesProvider.getEntitiesForRegion(message.getAreaOfInterest())
            }
        }
    }

    private fun onRasterTilesRequestMessage(device: BluetoothDevice, bytes: ByteArray) {
        findClient(device)?.also { client ->
            val message = RasterTilesRequestMessage(bytes)
            client.setSendRequestId(
                TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID,
                message.requestId
            )
            rasterTilesExecutor.execute {
                rasterTilesProvider.getTilesForRegion(message.getAreaOfInterest(), message.zoom())
            }
        } ?: Log.e(TAG, "cannot find client corresponds to device ${device.address}")
    }

    override fun onRasterTilesFetched(tiles: List<Tile>) {
        Log.d(TAG, "onRasterTilesFetched")
        sendTilesViaClassicBluetooth(tiles)
//        notifyRasterTilesAllDevices(tiles)
    }

    override fun onEntitiesFetched(entities: List<BaseEntity>) {
        Log.d(TAG, "onEntitiesFetched")
        notifyEntitiesAllDevices(entities)
    }

    // classic bluetooth

    private fun sendTilesViaClassicBluetooth(tiles: List<Tile>) {
        acceptConnectionsToRasterTilesSocket(object :
            ServerClassicBluetoothManager.BluetoothSocketListener {
            override fun onWriteToSocketPerformed(
                device: BluetoothDevice,
                uuid: UUID,
                bytes: ByteArray
            ) {
                super.onWriteToSocketPerformed(device, uuid, bytes)
                Log.i(TAG, "classic bluetooth write to characteristic $uuid and device ${device.address} was successfully")
            }

            override fun onSocketReady(device: BluetoothDevice, uuid: UUID) {
                super.onSocketReady(device, uuid)
                Log.d(TAG, "second - socket ready for uuid $uuid")
                try {
                    clients[0].classicBluetoothDevice = device // TODO: change immediately - must find a way to recognize who is the client by the classic bluetooth address!!!
                    sendRasterTiles(clients[0].device, uuid, tiles) { device, uuid, value ->
                        findClient(device)?.let {
                            Log.d(TAG, "about to write messege in classic bluetooth, size is ${value.size}")
                            it.classicBluetoothDevice?.let {
                                serverClassicBluetoothManager.write(it, uuid, value)
                            }
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "write to classic bluetooth failed with $ex\n${ex.stackTrace}")
                }
            }

            override fun onSocketValueChanged(
                bytes: ByteArray,
                device: BluetoothDevice,
                uuid: UUID
            ) {
                super.onSocketValueChanged(bytes, device, uuid)
                try {
                    val message = AckPacketMessage.fromByteArray(bytes)
                    findClientByClassicBluetooth(device)?.getSendTracker(uuid)?.let {
                        if (it.requestId == message.requestId) {
                            if (it.hasNext()) {
                                it.next()?.let {
                                    serverClassicBluetoothManager.write(device, uuid, it.toByteArray())
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "onSocketValueChanged failure -", ex)
                }
            }

            override fun onSocketError(
                device: BluetoothDevice,
                uuid: UUID,
                exception: java.lang.Exception
            ) {
                super.onSocketError(device, uuid, exception)
                Log.e(TAG, "classic bluetooth failed with $exception\n${exception.stackTrace}")
            }
        })
    }

    private fun sendRasterTiles(
        device: BluetoothDevice,
        uuid: UUID,
        tiles: List<Tile>,
        write: (BluetoothDevice, UUID, ByteArray) -> Unit
    ) {
        CoroutineScope(dispatcherProvider.default()).launch {//
            try {
                val message = RasterTilesNotificationMessage(tiles)
                findClient(device)?.createSendTracker(uuid, message, ServerClassicBluetoothManager.MAX_NUMBER_OF_BYTES, shouldDivide = true)?.also {
                    it.setNewOnReadyCallback {
//                        while (it.hasNext()) {
                        if (it.hasNext()) {
                            it.next()?.let { packet ->
                                Log.d(TAG, "sending tile ${packet.part} / ${packet.totalNumberOfParts}")
                                write(
                                    device,
                                    uuid,
                                    packet.toByteArray()
                                )
                            }
//                            Thread.sleep(200)
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "exception in sendMessage, ex $ex\n${ex.stackTrace}")
            }
        }//
    }

    private fun acceptConnectionsToRasterTilesSocket(listener: ServerClassicBluetoothManager.BluetoothSocketListener = this) {
        acceptConnectionsToSocket(RASTER_TILES_SOCKET_UUID, "RASTER", listener)
    }

    private fun stopAcceptConnectionsToRasterTilesSocket() {
        stopAcceptingConnectionsToSocket(RASTER_TILES_SOCKET_UUID)
    }

    private fun acceptConnectionsToSocket(uuid: UUID, name: String, listener: ServerClassicBluetoothManager.BluetoothSocketListener = this) {
        serverClassicBluetoothManager.startAcceptingConnectionsToSocket(uuid, name, listener)
    }

    private fun stopAcceptingConnectionsToSocket(uuid: UUID) {
        serverClassicBluetoothManager.stopAcceptingConnectionsToSocket(uuid)
    }

    private fun onRasterTilesSocketReady() {
        Log.d(TAG, "raster tiles socket is ready")
    }

    private fun onUnknownSocketReady(uuid: UUID) {
        Log.d(TAG, "unknown socket $uuid is ready")
    }

    override fun onSocketReady(device: BluetoothDevice, uuid: UUID) {
        super.onSocketReady(device, uuid)
        Log.i(TAG, "socket ready for ${device.address} for socket with uuid $uuid")
        when (uuid) {
            RASTER_TILES_SOCKET_UUID -> onRasterTilesSocketReady()
            else -> onUnknownSocketReady(uuid)
        }
    }

    override fun onSocketError(
        device: BluetoothDevice,
        uuid: UUID,
        exception: java.lang.Exception
    ) {
        super.onSocketError(device, uuid, exception)
        Log.e(TAG, "error occurred device ${device.address} with uuid $uuid: $exception")
        Log.e(TAG, "${exception.stackTrace}")
    }

    override fun onSocketTerminated(device: BluetoothDevice, uuid: UUID) {
        super.onSocketTerminated(device, uuid)
        Log.i(TAG, "socket terminated to device ${device.address} for socket with uuid $uuid")
    }


    companion object {
        private const val TAG = "GattServer"
        private val RASTER_TILES_SOCKET_UUID =
            TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID
    }

}