package com.latta.timelog.data

data class AppUsage(
    val packageName: String,
    val appName: String,
    val usageMillis: Long,
    val launchCount: Int,
    val lastUsed: Long
)

data class HourUsage(val hour: Int, val millis: Long)
