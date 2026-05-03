package com.carcomplianceapp.data.local.dao

import androidx.room.*
import com.carcomplianceapp.data.local.entity.CarEntity
import com.carcomplianceapp.data.local.entity.ComplianceTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {

    @Query("SELECT * FROM cars ORDER BY createdAt DESC")
    fun getAllCars(): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars WHERE id = :id")
    suspend fun getCarById(id: Long): CarEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: CarEntity): Long

    @Update
    suspend fun updateCar(car: CarEntity)

    @Delete
    suspend fun deleteCar(car: CarEntity)

    @Query("DELETE FROM cars WHERE id = :id")
    suspend fun deleteCarById(id: Long)
}

@Dao
interface ComplianceTaskDao {

    @Query("SELECT * FROM compliance_tasks WHERE carId = :carId ORDER BY dueDate IS NULL ASC, dueDate ASC")
    fun getTasksForCar(carId: Long): Flow<List<ComplianceTaskEntity>>

    @Query("SELECT * FROM compliance_tasks WHERE carId = :carId AND status != 'DONE' ORDER BY dueDate IS NULL ASC, dueDate ASC")
    fun getActiveTasksForCar(carId: Long): Flow<List<ComplianceTaskEntity>>

    @Query("SELECT * FROM compliance_tasks WHERE status = 'UPCOMING' OR status = 'OVERDUE' ORDER BY dueDate IS NULL ASC, dueDate ASC")
    fun getAllActiveTasks(): Flow<List<ComplianceTaskEntity>>

    @Query("SELECT * FROM compliance_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): ComplianceTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ComplianceTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<ComplianceTaskEntity>)

    @Update
    suspend fun updateTask(task: ComplianceTaskEntity)

    @Query("UPDATE compliance_tasks SET status = 'DONE', completedAt = :completedAt WHERE id = :id")
    suspend fun markDone(id: Long, completedAt: String)

    @Query("UPDATE compliance_tasks SET status = 'SNOOZED', snoozedUntil = :until WHERE id = :id")
    suspend fun snoozeTask(id: Long, until: String)

    @Query("DELETE FROM compliance_tasks WHERE carId = :carId")
    suspend fun deleteTasksForCar(carId: Long)

    @Query("DELETE FROM compliance_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)
}
