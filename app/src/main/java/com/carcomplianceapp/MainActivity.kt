package com.carcomplianceapp

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carcomplianceapp.data.local.PreferencesManager
import com.carcomplianceapp.ui.AppNavGraph
import com.carcomplianceapp.ui.Routes
import com.carcomplianceapp.ui.theme.CarComplianceTheme
import com.carcomplianceapp.worker.DeadlineNotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltAndroidApp
class CarComplianceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DeadlineNotificationWorker.createNotificationChannel(this)
        DeadlineNotificationWorker.scheduleDaily(this)
    }
}

@HiltViewModel
class StartViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    val onboardingDone: StateFlow<Boolean?> = preferencesManager.onboardingDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarComplianceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startViewModel: StartViewModel = hiltViewModel()
                    val onboardingDone by startViewModel.onboardingDone.collectAsState()

                    // Wait for prefs to load before choosing start destination
                    when (val done = onboardingDone) {
                        null -> {} // splash / loading (blank is fine for MVP)
                        else -> {
                            val start = if (done) Routes.MAIN else Routes.WELCOME
                            AppNavGraph(startDestination = start)
                        }
                    }
                }
            }
        }
    }
}
