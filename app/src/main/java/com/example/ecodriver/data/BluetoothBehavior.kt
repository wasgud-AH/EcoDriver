@file:Suppress("DEPRECATION", "MissingPermission")
package com.example.ecodriver.data


import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "BluetoothBehavior"

// Serial Port UUID - every ELM327 uses this?
private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

// Returns phone bluetooth adapter or null
fun bluetoothAdapter(): BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

/**
 * Picks one of the already paired devices
 * Prefers "obd" in the name, otherwise falls back to the first paired device
 * Returns null if Bluetooth is off or nothing is paired
 */
fun pickObdDevice(): BluetoothDevice? {
    val adapter = bluetoothAdapter() ?: return null
    if (!adapter.isEnabled) return null
    val bonded = adapter.bondedDevices ?: return null
    return bonded.firstOrNull { it.name?.contains("obd", ignoreCase = true) == true }
        ?: bonded.firstOrNull()
}

// Opens an RFCOMM socket, returns null if fails
suspend fun connect(device: BluetoothDevice): BluetoothSocket? =
    withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter() ?: return@withContext null
        try { adapter.cancelDiscovery() } catch (_: Exception) {}

        val socket = try {
            device.createRfcommSocketToServiceRecord(SPP_UUID)
        } catch (e: Exception) {
            Log.e(TAG, "create socket failed", e)
            return@withContext null
        }

        try {
            socket.connect()
            socket
        } catch (e: Exception) {
            Log.e(TAG, "connect failed", e)
            try { socket.close() } catch (_: Exception) {}
            null
        }
    }

/**
 * Call from UI
 * Finds a device, connects, runs readObd, closes the socket
 * Returns true
 */
suspend fun runBluetoothObdSession(): Boolean = withContext(Dispatchers.IO) {
    val device = pickObdDevice()
    if (device == null) {
        Log.e(TAG, "No paired Bluetooth device — pair the ELM327 in Settings first")
        return@withContext false
    }
    Log.d(TAG, "Using ${device.name} (${device.address})")

    val socket = connect(device) ?: return@withContext false

    try {
        readObd(socket.inputStream, socket.outputStream)
        true
    } catch (e: Exception) {
        Log.e(TAG, "OBD session failed", e)
        false
    } finally {
        try { socket.close() } catch (_: Exception) {}
    }
}