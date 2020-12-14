package com.example.tzufserver.data.model

import com.example.tzufserver.extension.toByteList
import com.mapbox.mapboxsdk.geometry.LatLng

open class BaseEntity(
    val id: String,
    val name: String,
    val positions: List<LatLng>
) {

    fun getPosition(index: Int) = positions[index]

    fun setPosition(newPosition: LatLng, index: Int) {
        getPosition(index).apply {
            latitude = newPosition.latitude
            longitude = newPosition.longitude
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other as? BaseEntity)?.let {
            it.id == this.id
        } ?: false
    }

    fun hasSameContent(other: BaseEntity): Boolean {
        return this == other
                && this.name == other.name
                && this.positions.size == other.positions.size
                && this.positions.zip(other.positions) { pos1, pos2 -> Pair(pos1, pos2) }
            .all { (p1, p2) -> p1 == p2 }
    }

    open fun toByteArray(): ByteArray {
        val bytes = mutableListOf<Byte>()

        bytes.add(id.length.toByte())
        bytes.addAll(id.toByteArray().toList())
        bytes.add(name.length.toByte())
        bytes.addAll(name.toByteArray().toList())
        bytes.add(positions.size.toByte())
        positions.forEach {
            bytes.addAll(it.latitude.toByteList())
            bytes.addAll(it.longitude.toByteList())
        }

        return bytes.toByteArray()
    }

}