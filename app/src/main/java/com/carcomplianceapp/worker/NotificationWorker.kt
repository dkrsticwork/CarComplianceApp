package com.carcomplianceapp.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.carcomplianceapp.MainActivity
import com.carcomplianceapp.R
import com.carcomplianceapp.data.local.PreferencesManager
import com.carcomplianceapp.data.repository.TaskRepository
import com.carcomplianceapp.domain.model.TaskStatus
import com.carcomplianceapp.domain.model.UrgencyLevel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltWorker
class DeadlineNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = preferencesManager.notificationPreferences.firstOrNull() ?: return Result.success()
        val tasks = taskRepository.getAllActiveTasks().firstOrNull() ?: return Result.success()
        val today = LocalDate.now()

        tasks.filter { it.status != TaskStatus.DONE && it.status != TaskStatus.SNOOZED }
            .forEach { task ->
                val dueDate = task.dueDate ?: return@forEach
                val daysUntil = ChronoUnit.DAYS.between(today, dueDate)

                val shouldNotify = when {
                    daysUntil < 0 -> true // overdue — always notify
                    daysUntil <= 1 && prefs.oneDay -> true
                    daysUntil <= 7 && prefs.sevenDays -> true
                    daysUntil <= 30 && prefs.thirtyDays -> true
                    else -> false
                }

                if (shouldNotify) {
                    sendNotification(
                        context = applicationContext,
                        taskId = task.id,
                        title = task.title,
                        daysUntil = daysUntil,
                        why = task.why
                    )
                }
            }

        return Result.success()
    }

    private fun sendNotification(
        context: Context,
        taskId: Long,
        title: String,
        daysUntil: Long,
        why: String
    ) {
        try {
            val notifText = when {
                daysUntil < 0 -> "Overdue by ${-daysUntil} day${if (-daysUntil != 1L) "s" else ""}"
                daysUntil == 0L -> "Due today"
                daysUntil == 1L -> "Due tomorrow"
                else -> "Due in $daysUntil days"
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("task_id", taskId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, taskId.toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_car_notification)
                .setContentTitle("Car Compliance: $title")
                .setContentText(notifText)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$notifText\n\nWhy: $why"))
                .setPriority(if (daysUntil < 0) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
        } catch (e: SecurityException) {
            // Notification permission not granted — silently skip
        }
    }

    companion object {
        const val CHANNEL_ID = "car_compliance_channel"
        const val WORK_TAG = "deadline_notifications"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Car Compliance Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for upcoming car legal and maintenance deadlines"
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }

        fun scheduleDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<DeadlineNotificationWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        }
    }
}

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            DeadlineNotificationWorker.scheduleDaily(context)
        }
    }
}
