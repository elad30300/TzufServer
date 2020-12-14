package com.example.tzufserver.utils.map

import com.mapbox.mapboxsdk.geometry.LatLng

class Boundaries(
    val topLeft: LatLng,
    val topRight: LatLng,
    val bottomLeft: LatLng,
    val bottomRight: LatLng
) {

    val maxLatitude = topLeft.latitude
    val minLatitude = bottomLeft.latitude
    val maxLongitude = topRight.longitude
    val minLongitude = topLeft.longitude


    fun hasPoint(point: LatLng): Boolean {
        return point.latitude in minLatitude..maxLatitude && point.longitude in minLongitude..maxLongitude
    }

    fun toGeneralBoundaries() = GeneralBoundaries(
        Point.fromLatLng(topLeft),
        Point.fromLatLng(topRight),
        Point.fromLatLng(bottomLeft),
        Point.fromLatLng(bottomRight),
        MapUtils.getEPSGWorldGeodeticeReference()
    )

    override fun equals(other: Any?): Boolean {
        return (other as? Boundaries)?.let {
            this.topLeft == other.topLeft && this.topRight == other.topRight && this.bottomLeft == other.bottomLeft && this.bottomRight == other.bottomRight
        } ?: false
    }

}