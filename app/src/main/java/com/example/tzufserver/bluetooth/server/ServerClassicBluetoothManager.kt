package com.example.tzufserver.bluetooth.server

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tzufserver.ApplicationSettings
import com.example.tzufserver.bluetooth.client.ClientClassicBluetoothManager
import com.example.tzufserver.bluetooth.client.ScanOptions
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
class ServerClassicBluetoothManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val applicationSettings: ApplicationSettings
) : AutoCloseable {
    private var bluetoothEnabled = false
    private val adapter: BluetoothAdapter by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private val canAcceptSockets = MutableLiveData<Boolean>(false)
    private var acceptThreads = mutableListOf<AcceptThread>()
    private val connections = mutableListOf<Connection>()

    init {
        if (applicationSettings.gattType == applicationSettings.GATT_TYPE_SERVER) {
            applicationSettings.bluetoothEnabled.observeForever {
                onBluetoothStatusChanged(it)
            }
        }
    }

    // bluetooth public api

    fun startAcceptingConnectionsToSocket(
        uuid: UUID,
        name: String,
        listener: BluetoothSocketListener
    ): Boolean {
        val acceptThread = findAcceptThread(uuid)
        findAcceptThread(uuid)?.let {
            it.close()
            acceptThreads.remove(it)
        }
        acceptThreads.add(AcceptThread(uuid, name, listener).apply { start() })
//        if (acceptThread == null) {
//            acceptThreads.add(AcceptThread(uuid, name, listener).apply { start() })
//            return true
//        }
        return true
    }

    fun stopAcceptingConnectionsToSocket(uuid: UUID) {
        findAcceptThread(uuid)?.close()
    }

//    fun stopConnectionToSocket(device: BluetoothDevice, uuid: UUID)

    // throws an exception
    fun write(device: BluetoothDevice, uuid: UUID, value: ByteArray) {
        Log.d(TAG, "about to write to device ${device.address} and uuid $uuid")
        (findConnection(device)?.getConnectionThread(uuid) as? ConnectedThread)?.write(value)
    }

    // private logic implementations

    private abstract inner class ConnectionThread(
        val serviceUuid: UUID,
        val serviceName: String = "",
        var socketListener: BluetoothSocketListener? = null
    ) : Thread(), Closeable

    private inner class AcceptThread(
        serviceUuid: UUID,
        serviceName: String = "",
        socketListener: BluetoothSocketListener? = null
    ) : ConnectionThread(serviceUuid, serviceName, socketListener) {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            adapter?.listenUsingRfcommWithServiceRecord(serviceName, serviceUuid)
//            adapter?.listenUsingInsecureRfcommWithServiceRecord(serviceName, serviceUuid)
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
//            while (true) {
                val socket: BluetoothSocket? = try {
                    Log.d(TAG, "start to accept connections to service uuid $serviceUuid")
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it, serviceUuid, serviceName, socketListener)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        override fun close() {
            try {
                Log.d(TAG, "closing server socket in accept thread")
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    // client bluetooth public api

    fun cancelAccepts() {
        acceptThreads.forEach { it.close() }
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
        canAcceptSockets.value = true
        makeBluetoothDiscoverable()
    }

    private fun makeBluetoothDiscoverable() {
        val discoverableIntent: Intent =
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                // TODO: try to make it always discoverable without value = 0 as it is discouraged according to https://developer.android.com/guide/topics/connectivity/bluetooth#FindDevices
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600)
            }
        context.startActivity(discoverableIntent)
    }

    private fun onBluetoothTurnedOff() {
        bluetoothEnabled = false
        canAcceptSockets.value = false
        cleanupResources()
    }

    override fun close() {
        cleanupResources()
    }

    private class Connection(val device: BluetoothDevice) : Closeable {
        private val connectionThreads = mutableMapOf<UUID, ConnectionThread>()

        fun setConnectionThread(uuid: UUID, connectionThread: ConnectionThread) {
            connectionThreads[uuid] = connectionThread
        }

        fun getConnectionThread(uuid: UUID): ConnectionThread? = connectionThreads[uuid]

        override fun close() {
            connectionThreads.forEach { it.value.close() }
        }
    }

    private fun cleanupResources() {
        cancelAccepts()
        connections.forEach { it.close() }
    }

    private fun findConnection(device: BluetoothDevice): Connection? = connections.find {
        it.device.address == device.address
    }

    private fun findAcceptThread(uuid: UUID): AcceptThread? =
        acceptThreads.find { it.serviceUuid == uuid }

    private fun hasConnection(device: BluetoothDevice) = findConnection(device) != null

    private fun manageMyConnectedSocket(
        socket: BluetoothSocket,
        uuid: UUID,
        serviceName: String,
        socketListener: BluetoothSocketListener? = null
    ) {
        with(ConnectedThread(socket, uuid, serviceName, socketListener)) {
            findConnection(socket.remoteDevice)?.apply {
                setConnectionThread(uuid, this@with)
            } ?: connections.add(Connection(socket.remoteDevice).apply {
                setConnectionThread(uuid, this@with)
            })
            start()
        }
    }

    private inner class ConnectedThread(
        private val mmSocket: BluetoothSocket,
//        private val device: BluetoothDevice,
        uuid: UUID,
        serviceName: String,
        socketListener: BluetoothSocketListener? = null
    ) :
        ConnectionThread(uuid, serviceName, socketListener) {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(MAX_NUMBER_OF_BYTES) // mmBuffer store for the stream

        override fun start() {
            super.start()
            socketListener?.onSocketReady(mmSocket.remoteDevice, serviceUuid)
        }

        override fun run() {
//            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                val availableBytes = mmInStream.available()
                if (availableBytes > 0) {
                    val mmBuffer = ByteArray(availableBytes)
                    var numBytes = try {
//                    mmInStream.
                        mmInStream.read(mmBuffer)
                    } catch (e: IOException) {
                        Log.e(TAG, "Input stream was disconnected", e)
                        socketListener?.onSocketError(mmSocket.remoteDevice, serviceUuid, e)
                        this.close()
                        break
                    }

                    if (numBytes > 0) {
                        Log.d(
                            TAG,
                            "read successfully from buffer for device ${mmSocket.remoteDevice.address} with socket $serviceUuid"
                        )

                        socketListener?.onSocketValueChanged(mmBuffer, mmSocket.remoteDevice, serviceUuid)
                        // Send the obtained bytes to the UI activity.
//                val readMsg = handler.obtainMessage(
//                    MESSAGE_READ, numBytes, -1,
//                    mmBuffer)
//                readMsg.sendToTarget()
                    }
                }
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
                socketListener?.onSocketError(mmSocket.remoteDevice, serviceUuid, e)
                return
            }

            socketListener?.onWriteToSocketPerformed(mmSocket.remoteDevice, serviceUuid, bytes)
            // Share the sent message with the UI activity.
//            val writtenMsg = handler.obtainMessage(
//                MESSAGE_WRITE, -1, -1, mmBuffer
//            )
        }


        override fun close() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    fun canAcceptSockets(): LiveData<Boolean> = canAcceptSockets

    interface BluetoothSocketListener {
        fun onSocketConnected(device: BluetoothDevice, uuid: UUID) {}

        fun onSocketTerminated(device: BluetoothDevice, uuid: UUID) {}

        fun onSocketReady(device: BluetoothDevice, uuid: UUID) {}

        fun onSocketValueChanged(bytes: ByteArray, device: BluetoothDevice, uuid: UUID) {}

        fun onWriteToSocketPerformed(device: BluetoothDevice, uuid: UUID, bytes: ByteArray) {}

        fun onSocketError(device: BluetoothDevice, uuid: UUID, exception: Exception) {}
    }

    companion object {
        private const val TAG = "ServerClsBtMng"
        const val MAX_NUMBER_OF_BYTES = 1000
    }

}