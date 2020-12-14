package com.example.tzufserver.provider

import androidx.lifecycle.MutableLiveData
import com.example.tzufserver.utils.map.Boundaries
import com.example.tzufserver.utils.map.Tile
import com.example.tzufserver.utils.map.ZoomType


interface RasterTilesProvider {

    val rasterTilesProviderAvailable: MutableLiveData<Boolean>
    var rasterTilesDelegate: Delegate?

    fun getTilesForRegion(boundaries: Boundaries, zoom: ZoomType)

    fun onRasterTilesFetched(tiles: List<Tile>) {
        rasterTilesDelegate?.onRasterTilesFetched(tiles)
    }

    interface Delegate {

        fun onRasterTilesFetched(tiles: List<Tile>)

    }

}