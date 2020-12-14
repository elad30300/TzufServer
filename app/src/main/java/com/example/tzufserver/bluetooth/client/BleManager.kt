package com.example.tzufserver.bluetooth.client

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.example.tzufserver.di.module.BleExecutor
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.exceptions.BleAlreadyConnectedException
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException
import com.polidea.rxandroidble2.scan.ScanSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @BleExecutor private val executor: Executor
) : GattCallback.Delegate {

//    private var rxBleClient: RxBleClient = RxBleClient.create(context)
//
//    private var scanDisposable: Disposable? = null

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var isScanning = false
    private var currentScanCallback: ScanCallback? = null
    private val bluetoothLeScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val remoteDevices = mutableListOf<RemoteDevice>()

    fun scan(
        scanOptions: ScanOptions,
        onScanCompleted: (BluetoothDevice) -> Unit,
        onScanFailed: () -> Unit,
        onScanError: (throwable: Throwable?) -> Unit
    ) {
        val scanFilters = mutableListOf<ScanFilter>().apply {
            scanOptions.serviceUuid?.let {
                add(
                    ScanFilter.Builder().setServiceUuid(ParcelUuid(it)).build()
                )
            }
            scanOptions.address?.let { add(ScanFilter.Builder().setDeviceAddress(it).build()) }
            scanOptions.remoteName?.let { add(ScanFilter.Builder().setDeviceName(it).build()) }
        }
        val scanSettings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        isScanning = true

        currentScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                super.onScanResult(callbackType, result)
                if (this != currentScanCallback) {
                    Log.d(
                        TAG,
                        "onScanResult - called by $this not current scan callback ${currentScanCallback}, doesn't continue"
                    )
                    return
                }
                isScanning = false
                result?.device?.let { device ->
                    remoteDevices.add(RemoteDevice(device))
                    onScanCompleted(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                onScanError(Exception("scan failed with error code $errorCode"))
            }
        }
        bluetoothLeScanner.startScan(scanFilters, scanSettings, currentScanCallback)
    }

//    fun scan(
//        scanOptions: ScanOptions,
//        onScanCompleted: (BluetoothDevice) -> Unit,
//        onScanFailed: () -> Unit,
//        onScanError: (throwable: Throwable?) -> Unit
//    ) {
////        executor.execute {
//        rxBleClient.scanBleDevices(
//            ScanSettings.Builder()
//                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                .build(),
//            scanOptions.toScanFilters()
//        ).subscribe(
//            {
//                Log.d(TAG, "on scan result with result ${it.bleDevice}")
//                it.bleDevice?.apply {
//                    scanDisposable?.dispose()
//                    scanDisposable = null
//                    onScanCompleted(bluetoothDevice)
//                } ?: onScanFailed()
//            },
//            {
//                Log.e(TAG, "failed to scan for device, throwable: $it")
//                it.printStackTrace()
//                onScanError(it)
//                scanDisposable?.dispose()
//                scan(scanOptions, onScanCompleted, onScanFailed, onScanError)
//            }, {
//                Log.d(TAG, "scan completed")
//            }, {
//                Log.d(TAG, "scan was subscripted")
//                scanDisposable?.dispose()
//                scanDisposable = it
//            }
//        )
////        }
//    }

    fun connect(
        address: String,
        onConnectionCompleted: () -> Unit,
        onConnectionTerminated: () -> Unit,
        onConnectionFailed: () -> Unit,
        onConnectError: (throwable: Throwable?) -> Unit
    ) {
        findRemoteDevice(address)?.let { remoteDevice ->
            if (!remoteDevice.isConnected) {
                Log.d(TAG, "about to connect to device ${remoteDevice.device.address}")
                val gattCallback = GattCallback().apply {
                    delegate = this@BleManager
                }
                remoteDevice.apply {
                    this.onConnectionCompleted = onConnectionCompleted
                    this.onConnectionTerminated = onConnectionTerminated
                    this.onConnectionFailed = onConnectionFailed
                    this.onConnectError = onConnectError
                }
                remoteDevice.gatt = remoteDevice.device.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            } else {
                onConnectError(Exception("try to connect to connected device $address"))
            }
        } ?: onConnectError(Exception("try to connect to connected device $address"))
    }

//    fun connect(
//        address: String,
//        onConnectionCompleted: () -> Unit,
//        onConnectionTerminated: () -> Unit,
//        onConnectionFailed: () -> Unit,
//        onConnectError: (throwable: Throwable?) -> Unit
//    ) {
//        val device = rxBleClient.getBleDevice(address)
//        val remoteDevice = RemoteDevice(device)
//        remoteDevices.add(remoteDevice)
//        device.establishConnection(false)
//            .subscribe(
//                {
//                    it?.also { connection ->
////                        connections[address] = it
//                        remoteDevice.connection = connection
//                        it.requestMtu(MTU)
//                            .subscribe({
//                                Log.d(TAG, "requested mtu properly of $it")
//                                onConnectionCompleted()
//                            }, {
//                                onConnectError(it)
//                                disconnect(remoteDevice)
//                            })
//                    } ?: onConnectionFailed()
//                },
//                {
//                    when (it) {
//                        is BleAlreadyConnectedException -> {
//                            Log.e(
//                                TAG,
//                                "tried to establish connection to already connected device $address"
//                            )
//                        }
//                        is BleDisconnectedException -> {
//                            onDisconnection(remoteDevice)
//                            onConnectionTerminated()
//                        }
//                        else -> {
//                            Log.e(TAG, "failed to connect to device, throwable: $it")
//                            it.printStackTrace()
//                            onConnectError(it)
//                            connect(
//                                address,
//                                onConnectionCompleted,
//                                onConnectionTerminated,
//                                onConnectionFailed,
//                                onConnectError
//                            )
//                        }
//                    }
//                },
//                {
//                    Log.d(
//                        TAG,
//                        "connection establishments was submitted for device $address"
//                    )
//                },
//                {
//                    remoteDevice.addDisposableResource(it)
//                }
//            )
//    }

    fun registerCharacteristicNotifications(
        uuid: UUID,
        address: String,
        onCharacteristicChanged: (ByteArray) -> Unit,
        onRegisterToNotificationsError: (throwable: Throwable?) -> Unit,
        onRegistered: () -> Unit
    ) {
        findRemoteDevice(address)?.apply {
            this.onCharacteristicChanged[uuid] = onCharacteristicChanged
            this.onRegisterToNotificationsErrorCallbacks[uuid] = onRegisterToNotificationsError
            this.onRegistered[uuid] = onRegistered
            this.onWrittenToDescriptor[uuid] = {
                setupCharacteristicNotifications(device, uuid)
            }
            writeEnablingNotificationsToClientConfigureDescriptor(device, uuid)
        }
    }

    private fun writeEnablingNotificationsToClientConfigureDescriptor(
        device: BluetoothDevice,
        uuid: UUID
    ) {
        writeToClientConfigureDescriptor(
            device,
            uuid,
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        )
    }

    private fun writeDisablingNotificationsToClientConfigureDescriptor(
        device: BluetoothDevice,
        uuid: UUID
    ) {
        writeToClientConfigureDescriptor(
            device,
            uuid,
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        )
    }

    private fun writeToClientConfigureDescriptor(
        device: BluetoothDevice,
        uuid: UUID,
        value: ByteArray
    ) {
        findRemoteDevice(device.address)?.let { remoteDevice ->
            remoteDevice.gatt?.let { gatt ->
                gatt.services.find { it.characteristics.any { it.uuid == uuid } }
                    ?.getCharacteristic(uuid)
                    ?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    ?.let { descriptor ->
                        descriptor.value = value
                        var count = 1000
                        while (count-- > 0 && !gatt?.writeDescriptor(descriptor)) {
                            Thread.sleep(10)
                        }
                        if (count == 0) {
                            remoteDevice.onRegisterToNotificationsErrorCallbacks[uuid]?.let {
                                it(
                                    Exception("Failed to write to descriptor")
                                )
                            }
                            return
                        }
                    }
            }
        }
    }

    private fun setupCharacteristicNotifications(device: BluetoothDevice, uuid: UUID) {
        findRemoteDevice(device.address)?.let { remoteDevice ->
            remoteDevice.gatt?.let { gatt ->
                gatt.services.find { it.characteristics.any { it.uuid == uuid } }
                    ?.getCharacteristic(uuid)
                    ?.let { characteristic ->
                        var count = 1000
                        while (count-- > 0 && !gatt?.setCharacteristicNotification(
                                characteristic,
                                true
                            )
                        ) {
                            Thread.sleep(10)
                        }
                        if (count == 0) {
                            remoteDevice.onRegisterToNotificationsErrorCallbacks[uuid]?.let {
                                it(
                                    Exception("Failed to setup notifications")
                                )
                            }
                            return
                        }
                        remoteDevice.onRegistered[uuid]?.let { it() }
                    }
            }
        }
    }

//    fun registerCharacteristicNotifications(
//        uuid: UUID,
//        address: String,
//        onCharacteristicChanged: (ByteArray) -> Unit,
//        onRegisterToNotificationsError: (throwable: Throwable?) -> Unit,
//        onRegistered: () -> Unit
//    ) {
//        findConnection(address)?.also { connection ->
//            connection.setupNotification(uuid)
//                .doOnError {
//                    Log.e(
//                        TAG,
//                        "failed to setup notifications to characteristic device $uuid, throwable: $it"
//                    )
//                    it.printStackTrace()
//                    onRegisterToNotificationsError(it)
//                }
//                .doOnNext {
//                    onRegistered()
//                }
//                .flatMap { it }
//                .subscribe(
//                    {
//                        onCharacteristicChanged(it)
//                    },
//                    {
//                        Log.e(
//                            TAG,
//                            "failed to setup notifications to characteristic device $uuid, throwable: $it"
//                        )
//                        it.printStackTrace()
//                        onRegisterToNotificationsError(it)
//                    }
//                )
//        }
//    }

    fun writeToCharacteristic(
        uuid: UUID,
        address: String,
        data: ByteArray,
        onWriteSucceeded: (address: String, data: ByteArray) -> Unit,
        onWriteFailed: (throwable: Throwable?) -> Unit
    ) {
        findRemoteDevice(address)?.apply {
            this.onWriteSucceeded[uuid] = onWriteSucceeded
            this.onWriteFailed[uuid] = onWriteFailed
            gatt?.services?.find { it.characteristics.any { it.uuid == uuid } }
                ?.getCharacteristic(uuid)?.let { characteristic ->
                    characteristic.value = data
                    gatt?.writeCharacteristic(characteristic)
            }

        }
    }

//    fun writeToCharacteristic(
//        uuid: UUID,
//        address: String,
//        data: ByteArray,
//        onWriteSucceeded: (address: String, data: ByteArray) -> Unit,
//        onWriteFailed: (throwable: Throwable?) -> Unit
//    ) {
//        findConnection(address)?.also { connection ->
//            connection.writeCharacteristic(uuid, data)
//                .subscribe(
//                    {
//                        onWriteSucceeded(address, data)
//                    },
//                    {
//                        onWriteFailed(it)
//                    }
//                )
//        }
//    }

    private fun disconnect(remoteDevice: RemoteDevice) {
        onDisconnection(remoteDevice)
    }

    private fun onDisconnection(remoteDevice: RemoteDevice) {
        remoteDevice.cleanupResources()
        remoteDevices.remove(remoteDevice)
    }

//    private fun findConnection(address: String): RxBleConnection? =
//        remoteDevices.find { address == it.device.address }?.connection

    private fun findRemoteDevice(address: String): RemoteDevice? =
        remoteDevices.find { address == it.device.address }

    // delegate
    override fun onConnected(gatt: BluetoothGatt?) {
        gatt?.let {
            if (!it.discoverServices()) {
                findRemoteDevice(it.device.address)?.apply { onConnectError(Exception("Connect failed with $device - cannot discover services")) }
            }
        }
    }

    override fun onDisconnected(gatt: BluetoothGatt?) {
        gatt?.let {
            findRemoteDevice(it.device.address)?.apply { onConnectionTerminated() }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?) {
        gatt?.let {
            findRemoteDevice(it.device.address)?.apply { onConnectionCompleted() }
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt?.let {
            findRemoteDevice(it.device.address)?.apply {

                this.onCharacteristicChanged[characteristic.uuid]?.let { it(characteristic?.value ?: ByteArray(0)) }
            }
        }
    }

    override fun onWriteDescriptorSuccess(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor) {
        gatt?.device?.let {
            findRemoteDevice(it.address)?.onWrittenToDescriptor?.get(descriptor.characteristic.uuid)
                ?.let { it() }
        }
    }

    override fun onWriteCharacteristicFailed(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt?.device?.let {
            findRemoteDevice(it.address)?.onWriteFailed?.get(characteristic.uuid)
                ?.let { it(Exception("write to descriptor failed")) }
        }
    }

    override fun onWriteCharacteristicSuccess(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt?.device?.let {device ->
            findRemoteDevice(device.address)?.onWriteSucceeded?.get(characteristic.uuid)
                ?.let { it(device.address, characteristic.value) }
        }
    }

    override fun onWriteDescriptorFailed(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor
    ) {
        gatt?.device?.let {
            findRemoteDevice(it.address)?.onRegisterToNotificationsErrorCallbacks?.get(descriptor.characteristic.uuid)
                ?.let { it(Exception("write to descriptor failed")) }
        }
    }

    class RemoteDevice(val device: BluetoothDevice) {
        //        private var disposables = CompositeDisposable()
        var isConnected = false
        var gatt: BluetoothGatt? = null
//        var connection: RxBleConnection? = null

        var onConnectionCompleted: () -> Unit = {}
        var onConnectionTerminated: () -> Unit = {}
        var onConnectionFailed: () -> Unit = {}
        var onConnectError: (throwable: Throwable?) -> Unit = {}
        var onWrittenToDescriptor: MutableMap<UUID, () -> Unit> = mutableMapOf()
        var onRegisterToNotificationsErrorCallbacks: MutableMap<UUID, (throwable: Throwable?) -> Unit> =
            mutableMapOf()
        var onRegistered: MutableMap<UUID, () -> Unit> = mutableMapOf()
        var onCharacteristicChanged: MutableMap<UUID, (ByteArray) -> Unit> = mutableMapOf()
        var onWriteSucceeded: MutableMap<UUID, (address: String, data: ByteArray) -> Unit> = mutableMapOf()
        var onWriteFailed: MutableMap<UUID, (throwable: Throwable?) -> Unit> = mutableMapOf()


        fun cleanupResources() {
//            connection = null
//            disposables.dispose()
//            disposables = CompositeDisposable()
        }

//        fun addDisposableResource(disposable: Disposable) {
//            disposables.addAll(disposable)
//        }
    }

    companion object {
        private const val TAG = "BleManager"
        private const val MTU =
            RxBleConnection.GATT_MTU_MAXIMUM //  todo: move this to ble manager
    }
}