package com.example.tzufserver.extension

import android.graphics.Bitmap
import com.example.tzufserver.provider.dispatchers.DefaultDispatcherProvider
import com.example.tzufserver.provider.dispatchers.DispatcherProvider
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import kotlin.math.ceil

// double < - > bytearray

fun ByteArray.toDouble(): Double {
    var bits: Long = 0

    for (i in 0..7) {
        bits = (bits shl 8) or (this[i].toLong() and 0xff)
    }
    return Double.fromBits(bits)
}

fun Double.toByteList(): List<Byte> {
    val bytes = mutableListOf<Byte>()
    var bits = this.toBits()

    for (i in 0..7) {
        bytes.add(0, (bits and 0xff).toByte())
        bits = bits shr 8
    }

    return bytes
}

fun Double.toByteArray() = this.toByteList().toByteArray()

// int < - > bytearray

fun ByteArray.toInt(): Int {
    var number: Int = 0

    for (i in 0..3) {
        number = (number shl 8) or (this[i].toInt() and 0xff)
    }
    return number
}

fun Int.toByteList(): List<Byte> {
    val bytes = mutableListOf<Byte>()
    var number = this

    for (i in 0..3) {
        bytes.add(0, (number and 0xff).toByte())
        number = number shr 8
    }

    return bytes
}

fun Int.toByteArray() = this.toByteList().toByteArray()

// bitmap <-> byetarray

fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val byteArray: ByteArray = stream.toByteArray()
//    this.recycle()
    return byteArray
//    val size = BitmapCompat.getAllocationByteCount(this)
//    val buffer = ByteBuffer.allocate(size)
//    val bytes = ByteArray(size)
//    this.copyPixelsToBuffer(buffer)
//    buffer.rewind()
//
//    buffer.get(bytes)
//
//    return bytes
////    return buffer.array()
}

suspend fun ByteArray.longDivide(size: Int, dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()): List<ByteArray> {
    var bytes = this
    val parts = MutableList(ceil(this.size.toFloat() / size.toFloat()).toInt()) { ByteArray(0) }
    val deffereds = mutableListOf<Deferred<ByteArray>>()
    var index = 0
    while (bytes.isNotEmpty()) {
        val end = size.coerceAtMost(bytes.size)
        withContext(dispatcherProvider.default()) {
            val partIndex = index++
            async {
                parts[partIndex] = bytes.sliceArray(0 until end)
            }
        }
        bytes = bytes.copyOfRange(end, bytes.size)
    }
    deffereds.awaitAll()
    return parts
}

fun ByteArray.dropUntil(until: Int) = this.copyOfRange(until, this.size)

