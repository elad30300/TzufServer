package com.example.tzufserver.geopackage

import android.content.Context
import android.util.Log
import com.example.tzufserver.utils.map.*
import com.mapbox.mapboxsdk.geometry.LatLng
import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.core.contents.Contents
import mil.nga.geopackage.core.contents.ContentsDataType
import mil.nga.geopackage.core.srs.SpatialReferenceSystem
import mil.nga.geopackage.factory.GeoPackageFactory
import mil.nga.sf.proj.ProjectionConstants
import org.locationtech.proj4j.ProjCoordinate
import java.io.File
import java.lang.Exception

class GeopackageManager constructor(
    context: Context,
    filepath: String
) {

    private var geopackage: GeoPackage

    init {
        val manager = GeoPackageFactory.getManager(context)
        manager.deleteAll()
        if (!manager.importGeoPackage(File(filepath))) {
            throw Exception("couldn't import geopackage $filepath into databases")
        }
        geopackage = manager.open(manager.databases()[0])
    }

    fun getEPSGWorldGeodeticeReference(): SpatialReferenceSystem? {
        return geopackage.spatialReferenceSystemDao.find {
            it.srsId == ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM.toLong()
        }
    }

    fun transform(
        coordinates: ProjCoordinate,
        originalSystem: SpatialReferenceSystem,
        destinationSystem: SpatialReferenceSystem
    ): ProjCoordinate {
        return originalSystem.projection
            .getTransformation(destinationSystem.projection)
            .transform(coordinates)
    }

    private fun getMatchingContentsForRegion(boundaries: Boundaries): List<Contents> {
        return geopackage.contentsDao.filter {
            if (it.dataType != ContentsDataType.TILES) {
                false
            }

            val intersection = boundaries.toGeneralBoundaries().findIntersectedRegion(
                GeneralBoundaries(
                    Point(it.minX, it.maxY),
                    Point(it.maxX, it.maxY),
                    Point(it.minX, it.minY),
                    Point(it.maxX, it.minY),
                    it.srs
                )
            )
            intersection != null
        }
    }

    private fun findBestAvailableZoomLevel(tableName: String, zoom: ZoomType): ZoomType {
        return geopackage.getTileDao(tableName).run {
            if (zoom in minZoom..maxZoom) {
                return zoom
            } else if (zoom < minZoom) {
                return minZoom.toInt()
            }
            maxZoom.toInt()
        }
    }

    private fun getTileMatrixRow(tableName: String, zoom: ZoomType) =
        geopackage.tileMatrixDao.find {
            it.tableName == tableName && it.zoomLevel == zoom.toLong()
        }

    private fun getTileMatrixSetRow(tableName: String) = geopackage.tileMatrixSetDao.find {
        it.tableName == tableName
    }

    private fun getTileMatrixDao(tableName: String) = geopackage.getTileDao(tableName)

    private fun findTileIndexesForPoint(
        tableName: String,
        zoom: ZoomType,
        point: Point
    ): Pair<Int, Int> {
        val tileMatrixSet = getTileMatrixSetRow(tableName)
            ?: throw Exception("Not found tile matrix set row for ${tableName}")
        val tileMatrix = getTileMatrixRow(tableName, zoom)
            ?: throw Exception("Not found tile matrix row for ${tableName}")
        val numberOfColumns = tileMatrix.matrixWidth.toInt()
        val numberOfRows = tileMatrix.matrixHeight.toInt()

        val miniTileWidth = (tileMatrixSet.maxX - tileMatrixSet.minX) / numberOfColumns.toDouble()
        val miniTileHeight = (tileMatrixSet.maxY - tileMatrixSet.minY) / numberOfRows.toDouble()

        var (column, row) = Pair(
            ((point.x - tileMatrixSet.minX) / miniTileWidth).toInt(),
            ((tileMatrixSet.maxY - point.y) / miniTileHeight).toInt()
        )

        // check if edges of tiles, for going back!!
        if (point.x > tileMatrixSet.minX && ((point.x - tileMatrixSet.minX) / miniTileWidth) - column.toDouble() == 0.0) {
            column -= 1
        }
        if (point.y < tileMatrixSet.maxY && ((tileMatrixSet.maxY - point.y) / miniTileHeight) - row.toDouble() == 0.0) {
            row -= 1
        }

        return Pair(column, row)
    }

    fun getTilesForRegion(boundaries: Boundaries, zoom: ZoomType): List<Tile> {
        val tiles = mutableListOf<Tile>()
        getMatchingContentsForRegion(boundaries).forEach { contents ->
            val bestZoom = findBestAvailableZoomLevel(contents.tableName, zoom)
            val tileMatrix = getTileMatrixRow(contents.tableName, bestZoom)
                ?: throw Exception("Not found tile matrix set row for ${contents.tableName}")
            val tileMatrixSet = getTileMatrixSetRow(contents.tableName)
                ?: throw Exception("Not found tile matrix set row for ${contents.tableName}")
            val tileMatrixDao = getTileMatrixDao(contents.tableName)

            val tilePyramidCoordinateSrs = tileMatrixSet.srs
            val epsgWorldGeodeticeReference = getEPSGWorldGeodeticeReference()
                ?: throw Exception("Couldn't find epsg geodetic wgs84 coordinate projection")

            val regionBoundaries = boundaries.toGeneralBoundaries()
            val regionTransformedBoundaries = regionBoundaries.transform(tilePyramidCoordinateSrs)
            val tileSetBoundaries = GeneralBoundaries(
                Point(tileMatrixSet.minX, tileMatrixSet.maxY),
                Point(tileMatrixSet.maxX, tileMatrixSet.maxY),
                Point(tileMatrixSet.minX, tileMatrixSet.minY),
                Point(tileMatrixSet.maxX, tileMatrixSet.minY),
                tilePyramidCoordinateSrs
            )

            regionTransformedBoundaries.findIntersectedRegion(tileSetBoundaries)
                ?.also { intersectRegion ->
                    val (topLeftMiniTileX, topLeftMiniTileY) = findTileIndexesForPoint(
                        contents.tableName,
                        bestZoom,
                        intersectRegion.topLeft
                    )
                    val (topRightMiniTileX, topRightMiniTileY) = findTileIndexesForPoint(
                        contents.tableName,
                        bestZoom,
                        intersectRegion.topRight
                    )
                    val (bottomLeftMiniTileX, bottomLeftMiniTileY) = findTileIndexesForPoint(
                        contents.tableName,
                        bestZoom,
                        intersectRegion.bottomLeft
                    )

                    val numberOfColumns = tileMatrix.matrixWidth
                    val numberOfRows = tileMatrix.matrixHeight
                    val miniTileWidth =
                        (tileMatrixSet.maxX - tileMatrixSet.minX) / numberOfColumns.toDouble()
                    val miniTileHeight =
                        (tileMatrixSet.maxY - tileMatrixSet.minY) / numberOfRows.toDouble()

                    for (tileX in topLeftMiniTileX..topRightMiniTileX) {
                        for (tileY in topLeftMiniTileY..bottomLeftMiniTileY) {
                            tileMatrixDao.queryForTile(
                                tileX.toLong(),
                                tileY.toLong(),
                                bestZoom.toLong()
                            )?.also { tileRow ->
                                val baseTileX = tileX * miniTileWidth + tileMatrixSet.minX
                                val baseTileY =
                                    (numberOfRows - tileY - 1) * miniTileHeight + tileMatrixSet.minY

                                val tileBottomLeftLatLng = transform(
                                    ProjCoordinate(baseTileX, baseTileY),
                                    tilePyramidCoordinateSrs,
                                    epsgWorldGeodeticeReference
                                ).run { LatLng(y, x) }
                                val tileBottomRightLatLng = transform(
                                    ProjCoordinate(baseTileX + miniTileWidth, baseTileY),
                                    tilePyramidCoordinateSrs,
                                    epsgWorldGeodeticeReference
                                ).run { LatLng(y, x) }
                                val tileTopLeftLatLng = transform(
                                    ProjCoordinate(baseTileX, baseTileY + miniTileHeight),
                                    tilePyramidCoordinateSrs,
                                    epsgWorldGeodeticeReference
                                ).run { LatLng(y, x) }
                                val tileTopRightLatLng = transform(
                                    ProjCoordinate(
                                        baseTileX + miniTileWidth,
                                        baseTileY + miniTileHeight
                                    ),
                                    tilePyramidCoordinateSrs,
                                    epsgWorldGeodeticeReference
                                ).run { LatLng(y, x) }
                                tiles.add(
                                    Tile(
                                        tileTopLeftLatLng,
                                        tileTopRightLatLng,
                                        tileBottomLeftLatLng,
                                        tileBottomRightLatLng,
                                        tileRow.tileDataBitmap,
                                        tileRow.zoomLevel.toInt()
                                    )
                                )
                            } ?: Log.e(
                                TAG,
                                "failed to get tile row, col = $tileX, row = $tileY, tableName = ${contents.tableName}, zoom = $bestZoom"
                            )
                        }
                    }
                }

        }
        return tiles
    }

    companion object {
        private const val TAG = "GpkgManager"
    }

}