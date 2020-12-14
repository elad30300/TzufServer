package com.example.tzufserver.strategy.annotations

import com.example.tzufserver.data.model.BaseEntity
import com.mapbox.geojson.Geometry
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.plugins.annotation.Annotation
import com.mapbox.mapboxsdk.style.layers.Layer

interface EntityAnnotationStrategy<
        G : Geometry,
        Y : Layer,
        T : Annotation<G>,
        O : Options<T>,
        D : OnAnnotationDragListener<T>,
        C : OnAnnotationClickListener<T>,
        L : OnAnnotationLongClickListener<T>,
        E : BaseEntity> {

    fun getAnnotationInstance(
        entity: E,
        manager: AnnotationManager<Y, T, O, D, C, L>?
    ): T?

//    fun updateAnnotationInstance(
//        oldEntity: E,
//        newEntity: E,
//        annotation: T,
//        manager: AnnotationManager<Y, T, O, D, C, L>?
//    )

}