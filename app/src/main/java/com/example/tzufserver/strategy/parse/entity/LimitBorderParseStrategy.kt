package com.example.tzufserver.strategy.parse.entity

import com.example.tzufserver.data.model.BaseEntity
import com.example.tzufserver.data.model.LimitingBoundariesEntity

class LimitBorderParseStrategy: EntityParseStrategy() {

    override fun parse(bytes: ByteArray): BaseEntity {
        return LimitingBoundariesEntity.fromBaseEntity(super.parse(dropTypeFromBytes(bytes)))
    }

}