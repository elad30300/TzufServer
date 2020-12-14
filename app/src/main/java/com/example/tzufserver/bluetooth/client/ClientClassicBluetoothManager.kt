package com.example.tzufserver.bluetooth.client

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.tzufserver.ApplicationSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientClassicBluetoothManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val applicationSettings: ApplicationSettings
) : AutoCloseable {
    private var bluetoothEnabled = false
    private val adapter: BluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private var scanResultReceiver: BroadcastReceiver? = null
    private val connections = mutableListOf<Connection>()

    init {
        if (applicationSettings.gattType == applicationSettings.GATT_TYPE_CLIENT) {
            applicationSettings.bluetoothEnabled.observeForever {
                onBluetoothStatusChanged(it)
            }
        }
    }

    // bluetooth public api

    fun scan(
        scanOptions: ScanOptions = ScanOptions(),
        onDeviceFound: (BluetoothDevice) -> Unit
    ): Boolean {
        if (bluetoothEnabled) {
            if (!adapter.isDiscovering) {
                listenToBluetoothScanResult(scanOptions) { onDeviceFound(it) }
                return adapter.startDiscovery()
            }
        }
        return false
    }

    fun cancelScan(): Boolean {
        stopListenToBluetoothScanResult()
        if (adapter.isDiscovering) {
            return adapter.cancelDiscovery()
        }
        return false
    }

    fun isScaning() = adapter.isDiscovering

    fun listenToUuidSocket(
        device: BluetoothDevice,
        uuid: UUID,
        listener: BluetoothSocketListener
    ) {
        with(ConnectThread(device, uuid, listener)) {
            if (!hasConnection(device)) {
                val connection = Connection(device).apply {
                    setConnectionThread(uuid, this@with)
                }
                connections.add(connection)
                start()
            }
        }
    }

    fun stopListenToUuidSocket(device: BluetoothDevice, uuid: UUID) {
        findConnection(device)?.getConnectionThread(uuid)?.close()
    }

    // private logic implementations

    private fun onBluetoothStatusChanged(enabled: Boolean) {
        if (enabled) {
            onBluetoothTurnedOn()
        } else {
            onBluetoothTurnedOff()
        }
    }

    private fun onBluetoothTurnedOn() {
        bluetoothEnabled = true
    }

    private fun onBluetoothTurnedOff() {
        bluetoothEnabled = false
        cleanupResources()
    }

    private fun listenToBluetoothScanResult(
        scanOptions: ScanOptions = ScanOptions(),
        onDeviceFound: (BluetoothDevice) -> Unit
    ) {
        try {
            stopListenToBluetoothScanResult()
            scanResultReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            val device =
                                intent?.let { it.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice }
                            if (!device.fetchUuidsWithSdp()) {
                                Log.e(
                                    TAG,
                                    "can't fetch uuids with sdp for device ${device.address}"
                                )
                                return
                            }
                            if (scanOptions.isDeviceMatchOptionsFilters(device)) {
                                cancelScan()
                                onDeviceFound(device)
                            }
                        }
                    }
                }
            }
            context.registerReceiver(scanResultReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        } catch (ex: Exception) {
            Log.e(TAG, "scanResultReceiver failed to register due to $ex\n${ex.stackTrace}")
        }
    }

    private fun stopListenToBluetoothScanResult() {
        try {
            context.unregisterReceiver(scanResultReceiver)
        } catch (ex: Exception) {
            Log.e(TAG, "scanResultReceiver failed to unregister due to $ex\n${ex.stackTrace}")
        } finally {
            scanResultReceiver = null
        }
    }

    override fun close() {
        cleanupResources()
    }

    //    private class Connection(devic) : Closeable {
    private class Connection(val device: BluetoothDevice) : Closeable {
        //        private val sockets = mutableMapOf<UUID, BluetoothSocket>()
        private val connectionThreads = mutableMapOf<UUID, ConnectionThread>()

        fun setConnectionThread(uuid: UUID, connectionThread: ConnectionThread) {
            connectionThreads[uuid] = connectionThread
        }

        fun getConnectionThread(uuid: UUID): ConnectionThread? = connectionThreads[uuid]
//        fun getConnectionThread(uuid: UUID): ConnectionThread? = connectionThreads.find { it.key == uuid }

//        fun getSocketFor(uuid: UUID): BluetoothSocket? = sockets[uuid]
//
//        fun setSocketFor(uuid: UUID, socket: BluetoothSocket) {
//            sockets[uuid] = socket
//        }

        override fun close() {
            connectionThreads.forEach { it.value.close() }
        }
    }

    private fun cleanupResources() {
        cancelScan()
        connections.forEach { it.close() }
    }

    private fun findConnection(device: BluetoothDevice): Connection? = connections.find {
        it.device.address == device.address
    }

    private fun hasConnection(device: BluetoothDevice) = findConnection(device) != null

    private abstract inner class ConnectionThread(
        val device: BluetoothDevice,
        val serviceUuid: UUID,
        var socketListener: BluetoothSocketListener? = null
    ) : Thread(), Closeable

    private inner class ConnectThread(
        device: BluetoothDevice,
        serviceUuid: UUID,
        socketListener: BluetoothSocketListener? = null
    ) :
        ConnectionThread(device, serviceUuid, socketListener) {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(serviceUuid)
        }

        override fun run() {
            super.run()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
//                var tryToConnect = true
//                while (tryToConnect) {
                    try {
                        socket.connect()
//                        tryToConnect = false

                        socketListener?.onSocketConnected(device, serviceUuid)

                        // The connection attempt succeeded. Perform work associated with
                        // the connection in a separate thread.
                        manageMyConnectedSocket(socket, device, serviceUuid, socketListener)
                    } catch (ex: Exception) {
                        socketListener?.onSocketError(device, serviceUuid, ex)
                        this.close()
                    }
                }
