package com.example.tzufserver.utils.map

import com.mapbox.mapboxsdk.geometry.LatLng
import mil.nga.geopackage.core.srs.SpatialReferenceSystem
import org.locationtech.proj4j.ProjCoordinate
import kotlin.math.max
import kotlin.math.min

class Point {

    var x: Double
    var y: Double

    constructor(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    constructor(point: Point) : this(point.x, point.y)

    fun toLatLng() = LatLng(max(min(y, 90.0), -90.0), max(min(x, 85.0), -85.0))

    fun transform(fromSrs: SpatialReferenceSystem, toSrs: SpatialReferenceSystem): Point {
        val projPoint = MapUtils.transform(ProjCoordinate(x, y), fromSrs, toSrs)
        return Point(projPoint.x, projPoint.y)
    }

    fun shift(dx: Double, dy: Double) = Point(x + dx, y + dy)

    companion object {

        fun fromLatLng(latLng: LatLng) = Point(latLng.longitude, latLng.latitude)

    }



}