package com.example.tzufserver.strategy.annotations

import com.example.tzufserver.data.model.PathEntity
import com.example.tzufserver.utils.ui.MapConstants
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property

class PathEntityAnnotationStrategy : EntityAnnotationStrategy<
        LineString,
        LineLayer,
        Line,
        LineOptions,
        OnLineDragListener,
        OnLineClickListener,
        OnLineLongClickListener,
        PathEntity> {

    override fun getAnnotationInstance(
        entity: PathEntity,
        manager: AnnotationManager<LineLayer, Line, LineOptions, OnLineDragListener, OnLineClickListener, OnLineLongClickListener>?
    ): Line? {
        return (manager as? LineManager)?.let {
            it.create(LineOptions()
                .withLatLngs(entity.positions)
                .withLineWidth(2.0f)
                .withLineColor("green")
                .withLineJoin(Property.LINE_JOIN_ROUND)
            )
        }
    }

}