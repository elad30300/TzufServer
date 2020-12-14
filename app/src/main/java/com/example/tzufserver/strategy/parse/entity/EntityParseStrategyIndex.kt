package com.example.tzufserver.strategy.parse.entity

import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi

object EntityParseStrategyIndex {

    fun getParser(type: Int): EntityParseStrategy? {
        return when(type) {
            TzufMapServerApi.ENEMY_TYPE_VALUE -> EnemyParseStrategy()
            TzufMapServerApi.TROOPS_TYPE_VALUE -> TroopsParseStrategy()
            TzufMapServerApi.LIMIT_BORDERS_TYPE_VALUE -> LimitBorderParseStrategy()
            TzufMapServerApi.PATH_TYPE_VALUE -> PathParseStrategy()
            else -> null
        }
    }

}