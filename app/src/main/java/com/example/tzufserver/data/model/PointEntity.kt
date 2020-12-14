package com.example.tzufserver.data.model

import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.mapbox.mapboxsdk.geometry.LatLng

open class PointEntity(
    id: String,
    name: String,
    position: LatLng
) : BaseEntity(id, name, listOf(position)) {

    fun getPosition() = getPosition(0)

    fun setPosition(newPosition: LatLng) {
        getPosition().apply {
            latitude = newPosition.latitude
            longitude = newPosition.longitude
        }
    }

}

class GeneralEnemyEntity(
    id: String,
    name: String,
    position: LatLng
) : PointEntity(id, name, position) {

    override fun toByteArray(): ByteArray {
        val bytes = mutableListOf<Byte>(TzufMapServerApi.ENEMY_TYPE_VALUE.toByte()).apply {
            addAll(super.toByteArray().toList())
        }
        return bytes.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        return other is GeneralEnemyEntity && super.equals(other)
    }

    companion object {
        fun fromBaseEntity(entity: BaseEntity) = GeneralEnemyEntity(entity.id, entity.name, entity.getPosition(0))
    }

}

class GeneralForcesEntity(
    id: String,
    name: String,
    position: LatLng
) : PointEntity(id, name, position) {

    override fun toByteArray(): ByteArray {
        val bytes = mutableListOf<Byte>(TzufMapServerApi.TROOPS_TYPE_VALUE.toByte()).apply {
            addAll(super.toByteArray().toList())
        }
        return bytes.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        return other is GeneralForcesEntity && super.equals(other)
    }

    companion object {
        fun fromBaseEntity(entity: BaseEntity) = GeneralForcesEntity(entity.id, entity.name, entity.getPosition(0))
    }
}