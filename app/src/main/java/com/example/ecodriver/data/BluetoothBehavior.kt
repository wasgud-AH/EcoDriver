@file:Suppress("DEPRECATION", "MissingPermission")
package com.example.ecodriver.data


import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "BluetoothBehavior"

// Serial Port UUID - every ELM327 uses this?
private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

enum class ConnectionState { Disconnected, Connecting, Connected }

// Read by ConnectionStatus; Compose redraws when these change
var elmStatus by mutableStateOf(ConnectionState.Disconnected)
var ecuStatus by mutableStateOf(ConnectionState.Disconnected)
// True when no saved device is paired; ConnectionStatus shows the picker
var needsDevicePick by mutableStateOf(false)

data class PairedDevice(val name: String, val address: String)

// Saved OBD adapter address, kept across app restarts
private fun prefs(context: Context) =
    context.getSharedPreferences("ecodriver", Context.MODE_PRIVATE)

fun savedObdAddress(context: Context): String? = prefs(context).getString("obd_address", null)

// Pass null to forget the device
fun saveObdAddress(context: Context, address: String?) =
    prefs(context).edit().putString("obd_address", address).apply()

// Returns phone bluetooth adapter or null
fun bluetoothAdapter(): BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

/**
 * Returns the paired device with the saved address
 * Returns null if nothing is saved or that device is no longer paired
 */
fun pickObdDevice(savedAddress: String?): BluetoothDevice? =
    bluetoothAdapter()?.bondedDevices?.firstOrNull { it.address == savedAddress }

// All paired devices, for the picker. Empty if permission is missing
fun pairedDevices(): List<PairedDevice> = try {
    bluetoothAdapter()?.bondedDevices.orEmpty().map { PairedDevice(it.name ?: "Unknown", it.address) }
} catch (e: SecurityException) {
    emptyList()
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
suspend fun runBluetoothObdSession(context: Context): Boolean = withContext(Dispatchers.IO) {
    elmStatus = ConnectionState.Connecting
    if (bluetoothAdapter()?.isEnabled != true) {
        Log.e(TAG, "Bluetooth is off")
        elmStatus = ConnectionState.Disconnected
        return@withContext false
    }
    val device = try {
        pickObdDevice(savedObdAddress(context))
    } catch (e: SecurityException) {
        Log.e(TAG, "Bluetooth permission not granted", e)
        elmStatus = ConnectionState.Disconnected
        return@withContext false
    }
    if (device == null) {
        Log.d(TAG, "No saved OBD device paired, asking the user to pick one")
        needsDevicePick = true
        elmStatus = ConnectionState.Disconnected
        return@withContext false
    }
    Log.d(TAG, "Using ${device.name} (${device.address})")

    val socket = connect(device)
    if (socket == null) {
        elmStatus = ConnectionState.Disconnected
        return@withContext false
    }
    elmStatus = ConnectionState.Connected
    ecuStatus = ConnectionState.Connecting

    try {
        readObd(socket.inputStream, socket.outputStream)
        true
    } catch (e: CancellationException) {
        // Normal Disconnect, not a failure
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "OBD session failed", e)
        false
    } finally {
        try { socket.close() } catch (_: Exception) {}
        elmStatus = ConnectionState.Disconnected
        ecuStatus = ConnectionState.Disconnected
        rpmText = "-"
    }
}