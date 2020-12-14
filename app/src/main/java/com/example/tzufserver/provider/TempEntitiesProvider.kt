package com.example.tzufserver.provider

import androidx.lifecycle.MutableLiveData
import com.example.tzufserver.data.model.*
import com.example.tzufserver.utils.map.Boundaries
import com.mapbox.mapboxsdk.geometry.LatLng
import javax.inject.Inject

class TempEntitiesProvider @Inject constructor() : EntitiesProvider {

    val entities = mutableListOf(
        GeneralEnemyEntity("1", "Viroflay", LatLng(48.8, 2.1667)),
        GeneralForcesEntity("2", "Chaville", LatLng(48.808026, 2.192418)),
        LimitingBoundariesEntity(
            "3",
            "gg",
            LatLng(48.8, 2.1667),
            LatLng(48.808026, 2.192418)
        ),
        PathEntity(
            "4", "path", listOf(
                LatLng(48.803, 2.157),
                LatLng(48.818026, 2.182418),
                LatLng(48.807026, 2.162418)
            )
        )
    )

    override val entitiesProviderAvailable: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>().also {
            it.value = true
        }
    }

    override var entitiesProviderDelegate: EntitiesProvider.Delegate? = null

    override fun getEntitiesForRegion(boundaries: Boundaries) {
        val relevantEntities = entities.filter {entity ->
            entity.positions.any {
                boundaries.hasPoint(it)
            }
        }
        entitiesProviderDelegate?.onEntitiesFetched(relevantEntities)
    }

}