package com.example.tzufserver.strategy.parse.entity

import com.example.tzufserver.data.model.BaseEntity
import com.example.tzufserver.data.model.PathEntity

class PathParseStrategy : EntityParseStrategy() {

    override fun parse(bytes: ByteArray): BaseEntity {
        return PathEntity.fromBaseEntity(super.parse(dropTypeFromBytes(bytes)))
    }

}