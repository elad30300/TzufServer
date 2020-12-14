package com.example.tzufserver.utils.map

import mil.nga.geopackage.core.srs.SpatialReferenceSystem
import org.locationtech.proj4j.ProjCoordinate

typealias ZoomType = Int

object MapUtils {

    fun transform(
        coordinates: ProjCoordinate,
        originalSystem: SpatialReferenceSystem,
        destinationSystem: SpatialReferenceSystem
    ): ProjCoordinate {
        return originalSystem.projection
            .getTransformation(destinationSystem.projection)
            .transform(coordinates)
    }

    fun getEPSGWorldGeodeticeReference(): SpatialReferenceSystem = SpatialReferenceSystem().apply {
        srsName = "WGS 84 geodetic"
        srsId = 4326
        organization = "EPSG"
        organizationCoordsysId = 4326
        definition =
            "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326”]]"
        description = "longitude/latitude coordinates in decimal degrees on the WGS 84 spheroid"
        definition_12_063 = null
    }

    fun getEPSGSphericalMercatorProjection(): SpatialReferenceSystem = SpatialReferenceSystem().apply {
        srsName = "WGS 84 / Pseudo-Mercator"
        srsId = 3875
        organization = "EPSG"
        organizationCoordsysId = 3875
        definition =
            "PROJCS[\"WGS 84 / Pseudo-Mercator\",GEOGCS[\"Popular Visualisation CRS\",DATUM[\"Popular_Visualisation_Datum\",SPHEROID[\"Popular Visualisation Sphere\",6378137,0,AUTHORITY[\"EPSG\",\"7059\"]],TOWGS84[0,0,0,0,0,0,0],AUTHORITY[\"EPSG\",\"6055\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4055\"]],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],PROJECTION[\"Mercator_1SP\"],PARAMETER[\"central_meridian\",0],PARAMETER[\"scale_factor\",1],PARAMETER[\"false_easting\",0],PARAMETER[\"false_northing\",0],AUTHORITY[\"EPSG\",\"3785\"],AXIS[\"X\",EAST],AXIS[\"Y\",NORTH]]"
        description = "Spherical Mercator projection coordinate system"
        definition_12_063 = null
    }

}