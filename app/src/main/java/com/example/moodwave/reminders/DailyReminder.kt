package com.example.moodwave.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.moodwave.MainActivity
import java.time.ZonedDateTime

object ReminderSchedule {
    fun next(hour: Int, minute: Int, now: ZonedDateTime): ZonedDateTime {
        require(hour in 0..23 && minute in 0..59)
        val today = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(hour, minute).atZone(now.zone)
    }
}

object DailyReminder {
    private const val CHANNEL = "daily_check_in"
    private const val ID = 4001
    fun preferences(context: Context) = context.getSharedPreferences("reminder", Context.MODE_PRIVATE)
    fun permitted(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE &&
            (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    fun schedule(context: Context) {
        val prefs = preferences(context)
        val manager = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(context, ID,
            Intent(context, ReminderReceiver::class.java).setAction("com.example.moodwave.REMIND"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.cancel(pending)
        if (prefs.getBoolean("enabled", false)) {
            val next = ReminderSchedule.next(prefs.getInt("hour", 20), prefs.getInt("minute", 0), ZonedDateTime.now())
            manager.setWindow(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), 15 * 60 * 1000L, pending)
        } else context.getSystemService(NotificationManager::class.java).cancel(ID)
    }
    fun show(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Registro diario", NotificationManager.IMPORTANCE_DEFAULT))
        if (!permitted(context)) return false
        val open = PendingIntent.getActivity(context, ID,
            Intent(context, MainActivity::class.java).putExtra("open_entry", true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(ID, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_edit).setContentTitle("Un momento para ti")
            .setContentText("Si te apetece, registra cómo te sientes hoy.")
            .setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
        return true
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.moodwave.REMIND" && DailyReminder.preferences(context).getBoolean("enabled", false)) {
            DailyReminder.show(context)
        }
        DailyReminder.schedule(context)
    }
}
