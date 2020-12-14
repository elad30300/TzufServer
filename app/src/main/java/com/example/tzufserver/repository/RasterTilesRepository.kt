package com.example.tzufserver.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tzufserver.data.model.BaseEntity
import com.example.tzufserver.di.module.RasterTilesBleGeoProvider
import com.example.tzufserver.provider.RasterTilesProvider
import com.example.tzufserver.utils.map.Boundaries
import com.example.tzufserver.utils.map.Tile
import com.example.tzufserver.utils.map.ZoomType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RasterTilesRepository @Inject constructor(@RasterTilesBleGeoProvider private val rasterTilesProvider: RasterTilesProvider) :
    RasterTilesProvider.Delegate {

    private val tiles: MutableLiveData<List<Tile>> by lazy {
        MutableLiveData<List<Tile>>().also {
            it.value = listOf()
        }
    }

    init {
        rasterTilesProvider.rasterTilesDelegate = this
    }

    fun getTilesForRegion(boundaries: Boundaries, zoom: ZoomType) {
        rasterTilesProvider.getTilesForRegion(boundaries, zoom)
    }

    override fun onRasterTilesFetched(tiles: List<Tile>) {
        this.tiles.postValue(tiles)
    }

    fun getRasterTilesProviderAvailable(): LiveData<Boolean> =
        rasterTilesProvider.rasterTilesProviderAvailable

    fun getTiles(): LiveData<List<Tile>> = tiles

}