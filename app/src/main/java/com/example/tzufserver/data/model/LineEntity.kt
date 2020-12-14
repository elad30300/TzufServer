package com.example.tzufserver.data.model

import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.mapbox.mapboxsdk.geometry.LatLng

open class LineEntity(
    id: String,
    name: String,
    firstPosition: LatLng,
    secondPosition: LatLng
) : BaseEntity(id, name, listOf(firstPosition, secondPosition)) {

    fun getFirstPosition() = getPosition(0)

    fun getSecondPosition() = getPosition(1)

    fun setFirstPosition(newPosition: LatLng) {
        getFirstPosition().apply {
            latitude = newPosition.latitude
            longitude = newPosition.longitude
        }
    }

    fun setSecondPosition(newPosition: LatLng) {
        getSecondPosition().apply {
            latitude = newPosition.latitude
            longitude = newPosition.longitude
        }
    }

}

class LimitingBoundariesEntity(
    id: String,
    name: String,
    firstPosition: LatLng,
    secondPosition: LatLng
) : LineEntity(id, name, firstPosition, secondPosition) {

    override fun toByteArray(): ByteArray {
        val bytes = mutableListOf<Byte>(TzufMapServerApi.LIMIT_BORDERS_TYPE_VALUE.toByte()).apply {
            addAll(super.toByteArray().toList())
        }
        return bytes.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        return other is LimitingBoundariesEntity && super.equals(other)
    }

    companion object {
        fun fromBaseEntity(entity: BaseEntity) = LimitingBoundariesEntity(
            entity.id,
            entity.name,
            entity.getPosition(0),
            entity.getPosition(1)
        )
    }

}