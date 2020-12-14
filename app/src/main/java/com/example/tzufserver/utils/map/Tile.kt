package com.example.tzufserver.utils.map

import android.graphics.Bitmap
import androidx.core.graphics.BitmapCompat
import com.example.tzufserver.extension.toByteArray
import com.example.tzufserver.extension.toByteList
import com.mapbox.mapboxsdk.geometry.LatLng

class Tile(
    val topLeft: LatLng,
    val topRight: LatLng,
    val bottomLeft: LatLng,
    val bottomRight: LatLng,
    val bitmap: Bitmap?,
    val zoom: ZoomType
) {

    val id: String = "tile_$zoom/${topLeft.latitude}/${topLeft.longitude}"

    override fun equals(other: Any?): Boolean {
        return (other as Tile?)?.let {
            this.topLeft.equals(it.topLeft)
                    && this.topRight.equals(it.topRight)
                    && this.bottomLeft.equals(it.bottomLeft)
                    && this.bottomRight.equals(it.bottomRight)
                    && this.zoom == it.zoom
        } ?: false
    }

    fun sameBitmapAs(other: Tile) =
       ( this.bitmap == null && this.bitmap == null)
               || (this.bitmap != null && other.bitmap != null && this.bitmap.sameAs(other.bitmap))

    fun sameAs(other: Tile) =
        this == other && this.sameBitmapAs(other)

    // TODO: refactor to parse strategy inside blueooth api instead of inside the class, it's not its responsibiliity
    fun toByteList(): List<Byte> {
        val bytes = mutableListOf<Byte>()

        bytes.addAll(zoom.toByteList())
        bytes.addAll(topLeft.latitude.toByteList())
        bytes.addAll(topLeft.longitude.toByteList())
        bytes.addAll(topRight.latitude.toByteList())
        bytes.addAll(topRight.longitude.toByteList())
        bytes.addAll(bottomLeft.latitude.toByteList())
        bytes.addAll(bottomLeft.longitude.toByteList())
        bytes.addAll(bottomRight.latitude.toByteList())
        bytes.addAll(bottomRight.longitude.toByteList())

        bitmap?.toByteArray()?.also { // if not null - add the length and the bitmap
            bytes.addAll(it.size.toByteList())
            bytes.addAll(it.toList())
        } ?: bytes.addAll(0.toByteList()) // if null -  add 0 length as the bitmap

        return bytes
    }

    fun toByteArray(): ByteArray {
        var bytes = ByteArray(0)
            .plus(zoom.toByteArray())
            .plus(topLeft.latitude.toByteArray())
            .plus(topLeft.longitude.toByteArray())
            .plus(topRight.latitude.toByteArray())
            .plus(topRight.longitude.toByteArray())
            .plus(bottomLeft.latitude.toByteArray())
            .plus(bottomLeft.longitude.toByteArray())
            .plus(bottomRight.latitude.toByteArray())
            .plus(bottomRight.longitude.toByteArray())


        bytes = bitmap?.toByteArray()?.let { // if not null - add the length and the bitmap
            bytes
                .plus(it.size.toByteArray())
                .plus(it)
        } ?: bytes.plus(0.toByteArray()) // if null -  add 0 length as the bitmap

        return bytes
    }

}