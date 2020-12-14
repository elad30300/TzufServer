package com.example.tzufserver.strategy.parse.entity

import android.util.Log
import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.example.tzufserver.data.model.BaseEntity
import com.example.tzufserver.extension.toDouble
import com.mapbox.mapboxsdk.geometry.LatLng

open class EntityParseStrategy {

    open fun parse(bytes: ByteArray): BaseEntity {
        var currentIndex = TzufMapServerApi.ENTITY_ID_LENGTH_LOCAL_INDEX
        val idLength = bytes[currentIndex++]
        val id = String(bytes.slice(currentIndex until (currentIndex + idLength)).toByteArray())
        currentIndex += idLength
        val nameLength = bytes[currentIndex++]
        val name = String(bytes.slice(currentIndex until (currentIndex + nameLength)).toByteArray())
        currentIndex += nameLength
        val numberOfCoordinates = bytes[currentIndex].toInt()
        ++currentIndex
        val coordinates = mutableListOf<LatLng>()
        for (i in 0 until numberOfCoordinates) {
            val latitude =
                bytes.slice(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                    .toByteArray().toDouble()
            currentIndex += TzufMapServerApi.COORDINATE_SIZE
            val longitude =
                bytes.slice(currentIndex until (currentIndex + TzufMapServerApi.COORDINATE_SIZE))
                    .toByteArray().toDouble()
            currentIndex += TzufMapServerApi.COORDINATE_SIZE
            coordinates.add(LatLng(latitude, longitude))
        }

        return BaseEntity(id, name, coordinates)
    }

    protected fun dropTypeFromBytes(bytes: ByteArray): ByteArray {
        val startIndex = TzufMapServerApi.ENTITIES_TYPE_LOCAL_INDEX + TzufMapServerApi.ENTITY_TYPE_SIZE
        return bytes.copyOfRange(startIndex, bytes.size)
    }

}