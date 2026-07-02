package com.amine.coachhabitudes

import android.app.*
import android.content.*
import java.time.*

object ReminderScheduler {
    fun scheduleToday(context: Context, tasks: List<HabitTask>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now()
        tasks.forEach { task ->
            val dateTime = runCatching { LocalDate.now().atTime(LocalTime.parse(task.time)) }.getOrNull() ?: return@forEach
            if (dateTime.isAfter(now) && task.status == TaskStatus.TODO) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    putExtra("title", task.title)
                    putExtra("text", task.subtitle.ifBlank { "Action à faire maintenant" })
                }
                val pendingIntent = PendingIntent.getBroadcast(context, task.id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pendingIntent)
            }
        }
    }
}
