package com.example.tzufserver.provider

import androidx.lifecycle.MutableLiveData
import com.example.tzufserver.data.model.BaseEntity
import com.example.tzufserver.utils.map.Boundaries

interface EntitiesProvider {

    val entitiesProviderAvailable: MutableLiveData<Boolean>
    var entitiesProviderDelegate: Delegate?

    fun onEntitiesFetched(entities: List<BaseEntity>) {
        entitiesProviderDelegate?.onEntitiesFetched(entities)
    }

    fun getEntitiesForRegion(boundaries: Boundaries)

    interface Delegate {

        fun onEntitiesFetched(entities: List<BaseEntity>)

    }
}