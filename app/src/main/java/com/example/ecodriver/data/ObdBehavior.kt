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
import com.github.eltonvs.obd.command.control.DistanceMILOnCommand
import com.github.eltonvs.obd.command.control.TimeSinceMILOnCommand
import com.github.eltonvs.obd.command.control.TimingAdvanceCommand
import com.github.eltonvs.obd.command.egr.CommandedEgrCommand
import com.github.eltonvs.obd.command.egr.EgrErrorCommand
import com.github.eltonvs.obd.command.engine.AbsoluteLoadCommand
import com.github.eltonvs.obd.command.engine.RelativeThrottlePositionCommand
import com.github.eltonvs.obd.command.engine.RuntimeCommand
import com.github.eltonvs.obd.command.fuel.CommandedEquivalenceRatioCommand
import com.github.eltonvs.obd.command.fuel.EthanolLevelCommand
import com.github.eltonvs.obd.command.fuel.FuelLevelCommand
import com.github.eltonvs.obd.command.fuel.FuelTypeCommand
import com.github.eltonvs.obd.command.pressure.BarometricPressureCommand
import com.github.eltonvs.obd.command.pressure.FuelPressureCommand
import com.github.eltonvs.obd.command.pressure.FuelRailGaugePressureCommand
import com.github.eltonvs.obd.command.pressure.FuelRailPressureCommand
import com.github.eltonvs.obd.command.temperature.AirIntakeTemperatureCommand
import com.github.eltonvs.obd.command.temperature.AmbientAirTemperatureCommand
import com.github.eltonvs.obd.command.temperature.OilTemperatureCommand
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "ObdBehavior"

var allSensorsOpen by mutableStateOf(false)

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

// Failed reads in a row per command. At MAX_FAILS a PID is skipped until the next retry.
private const val MAX_FAILS = 3
private const val RETRY_SKIPPED_MS = 60_000L
private val failures = mutableMapOf<String, Int>()

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

//These variables are used when tapping on the All Sensors screen, in which the extra OBD PIDS
//that aren't used for MPG calulations to be present should the user want that information.
//We use the Extras Delay in order to avoid incurring the additional overhead from the loop needing to
//check all the extra PIDS just in case the user accidentally taps on the All Sensors screen.

private const val EXTRAS_PER_LOOP = 2
private const val EXTRAS_DELAY_MS = 3_000L
private val extraCommands = listOf(
    FuelTypeCommand(), DistanceMILOnCommand(), TimeSinceMILOnCommand(),
    AirIntakeTemperatureCommand(), AmbientAirTemperatureCommand(), OilTemperatureCommand(),
    FuelLevelCommand(), EthanolLevelCommand(), FuelPressureCommand(),
    FuelRailPressureCommand(), FuelRailGaugePressureCommand(),
    FuelTrimCommand(FuelTrimBank.SHORT_TERM_BANK_2), FuelTrimCommand(FuelTrimBank.LONG_TERM_BANK_2),
    AbsoluteLoadCommand(), RelativeThrottlePositionCommand(), RuntimeCommand(),
    TimingAdvanceCommand(), BarometricPressureCommand(),
    CommandedEquivalenceRatioCommand(), CommandedEgrCommand(), EgrErrorCommand(),
)


suspend fun readObd(inputStream: InputStream, outputStream: OutputStream) {
    TripState.reset(SystemClock.elapsedRealtime())
    TripRecorder.start()
    obdValues.clear()
    obdNumbers.clear()
    obdUnits.clear()
    failures.clear()
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
    var lastRetryMs = 0L // 0 until the first full loop has filled in the values

    //These variables are only used when the user's seeing all Sensors
    var extraIndex = 0
    var sensorsOpenedMs = 0L

    try {
        while (currentCoroutineContext().isActive) {
            val readings = fastCommands.map { readInto(obdConnection, it) }
            if (loop++ % SLOW_EVERY_N_LOOPS == 0) slowCommands.forEach { readInto(obdConnection, it) }

            if(allSensorsOpen){
                val t = SystemClock.elapsedRealtime()
                if(sensorsOpenedMs == 0L) sensorsOpenedMs = t
                if(t - sensorsOpenedMs >= EXTRAS_DELAY_MS) repeat(EXTRAS_PER_LOOP){
                    readInto(obdConnection,extraCommands[extraIndex++ % extraCommands.size])
                }
            } else if(sensorsOpenedMs != 0L){
                sensorsOpenedMs = 0L
                extraCommands.forEach {
                    obdValues.remove(it.name); obdNumbers.remove(it.name); obdUnits.remove(it.name)
                    }
                }

            val rpmOk = readings.first() != null // RPM is first in fastCommands
            ecuStatus = if (rpmOk) ConnectionState.Connected else ConnectionState.Connecting
            val nowMs = SystemClock.elapsedRealtime()
            if (lastRetryMs == 0L) {
                lastRetryMs = nowMs
            } else if (nowMs - lastRetryMs >= RETRY_SKIPPED_MS) {
                // One more try each: a PID that fails again goes straight back to skipped
                failures.replaceAll { _, n -> minOf(n, MAX_FAILS - 1) }
                lastRetryMs = nowMs
                Log.d(TAG, "Retrying skipped PIDs")
            }
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
    failures.remove(it.name) // a Rescan always asks again
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
    val fails = failures[cmd.name] ?: 0
    if (fails >= MAX_FAILS) return null
    val r = readOrNull { connection.run(cmd) }
    if (r == null) {
        // RPM is never skipped: the loop uses it to tell whether the car is answering
        if (cmd !is RPMCommand) {
            failures[cmd.name] = fails + 1
            if (fails + 1 == MAX_FAILS) Log.d(TAG, "Skipping ${cmd.name} after $MAX_FAILS failed reads")
        }
        return null
    }
    failures.remove(cmd.name)
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
