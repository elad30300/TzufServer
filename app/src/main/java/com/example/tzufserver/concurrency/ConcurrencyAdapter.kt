package com.example.tzufserver.concurrency

import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConcurrencyAdapter @Inject constructor() {

//    fun runInDefaultBackground(block: () -> Unit) {
//        runInBackground(Dispatchers.Default, block)
//    }
//
//    private fun runInBackground(dispatcher: CoroutineDispatcher, block: () -> Unit) {
//        CoroutineScope(dispatcher).launch {
//            suspend {
//                block()
//            }
//        }
//    }
//
//    private fun <T> runInBackgroundWithTrackAndResponse(
//        dispatcher: CoroutineDispatcher,
//        block: () -> T
//    ): Deferred<suspend () -> T> {
//        val deffered = CoroutineScope(dispatcher).async {
//            suspend {
//                block()
//            }
//        }
//        return deffered
//    }
//
//    private suspend fun runInBackgroundWithTrack(
//        dispatcher: CoroutineDispatcher,
//        block: () -> Unit
//    ): Deferred<suspend () -> Unit> = runInBackgroundWithTrackAndResponse(dispatcher, block)
//
//    private fun <E> forEachInBackgroundAndAwaitAll(
//        dispatcher: CoroutineDispatcher,
//        collection: Collection<E>,
//        block: (E) -> Unit
//    ) {
//        val defferes = mutableListOf<Deferred<suspend () -> Unit>>()
//        collection.forEach {
//            defferes.add(runInBackgroundWithTrack(Dispatchers.Default) { block(it) })
//        }
//        defferes.forEach {
//            it.await()
//        }
//    }
//
//    private fun <E> forEachInBackgroundAndAwaitAll(
//        collection: Collection<E>,
//        block: (E) -> Unit
//    ) {
//        forEachInBackgroundAndAwaitAll(Dispatchers.Default, collection, block)
//    }

}