//            }
        }

        override fun close() {
            try {
                mmSocket?.close()
                socketListener?.onSocketTerminated(device, serviceUuid)
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    private fun manageMyConnectedSocket(
        socket: BluetoothSocket,
        device: BluetoothDevice,
        uuid: UUID,
        socketListener: BluetoothSocketListener? = null
    ) {
        with(ConnectedThread(socket, device, uuid, socketListener)) {
            findConnection(socket.remoteDevice)?.setConnectionThread(uuid, this)
            start()
        }
    }

    private inner class ConnectedThread(
        private val mmSocket: BluetoothSocket,
        device: BluetoothDevice,
        uuid: UUID,
        socketListener: BluetoothSocketListener? = null
    ) :
        ConnectionThread(device, uuid, socketListener) {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun start() {
            super.start()
            socketListener?.onSocketReady(device, serviceUuid)
        }

        override fun run() {
//            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                var numBytes = try {
//                    mmInStream.
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.e(TAG, "Input stream was disconnected", e)
                    socketListener?.onSocketError(device, serviceUuid, e)
                    this.close()
                    break
                }

                Log.d(
                    TAG,
                    "read successfully from buffer for device ${device.address} with socket $serviceUuid"
                )

                socketListener?.onSocketValueChanged(mmBuffer, device, serviceUuid)
                // Send the obtained bytes to the UI activity.
//                val readMsg = handler.obtainMessage(
//                    MESSAGE_READ, numBytes, -1,
//                    mmBuffer)
//                readMsg.sendToTarget()
            }
        }

        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
//                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
//                val bundle = Bundle().apply {
//                    putString("toast", "Couldn't send data to the other device")
//                }
//                writeErrorMsg.data = bundle
//                handler.sendMessage(writeErrorMsg)
                socketListener?.onSocketError(device, serviceUuid, e)
                this.close()
                return
            }

            socketListener?.onWriteToSocketPerformed(device, serviceUuid, bytes)
            // Share the sent message with the UI activity.
//            val writtenMsg = handler.obtainMessage(
//                MESSAGE_WRITE, -1, -1, mmBuffer
//            )
        }


        override fun close() {
            try {
                mmSocket.close()
                socketListener?.onSocketTerminated(device, serviceUuid)
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    interface BluetoothSocketListener {
        fun onSocketConnected(device: BluetoothDevice, uuid: UUID) {}

        fun onSocketTerminated(device: BluetoothDevice, uuid: UUID) {}

        fun onSocketReady(device: BluetoothDevice, uuid: UUID) {}

        fun onSocketValueChanged(bytes: ByteArray, device: BluetoothDevice, uuid: UUID) {}

        fun onWriteToSocketPerformed(device: BluetoothDevice, uuid: UUID, bytes: ByteArray) {}

        fun onSocketError(device: BluetoothDevice, uuid: UUID, exception: Exception) {}
    }


    companion object {
        private const val TAG = "ClientClsBtMng"
    }

}