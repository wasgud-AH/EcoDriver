package com.example.ecodriver.data

import android.database.sqlite.SQLiteConstraintException

private const val SAMPLE_EVERY_N_LOOPS = 4 // readObd loops every ~250 ms, so ~1 sample/s

/** Logs the running TripState to Room. Driven by readObd(). */
object TripRecorder {
    private var tripId: Long? = null
    private var startWallMs = 0L
    private var loops = 0
    private var seenAccel = 0
    private var seenBrake = 0
    private var idleLogged = false
    private val mphSamples = mutableListOf<Double>()

    /** Call right after TripState.reset(). */
    suspend fun start(vehicleId: Long? = null) {
        val vid = vehicleId ?: ensureActiveVehicle()
        startWallMs = System.currentTimeMillis()
        loops = 0
        seenAccel = 0
        seenBrake = 0
        idleLogged = false
        mphSamples.clear()
        tripId = EcoDb.db.trips().insert(Trip(vehicleId = vid, startMs = startWallMs))
    }

    // TripState runs on elapsedRealtime; the DB stores wall-clock time.
    private fun wallMs(nowMs: Long) = startWallMs + (nowMs - TripState.startMs)

    /** Call right after TripState.update(nowMs), with the same nowMs. */
    suspend fun tick(nowMs: Long, numbers: Map<String, Double> = obdNumbers) {
        val id = tripId ?: return
        try {
            record(id, nowMs, numbers)
        } catch (e: SQLiteConstraintException) {
            // The running trip was deleted (Delete all trips, or its vehicle). Stop logging it
            // instead of letting the exception end the OBD session.
            tripId = null
        }
    }

    private suspend fun record(id: Long, nowMs: Long, numbers: Map<String, Double>) {
        val db = EcoDb.db
        val t = wallMs(nowMs)

        if (TripState.harshAccelCount > seenAccel) {
            seenAccel = TripState.harshAccelCount
            db.events().insert(Event(tripId = id, type = EventType.HARSH_ACCEL, timeMs = t, value = TripState.lastHarshRate))
        }
        if (TripState.harshBrakeCount > seenBrake) {
            seenBrake = TripState.harshBrakeCount
            db.events().insert(Event(tripId = id, type = EventType.HARSH_BRAKE, timeMs = t, value = TripState.lastHarshRate))
        }
        if (TripState.idleSeconds >= IDLE_CUE_S) {
            if (!idleLogged) {
                idleLogged = true
                db.events().insert(Event(tripId = id, type = EventType.IDLE_LONG, timeMs = t, value = TripState.idleSeconds))
            }
        } else {
            idleLogged = false
        }

        if (loops++ % SAMPLE_EVERY_N_LOOPS != 0) return
        mphSamples += TripState.mph
        db.samples().insert(
            Sample(
                tripId = id,
                timeMs = t,
                rpm = numbers[Pid.RPM],
                mph = TripState.mph,
                throttlePct = numbers[Pid.THROTTLE],
                loadPct = numbers[Pid.LOAD],
                coolantF = numbers[Pid.COOLANT]?.let(::cToF),
                mafGps = numbers[Pid.MAF],
                mapKpa = numbers[Pid.MAP],
            )
        )
    }

    /** Writes the totals, or drops the trip when it was too short to matter. */
    suspend fun finish() {
        val id = tripId ?: return
        tripId = null
        val trips = EcoDb.db.trips()
        val distance = TripState.distanceMi
        if (distance < MIN_TRIP_MI) {
            trips.deleteById(id)
            return
        }
        val durationS = (TripState.nowMs - TripState.startMs) / 1000.0
        val idlePct = if (durationS > 0) TripState.idleTotalSec / durationS * 100 else 0.0
        val harshPer10Mi = (TripState.harshAccelCount + TripState.harshBrakeCount) / distance * 10
        val fuel = TripState.fuelGal
        val trip = trips.byId(id) ?: return
        trips.update(
            trip.copy(
                endMs = wallMs(TripState.nowMs),
                distanceMi = distance,
                fuelGal = fuel,
                mpg = if (fuel > 0) distance / fuel else null,
                score = smoothnessScore(harshPer10Mi, idlePct),
                consistency = consistency(mphSamples),
                idleSec = TripState.idleTotalSec,
                harshAccelCount = TripState.harshAccelCount,
                harshBrakeCount = TripState.harshBrakeCount,
                dfcoSec = TripState.dfcoSec,
            )
        )
    }
}
