package com.example.tzufserver.bluetooth.server

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi

class GeoService : BluetoothGattService(TzufMapServerApi.GEO_SERVICE_UUID, SERVICE_TYPE_PRIMARY) {

    init {
        addEntitiesRequestCharacteristic()
        addRasterTilesRequestCharacteristic()
        addEntitiesNotificationCharacteristic()
        addRasterTilesNotificationCharacteristic()
        addEntitiesNotificationAckCharacteristic()
        addRasterTilesNotificationAcktCharacteristic()
    }

    private fun addEntitiesRequestCharacteristic() {
        val characteristic = BluetoothGattCharacteristic(
            TzufMapServerApi.ENTITIES_REQUEST_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        )
        addCharacteristic(characteristic)
    }

    private fun addRasterTilesRequestCharacteristic() {
        val characteristic = BluetoothGattCharacteristic(
            TzufMapServerApi.RASTER_TILES_REQUEST_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        )
        addCharacteristic(characteristic)
    }

    private fun addEntitiesNotificationCharacteristic() {
        val characteristic = BluetoothGattCharacteristic(
            TzufMapServerApi.ENTITIES_NOTIFICATIONS_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_INDICATE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        )
        val clientConfigureDescriptor = BluetoothGattDescriptor(
            TzufMapServerApi.CLIENT_CONFIG_DESCRIPTOR_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        characteristic.addDescriptor(clientConfigureDescriptor)
        addCharacteristic(characteristic)
    }

    private fun addRasterTilesNotificationCharacteristic() {
        val characteristic = BluetoothGattCharacteristic(
            TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_INDICATE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        )
        val clientConfigureDescriptor = BluetoothGattDescriptor(
            TzufMapServerApi.CLIENT_CONFIG_DESCRIPTOR_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        characteristic.addDescriptor(clientConfigureDescriptor)
        addCharacteristic(characteristic)
    }

    private fun addEntitiesNotificationAckCharacteristic() {
        val characteristic = BluetoothGattCharacteristic(
            TzufMapServerApi.ENTITIES_NOTIFICATIONS_ACK_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        )
        addCharacteristic(characteristic)
    }

    private fun addRasterTilesNotificationAcktCharacteristic() {
        val characteristic = BluetoothGattCharacteristic(
            TzufMapServerApi.RASTER_TILES_NOTIFICATIONS_ACK_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
                    or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        )
        addCharacteristic(characteristic)
    }

}