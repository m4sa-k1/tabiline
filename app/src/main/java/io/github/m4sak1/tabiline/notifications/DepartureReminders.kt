package io.github.m4sak1.tabiline.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.m4sak1.tabiline.MainActivity
import io.github.m4sak1.tabiline.R
import io.github.m4sak1.tabiline.TabilineApplication
import io.github.m4sak1.tabiline.core.model.*
import io.github.m4sak1.tabiline.ui.components.departureBoardingLabel
import io.github.m4sak1.tabiline.ui.components.serviceLabel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.format.DateTimeFormatter

internal fun reminderTime(leg: TransportLeg, settings: UserSettings): Instant =
    leg.departure.minusSeconds((settings.notificationMinutes[leg.mode] ?: 10).coerceIn(0, 999) * 60L)

internal fun shouldDeliver(leg: TransportLeg, settings: UserSettings, scheduledAt: Long, now: Instant): Boolean =
    settings.notificationsEnabled && now < leg.departure.plusSeconds(60) &&
        reminderTime(leg, settings).toEpochMilli() == scheduledAt && now >= reminderTime(leg, settings)

class DepartureReminders(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val registry = context.getSharedPreferences("departure_alarms", Context.MODE_PRIVATE)

    fun exactAllowed() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    private fun operation(id: Long, time: Long = 0) = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, DepartureReminderReceiver::class.java)
            .setAction(ACTION_REMIND).setData(Uri.parse("tabiline://reminder/$id"))
            .putExtra("legId", id).putExtra("scheduledAt", time),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    @Synchronized
    fun sync(trips: List<TripWithLegs>, settings: UserSettings) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "出発のお知らせ", NotificationManager.IMPORTANCE_HIGH))
        val old = registry.getStringSet("ids", emptySet()).orEmpty().toSet()
        val now = Instant.now()
        val upcoming = if (settings.notificationsEnabled) trips.flatMap { it.legs }
            .filter { reminderTime(it, settings) > now } else emptyList()
        val ids = upcoming.map { it.id.toString() }.toSet()
        // Persist the union first so an interrupted reschedule can always cancel stale alarms.
        registry.edit().putStringSet("ids", old + ids).commit()
        old.forEach { alarms.cancel(operation(it.toLong())) }
        if (!settings.notificationsEnabled) manager.cancelAll()
        upcoming.forEach { leg ->
            val time = reminderTime(leg, settings).toEpochMilli()
            val intent = operation(leg.id, time)
            try {
                if (exactAllowed()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
                else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
            } catch (_: SecurityException) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
            }
        }
        registry.edit().putStringSet("ids", ids).commit()
    }

    fun show(leg: TransportLeg) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java).setAction(ACTION_TODAY)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val route = if (leg.mode == TransportMode.FREE_TIME) leg.departurePlace.ifBlank { "空き時間" }
            else "${leg.departurePlace} → ${leg.arrivalPlace}"
        val text = listOf(leg.serviceLabel, leg.departureBoardingLabel).filter(String::isNotBlank).joinToString(" ・ ")
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("${leg.departureLocal.format(DateTimeFormatter.ofPattern("HH:mm"))} $route")
            .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        try { context.getSystemService(NotificationManager::class.java).notify("leg-${leg.id}", 0, notification) }
        catch (_: SecurityException) { /* Notification permission may have been revoked. */ }
    }

    companion object {
        const val CHANNEL = "departures"
        const val ACTION_REMIND = "io.github.m4sak1.tabiline.REMIND"
        const val ACTION_TODAY = "io.github.m4sak1.tabiline.OPEN_TODAY"
    }
}

class DepartureReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_000) {
                    val container = (context.applicationContext as TabilineApplication).container
                    val settings = container.settings.settings.first()
                    if (intent.action == DepartureReminders.ACTION_REMIND) {
                        val leg = container.trips.getLeg(intent.getLongExtra("legId", -1))
                        val now = Instant.now()
                        if (leg != null && shouldDeliver(leg, settings, intent.getLongExtra("scheduledAt", -1), now)) container.reminders.show(leg)
                    } else {
                        container.reminders.sync(container.trips.observeTrips().first(), settings)
                    }
                }
            } catch (error: Exception) {
                android.util.Log.w("DepartureReminder", "Unable to process departure reminder", error)
            } finally { pending.finish() }
        }
    }
}
