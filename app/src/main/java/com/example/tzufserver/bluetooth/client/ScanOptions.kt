package com.example.tzufserver.bluetooth.client

import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid
import com.polidea.rxandroidble2.scan.ScanFilter
import java.util.*

class ScanOptions {

    var remoteName: String? = null
    var serviceUuid: UUID? = null
    var address: String? = null

    fun withRemoteName(name: String): ScanOptions {
        remoteName = name
        return this
    }

    fun withServiceUuid(uuid: UUID): ScanOptions {
        serviceUuid = uuid
        return this
    }

    fun toScanFilters(): ScanFilter {
        var builder = ScanFilter.Builder()
        remoteName?.also { builder = builder.setDeviceName(it) }
        serviceUuid?.also { builder = builder.setServiceUuid(ParcelUuid(it)) }
        address?.let { builder = builder.setDeviceAddress(it) }
        return builder.build()
    }

    fun isDeviceMatchOptionsFilters(device: BluetoothDevice): Boolean {
        return (remoteName == null && serviceUuid == null && address == null)
                || ((remoteName?.let { it == device.name } ?: true)
                && (address?.let { it == device.address } ?: true)
                && (serviceUuid?.let { device.uuids?.any { it.uuid == serviceUuid } } ?: true))
    }

}