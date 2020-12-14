package com.example.tzufserver

//import android.R
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.tzufserver.bluetooth.server.TzufGattServer
import com.example.tzufserver.data.model.BaseEntity
import com.example.tzufserver.strategy.annotations.AnnotationsStrategiesIndex
import com.example.tzufserver.utils.map.Boundaries
import com.example.tzufserver.utils.map.GeneralBoundaries
import com.example.tzufserver.utils.map.MapUtils
import com.example.tzufserver.utils.map.Tile
import com.example.tzufserver.utils.ui.MapConstants
import com.example.tzufserver.viewmodel.MapViewModel
import com.mapbox.geojson.Geometry
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngQuad
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.plugins.annotation.Annotation
import com.mapbox.mapboxsdk.style.layers.RasterLayer
import com.mapbox.mapboxsdk.style.sources.ImageSource
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


/**
 * Use an ImageSource to add an image to the map.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mapView: MapView? = null

    @Inject
    lateinit var viewModel: MapViewModel

    @Inject
    lateinit var applicationSettings: ApplicationSettings

    private lateinit var style: Style
    private lateinit var mapboxMap: MapboxMap
    private lateinit var symbolManager: SymbolManager
    private lateinit var lineManager: LineManager
    private lateinit var navigationMapRoute: NavigationMapRoute

    private val displayedEntities = mutableMapOf<BaseEntity, Annotation<out Geometry>>()

    //    private val displayedLineEntities = mutableMapOf<BaseEntity, Line>()
//    private val displayedEntities = mutableMapOf<BaseEntity, out Annotation>()
//    private val displayedEntities = mutableMapOf<BaseEntity, out Annotation>()
    private val displayedTiles = mutableListOf<Tile>()

    @Inject
    lateinit var gattServer: TzufGattServer

    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private fun initializeDependencies() {
//        viewModel =
//            ViewModelProvider(this, MapViewModel.Factory(this, this)).get(MapViewModel::class.java)
        viewModel.entities.observe(this, Observer { entities ->
            onEntitiesUpdated(entities)
        })
        viewModel.getEntitiesProviderAvailable()
            .observe(this, Observer { available ->
                Log.i(TAG, "entities provider state changed to $available")
                if (available) {
                    onEntitiesProviderAvailable()
                } else {
                    onEntitiesProviderUnavailable()
                }
            })
        viewModel.rasterTiles.observe(this, Observer { tiles ->
            onRasterTilesUpdated(tiles)
        })
        viewModel.getRasterTilesProviderAvailable().observe(this, Observer { available ->
            Log.i(TAG, "raster tiles provider state changed to $available")
            if (available) {
                onRasterTilesProviderAvailable()
            } else {
                onRasterTilesProviderUnavailable()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializePermissions()

        initializeDependencies()

        // bluetooth
        checkIfBluetoothOn()

        // for server only!
//        if (applicationSettings.gattType == applicationSettings.GATT_TYPE_SERVER) {
//            makeBluetoothDiscoverable()
//        }

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
        mapView!!.onCreate(savedInstanceState)
    }

    private fun makeBluetoothDiscoverable() {
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            // TODO: try to make it always discoverable without value = 0 as it is discouraged according to https://developer.android.com/guide/topics/connectivity/bluetooth#FindDevices
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600)
        }
        startActivity(discoverableIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSIONS_REQUEST_ID) {
            onStoragePermissionsResult(permissions, grantResults)
        }
    }

    private fun initializePermissions() {
        requestPermissions(
            PERMISSIONS_STORAGE,
            STORAGE_PERMISSIONS_REQUEST_ID
        )
    }

    private fun onStoragePermissionsResult(permissions: Array<out String>, grantResults: IntArray) {
        val approvedResults = grantResults.filter { it == PackageManager.PERMISSION_GRANTED }
        if (approvedResults.size == PERMISSIONS_STORAGE.size) {
            initializeStoragePermissionsDependentDependencies()
        } else {
            initializePermissions()
        }
    }

    private fun initializeStoragePermissionsDependentDependencies() {
        applicationSettings.locationPermissionsGranted.value = true
        viewModel.initializeStoragePermissionsDependentDependencies()
        mapView!!.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
            Style.Builder().fromUri("asset://local_style_file.json")
        ) { style -> // Set the latitude and longitude values for the image's four corners
            onStyleLoaded(mapboxMap, style)
        }
    }

    private fun getBitmapFromResource(resId: Int): Bitmap? =
        BitmapFactory.decodeResource(resources, resId)

    private fun addImageFromResourceToStyle(style: Style, imageStyleId: String, resId: Int) {
        getBitmapFromResource(resId)?.also {
            style.addImage(imageStyleId, it)
        }
    }

    private fun initializeStyleDependentDependencies(mapboxMap: MapboxMap, style: Style) {
        this.style = style.also {
            addImageFromResourceToStyle(
                it,
                MapConstants.GENERAL_ENEMY_ANNOTATION_ICON_ID,
                R.drawable.ic_general_enemy_annotation_round
            )
            addImageFromResourceToStyle(
                it,
                MapConstants.GENERAL_FORCES_ANNOTATION_ICON_ID,
                R.drawable.blue_circle
            )
        }
        symbolManager = SymbolManager(mapView!!, mapboxMap, style)
        lineManager = LineManager(mapView!!, mapboxMap, style)
        navigationMapRoute = NavigationMapRoute(mapView!!, mapboxMap).also {
        }
    }

    private fun onStyleLoaded(mapboxMap: MapboxMap, style: Style) {
        initializeStyleDependentDependencies(mapboxMap, style)

        val zoom = viewModel.currentZoom
        val baseLatLng = viewModel.currentTarget


        val initialCameraPosition = mapboxMap.cameraPosition.run {
            CameraPosition.Builder(this)
                .target(baseLatLng)
                .zoom(zoom)
                .build()
        }
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(initialCameraPosition))

        mapboxMap.addOnCameraIdleListener {
            onCameraFinishToMove()
        }

        mapboxMap.addOnMapLongClickListener {
            onMapLongClick(it)
        }
    }

    private fun onMapLongClick(position: LatLng): Boolean {
        // todo: implement it i want saving entities in activity, probavly will be only relevant to server side in poc
        return true
    }

    private fun extendRegion(
        boundaries: Boundaries,
        northSouthExtensionFactor: Double,
        westEastExtensionFactor: Double
    ): Boundaries {
        val epsgMercatorProjectedBoundaries = boundaries.toGeneralBoundaries()
            .transform(MapUtils.getEPSGSphericalMercatorProjection())
        val dx = westEastExtensionFactor * epsgMercatorProjectedBoundaries.getWidth()
        val dy = northSouthExtensionFactor * epsgMercatorProjectedBoundaries.getHeight()
        return GeneralBoundaries(
            epsgMercatorProjectedBoundaries.topLeft.shift(
                -dx,
                dy
            ),
            epsgMercatorProjectedBoundaries.topRight.shift(
                dx,
                dy
            ),
            epsgMercatorProjectedBoundaries.bottomLeft.shift(
                -dx,
                -dy
            ),
            epsgMercatorProjectedBoundaries.bottomRight.shift(
                dx,
                -dy
            ),
            epsgMercatorProjectedBoundaries.srs
        ).toLatLngBoundaries()
    }

    private fun getAreaOfInterestRegion(): Boundaries {
        val visibleRegion = mapboxMap.projection.visibleRegion

        return extendRegion(
            Boundaries(
                visibleRegion.latLngBounds.northWest,
                visibleRegion.latLngBounds.northEast,
                visibleRegion.latLngBounds.southWest,
                visibleRegion.latLngBounds.southEast
            ),
            NORTH_SOUTH_REGION_EXTENSION_FACTOR,
            WEST_EAST_REGION_EXTENSION_FACTOR
        )
    }

    private fun onCameraFinishToMove() {
        mapboxMap.cameraPosition.apply {
            viewModel.currentZoom = zoom
            viewModel.currentTarget = target

            val regionToDisplay = getAreaOfInterestRegion()

            Log.d(TAG, "current zoom: $zoom")

            if (viewModel.getRasterTilesProviderAvailable().value!!) {
                viewModel.fetchTilesForRegion(regionToDisplay, zoom.toInt())
            }

            if (viewModel.getEntitiesProviderAvailable().value!!) {
                viewModel.fetchEntitiesForRegion(regionToDisplay)
            }
        }
    }

    fun removeTile(tile: Tile) {
        removeTileFromMap(tile)
        displayedTiles.remove(tile)
    }

    private fun removeTileFromMap(tile: Tile) {
        if (isMapHasTile(tile)) {
            if (!style.removeLayer(tile.id)) Log.e(
                TAG,
                "image layer ${tile.id} not removed properly"
            )
            if (!style.removeSource(tile.id)) Log.e(
                TAG,
                "image source ${tile.id} not removed properly"
            )
        }
    }

    private fun isMapHasTile(tile: Tile): Boolean = style.layers.find { it.id == tile.id } != null

    private fun putTile(tile: Tile) {
        try {
            val quad = LatLngQuad(
                tile.topLeft,
                tile.topRight,
                tile.bottomRight,
                tile.bottomLeft
            )
            val idImageSource = tile.id
            val idImageLayer = idImageSource

            style.addSource(
                ImageSource(
                    idImageSource,
                    quad,
                    tile.bitmap!!
                )
            )

            // Create a raster layer and use the imageSource's ID as the layer's data. Then add a RasterLayer to the map.
            style.addLayerBelow(
                RasterLayer(
                    idImageLayer,
                    idImageSource
                ),
                "com.mapbox.annotations.points"
            )

            displayedTiles.add(tile)
        } catch (exception: Exception) {
            onFailure(exception)
        }
    }

    private fun handleAnnotationForEntity(entity: BaseEntity): Annotation<out Geometry>? =
        AnnotationsStrategiesIndex.createAnnotationInstance(
            entity,
            symbolManager,
            lineManager,
            null,
            null
        )

    private fun addEntity(entity: BaseEntity) {
        handleAnnotationForEntity(entity)?.also { annotation ->
            displayedEntities[entity] = annotation
        }
    }

    fun removeEntity(entity: BaseEntity) {
//        TODO("Not yet implemented")
    }

    private fun onFailure(exception: Exception? = null) {
        Log.e("Exception", "exception $exception")
        exception?.printStackTrace()
        Toast.makeText(
            this,
            "An error occurred, see logs for further information: ${exception?.message}",
            Toast.LENGTH_LONG
        ).show()
    }

    /* check if BT is on/off
     * on - do nothing
      off - popup to open BT*/
    private fun checkIfBluetoothOn() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Phone doesn't support BT
            Toast.makeText(this, "BT not supported exit app", Toast.LENGTH_SHORT).show()
            this.finishAndRemoveTask()
        } else if (!bluetoothAdapter.isEnabled) {
            // BT is disabled and need to be open
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            onBluetoothStateChanged(BluetoothAdapter.STATE_ON)
        }
    }

    private fun onEntitiesUpdated(entities: List<BaseEntity>) {
        runOnUiThread {
            displayedEntities.forEach { (entity, annotation) ->
                if (entities.find { it == entity } != null) {
                    displayedEntities.remove(entity)
                }
            }

            entities.forEach {
                val displayedEntity = displayedEntities.keys.find { displayed -> displayed == it }
                if (displayedEntity != null) {
                    val annotation = displayedEntities[displayedEntity]
//                updateIfNeccsery
                } else {
                    addEntity(it)
                }
            }
        }
    }

    private fun onRasterTilesUpdated(tiles: List<Tile>) {
        runOnUiThread {
            Log.d(TAG, "onRasterTilesUpdated in main activity")
            displayedTiles.filter { tile ->  tiles.find { it == tile } == null }.forEach {
                removeTile(it)
            }

            tiles.filter { tile -> displayedTiles.find { it == tile } == null }.forEach {
                putTile(it)
            }
        }
    }

    private fun onEntitiesProviderAvailable() {
        Log.i(TAG, "entities provider is available")
        viewModel.fetchEntitiesForRegion(getAreaOfInterestRegion())
    }

    private fun onEntitiesProviderUnavailable() {
        Log.i(TAG, "entities provider is un-available")
    }

    private fun onRasterTilesProviderAvailable() {
        Log.i(TAG, "raster tiles provider is available")
        viewModel.fetchTilesForRegion(getAreaOfInterestRegion(), viewModel.currentZoom.toInt())
    }

    private fun onRasterTilesProviderUnavailable() {
        Log.i(TAG, "raster tiles provider is un-available")
    }

    private fun listenToBluetoothStateChanges() {
        try {
            stopListenToBluetoothStateChanges()
            registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        } catch (ex: Exception) {
            Log.d(TAG, "listenToBluetoothStateChanges called unregisterReceiver to non-exist bluetoothReceiver, ex $ex")
        }
    }

    private fun stopListenToBluetoothStateChanges() {
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (ex: Exception) {
            Log.d(TAG, "stopListenToBluetoothStateChanges called unregisterReceiver to non-exist bluetoothReceiver, ex $ex")
        }
    }

    private fun onBluetoothStateChanged(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_OFF -> applicationSettings.bluetoothEnabled.value = false
            BluetoothAdapter.STATE_ON -> applicationSettings.bluetoothEnabled.value = true
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                onBluetoothStateChanged(
                    state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        -1
                    )
                )
            }
        }
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    public override fun onResume() {
        super.onResume()
        listenToBluetoothStateChanges()
        mapView!!.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    public override fun onPause() {
        super.onPause()
        stopListenToBluetoothStateChanges()
        mapView!!.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val STORAGE_PERMISSIONS_REQUEST_ID = 1234
        private const val REQUEST_ENABLE_BT = 1235
        private const val NORTH_SOUTH_REGION_EXTENSION_FACTOR = 1.0
        private const val WEST_EAST_REGION_EXTENSION_FACTOR = 1.0
    }
}
