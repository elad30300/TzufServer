package com.example.tzufserver.utils.map

import mil.nga.geopackage.core.srs.SpatialReferenceSystem
import org.locationtech.proj4j.ProjCoordinate

class GeneralBoundaries {

    var topLeft: Point
    var topRight: Point
    var bottomLeft: Point
    var bottomRight: Point
    var srs: SpatialReferenceSystem

    constructor(
        topLeft: Point,
        topRight: Point,
        bottomLeft: Point,
        bottomRight: Point,
        srs: SpatialReferenceSystem
    ) {
        this.topLeft = topLeft
        this.topRight = topRight
        this.bottomLeft = bottomLeft
        this.bottomRight = bottomRight
        this.srs = srs
    }

    constructor(boundaries: GeneralBoundaries) : this(
        Point(boundaries.topLeft),
        Point(boundaries.topRight),
        Point(boundaries.bottomLeft),
        Point(boundaries.bottomRight),
        boundaries.srs
    )

    fun transform(toSrs: SpatialReferenceSystem): GeneralBoundaries {
        val projectedTopLeft = MapUtils.transform(ProjCoordinate(topLeft.x, topLeft.y), srs, toSrs)
        val projectedTopRight =
            MapUtils.transform(ProjCoordinate(topRight.x, topRight.y), srs, toSrs)
        val projectedBottomLeft =
            MapUtils.transform(ProjCoordinate(bottomLeft.x, bottomLeft.y), srs, toSrs)
        val projectedBottomRight =
            MapUtils.transform(ProjCoordinate(bottomRight.x, bottomRight.y), srs, toSrs)
        return GeneralBoundaries(
            Point(projectedTopLeft.x, projectedTopLeft.y),
            Point(projectedTopRight.x, projectedTopRight.y),
            Point(projectedBottomLeft.x, projectedBottomLeft.y),
            Point(projectedBottomRight.x, projectedBottomRight.y),
            toSrs
        )
    }

    fun toLatLngBoundaries(): Boundaries {
        val thisEpsgWorldGeodeticBoundaries = if (srs.equals(MapUtils.getEPSGWorldGeodeticeReference())) this else this.transform(MapUtils.getEPSGWorldGeodeticeReference())
        return Boundaries(
            thisEpsgWorldGeodeticBoundaries.topLeft.toLatLng(),
            thisEpsgWorldGeodeticBoundaries.topRight.toLatLng(),
            thisEpsgWorldGeodeticBoundaries.bottomLeft.toLatLng(),
            thisEpsgWorldGeodeticBoundaries.bottomRight.toLatLng()
        )
    }

    // TODO: write test for this function!! important cause its the basics!!
    fun findIntersectedRegion(other: GeneralBoundaries): GeneralBoundaries? {
        val thisBoundaries = if (this.srs.equals(other.srs)) this else this.transform(other.srs)
        var hasIntersection = false
        val intersectionBoundaries = GeneralBoundaries(thisBoundaries)

        if (other.getLeft() <= this.getLeft() && this.getLeft() <= other.getRight()) {
            hasIntersection = true
        } else if (this.getLeft() <= other.getLeft() && other.getLeft() <= this.getRight()) {
            hasIntersection = true
            intersectionBoundaries.topLeft.x = other.getLeft()
            intersectionBoundaries.bottomLeft.x = other.getLeft()
        }
        if (other.getLeft() <= this.getRight() && this.getRight() <= other.getRight()) {
            hasIntersection = true
        } else if (this.getLeft() <= other.getRight() && other.getRight() <= this.getRight()) {
            hasIntersection = true
            intersectionBoundaries.topRight.x = other.getRight()
            intersectionBoundaries.bottomRight.x = other.getRight()
        }
        if (other.getBottom() <= this.getBottom() && this.getBottom() <= other.getTop()) {
            hasIntersection = true
        } else if (this.getBottom() <= other.getBottom() && other.getBottom() <= this.getTop()) {
            hasIntersection = true
            intersectionBoundaries.bottomLeft.y = other.getBottom()
            intersectionBoundaries.bottomRight.y = other.getBottom()
        }
        if (other.getBottom() <= this.getTop() && this.getTop() <= other.getTop()) {
            hasIntersection = true
        } else if (this.getBottom() <= other.getTop() && other.getTop() <= this.getTop()) {
            hasIntersection = true
            intersectionBoundaries.topLeft.y = other.getTop()
            intersectionBoundaries.topRight.y = other.getTop()
        }

        if (!hasIntersection) {
            return null
        }
        return intersectionBoundaries
    }

    fun getLeft() = topLeft.x

    fun getRight() = topRight.x

    fun getTop() = topRight.y

    fun getBottom() = bottomRight.y

    fun getHeight() = getTop() - getBottom()

    fun getWidth() = getRight() - getLeft()

    override fun toString(): String {
        return "GeneralBoundaries(left: ${getLeft()}, right: ${getRight()}, bottom: ${getBottom()}, top: ${getTop()})"
    }

}