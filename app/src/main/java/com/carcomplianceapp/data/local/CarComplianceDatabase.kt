package com.carcomplianceapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.carcomplianceapp.data.local.dao.CarDao
import com.carcomplianceapp.data.local.dao.ComplianceTaskDao
import com.carcomplianceapp.data.local.entity.CarEntity
import com.carcomplianceapp.data.local.entity.ComplianceTaskEntity

@Database(
    entities = [CarEntity::class, ComplianceTaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CarComplianceDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun taskDao(): ComplianceTaskDao

    companion object {
        const val DATABASE_NAME = "car_compliance.db"
    }
}
