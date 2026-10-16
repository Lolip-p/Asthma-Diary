package com.lolipp.asthmadiary.notif

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.lolipp.asthmadiary.R
import com.lolipp.asthmadiary.data.AppDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

object Reminders {
    const val CHANNEL_ID = "peakflow_reminders"
    private const val MORNING_WORK = "peakflow_morning"
    private const val EVENING_WORK = "peakflow_evening"
    private const val ABSENT_WORK = "peakflow_absent"

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
        schedule(context, MORNING_WORK, hour, minute, WorkerKind.MORNING)

    fun scheduleEvening(context: Context, hour: Int, minute: Int) =
        schedule(context, EVENING_WORK, hour, minute, WorkerKind.EVENING)

    fun cancelMorning(context: Context) =
        WorkManager.getInstance(context).cancelUniqueWork(MORNING_WORK)

    fun cancelEvening(context: Context) =
        WorkManager.getInstance(context).cancelUniqueWork(EVENING_WORK)

    /** Каждый день в 21:00 проверяем: если сегодня не было замеров — пинаем. */
    fun scheduleAbsentCheck(context: Context, enabled: Boolean) {
        if (!enabled) {
            WorkManager.getInstance(context).cancelUniqueWork(ABSENT_WORK)
            return
        }
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        val delay = target.timeInMillis - now.timeInMillis
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("kind" to WorkerKind.ABSENT.name))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(ABSENT_WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    private fun schedule(
        context: Context, name: String, hour: Int, minute: Int, kind: WorkerKind
    ) {
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
            .setInputData(workDataOf("kind" to kind.name))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

enum class WorkerKind { MORNING, EVENING, ABSENT }

class ReminderWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        val kindStr = inputData.getString("kind") ?: WorkerKind.MORNING.name
        val kind = try { WorkerKind.valueOf(kindStr) } catch (_: Exception) { WorkerKind.MORNING }
        val ctx = applicationContext
        Reminders.ensureChannel(ctx)

        when (kind) {
            WorkerKind.MORNING -> notify(ctx, ctx.getString(R.string.notif_morning_title),
                ctx.getString(R.string.notif_morning_text), 1001)
            WorkerKind.EVENING -> notify(ctx, ctx.getString(R.string.notif_evening_title),
                ctx.getString(R.string.notif_evening_text), 1002)
            WorkerKind.ABSENT -> {
                val since = startOfToday()
                val count = kotlinx.coroutines.runBlocking {
                    com.lolipp.asthmadiary.data.AppDatabase.get(ctx).peakFlowDao().countSince(since)
                }
                if (count == 0) {
                    notify(ctx, ctx.getString(R.string.notif_absent_title),
                        ctx.getString(R.string.notif_absent_text), 1003)
                }
            }
        }
        return Result.success()
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun notify(ctx: Context, title: String, text: String, id: Int) {
        val notif = NotificationCompat.Builder(ctx, Reminders.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, notif)
        } catch (_: SecurityException) { }
    }
}
