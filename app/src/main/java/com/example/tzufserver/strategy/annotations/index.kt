package com.example.tzufserver.strategy.annotations

import com.example.tzufserver.data.model.*
import com.example.tzufserver.strategy.annotations.EntityAnnotationStrategy
import com.example.tzufserver.strategy.annotations.GeneralEnemyEntityAnnotationStrategy
import com.mapbox.geojson.Geometry
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.plugins.annotation.Annotation

object AnnotationsStrategiesIndex {

    fun createAnnotationInstance(
        entity: BaseEntity,
        symbolManager: SymbolManager?,
        lineManager: LineManager?,
        fillManager: FillManager?,
        circleManager: CircleManager?
    ): Annotation<out Geometry>? = when (entity) {
        is GeneralForcesEntity -> GeneralForcesEntityAnnotationStrategy().getAnnotationInstance(entity, symbolManager)
        is GeneralEnemyEntity -> GeneralEnemyEntityAnnotationStrategy().getAnnotationInstance(entity, symbolManager)
        is LimitingBoundariesEntity -> LimitingBoundariesEntityAnnotationStrategy().getAnnotationInstance(entity, lineManager)
        is PathEntity -> PathEntityAnnotationStrategy().getAnnotationInstance(entity, lineManager)
        else -> null
    }

}