package com.example.tzufserver.bluetooth.api.protocol.message

import com.example.tzufserver.provider.dispatchers.DefaultDispatcherProvider
import com.example.tzufserver.provider.dispatchers.DispatcherProvider

abstract class GeoMessage(val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()) {

    constructor(bytes: ByteArray, dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()): this(dispatcherProvider)

    open suspend fun toByteArray() = ByteArray(0)

}