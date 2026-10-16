package com.lolipp.asthmadiary.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.lolipp.asthmadiary.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

object Reminders {
    const val CHANNEL_ID = "peakflow_reminders"
    private const val MORNING_WORK = "peakflow_morning"
    private const val EVENING_WORK = "peakflow_evening"

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = context.getString(R.string.notif_channel_desc) }
            )
        }
    }

    fun scheduleMorning(context: Context, hour: Int, minute: Int) =
        schedule(context, MORNING_WORK, hour, minute, isMorning = true)

    fun scheduleEvening(context: Context, hour: Int, minute: Int) =
        schedule(context, EVENING_WORK, hour, minute, isMorning = false)

    fun cancelMorning(context: Context) =
        WorkManager.getInstance(context).cancelUniqueWork(MORNING_WORK)

    fun cancelEvening(context: Context) =
        WorkManager.getInstance(context).cancelUniqueWork(EVENING_WORK)

    private fun schedule(context: Context, name: String, hour: Int, minute: Int, isMorning: Boolean) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("isMorning" to isMorning))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

class ReminderWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        val isMorning = inputData.getBoolean("isMorning", true)
        val ctx = applicationContext
        Reminders.ensureChannel(ctx)

        val title = if (isMorning) ctx.getString(R.string.notif_morning_title)
                    else ctx.getString(R.string.notif_evening_title)
        val text = if (isMorning) ctx.getString(R.string.notif_morning_text)
                   else ctx.getString(R.string.notif_evening_text)

        val notif = NotificationCompat.Builder(ctx, Reminders.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(if (isMorning) 1001 else 1002, notif)
        } catch (_: SecurityException) { }
        return Result.success()
    }
}
