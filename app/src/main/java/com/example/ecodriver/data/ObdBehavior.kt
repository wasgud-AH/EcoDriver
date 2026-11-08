package com.example.ecodriver.data

import android.util.Log
import com.github.eltonvs.obd.command.AdaptiveTimingMode
import com.github.eltonvs.obd.command.BadResponseException
import com.github.eltonvs.obd.command.ObdProtocols
import com.github.eltonvs.obd.command.Switcher
import com.github.eltonvs.obd.command.at.ResetAdapterCommand
import com.github.eltonvs.obd.command.at.SelectProtocolCommand
import com.github.eltonvs.obd.command.at.SetAdaptiveTimingCommand
import com.github.eltonvs.obd.command.at.SetEchoCommand
import com.github.eltonvs.obd.command.at.SetHeadersCommand
import com.github.eltonvs.obd.command.at.SetLineFeedCommand
import com.github.eltonvs.obd.command.at.SetSpacesCommand
import com.github.eltonvs.obd.command.control.TroubleCodesCommand
import com.github.eltonvs.obd.command.control.VINCommand
import com.github.eltonvs.obd.command.engine.RPMCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "ObdBehavior"

// Latest RPM reading, shown by ConnectionStatus
var rpmText by mutableStateOf("-")

suspend fun readObd(inputStream: InputStream, outputStream: OutputStream) {
    val obdConnection = ObdDeviceConnection(inputStream, outputStream)

    // Recommended low-noise ELM327 setup
    obdConnection.run(ResetAdapterCommand())
    obdConnection.run(SetEchoCommand(Switcher.OFF))
    obdConnection.run(SetLineFeedCommand(Switcher.OFF))
    obdConnection.run(SetSpacesCommand(Switcher.OFF))
    obdConnection.run(SetHeadersCommand(Switcher.OFF))
    obdConnection.run(SetAdaptiveTimingCommand(AdaptiveTimingMode.AUTO_1))
    obdConnection.run(SelectProtocolCommand(ObdProtocols.AUTO))

    // First PID makes the adapter search for the car's protocol, which can take ~10 s
    readOrNull { obdConnection.run(RPMCommand(), maxRetries = 20) }

    // Read once: these don't change during a drive
    val vin = readOrNull { obdConnection.run(VINCommand()) }
    val troubleCodes = readOrNull { obdConnection.run(TroubleCodesCommand()) }
    Log.d(TAG, "VIN: ${vin?.value ?: "n/a"}, DTC: ${troubleCodes?.value?.ifBlank { "none" } ?: "n/a"}")

    while (currentCoroutineContext().isActive) {
        val rpm = readOrNull { obdConnection.run(RPMCommand()) }
        if (rpm != null) {
            ecuStatus = ConnectionState.Connected
            rpmText = "${rpm.value} ${rpm.unit}"
        } else {
            ecuStatus = ConnectionState.Connecting
        }
        delay(250)
    }
}

// Null when the car answers with an error (NO DATA, UNABLE TO CONNECT...).
// Socket errors still throw and end the session in runBluetoothObdSession.
private inline fun <T> readOrNull(block: () -> T): T? = try {
    block()
} catch (e: BadResponseException) {
    Log.w(TAG, "OBD read failed: ${e.message}")
    null
}
