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
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Configuration
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
class CarComplianceApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        DeadlineNotificationWorker.createNotificationChannel(this)
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
        DeadlineNotificationWorker.scheduleDaily(this)
        setContent {
            CarComplianceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startViewModel: StartViewModel = hiltViewModel()
                    val onboardingDone by startViewModel.onboardingDone.collectAsState()

                    when (val done = onboardingDone) {
                        null -> {}
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
