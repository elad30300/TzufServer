package com.example.tzufserver.bluetooth.api.map

import com.example.tzufserver.data.model.BaseEntity
import java.util.*

annotation class ApiVersion(val version: Int)

@ApiVersion(1)
object TzufMapServerApi {

    val apiVersion = TzufMapServerApi::class.java.getAnnotation(ApiVersion::class.java)?.version

    val GEO_SERVICE_UUID: UUID = UUID.fromString("69EEFB3E-0ACE-4F26-B4AD-6D44B5A1E7A5")
    val ENTITIES_REQUEST_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("45F151E9-EFBE-443E-AF49-6C551CB91C59")
    val RASTER_TILES_REQUEST_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("b07ead5a-e3cd-4cc7-87be-f46203426d96")
    val ENTITIES_NOTIFICATIONS_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("BFFCA590-500A-495A-84BC-F3D5E3953E83")
    val RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("5E094DE2-2F39-477B-B37B-C52798923B8C")
    val RASTER_TILES_NOTIFICATIONS_ACK_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("0b0cb9f8-280f-4407-96ae-88a82290ee0f")
    val ENTITIES_NOTIFICATIONS_ACK_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("f0acd1f1-be77-478a-bb39-b552a14865a1")
    val CLIENT_CONFIG_DESCRIPTOR_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val ackCharacteristics = mapOf(
        RASTER_TILES_NOTIFICATIONS_CHARACTERISTIC_UUID to RASTER_TILES_NOTIFICATIONS_ACK_CHARACTERISTIC_UUID,
        ENTITIES_NOTIFICATIONS_CHARACTERISTIC_UUID to ENTITIES_NOTIFICATIONS_ACK_CHARACTERISTIC_UUID
    )

    // entities api values
    const val ENEMY_TYPE_VALUE = 1
    const val TROOPS_TYPE_VALUE = 2
    const val LIMIT_BORDERS_TYPE_VALUE = 3
    const val PATH_TYPE_VALUE = 4

    // general messages constants
    const val REQUEST_ID_INDEX = 0
    const val COORDINATE_SIZE = 8 // TODO: change to sizeof(double)
    const val BITMAP_LENGTH_SIZE = 4 // TODO: change to sizeof(int)
    const val ZOOM_SIZE = 4
    const val BOOLEAN_TRUE_VALUE = 1
    const val BOOLEAN_FALSE_VALUE = 0

    const val ENTITIES_TYPE_LOCAL_INDEX = 0
    const val ENTITY_TYPE_SIZE = 1

    // for base entity
    const val ENTITY_ID_LENGTH_LOCAL_INDEX = 0
    const val ENTITY_ID_LENGTH_SIZE = 1
    const val ENTITY_ID_START_LOCAL_INDEX = ENTITY_ID_LENGTH_LOCAL_INDEX + 1
    const val ENTITY_NAME_LENGTH_SIZE = 1
    const val ENTITY_POSITIONS_AMOUNT_SIZE = 1

    // raster tiles notification indexed
//    const val RASTER_TILES_AMOUNT_INDEX = REQUEST_ID_INDEX + 1
//    const val RASTER_TILES_LIST_START_INDEX = RASTER_TILES_AMOUNT_INDEX + 1
    const val RASTER_TILES_LIST_START_INDEX = 0
    const val RASTER_TILE_ZOOM_START_LOCAL_INDEX = 0
    const val RASTER_TILE_BOUNDARIES_START_LOCAL_INDEX =
        RASTER_TILE_ZOOM_START_LOCAL_INDEX + ZOOM_SIZE
    const val TOP_LEFT_RASTER_TILE_START_LOCAL_INDEX = RASTER_TILE_BOUNDARIES_START_LOCAL_INDEX
    const val TOP_RIGHT_RASTER_TILE_START_LOCAL_INDEX =
        RASTER_TILE_BOUNDARIES_START_LOCAL_INDEX + 2 * COORDINATE_SIZE
    const val BOTTOM_LEFT_RASTER_TILE_START_LOCAL_INDEX =
        RASTER_TILE_BOUNDARIES_START_LOCAL_INDEX + 4 * COORDINATE_SIZE
    const val BOTTOM_RIGHT_RASTER_TILE_START_LOCAL_INDEX =
        RASTER_TILE_BOUNDARIES_START_LOCAL_INDEX + 6 * COORDINATE_SIZE
    const val BITMAP_LENGTH_RASTER_TILE_LOCAL_INDEX =
        RASTER_TILE_BOUNDARIES_START_LOCAL_INDEX + 8 * COORDINATE_SIZE
    const val BITMAP_DATA_RASTER_TILE_LOCAL_INDEX =
        BITMAP_LENGTH_RASTER_TILE_LOCAL_INDEX + BITMAP_LENGTH_SIZE


    // helper functions

    fun booleanToMessageByte(boolean: Boolean): Byte {
        return if (boolean) BOOLEAN_TRUE_VALUE.toByte() else BOOLEAN_FALSE_VALUE.toByte()
    }

    fun messageByteToBoolean(byte: Byte): Boolean {
        if (byte == BOOLEAN_TRUE_VALUE.toByte()) {
            return true
        }
        if (byte == BOOLEAN_TRUE_VALUE.toByte()) {
            return false
        }
        throw Exception("try to convert byte to boolean but value is not okay, value: $byte")
    }

    fun getUuidToBeAckedByAckUuid(ackUuid: UUID): UUID? =
        ackCharacteristics.filter { (uuid, ack) -> ack == ackUuid }.keys.firstOrNull()
}