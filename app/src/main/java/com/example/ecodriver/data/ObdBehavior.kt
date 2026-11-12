package com.example.ecodriver.data

import android.os.SystemClock
import android.util.Log
import com.github.eltonvs.obd.command.AdaptiveTimingMode
import com.github.eltonvs.obd.command.BadResponseException
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdResponse
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
import com.github.eltonvs.obd.command.control.DTCNumberCommand
import com.github.eltonvs.obd.command.control.DistanceSinceCodesClearedCommand
import com.github.eltonvs.obd.command.control.MILOnCommand
import com.github.eltonvs.obd.command.control.ModuleVoltageCommand
import com.github.eltonvs.obd.command.control.PendingTroubleCodesCommand
import com.github.eltonvs.obd.command.control.PermanentTroubleCodesCommand
import com.github.eltonvs.obd.command.control.ResetTroubleCodesCommand
import com.github.eltonvs.obd.command.control.TimeSinceCodesClearedCommand
import com.github.eltonvs.obd.command.engine.LoadCommand
import com.github.eltonvs.obd.command.engine.MassAirFlowCommand
import com.github.eltonvs.obd.command.engine.RPMCommand
import com.github.eltonvs.obd.command.engine.SpeedCommand
import com.github.eltonvs.obd.command.engine.ThrottlePositionCommand
import com.github.eltonvs.obd.command.fuel.FuelConsumptionRateCommand
import com.github.eltonvs.obd.command.fuel.FuelTrimCommand
import com.github.eltonvs.obd.command.fuel.FuelTrimCommand.FuelTrimBank
import com.github.eltonvs.obd.command.pressure.IntakeManifoldPressureCommand
import com.github.eltonvs.obd.command.temperature.EngineCoolantTemperatureCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "ObdBehavior"

// Latest RPM reading, shown by ConnectionStatus
var rpmText by mutableStateOf("-")

// Latest readings keyed by the command's own .name
val obdValues = mutableStateMapOf<String, String>()
val obdNumbers = mutableStateMapOf<String, Double>()
val obdUnits = mutableStateMapOf<String, String>()

// Read every loop. FuelConsumptionRate is optional: TripState falls back to MAF.
private val fastCommands = listOf(
    RPMCommand(), SpeedCommand(), ThrottlePositionCommand(),
    MassAirFlowCommand(), FuelConsumptionRateCommand(),
)

// Read at session start and on DiagRequest: these don't change during a drive
private val onceCommands = listOf(
    VINCommand(), MILOnCommand(), DTCNumberCommand(), TroubleCodesCommand(),
    PendingTroubleCodesCommand(), PermanentTroubleCodesCommand(),
    DistanceSinceCodesClearedCommand(), TimeSinceCodesClearedCommand(),
)

enum class DiagRequest { Rescan, Clear }

// Set by DiagnosticToolsScreen; readObd handles it between reads and sets it back to null
var diagRequest by mutableStateOf<DiagRequest?>(null)

// Read every SLOW_EVERY_N_LOOPS loops (~1 s) for Live Data and the trip samples
private const val SLOW_EVERY_N_LOOPS = 4
private val slowCommands = listOf(
    LoadCommand(), EngineCoolantTemperatureCommand(), IntakeManifoldPressureCommand(),
    FuelTrimCommand(FuelTrimBank.SHORT_TERM_BANK_1), FuelTrimCommand(FuelTrimBank.LONG_TERM_BANK_1),
    ModuleVoltageCommand(),
)

suspend fun readObd(inputStream: InputStream, outputStream: OutputStream) {
    TripState.reset(SystemClock.elapsedRealtime())
    TripRecorder.start()
    obdValues.clear()
    obdNumbers.clear()
    obdUnits.clear()
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

    readOnceCommands(obdConnection)
    Log.d(TAG, "VIN: ${obdValues[Pid.VIN] ?: "n/a"}, DTC: ${obdValues[Pid.DTC_STORED]?.ifBlank { "none" } ?: "n/a"}")

    var loop = 0
    try {
        while (currentCoroutineContext().isActive) {
            val readings = fastCommands.map { readInto(obdConnection, it) }
            if (loop++ % SLOW_EVERY_N_LOOPS == 0) slowCommands.forEach { readInto(obdConnection, it) }
            val rpmOk = readings.first() != null // RPM is first in fastCommands
            ecuStatus = if (rpmOk) ConnectionState.Connected else ConnectionState.Connecting
            val nowMs = SystemClock.elapsedRealtime()
            TripState.update(nowMs)
            TripRecorder.tick(nowMs)
            diagRequest?.let { handleDiag(obdConnection, it); diagRequest = null }
            delay(250)
        }
    } finally {
        // The session ends by cancellation, so the final write must not be cancelled with it
        withContext(NonCancellable) { TripRecorder.finish() }
    }
}

// Drops old values first so a code that is gone (or a failed read) doesn't linger on screen
private suspend fun readOnceCommands(connection: ObdDeviceConnection) = onceCommands.forEach {
    obdValues.remove(it.name)
    obdNumbers.remove(it.name)
    readInto(connection, it)
}

private suspend fun handleDiag(connection: ObdDeviceConnection, request: DiagRequest) {
    // The screen gates Clear too; re-check here because the car may have moved since the tap
    if (request == DiagRequest.Clear && ecuStatus == ConnectionState.Connected && TripState.mph == 0.0) {
        readOrNull { connection.run(ResetTroubleCodesCommand()) }
    }
    readOnceCommands(connection)
}

// Stores the reading in obdValues/obdNumbers. Null when the read fails.
private suspend fun readInto(connection: ObdDeviceConnection, cmd: ObdCommand): ObdResponse? {
    val r = readOrNull { connection.run(cmd) } ?: return null
    obdValues[cmd.name] = r.value
    obdUnits[cmd.name] = r.unit
    r.value.toDoubleOrNull()?.let { obdNumbers[cmd.name] = it }
    if (cmd is RPMCommand) rpmText = "${r.value} ${r.unit}"
    return r
}

// Null when the car answers with an error (NO DATA, UNABLE TO CONNECT...).
// Socket errors still throw and end the session in runBluetoothObdSession.
private inline fun <T> readOrNull(block: () -> T): T? = try {
    block()
} catch (e: BadResponseException) {
    Log.w(TAG, "OBD read failed: ${e.message}")
    null
}
