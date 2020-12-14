package com.example.tzufserver

import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationSettings @Inject constructor() {

    val bluetoothEnabled = MutableLiveData<Boolean>(false)
    val locationPermissionsGranted = MutableLiveData<Boolean>(false)

    val GATT_TYPE_CLIENT = "client"
    val GATT_TYPE_SERVER = "server"
    val gattType = GATT_TYPE_SERVER

}