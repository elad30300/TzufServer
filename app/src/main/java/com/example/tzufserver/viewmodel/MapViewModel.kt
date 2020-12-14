package com.example.tzufserver.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.tzufserver.data.model.BaseEntity
import com.example.tzufserver.repository.EntitiesRepository
import com.example.tzufserver.repository.RasterTilesRepository
import com.example.tzufserver.utils.map.Boundaries
import com.example.tzufserver.utils.map.Tile
import com.example.tzufserver.utils.map.ZoomType
import com.mapbox.mapboxsdk.geometry.LatLng
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class MapViewModel @Inject constructor(
    private val rasterTilesRepository: RasterTilesRepository,
    private val entitiesRepository: EntitiesRepository
) : ViewModel() {

    val entities: LiveData<List<BaseEntity>> = entitiesRepository.getEntities()
    val rasterTiles: LiveData<List<Tile>> = rasterTilesRepository.getTiles()

    var currentZoom: Double = 11.0
    var currentTarget: LatLng = LatLng(48.8, 2.1667) // paris

//    var delegate: Delegate? = null

    fun initializeStoragePermissionsDependentDependencies() {
//        mapProvider = BleGeoProvider(context, BleManager(context))
    }

    fun fetchTilesForRegion(boundaries: Boundaries, zoom: ZoomType) {
        getRasterTilesProviderAvailable().value?.also {
            if (it) {
                rasterTilesRepository.getTilesForRegion(boundaries, zoom)
            }
        } ?: Log.e(TAG, "fetchTilesForRegion - tiles provider availability value is null")
    }

    fun fetchEntitiesForRegion(boundaries: Boundaries) {
        getEntitiesProviderAvailable().value?.also {
            if (it) {
                entitiesRepository.getEntitiesForRegion(boundaries)
            }
        } ?: Log.e(TAG, "fetchEntitiesForRegion - entities provider availability value is null")
    }

    fun getRasterTilesProviderAvailable() = rasterTilesRepository.getRasterTilesProviderAvailable()

    fun getEntitiesProviderAvailable() = entitiesRepository.getEntitiesProviderAvailable()

//    interface Delegate {
//
//        fun putTile(tile: Tile)
//
//        fun removeTile(tile: Tile)
//
//        fun addEntity(entity: BaseEntity)
//
//        fun removeEntity(entity: BaseEntity)
//
//    }

    companion object {
        private const val TAG = "MapViewModel"
    }
}