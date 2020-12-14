package com.example.tzufserver.strategy.annotations

import android.graphics.Color
import com.example.tzufserver.R
import com.example.tzufserver.data.model.GeneralEnemyEntity
import com.example.tzufserver.utils.ui.MapConstants
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.SymbolLayer

class GeneralEnemyEntityAnnotationStrategy : PointEntityAnnotationStrategy<GeneralEnemyEntity>() {

    override fun getAnnotationInstance(
        entity: GeneralEnemyEntity,
        manager: AnnotationManager<SymbolLayer, Symbol, SymbolOptions, OnSymbolDragListener, OnSymbolClickListener, OnSymbolLongClickListener>?
    ): Symbol? {
        return super.getAnnotationInstance(entity, manager)?.apply {
            iconImage = MapConstants.GENERAL_ENEMY_ANNOTATION_ICON_ID
            setTextColor(Color.RED)
            manager?.update(this)
        }
    }

//    override fun getAnnotationInstance(
//        entity: GeneralEnemyEntity,
//        manager: AnnotationManager<Layer, Symbol, Options<Symbol>, OnAnnotationDragListener<Symbol>, OnAnnotationClickListener<Symbol>, OnAnnotationLongClickListener<Symbol>>
//    ): Symbol {
//        return super.getAnnotationInstance(entity, manager).apply {
//            iconImage = R.drawable.ic_general_enemy_entity_annotation.toString()
//        }
//    }


}