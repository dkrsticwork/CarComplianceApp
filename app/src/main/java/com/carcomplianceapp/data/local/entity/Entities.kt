package com.carcomplianceapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,
    val make: String,
    val model: String,
    val year: Int,
    val fuelType: String,
    val countryCode: String,
    val lastServiceDate: String?,       // ISO date string
    val insuranceExpiry: String?,
    val registrationExpiry: String?,
    val odometerKm: Int?,
    val attachedDocumentPaths: String,  // JSON array
    val createdAt: String
)

@Entity(
    tableName = "compliance_tasks",
    foreignKeys = [ForeignKey(
        entity = CarEntity::class,
        parentColumns = ["id"],
        childColumns = ["carId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("carId")]
)
data class ComplianceTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val title: String,
    val category: String,
    val dueDate: String?,
    val dueDateWindow: String,
    val status: String,
    val urgency: String,
    val why: String,
    val isUserEditable: Boolean,
    val isUserOverride: Boolean,
    val snoozedUntil: String?,
    val completedAt: String?,
    val createdAt: String
)
