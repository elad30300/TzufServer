package com.example.tzufserver.strategy.parse.entity

import com.example.tzufserver.data.model.BaseEntity
import com.example.tzufserver.data.model.GeneralForcesEntity

class TroopsParseStrategy: EntityParseStrategy() {

    override fun parse(bytes: ByteArray): BaseEntity {
        return GeneralForcesEntity.fromBaseEntity(super.parse(dropTypeFromBytes(bytes)))
    }

}