package com.music.bitchord.data

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.music.bitchord.MainActivity
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Keeps release checks periodic while the setting is on and reports a new release once. */
object AppUpdateWork {
    private const val WORK_NAME = "soundchord_release_check"
    private const val CHANNEL_ID = "app_updates"
    private const val PREFS = "app_update_notifications"
    private const val KEY_NOTIFIED_VERSION = "notified_version"

    fun init(app: Application) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            AppSettings.autoUpdateCheck.collect { enabled ->
                if (enabled) schedule(app) else WorkManager.getInstance(app).cancelUniqueWork(WORK_NAME)
            }
        }
    }

    private fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(24, TimeUnit.HOURS, 4, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    internal fun notifyIfNew(context: Context, version: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_NOTIFIED_VERSION, null) == version) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.about_updates), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val launch = PendingIntent.getActivity(
            context,
            16,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(context.getString(R.string.update_available, version))
            .setContentText(context.getString(R.string.update_available_body, version))
            .setContentIntent(launch)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(16, notification)
        prefs.edit().putString(KEY_NOTIFIED_VERSION, version).apply()
    }
}

class AppUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!AppSettings.autoUpdateCheck.value) return Result.success()
        return when (AppUpdateChecker.check()) {
            AppUpdateChecker.CheckResult.UPDATE_AVAILABLE -> {
                AppUpdateChecker.available.value?.let { AppUpdateWork.notifyIfNew(applicationContext, it.version) }
                Result.success()
            }
            AppUpdateChecker.CheckResult.UP_TO_DATE -> Result.success()
            AppUpdateChecker.CheckResult.FAILED -> Result.retry()
        }
    }
}
