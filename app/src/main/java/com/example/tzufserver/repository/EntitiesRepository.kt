package com.example.tzufserver.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.tzufserver.data.model.*
import com.example.tzufserver.di.module.EntitiesBleGeoProvider
import com.example.tzufserver.provider.EntitiesProvider
import com.example.tzufserver.utils.map.Boundaries
import com.mapbox.mapboxsdk.geometry.LatLng
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitiesRepository @Inject constructor(@EntitiesBleGeoProvider private val entitiesProvider: EntitiesProvider) :
    EntitiesProvider.Delegate {

    private val entities: MutableLiveData<List<BaseEntity>> by lazy {
        MutableLiveData<List<BaseEntity>>().also {
            it.value = listOf()
        }
    }


    init {
        entitiesProvider.entitiesProviderDelegate = this
    }


    fun getEntitiesForRegion(boundaries: Boundaries) {
        entitiesProvider.getEntitiesForRegion(boundaries)
    }

    override fun onEntitiesFetched(entities: List<BaseEntity>) {
        return this.entities.postValue(entities)
    }

    fun getEntities(): LiveData<List<BaseEntity>> = this.entities

    fun getEntitiesProviderAvailable(): LiveData<Boolean> =
        entitiesProvider.entitiesProviderAvailable

}