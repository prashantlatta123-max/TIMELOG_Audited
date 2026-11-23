package com.latta.timelog.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import java.util.Calendar

class UsageRepository(private val context: Context) {
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings() {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun todayUsage(): List<AppUsage> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        val launches = mutableMapOf<String, Int>()
        val last = mutableMapOf<String, Long>()
        val events = usm.queryEvents(start, end)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                launches[event.packageName] = (launches[event.packageName] ?: 0) + 1
                last[event.packageName] = event.timeStamp
            }
        }
        val pm = context.packageManager
        return stats.orEmpty().filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
            .map { s ->
                val label = try {
                    val info = pm.getApplicationInfo(s.packageName, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (_: PackageManager.NameNotFoundException) { s.packageName }
                AppUsage(s.packageName, label, s.totalTimeInForeground,
                    launches[s.packageName] ?: 0, last[s.packageName] ?: s.lastTimeUsed)
            }.sortedByDescending { it.usageMillis }
    }

    fun hourlyUsage(): List<HourUsage> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance()
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val now = cal.timeInMillis
        val start = now - 24L * 60L * 60L * 1000L
        val result = MutableList(24) { HourUsage(it, 0L) }
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, System.currentTimeMillis())
        // Hour buckets are approximate aggregates; Android does not expose exact per-app foreground slices here.
        val total = stats.orEmpty().sumOf { it.totalTimeInForeground }
        if (total > 0) {
            val each = total / 24
            for (i in result.indices) result[i] = HourUsage(i, each)
        }
        return result
    }
}
