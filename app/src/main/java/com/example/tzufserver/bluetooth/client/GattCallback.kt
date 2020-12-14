package com.example.tzufserver.bluetooth.client

import android.bluetooth.*
import android.util.Log

class GattCallback() : BluetoothGattCallback() {

    private var serverGatt: BluetoothGatt? = null

    var delegate: Delegate? = null

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
        characteristic?.let { delegate?.onCharacteristicChanged(gatt, it) }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        characteristic?.let {
            if (status == 0) {
                delegate?.onWriteCharacteristicSuccess(gatt, it)
            } else {
                delegate?.onWriteCharacteristicFailed(gatt, it)
            }
        }
    }

    override fun onConnectionStateChange(
        gatt: BluetoothGatt?,
        status: Int,
        newState: Int
    ) {
        super.onConnectionStateChange(gatt, status, newState)
        Log.d(
            TAG,
            "on connection state changed for address ${gatt?.device?.address}, status $status, state $newState"
        )
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.device?.let { onConnected(gatt) }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                onDisconnected(gatt)
            }
        } else {
            if (status == 133 || status == 128) {
                Thread.sleep(100)
            }
            onDisconnected(gatt)
        }
    }

    private fun onConnected(gatt: BluetoothGatt?) {
        serverGatt = gatt
        delegate?.onConnected(gatt)
    }

    private fun onDisconnected(gatt: BluetoothGatt?) {
        serverGatt = null
        delegate?.onDisconnected(gatt)
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorRead(gatt, descriptor, status)
        Log.d(
            TAG,
            "onDescriptorRead device ${gatt?.device?.address} of char ${descriptor?.characteristic?.uuid} with status $status"
        )
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorWrite(gatt, descriptor, status)
        Log.d(
            TAG,
            "onDescriptorWrite to device ${gatt?.device?.address} to char ${descriptor?.characteristic?.uuid} with status $status"
        )
        descriptor?.let {
            if (status == 0) {
                delegate?.onWriteDescriptorSuccess(gatt, it)
            } else {
                delegate?.onWriteDescriptorFailed(gatt, it)
            }
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        super.onMtuChanged(gatt, mtu, status)
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        Log.d(
            TAG,
            "on read remote rssi device ${gatt?.device?.address}, status $status, rssi $rssi"
        )
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        delegate?.onServicesDiscovered(gatt)
    }


    interface Delegate {

        fun onConnected(gatt: BluetoothGatt?)

        fun onDisconnected(gatt: BluetoothGatt?)

        fun onServicesDiscovered(gatt: BluetoothGatt?)

        fun onWriteDescriptorSuccess(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor)

        fun onWriteDescriptorFailed(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor)

        fun onWriteCharacteristicSuccess(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        )

        fun onWriteCharacteristicFailed(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        )

        fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic
        )
    }

    companion object {
        const val TAG = "GattCallback"
    }
}