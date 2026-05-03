package com.carcomplianceapp.data.repository

import com.carcomplianceapp.data.local.entity.CarEntity
import com.carcomplianceapp.data.local.entity.ComplianceTaskEntity
import com.carcomplianceapp.data.remote.AiTask
import com.carcomplianceapp.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE
private val gson = Gson()

fun CarEntity.toDomain(): Car = Car(
    id = id,
    nickname = nickname,
    make = make,
    model = model,
    year = year,
    fuelType = runCatching { FuelType.valueOf(fuelType) }.getOrDefault(FuelType.UNKNOWN),
    countryCode = countryCode,
    lastServiceDate = lastServiceDate?.let { runCatching { LocalDate.parse(it, DATE_FMT) }.getOrNull() },
    insuranceExpiry = insuranceExpiry?.let { runCatching { LocalDate.parse(it, DATE_FMT) }.getOrNull() },
    registrationExpiry = registrationExpiry?.let { runCatching { LocalDate.parse(it, DATE_FMT) }.getOrNull() },
    odometerKm = odometerKm,
    attachedDocumentPaths = runCatching {
        gson.fromJson<List<String>>(attachedDocumentPaths, object : TypeToken<List<String>>() {}.type)
    }.getOrDefault(emptyList()),
    createdAt = runCatching { LocalDate.parse(createdAt, DATE_FMT) }.getOrDefault(LocalDate.now())
)

fun Car.toEntity(): CarEntity = CarEntity(
    id = id,
    nickname = nickname,
    make = make,
    model = model,
    year = year,
    fuelType = fuelType.name,
    countryCode = countryCode,
    lastServiceDate = lastServiceDate?.format(DATE_FMT),
    insuranceExpiry = insuranceExpiry?.format(DATE_FMT),
    registrationExpiry = registrationExpiry?.format(DATE_FMT),
    odometerKm = odometerKm,
    attachedDocumentPaths = gson.toJson(attachedDocumentPaths),
    createdAt = createdAt.format(DATE_FMT)
)

fun ComplianceTaskEntity.toDomain(): ComplianceTask = ComplianceTask(
    id = id,
    carId = carId,
    title = title,
    category = runCatching { TaskCategory.valueOf(category) }.getOrDefault(TaskCategory.MAINTENANCE),
    dueDate = dueDate?.let { runCatching { LocalDate.parse(it, DATE_FMT) }.getOrNull() },
    dueDateWindow = dueDateWindow,
    status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.UPCOMING),
    urgency = runCatching { UrgencyLevel.valueOf(urgency) }.getOrDefault(UrgencyLevel.LOW),
    why = why,
    isUserEditable = isUserEditable,
    isUserOverride = isUserOverride,
    snoozedUntil = snoozedUntil?.let { runCatching { LocalDate.parse(it, DATE_FMT) }.getOrNull() },
    completedAt = completedAt?.let { runCatching { LocalDate.parse(it, DATE_FMT) }.getOrNull() },
    createdAt = runCatching { LocalDate.parse(createdAt, DATE_FMT) }.getOrDefault(LocalDate.now())
)

fun ComplianceTask.toEntity(): ComplianceTaskEntity = ComplianceTaskEntity(
    id = id,
    carId = carId,
    title = title,
    category = category.name,
    dueDate = dueDate?.format(DATE_FMT),
    dueDateWindow = dueDateWindow,
    status = status.name,
    urgency = urgency.name,
    why = why,
    isUserEditable = isUserEditable,
    isUserOverride = isUserOverride,
    snoozedUntil = snoozedUntil?.format(DATE_FMT),
    completedAt = completedAt?.format(DATE_FMT),
    createdAt = createdAt.format(DATE_FMT)
)

fun AiTask.toDomain(carId: Long): ComplianceTask {
    val today = LocalDate.now()
    val parsedDue = dueDate?.let { runCatching { LocalDate.parse(it, DATE_FMT) }.getOrNull() }
    val urgencyLevel = when {
        urgency.uppercase() == "CRITICAL" -> UrgencyLevel.CRITICAL
        urgency.uppercase() == "HIGH" -> UrgencyLevel.HIGH
        urgency.uppercase() == "MEDIUM" -> UrgencyLevel.MEDIUM
        parsedDue != null && parsedDue.isBefore(today) -> UrgencyLevel.CRITICAL
        parsedDue != null && parsedDue.isBefore(today.plusDays(7)) -> UrgencyLevel.HIGH
        parsedDue != null && parsedDue.isBefore(today.plusDays(30)) -> UrgencyLevel.MEDIUM
        else -> UrgencyLevel.LOW
    }
    val status = when {
        parsedDue != null && parsedDue.isBefore(today) -> TaskStatus.OVERDUE
        else -> TaskStatus.UPCOMING
    }
    return ComplianceTask(
        carId = carId,
        title = title,
        category = runCatching { TaskCategory.valueOf(category.uppercase()) }.getOrDefault(TaskCategory.MAINTENANCE),
        dueDate = parsedDue,
        dueDateWindow = dueDateWindow.ifBlank { dueDate ?: "To be determined" },
        status = status,
        urgency = urgencyLevel,
        why = why,
        isUserEditable = true
    )
}
