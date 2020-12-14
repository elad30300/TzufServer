package com.example.tzufserver.strategy.parse.entity

import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.example.tzufserver.data.model.BaseEntity
import com.example.tzufserver.data.model.GeneralEnemyEntity

class EnemyParseStrategy: EntityParseStrategy() {

    override fun parse(bytes: ByteArray): BaseEntity {
        return GeneralEnemyEntity.fromBaseEntity(super.parse(dropTypeFromBytes(bytes)))
    }

}