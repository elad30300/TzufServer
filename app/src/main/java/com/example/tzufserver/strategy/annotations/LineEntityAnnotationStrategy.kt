package com.example.tzufserver.strategy.annotations

import android.graphics.Color
import com.example.tzufserver.data.model.LineEntity
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.utils.ColorUtils

open class LineEntityAnnotationStrategy<E : LineEntity> : EntityAnnotationStrategy<
        LineString,
        LineLayer,
        Line,
        LineOptions,
        OnLineDragListener,
        OnLineClickListener,
        OnLineLongClickListener,
        E
        > {

    override fun getAnnotationInstance(
        entity: E,
        manager: AnnotationManager<LineLayer, Line, LineOptions, OnLineDragListener, OnLineClickListener, OnLineLongClickListener>?
    ): Line? {
        return (manager as? LineManager)?.let {
            it.create(LineOptions()
                .withLatLngs(entity.positions)
                .withLineWidth(2.0f)
            )
        }
    }

}