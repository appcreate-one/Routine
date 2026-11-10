package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedDataDao {
    @Query("SELECT * FROM shared_data ORDER BY timestamp DESC")
    fun getAllSharedData(): Flow<List<SharedData>>

    @Query("SELECT COUNT(*) FROM shared_data")
    fun getTotalSharedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedData(data: SharedData): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSharedData(items: List<SharedData>)

    @Update
    suspend fun updateSharedData(data: SharedData)

    @Delete
    suspend fun deleteSharedData(data: SharedData)

    @Query("DELETE FROM shared_data")
    suspend fun clearAllSharedData()
}

@Dao
interface DailyRoutineDao {
    @Query("SELECT * FROM daily_routine ORDER BY id ASC")
    fun getAllRoutines(): Flow<List<DailyRoutine>>

    @Query("SELECT * FROM daily_routine WHERE id = :taskId LIMIT 1")
    suspend fun getRoutineById(taskId: Int): DailyRoutine?

    @Query("SELECT COUNT(*) FROM daily_routine")
    suspend fun getRoutineCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: DailyRoutine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRoutines(routines: List<DailyRoutine>)

    @Update
    suspend fun updateRoutine(routine: DailyRoutine)
}
