package com.carcomplianceapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.carcomplianceapp.domain.model.AiProvider
import com.carcomplianceapp.domain.model.ApiKeyConfig
import com.carcomplianceapp.domain.model.NotificationPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "car_compliance_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_API_PROVIDER = stringPreferencesKey("api_provider")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_ACTIVE_CAR_ID = longPreferencesKey("active_car_id")
        val KEY_NOTIF_30 = booleanPreferencesKey("notif_30")
        val KEY_NOTIF_7 = booleanPreferencesKey("notif_7")
        val KEY_NOTIF_1 = booleanPreferencesKey("notif_1")
        val KEY_LAST_API_ERROR = stringPreferencesKey("last_api_error")
    }

    val apiKeyConfig: Flow<ApiKeyConfig?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val key = prefs[KEY_API_KEY] ?: return@map null
            val providerName = prefs[KEY_API_PROVIDER] ?: return@map null
            val provider = runCatching { AiProvider.valueOf(providerName) }.getOrNull() ?: return@map null
            ApiKeyConfig(rawKey = key, provider = provider, isValid = true)
        }

    val onboardingDone: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_ONBOARDING_DONE] ?: false }

    val activeCarId: Flow<Long?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_ACTIVE_CAR_ID] }

    val notificationPreferences: Flow<NotificationPreferences> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            NotificationPreferences(
                thirtyDays = prefs[KEY_NOTIF_30] ?: true,
                sevenDays = prefs[KEY_NOTIF_7] ?: true,
                oneDay = prefs[KEY_NOTIF_1] ?: true
            )
        }

    val lastApiError: Flow<String?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[KEY_LAST_API_ERROR] }

    suspend fun saveApiKey(key: String, provider: AiProvider) {
        dataStore.edit { prefs ->
            prefs[KEY_API_KEY] = key
            prefs[KEY_API_PROVIDER] = provider.name
            prefs.remove(KEY_LAST_API_ERROR)
        }
    }

    suspend fun clearApiKey() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_API_KEY)
            prefs.remove(KEY_API_PROVIDER)
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setActiveCarId(id: Long) {
        dataStore.edit { it[KEY_ACTIVE_CAR_ID] = id }
    }

    suspend fun updateNotificationPreferences(prefs: NotificationPreferences) {
        dataStore.edit {
            it[KEY_NOTIF_30] = prefs.thirtyDays
            it[KEY_NOTIF_7] = prefs.sevenDays
            it[KEY_NOTIF_1] = prefs.oneDay
        }
    }

    suspend fun saveApiError(error: String) {
        dataStore.edit { it[KEY_LAST_API_ERROR] = error }
    }

    suspend fun clearApiError() {
        dataStore.edit { it.remove(KEY_LAST_API_ERROR) }
    }
}
