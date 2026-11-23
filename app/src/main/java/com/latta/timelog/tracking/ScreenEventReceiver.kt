package com.latta.timelog.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class ScreenEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = SleepStateStore(context)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reinitialize persistent tracking state after device restart.
                // Screen on/off broadcasts require a runtime-registered receiver.
            }
            Intent.ACTION_SCREEN_OFF -> {
                val now = System.currentTimeMillis()
                store.setScreenOffAt(now)
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<SleepConfirmWorker>()
                        .setInitialDelay(65, TimeUnit.MINUTES).build()
                )
            }
            Intent.ACTION_USER_PRESENT -> {
                // First unlock is only a candidate. Confirm awaken after sustained foreground use.
                store.setPendingWakeAt(System.currentTimeMillis())
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<WakeConfirmWorker>()
                        .setInitialDelay(5, TimeUnit.MINUTES).build()
                )
            }
        }
    }
}

class SleepConfirmWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = SleepStateStore(applicationContext)
        val off = store.screenOffAt()
        if (off > 0 && System.currentTimeMillis() - off >= 65 * 60 * 1000L &&
            store.pendingWakeAt() < off) {
            store.setSleepStart(off)
        }
        return Result.success()
    }
}

class WakeConfirmWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = SleepStateStore(applicationContext)
        val wake = store.pendingWakeAt()
        if (wake > 0 && wake > store.sleepStart()) {
            // Unlock is the available system-level human-presence signal.
            // A five-minute confirmation window reduces accidental wake records.
            store.setAwakenAt(wake)
        }
        return Result.success()
    }
}
