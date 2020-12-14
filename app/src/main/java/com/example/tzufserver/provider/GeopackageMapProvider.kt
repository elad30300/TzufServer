package com.example.tzufserver.provider

import android.content.Context
import android.os.Environment
import androidx.lifecycle.MutableLiveData
import com.example.tzufserver.ApplicationSettings
import com.example.tzufserver.geopackage.GeopackageManager
import com.example.tzufserver.utils.map.Boundaries
import com.example.tzufserver.utils.map.ZoomType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeopackageMapProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    applicationSettings: ApplicationSettings
) :
    RasterTilesProvider {

    override val rasterTilesProviderAvailable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>(false)
    }
    override var rasterTilesDelegate: RasterTilesProvider.Delegate? = null
    private var geopackageManager: GeopackageManager? = null

    init {
        if (applicationSettings.gattType == applicationSettings.GATT_TYPE_SERVER) {
            applicationSettings.locationPermissionsGranted.observeForever {
                onStoragePermissionsChanged(it)
            }
        }
    }

    private fun onStoragePermissionsChanged(hasPermissions: Boolean) {
        if (hasPermissions) {
            onStoragePermissionsGranted()
        } else {
            onStoragePermissionsDenied()
        }
    }

    private fun onStoragePermissionsGranted() {
        geopackageManager = GeopackageManager(context, GEOPACKAGE_FILEPATH)
        rasterTilesProviderAvailable.postValue(true)
    }

    private fun onStoragePermissionsDenied() {
        geopackageManager = null
        rasterTilesProviderAvailable.postValue(false)
    }

    override fun getTilesForRegion(boundaries: Boundaries, zoom: ZoomType) {
        geopackageManager?.getTilesForRegion(boundaries, zoom)?.also { tiles ->
            onRasterTilesFetched(tiles)
        }
    }

    companion object {
        //        private val GEOPACKAGE_FILEPATH = "${Environment.getExternalStorageDirectory().path}/MapTest/GeopackageSample/example.gpkg"
        private val GEOPACKAGE_FILEPATH =
            "${Environment.getExternalStorageDirectory().path}/MapTest/GeopackageSample/metromap.gpkg"
    }

}