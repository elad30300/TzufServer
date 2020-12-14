package com.example.tzufserver.strategy.annotations

import android.graphics.Color
import com.example.tzufserver.R
import com.example.tzufserver.data.model.GeneralForcesEntity
import com.example.tzufserver.utils.ui.MapConstants
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.SymbolLayer

class GeneralForcesEntityAnnotationStrategy : PointEntityAnnotationStrategy<GeneralForcesEntity>() {

    override fun getAnnotationInstance(
        entity: GeneralForcesEntity,
        manager: AnnotationManager<SymbolLayer, Symbol, SymbolOptions, OnSymbolDragListener, OnSymbolClickListener, OnSymbolLongClickListener>?
    ): Symbol? {
        return super.getAnnotationInstance(entity, manager)?.apply {
            iconImage = MapConstants.GENERAL_FORCES_ANNOTATION_ICON_ID
            setTextColor(Color.BLUE)
            manager?.update(this)
        }
    }

}