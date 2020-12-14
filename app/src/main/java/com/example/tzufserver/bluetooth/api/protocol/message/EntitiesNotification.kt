package com.example.tzufserver.bluetooth.api.protocol.message

import com.example.tzufserver.bluetooth.api.map.TzufMapServerApi
import com.example.tzufserver.data.model.BaseEntity
import com.example.tzufserver.extension.dropUntil
import com.example.tzufserver.provider.dispatchers.DefaultDispatcherProvider
import com.example.tzufserver.provider.dispatchers.DispatcherProvider
import com.example.tzufserver.strategy.parse.entity.EntityParseStrategyIndex
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class EntitiesNotification : GeoMessage {

    private var entities = mutableListOf<BaseEntity>()

    constructor(entities: List<BaseEntity>, dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()) : super(dispatcherProvider) {
        this.entities = entities.toMutableList()
    }

    fun getEntities(): List<BaseEntity> = entities

    override suspend fun toByteArray(): ByteArray {
        return withContext(dispatcherProvider.default()) {
            var bytes = super.toByteArray()
                .plus(entities.size.toByte())

            val defferds = mutableListOf<Deferred<Unit>>()
            val mutex = Mutex()
            entities.forEach {
                defferds.add(async {
                    mutex.withLock {
                        bytes = bytes.plus(it.toByteArray())
                    }
                })
            }
            defferds.awaitAll()

            return@withContext bytes
        }
    }

    companion object {
        const val AMOUNT_INDEX = 0
        const val LIST_START_INDEX = AMOUNT_INDEX + 1

        suspend fun fromByteArray(bytes: ByteArray, dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()): EntitiesNotification {
            val entities = mutableListOf<BaseEntity>()

            val numberOfEntities = bytes[AMOUNT_INDEX]
            var listBytes = bytes.dropUntil(LIST_START_INDEX)
            while (listBytes.isNotEmpty()) {
                cropEntityFromList(listBytes)?.also { entity ->
                    entities.add(entity)
                } ?: throw Exception("Entities Notification - problem in parsing message")
                listBytes = listBytes.dropUntil(findNumberOfBytesToDrop(listBytes))
            }

            return EntitiesNotification(entities, dispatcherProvider)
        }

        private fun cropEntityFromList(bytes: ByteArray): BaseEntity? =
            EntityParseStrategyIndex.getParser(bytes[TzufMapServerApi.ENTITIES_TYPE_LOCAL_INDEX].toInt())
                ?.parse(bytes)

        private fun findNumberOfBytesToDrop(bytes: ByteArray): Int {
            var bytesToDrop =
                TzufMapServerApi.ENTITY_TYPE_SIZE + TzufMapServerApi.ENTITY_ID_LENGTH_SIZE + bytes[TzufMapServerApi.ENTITY_ID_LENGTH_SIZE]
            bytesToDrop += TzufMapServerApi.ENTITY_NAME_LENGTH_SIZE
            bytesToDrop += bytes[bytesToDrop - 1] // at name length
            bytesToDrop += TzufMapServerApi.ENTITY_POSITIONS_AMOUNT_SIZE
            bytesToDrop += bytes[bytesToDrop - 1] * 2 * TzufMapServerApi.COORDINATE_SIZE

            return bytesToDrop
        }

    }

}