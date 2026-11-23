package com.latta.timelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.latta.timelog.data.AppUsage
import com.latta.timelog.data.UsageRepository
import com.latta.timelog.tracking.SleepStateStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF22C55E)
private val Red = Color(0xFFEF4444)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TimeLogApp() }
    }
}

@Composable
fun TimeLogApp() {
    val context = LocalContext.current
    var dark by remember { mutableStateOf(true) }
    val bg = if (dark) Color.Black else Color(0xFFF5F7FA)
    val surface = if (dark) Color(0xFF101210) else Color.White
    val text = if (dark) Color.White else Color(0xFF17191D)
    val muted = if (dark) Color(0xFF929A93) else Color(0xFF737B87)
    var page by remember { mutableStateOf("Dashboard") }
    var expanded by remember { mutableStateOf(false) }
    var usage by remember { mutableStateOf(emptyList<AppUsage>()) }
    var hasAccess by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val repo = remember { UsageRepository(context) }
    val sleep = remember { SleepStateStore(context) }

    LaunchedEffect(refresh) {
        hasAccess = repo.hasUsageAccess()
        if (hasAccess) usage = repo.todayUsage()
    }

    MaterialTheme(colorScheme = if (dark) darkColorScheme(
        background = bg, surface = surface, primary = Green, onBackground = text, onSurface = text
    ) else lightColorScheme(
        background = bg, surface = surface, primary = Color(0xFF16A34A), onBackground = text, onSurface = text
    )) {
        Row(Modifier.fillMaxSize().background(bg)) {
            Column(
                Modifier.width(if (expanded) 208.dp else 64.dp).fillMaxHeight()
                    .background(surface).padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.Menu, null, tint = text)
                }
                Spacer(Modifier.height(12.dp))
                val nav = listOf(
                    "Dashboard" to Icons.Default.Dashboard,
                    "App Usage" to Icons.Default.Smartphone,
                    "Hourly Timeline" to Icons.Default.Schedule,
                    "Usage History" to Icons.Default.CalendarMonth,
                    "Analytics" to Icons.Default.BarChart,
                    "Sleep Tracking" to Icons.Default.Bedtime,
                    "Awaken Records" to Icons.Default.WbSunny,
                    "Sleep History" to Icons.Default.History,
                    "Smart Alerts" to Icons.Default.Notifications,
                    "Usage Limits" to Icons.Default.Flag,
                    "Focus Rules" to Icons.Default.Shield,
                    "Backup & Restore" to Icons.Default.Storage,
                    "Settings" to Icons.Default.Settings,
                    "About TIMELOG" to Icons.Default.Info
                )
                nav.forEach { (name, icon) ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp)
                            .background(if (page == name) Green.copy(alpha = 0.14f) else Color.Transparent,
                                RoundedCornerShape(12.dp))
                            .clickable { page = name }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
                    ) {
                        Icon(icon, null, tint = if (page == name) Green else muted)
                        if (expanded) {
                            Spacer(Modifier.width(10.dp))
                            Text(name, color = text, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { dark = !dark }) {
                    Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, null, tint = muted)
                }
            }

            Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("TIMELOG", color = text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(page.uppercase(), color = muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    }
                    IconButton(onClick = { refresh++ }) { Icon(Icons.Default.Refresh, null, tint = text) }
                }
                when (page) {
                    "Dashboard" -> DashboardScreen(usage, hasAccess, repo, sleep, text, muted, surface, onRefresh = { refresh++ })
                    "App Usage" -> AppUsageScreen(usage, text, muted, surface)
                    "Sleep Tracking", "Awaken Records", "Sleep History" ->
                        SleepScreen(sleep, text, muted, surface, page)
                    "Hourly Timeline" -> InfoScreen("Hourly Timeline", "Hourly breakdown is being prepared from system usage events.", text, muted, surface)
                    "Usage History" -> InfoScreen("Usage History", "Lifetime local history and date filters.", text, muted, surface)
                    "Analytics" -> InfoScreen("Analytics", "Daily, weekly and monthly usage trends.", text, muted, surface)
                    "Backup & Restore" -> BackupScreen(text, muted, surface)
                    "Settings" -> SettingsScreen(text, muted, surface, onTheme = { dark = it })
                    else -> InfoScreen(page, "TIMELOG • Offline-first digital wellbeing", text, muted, surface)
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    usage: List<AppUsage>, access: Boolean, repo: UsageRepository, sleep: SleepStateStore,
    text: Color, muted: Color, surface: Color, onRefresh: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Your day at a glance", color = muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            val total = usage.sumOf { it.usageMillis }
            StatCard("SCREEN TIME", formatDuration(total), "Today's foreground app usage", text, muted, surface)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { StatCard("SLEEP START", formatTime(sleep.sleepStart()), "Estimated", text, muted, surface) }
                Box(Modifier.weight(1f)) { StatCard("AWAKEN", formatTime(sleep.awakenAt()), "Estimated", text, muted, surface) }
            }
        }
        if (!access) item {
            Card(colors = CardDefaults.cardColors(containerColor = surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Usage access required", color = text, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Allow TIMELOG to access app usage statistics to show app time and opening counts.", color = muted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { repo.openUsageAccessSettings() }) { Text("Grant Usage Access") }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Most used apps", color = text, fontWeight = FontWeight.SemiBold)
                Text("TODAY", color = Green, fontSize = 10.sp)
            }
        }
        items(usage.take(5)) { app ->
            AppUsageRow(app, text, muted, surface)
        }
        item {
            Text("Tracking note: Android reports app foreground time and launch events. Exact touch-level interaction is not available to ordinary apps.", color = muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AppUsageScreen(usage: List<AppUsage>, text: Color, muted: Color, surface: Color) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Today's app activity", color = muted, fontSize = 13.sp) }
        items(usage) { AppUsageRow(it, text, muted, surface) }
    }
}

