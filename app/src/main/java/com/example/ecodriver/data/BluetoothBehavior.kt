package com.example.ecodriver.data

/*Importing bluetooth APIs needed later to get input stream for Kotlin OBD API */

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

@SuppressLint("MissingPermission")
suspend fun connectToElm(context: Context): BluetoothSocket = withContext(Dispatchers.IO) {
    val adapter = context.getSystemService(BluetoothManager::class.java).adapter
        ?: error("This phone has no Bluetooth")
    // shortcut: picks the first paired device with "OBD" in its name; add a device picker if yours is named differently
    val device = adapter.bondedDevices.firstOrNull { it.name?.contains("OBD", ignoreCase = true) == true }
        ?: error("No paired OBD adapter. Pair it in Android Bluetooth settings first.")
    device.createRfcommSocketToServiceRecord(SPP_UUID).apply { connect() }
}