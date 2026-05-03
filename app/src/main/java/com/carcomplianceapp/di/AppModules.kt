package com.carcomplianceapp.di

import android.content.Context
import androidx.room.Room
import com.carcomplianceapp.data.local.CarComplianceDatabase
import com.carcomplianceapp.data.local.dao.CarDao
import com.carcomplianceapp.data.local.dao.ComplianceTaskDao
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CarComplianceDatabase =
        Room.databaseBuilder(
            context,
            CarComplianceDatabase::class.java,
            CarComplianceDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun provideCarDao(db: CarComplianceDatabase): CarDao = db.carDao()

    @Provides
    fun provideTaskDao(db: CarComplianceDatabase): ComplianceTaskDao = db.taskDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
