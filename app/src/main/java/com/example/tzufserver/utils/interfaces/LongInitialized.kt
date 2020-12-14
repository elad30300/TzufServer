package com.example.tzufserver.utils.interfaces

interface LongInitialized {

    var onReadyCallback: (() -> Unit)?
    var isReady: Boolean

    private fun runOnReadyCallback() {
        onReadyCallback?.also { it() }
    }

    fun onReady() {
        synchronized(this.isReady) {
            this.isReady = true
        }
        runOnReadyCallback()
    }

    fun setNewOnReadyCallback(onReadyCallback: () -> Unit) {
        this.onReadyCallback = onReadyCallback
        val isReady: Boolean
        synchronized(this.isReady) {
            isReady = this.isReady
        }
        if ((isReady)) {
            runOnReadyCallback()
        }
    }

    fun removeOnReadyCallback() {
        onReadyCallback = null
    }

}