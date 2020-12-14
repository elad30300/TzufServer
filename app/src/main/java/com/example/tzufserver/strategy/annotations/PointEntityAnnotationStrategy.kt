package com.example.tzufserver.strategy.annotations

import com.example.tzufserver.data.model.PointEntity
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.SymbolLayer

open class PointEntityAnnotationStrategy<E : PointEntity> : EntityAnnotationStrategy<
        Point,
        SymbolLayer,
        Symbol,
        SymbolOptions,
        OnSymbolDragListener,
        OnSymbolClickListener,
        OnSymbolLongClickListener,
        E> {

    override fun getAnnotationInstance(
        entity: E,
        manager: AnnotationManager<SymbolLayer, Symbol, SymbolOptions, OnSymbolDragListener, OnSymbolClickListener, OnSymbolLongClickListener>?
    ): Symbol? {
        return (manager as? SymbolManager)?.let {
            it.iconAllowOverlap = true
            it.iconIgnorePlacement = true
            return it.create(
                SymbolOptions()
                    .withLatLng(entity.getPosition())
                    .withTextField(entity.name)
                    .withIconSize(0.1F)
                    .withTextAnchor(Property.TEXT_ANCHOR_TOP)
                    .withTextOffset(arrayOf(0.0f, 0.5f))
            )
        }
    }

//    override fun updateAnnotationInstance(
//        oldEntity: E,
//        newEntity: E,
//        annotation: Symbol,
//        manager: AnnotationManager<SymbolLayer, Symbol, SymbolOptions, OnSymbolDragListener, OnSymbolClickListener, OnSymbolLongClickListener>?
//    ) {
//        // TODO: implement
//    }

}