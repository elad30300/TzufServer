package com.example.tzufserver.strategy.annotations

import android.graphics.Color
import com.example.tzufserver.data.model.LimitingBoundariesEntity
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.style.layers.LineLayer

class LimitingBoundariesEntityAnnotationStrategy :
    LineEntityAnnotationStrategy<LimitingBoundariesEntity>() {

    override fun getAnnotationInstance(
        entity: LimitingBoundariesEntity,
        manager: AnnotationManager<LineLayer, Line, LineOptions, OnLineDragListener, OnLineClickListener, OnLineLongClickListener>?
    ): Line? {
        return super.getAnnotationInstance(entity, manager)?.apply {
            setLineColor(Color.BLACK)
            manager?.update(this)
        }
    }

}