@Composable
private fun AppUsageRow(app: AppUsage, text: Color, muted: Color, surface: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(app.appName, color = text, fontWeight = FontWeight.Medium)
                Text("${app.launchCount} openings", color = muted, fontSize = 11.sp)
            }
            Text(formatDuration(app.usageMillis), color = Green, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SleepScreen(sleep: SleepStateStore, text: Color, muted: Color, surface: Color, page: String) {
    val start = sleep.sleepStart()
    val wake = sleep.awakenAt()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("SLEEP START", formatTime(start), "Confirmed after 65 minutes of screen inactivity", text, muted, surface)
        StatCard("AWAKEN TIME", formatTime(wake), "First unlock candidate; confirmation is approximate", text, muted, surface)
        StatCard("SLEEP WINDOW", if (start > 0 && wake > start) formatDuration(wake - start) else "—", "Phone inactivity window, not medical sleep measurement", text, muted, surface)
        Text("Sleep records are estimates. Notifications and network traffic do not count as unlocks. Android broadcasts may be delayed or restricted by device manufacturers.", color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun StatCard(title: String, value: String, subtitle: String, text: Color, muted: Color, surface: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
            Text(value, color = text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoScreen(title: String, description: String, text: Color, muted: Color, surface: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(title, "Coming next", description, text, muted, surface)
        Text("This starter project establishes the navigation and visual system. This section is a development placeholder.", color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun BackupScreen(text: Color, muted: Color, surface: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("LOCAL DATA", "On this device", "No cloud account required", text, muted, surface)
        Text("Backup and restore actions will use Android's Storage Access Framework so you choose the local file location.", color = muted, fontSize = 12.sp)
        Text("Backup export/import is scaffolded in this first build and requires the database schema to be finalized.", color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsScreen(text: Color, muted: Color, surface: Color, onTheme: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Appearance", color = text, fontWeight = FontWeight.SemiBold)
        Card(colors = CardDefaults.cardColors(containerColor = surface)) {
            Column(Modifier.padding(14.dp)) {
                Text("Dark theme", color = text)
                Text("Use Pure Black OLED appearance", color = muted, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onTheme(false) }) { Text("Frost White") }
                    Button(onClick = { onTheme(true) }) { Text("Pure Black") }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0m"
    val mins = ms / 60000
    val h = mins / 60
    val m = mins % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
private fun formatTime(ms: Long): String =
    if (ms <= 0) "Not recorded" else SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ms))
