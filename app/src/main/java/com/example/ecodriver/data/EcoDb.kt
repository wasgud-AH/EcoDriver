package com.example.ecodriver.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val DB_NAME = "ecodriver.db"
const val KEY_ACTIVE_VEHICLE = "active_vehicle_id"
const val KEY_KEEP_SCREEN_ON = "keep_screen_on"

fun ecoPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences("ecodriver", Context.MODE_PRIVATE)

@Entity
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val make: String,
    val model: String,
    val year: Int,
    val tankGal: Double,
    val baselineMpg: Double,
    val fuelPricePerGal: Double,
) {
    val displayName: String get() = listOf(if (year > 0) "$year" else "", make, model).filter { it.isNotBlank() }.joinToString(" ")
}

@Entity(
    foreignKeys = [ForeignKey(Vehicle::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val startMs: Long,
    val endMs: Long = 0, // 0 until finish(): the trip is still running or the app died mid-trip
    val distanceMi: Double = 0.0,
    val fuelGal: Double = 0.0,
    val mpg: Double? = null, // null without fuel data
    val score: Double = 0.0,
    val consistency: Double? = null,
    val idleSec: Double = 0.0,
    val harshAccelCount: Int = 0,
    val harshBrakeCount: Int = 0,
    val dfcoSec: Double = 0.0,
)

@Entity(
    foreignKeys = [ForeignKey(Trip::class, ["id"], ["tripId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tripId")],
)
data class Sample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val timeMs: Long,
    val rpm: Double?,
    val mph: Double,
    val throttlePct: Double?,
    val loadPct: Double?,
    val coolantF: Double?,
    val mafGps: Double?,
    val mapKpa: Double?,
)

enum class EventType { HARSH_ACCEL, HARSH_BRAKE, IDLE_LONG }

@Entity(
    foreignKeys = [ForeignKey(Trip::class, ["id"], ["tripId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tripId")],
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val type: EventType,
    val timeMs: Long,
    val value: Double, // mph/s for harsh events, seconds for IDLE_LONG
)

@Dao
interface VehicleDao {
    @Query("SELECT * FROM Vehicle ORDER BY id") fun all(): Flow<List<Vehicle>>
    @Query("SELECT * FROM Vehicle WHERE id = :id") suspend fun byId(id: Long): Vehicle?
    @Query("SELECT * FROM Vehicle WHERE id = :id") fun watch(id: Long): Flow<Vehicle?>
    @Query("SELECT id FROM Vehicle ORDER BY id LIMIT 1") suspend fun firstId(): Long?
    @Query("SELECT COUNT(*) FROM Vehicle") fun count(): Flow<Int>
    @Insert suspend fun insert(v: Vehicle): Long
    @Update suspend fun update(v: Vehicle)
    @Delete suspend fun delete(v: Vehicle)
}

@Dao
interface TripDao {
    @Query("SELECT * FROM Trip WHERE vehicleId = :vehicleId AND endMs > 0 ORDER BY startMs DESC")
    fun forVehicle(vehicleId: Long): Flow<List<Trip>>
    @Query("SELECT * FROM Trip WHERE id = :id") fun watch(id: Long): Flow<Trip?>
    @Query("SELECT * FROM Trip ORDER BY startMs") suspend fun all(): List<Trip>
    @Query("SELECT COUNT(*) FROM Trip") fun count(): Flow<Int>
    @Insert suspend fun insert(t: Trip): Long
    @Update suspend fun update(t: Trip)
    @Query("SELECT * FROM Trip WHERE id = :id") suspend fun byId(id: Long): Trip?
    @Query("DELETE FROM Trip WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM Trip") suspend fun deleteAll()
}

@Dao
interface SampleDao {
    @Insert suspend fun insert(s: Sample)
    @Query("SELECT * FROM Sample WHERE tripId = :tripId ORDER BY timeMs") fun forTrip(tripId: Long): Flow<List<Sample>>
    @Query("SELECT * FROM Sample ORDER BY tripId, timeMs") suspend fun all(): List<Sample>
    @Query("SELECT COUNT(*) FROM Sample") fun count(): Flow<Int>
}

@Dao
interface EventDao {
    @Insert suspend fun insert(e: Event)
    @Query("SELECT * FROM Event WHERE tripId = :tripId ORDER BY timeMs") fun forTrip(tripId: Long): Flow<List<Event>>
    @Query("SELECT COUNT(*) FROM Event") fun count(): Flow<Int>
}

@Database(entities = [Vehicle::class, Trip::class, Sample::class, Event::class], version = 1, exportSchema = false)
abstract class EcoDb : RoomDatabase() {
    abstract fun vehicles(): VehicleDao
    abstract fun trips(): TripDao
    abstract fun samples(): SampleDao
    abstract fun events(): EventDao

    companion object {
        @Volatile private var instance: EcoDb? = null
        lateinit var appContext: Context
            private set

        /** Called from ObdViewModel before any screen or the trip recorder needs the DB. */
        fun init(context: Context) = synchronized(this) {
            if (instance == null) {
                appContext = context.applicationContext
                instance = Room.databaseBuilder(appContext, EcoDb::class.java, DB_NAME).build()
                activeVehicleId = ecoPrefs(appContext).getLong(KEY_ACTIVE_VEHICLE, 0L)
            }
        }

        val db: EcoDb get() = checkNotNull(instance) { "EcoDb.init() not called" }
    }
}

/** 0 until ensureActiveVehicle() runs. Room ids start at 1. */
var activeVehicleId by mutableStateOf(0L)
    private set

fun setActiveVehicle(id: Long) {
    activeVehicleId = id
    ecoPrefs(EcoDb.appContext).edit { putLong(KEY_ACTIVE_VEHICLE, id) }
}

private val vehicleLock = Mutex()

/** Returns a valid active vehicle id, creating "My car" when there are no vehicles. */
suspend fun ensureActiveVehicle(): Long = vehicleLock.withLock {
    val dao = EcoDb.db.vehicles()
    if (activeVehicleId != 0L && dao.byId(activeVehicleId) != null) {
        activeVehicleId
    } else {
        val id = dao.firstId() ?: dao.insert(
            Vehicle(make = "My car", model = "", year = 0, tankGal = 12.0, baselineMpg = 25.0, fuelPricePerGal = 3.50)
        )
        setActiveVehicle(id)
        id
    }
}
