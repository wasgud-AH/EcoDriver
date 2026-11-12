package com.example.ecodriver.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Gaps longer than this (adapter stall, protocol search) are not integrated.
private const val MAX_DT_S = 5.0

object TripState {
    var startMs by mutableStateOf(0L)
    var nowMs by mutableStateOf(0L)
    var mph by mutableStateOf(0.0)
    var galPerHour by mutableStateOf<Double?>(null)
    var distanceMi by mutableStateOf(0.0)
    var fuelGal by mutableStateOf(0.0)
    var idleSeconds by mutableStateOf(0.0)
    var dfco by mutableStateOf(false)
    var lastHarsh by mutableStateOf<HarshEvent?>(null)
    var lastHarshMs by mutableStateOf(0L)

    // Running totals for the trip log
    var idleTotalSec by mutableStateOf(0.0)
    var dfcoSec by mutableStateOf(0.0)
    var harshAccelCount by mutableStateOf(0)
    var harshBrakeCount by mutableStateOf(0)
    var lastHarshRate = 0.0 // mph/s of the latest counted harsh event
        private set

    private var lastMs = 0L

    fun reset(nowMs: Long) {
        startMs = nowMs
        this.nowMs = nowMs
        mph = 0.0
        galPerHour = null
        distanceMi = 0.0
        fuelGal = 0.0
        idleSeconds = 0.0
        dfco = false
        lastHarsh = null
        lastHarshMs = 0L
        idleTotalSec = 0.0
        dfcoSec = 0.0
        harshAccelCount = 0
        harshBrakeCount = 0
        lastHarshRate = 0.0
        lastMs = 0L
    }

    fun update(nowMs: Long, numbers: Map<String, Double> = obdNumbers) {
        val newMph = (numbers[Pid.SPEED] ?: 0.0) * KMH_TO_MPH // the library reports km/h
        val rpm = numbers[Pid.RPM] ?: 0.0
        val gph = numbers[Pid.FUEL_RATE]?.let(::galPerHourFromLph)
            ?: numbers[Pid.MAF]?.let(::galPerHourFromMaf)

        val newDfco = isDfco(newMph, numbers[Pid.THROTTLE], gph)
        val dtS = if (lastMs == 0L) 0.0 else (nowMs - lastMs) / 1000.0
        if (dtS in 0.0..MAX_DT_S) {
            val dtH = dtS / 3600
            distanceMi += newMph * dtH
            fuelGal += (gph ?: 0.0) * dtH
            harshEvent(mph, newMph, dtS)?.let {
                if (it != lastHarsh || nowMs - lastHarshMs > HARSH_DEBOUNCE_MS) {
                    if (it == HarshEvent.Accel) harshAccelCount++ else harshBrakeCount++
                    lastHarshRate = (newMph - mph) / dtS
                }
                lastHarsh = it
                lastHarshMs = nowMs
            }
            val idling = newMph == 0.0 && rpm > 0
            idleSeconds = if (idling) idleSeconds + dtS else 0.0
            if (idling) idleTotalSec += dtS
            if (newDfco) dfcoSec += dtS
        }

        lastMs = nowMs
        this.nowMs = nowMs
        mph = newMph
        galPerHour = gph
        dfco = newDfco
    }

    val cue: Cue get() = pickCue(lastHarsh, nowMs - lastHarshMs, idleSeconds, dfco)
}
