package com.carcomplianceapp.data.repository

import com.carcomplianceapp.data.local.PreferencesManager
import com.carcomplianceapp.data.local.dao.CarDao
import com.carcomplianceapp.data.local.dao.ComplianceTaskDao
import com.carcomplianceapp.data.remote.AiApiService
import com.carcomplianceapp.data.remote.AiResult
import com.carcomplianceapp.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarRepository @Inject constructor(
    private val carDao: CarDao
) {
    fun getAllCars(): Flow<List<Car>> = carDao.getAllCars().map { list -> list.map { it.toDomain() } }

    suspend fun getCarById(id: Long): Car? = carDao.getCarById(id)?.toDomain()

    suspend fun insertCar(car: Car): Long = carDao.insertCar(car.toEntity())

    suspend fun updateCar(car: Car) = carDao.updateCar(car.toEntity())

    suspend fun deleteCar(car: Car) = carDao.deleteCar(car.toEntity())

    suspend fun deleteCarById(id: Long) = carDao.deleteCarById(id)
}

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: ComplianceTaskDao,
    private val aiApiService: AiApiService,
    private val preferencesManager: PreferencesManager
) {
    fun getTasksForCar(carId: Long): Flow<List<ComplianceTask>> =
        taskDao.getTasksForCar(carId).map { list -> list.map { it.toDomain() } }

    fun getActiveTasksForCar(carId: Long): Flow<List<ComplianceTask>> =
        taskDao.getActiveTasksForCar(carId).map { list -> list.map { it.toDomain() } }

    fun getAllActiveTasks(): Flow<List<ComplianceTask>> =
        taskDao.getAllActiveTasks().map { list -> list.map { it.toDomain() } }

    suspend fun insertTask(task: ComplianceTask): Long = taskDao.insertTask(task.toEntity())

    suspend fun insertTasks(tasks: List<ComplianceTask>) = taskDao.insertTasks(tasks.map { it.toEntity() })

    suspend fun updateTask(task: ComplianceTask) = taskDao.updateTask(task.toEntity())

    suspend fun markDone(id: Long) {
        taskDao.markDone(id, LocalDate.now().format(DateTimeFormatter.ISO_DATE))
    }

    suspend fun snoozeTask(id: Long, days: Long = 7) {
        taskDao.snoozeTask(id, LocalDate.now().plusDays(days).format(DateTimeFormatter.ISO_DATE))
    }

    suspend fun deleteTasksForCar(carId: Long) = taskDao.deleteTasksForCar(carId)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)

    // ── AI generation ─────────────────────────────────────────────────────────

    suspend fun generateAndSaveTasks(
        car: Car,
        apiKeyConfig: ApiKeyConfig,
        documentSummaries: List<String> = emptyList()
    ): Result<Int> {
        return when (val result = aiApiService.generateComplianceTasks(car, apiKeyConfig, documentSummaries)) {
            is AiResult.Success -> {
                // Clear old AI-generated tasks (keep user overrides)
                val existing = taskDao.getActiveTasksForCar(car.id)
                val domainTasks = result.tasks.map { it.toDomain(car.id) }
                // Delete non-override tasks for this car
                taskDao.deleteTasksForCar(car.id)
                // Re-insert
                taskDao.insertTasks(domainTasks.map { it.toEntity() })
                preferencesManager.clearApiError()
                Result.success(domainTasks.size)
            }
            is AiResult.Error -> {
                preferencesManager.saveApiError(result.message)
                Result.failure(Exception(result.message))
            }
        }
    }
}
