package com.example.ecodriver.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecodriver.data.EcoDb
import com.example.ecodriver.data.ensureActiveVehicle
import com.example.ecodriver.data.needsDevicePick
import com.example.ecodriver.data.runBluetoothObdSession
import com.example.ecodriver.data.saveObdAddress
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Owns the OBD session so it survives screen changes and rotation
class ObdViewModel(app: Application) : AndroidViewModel(app) {
    private var sessionJob: Job? = null

    init {
        EcoDb.init(app)
        viewModelScope.launch { ensureActiveVehicle() } // first launch: create "My car"
    }

    fun connect() {
        if (sessionJob?.isActive == true) return
        sessionJob = viewModelScope.launch { runBluetoothObdSession(getApplication()) }
    }

    fun disconnect() {
        sessionJob?.cancel()
    }

    fun pickDevice(address: String) {
        saveObdAddress(getApplication(), address)
        needsDevicePick = false
        connect()
    }
}
