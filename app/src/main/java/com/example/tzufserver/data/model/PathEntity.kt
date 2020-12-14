package com.example.tzufserver.data.model

import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.mapbox.mapboxsdk.geometry.LatLng

class PathEntity(
    id: String,
    name: String,
    positions: List<LatLng>
) : BaseEntity(id, name, positions) {

    override fun toByteArray(): ByteArray {
        val bytes = mutableListOf<Byte>(TzufMapServerApi.PATH_TYPE_VALUE.toByte()).apply {
            addAll(super.toByteArray().toList())
        }
        return bytes.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        return other is PathEntity && super.equals(other)
    }

    companion object {
        fun fromBaseEntity(entity: BaseEntity) =
            PathEntity(entity.id, entity.name, entity.positions)
    }

}