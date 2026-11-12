package com.example.ecodriver.data

import java.util.Locale
import kotlin.math.roundToInt

// Pure trip math. No Android or Compose imports, so it runs in JVM unit tests.

const val HARSH_ACCEL_MPH_PER_S = 7.0
const val HARSH_BRAKE_MPH_PER_S = 8.0
const val HARSH_CUE_MS = 5_000L
const val IDLE_CUE_S = 60.0

// "About 0": absolute throttle on some cars rests a few % above 0 when closed.
const val THROTTLE_CLOSED_PCT = 2.0
const val FUEL_FLOW_ZERO_GPH = 0.01

const val KMH_TO_MPH = 0.621371
private const val LITERS_PER_GALLON = 3.78541

enum class HarshEvent { Accel, Brake }

sealed interface Cue {
    data class Harsh(val event: HarshEvent) : Cue
    data class Idling(val seconds: Int) : Cue
    data object Coasting : Cue
    data object Steady : Cue
}

fun galPerHourFromMaf(mafGps: Double): Double = mafGps * 3600 / 14.7 / 2799

fun galPerHourFromLph(litersPerHour: Double): Double = litersPerHour / LITERS_PER_GALLON

/** Null while stopped or with no fuel data. Infinity during fuel cut. */
fun instantMpg(mph: Double, galPerHour: Double?): Double? = when {
    mph < 1 || galPerHour == null -> null
    galPerHour < FUEL_FLOW_ZERO_GPH -> Double.POSITIVE_INFINITY
    else -> mph / galPerHour
}

fun instantMpgText(mpg: Double?): String = when {
    mpg == null -> "--"
    mpg >= 99.5 -> "99+"
    else -> mpg.roundToInt().toString()
}

/** Null until the trip passes 0.1 mi. */
fun tripMpg(distanceMi: Double, fuelGal: Double): Double? = when {
    distanceMi <= 0.1 -> null
    fuelGal <= 0 -> Double.POSITIVE_INFINITY
    else -> distanceMi / fuelGal
}

fun tripMpgText(mpg: Double?): String = when {
    mpg == null -> "--"
    mpg >= 99.95 -> "99+"
    else -> String.format(Locale.US, "%.1f", mpg)
}

fun isDfco(mph: Double, throttlePct: Double?, galPerHour: Double?): Boolean =
    mph > 5 && throttlePct != null && throttlePct <= THROTTLE_CLOSED_PCT &&
        galPerHour != null && galPerHour < FUEL_FLOW_ZERO_GPH

fun harshEvent(prevMph: Double, mph: Double, dtS: Double): HarshEvent? {
    if (dtS <= 0) return null
    val rate = (mph - prevMph) / dtS
    return when {
        rate > HARSH_ACCEL_MPH_PER_S -> HarshEvent.Accel
        rate < -HARSH_BRAKE_MPH_PER_S -> HarshEvent.Brake
        else -> null
    }
}

fun pickCue(lastHarsh: HarshEvent?, msSinceHarsh: Long, idleSeconds: Double, dfco: Boolean): Cue = when {
    lastHarsh != null && msSinceHarsh <= HARSH_CUE_MS -> Cue.Harsh(lastHarsh)
    idleSeconds >= IDLE_CUE_S -> Cue.Idling(idleSeconds.toInt())
    dfco -> Cue.Coasting
    else -> Cue.Steady
}

fun elapsedText(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

// --- Trip report math ---

const val MIN_TRIP_MI = 0.1
const val HARSH_DEBOUNCE_MS = 2_000L // one braking that spans several 250 ms reads counts once
const val SMOOTH_PENALTY_PER_HARSH_PER_10MI = 10.0
const val SMOOTH_PENALTY_PER_IDLE_PCT = 0.5
const val CONSISTENCY_MIN_MPH = 5.0
const val CONSISTENCY_MIN_SAMPLES = 30
private const val KPA_TO_PSI = 0.145038

fun smoothnessScore(harshPer10Mi: Double, idlePct: Double): Double =
    (100 - SMOOTH_PENALTY_PER_HARSH_PER_10MI * harshPer10Mi - SMOOTH_PENALTY_PER_IDLE_PCT * idlePct)
        .coerceIn(0.0, 100.0)

/** 100 × (1 − coefficient of variation) of moving speed. Null with too few moving samples. */
fun consistency(mphSamples: List<Double>): Double? {
    val moving = mphSamples.filter { it > CONSISTENCY_MIN_MPH }
    if (moving.size < CONSISTENCY_MIN_SAMPLES) return null
    val mean = moving.average()
    val sd = kotlin.math.sqrt(moving.sumOf { (it - mean) * (it - mean) } / moving.size)
    return (100 * (1 - sd / mean)).coerceIn(0.0, 100.0)
}

/** Positive when the trip used less fuel than the baseline MPG would have. */
fun savingsUsd(distanceMi: Double, fuelGal: Double, baselineMpg: Double, pricePerGal: Double): Double =
    (distanceMi / baselineMpg - fuelGal) * pricePerGal

fun kmhToMph(kmh: Double): Double = kmh * KMH_TO_MPH
fun cToF(c: Double): Double = c * 9 / 5 + 32
fun kpaToPsi(kpa: Double): Double = kpa * KPA_TO_PSI

fun durationText(ms: Long): String {
    val min = ms / 60_000
    return if (min < 60) "$min min" else String.format(Locale.US, "%d h %02d min", min / 60, min % 60)
}

fun usdText(usd: Double): String = String.format(Locale.US, "$%.2f", kotlin.math.abs(usd))
