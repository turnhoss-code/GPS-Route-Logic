package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.DamageEvent
import com.example.data.model.DiagnosticScanRecord
import com.example.data.model.MaintenanceTask
import com.example.data.model.SavedRouteRecord
import com.example.data.model.TripLog
import com.example.data.model.VehicleProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM diagnostic_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<DiagnosticScanRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: DiagnosticScanRecord): Long

    @Query("DELETE FROM diagnostic_scans WHERE id = :id")
    suspend fun deleteScan(id: Long)

    @Query("DELETE FROM diagnostic_scans")
    suspend fun clearAllScans()
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicle_profiles ORDER BY isDefault DESC, id DESC")
    fun getAllVehicles(): Flow<List<VehicleProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleProfile): Long

    @Query("UPDATE vehicle_profiles SET isDefault = 0")
    suspend fun clearDefaultVehicles()

    @Query("UPDATE vehicle_profiles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultVehicle(id: Long)

    @Query("DELETE FROM vehicle_profiles WHERE id = :id")
    suspend fun deleteVehicle(id: Long)

    @Query("DELETE FROM vehicle_profiles")
    suspend fun clearAllVehicles()

    @Query("SELECT COUNT(*) FROM vehicle_profiles")
    suspend fun getVehicleCount(): Int
}

@Dao
interface RouteDao {
    @Query("SELECT * FROM saved_routes ORDER BY timestamp DESC")
    fun getAllSavedRoutes(): Flow<List<SavedRouteRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SavedRouteRecord): Long

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun deleteRoute(id: Long)

    @Query("DELETE FROM saved_routes")
    suspend fun clearAllRoutes()
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_tasks")
    fun getAllTasks(): Flow<List<MaintenanceTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: MaintenanceTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<MaintenanceTask>)

    @Query("UPDATE maintenance_tasks SET lastCompletedMiles = :miles, lastCompletedDate = :timestamp WHERE id = :id")
    suspend fun markCompleted(id: String, miles: Int, timestamp: Long)

    @Query("DELETE FROM maintenance_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)
}

@Dao
interface DamageDao {
    @Query("SELECT * FROM damage_events ORDER BY timestamp DESC")
    fun getAllDamageEvents(): Flow<List<DamageEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DamageEvent): Long

    @Query("DELETE FROM damage_events")
    suspend fun clearAll()
}

@Dao
interface TripLogDao {
    @Query("SELECT * FROM trip_logs ORDER BY timestamp DESC")
    fun getAllTrips(): Flow<List<TripLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripLog): Long

    @Query("DELETE FROM trip_logs")
    suspend fun clearAll()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int
}

@Database(
    entities = [
        DiagnosticScanRecord::class,
        VehicleProfile::class,
        SavedRouteRecord::class,
        MaintenanceTask::class,
        DamageEvent::class,
        TripLog::class,
        ChatMessageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun routeDao(): RouteDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun damageDao(): DamageDao
    abstract fun tripLogDao(): TripLogDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gps_route_logic_database.db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
