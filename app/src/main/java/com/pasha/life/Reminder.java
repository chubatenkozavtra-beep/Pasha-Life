package com.pasha.life;

import android.app.*;
import android.content.*;
import android.os.Build;
import org.json.*;
import java.util.Calendar;

public class Reminder extends BroadcastReceiver {

    public static void saveAndSchedule(Context c, String json) {
        c.getSharedPreferences("reminders", 0)
            .edit().putString("tasks", json).apply();
        scheduleAll(c);
    }

    public static void scheduleAll(Context c) {
        AlarmManager am = (AlarmManager)
            c.getSystemService(Context.ALARM_SERVICE);

        android.content.SharedPreferences prefs =
            c.getSharedPreferences("reminders", 0);

        int oldCount = prefs.getInt("alarmCount", 0);

        for (int i = 0; i < oldCount; i++) {
            PendingIntent old = PendingIntent.getBroadcast(
                c, i, new Intent(c, Reminder.class),
                PendingIntent.FLAG_NO_CREATE |
                PendingIntent.FLAG_IMMUTABLE
            );
            if (old != null) {
                am.cancel(old);
                old.cancel();
            }
        }

        int count = 0;

        try {
            JSONArray tasks = new JSONArray(
                prefs.getString("tasks", "[]")
            );

            for (int i = 0; i < tasks.length(); i++) {
                JSONObject task = tasks.getJSONObject(i);
                String[] time = task.getString("time").split(":");
                JSONArray days = task.optJSONArray("days");

                for (int d = 0; d < 7; d++) {
                    boolean enabled = days == null;

                    if (days != null) {
                        for (int j = 0; j < days.length(); j++) {
                            if (days.optInt(j, -1) == d)
                                enabled = true;
                        }
                    }

                    if (!enabled) continue;

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY,
                        Integer.parseInt(time[0]));
                    cal.set(Calendar.MINUTE,
                        Integer.parseInt(time[1]));
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);

                    int targetDay = d + 1;

                    while (cal.get(Calendar.DAY_OF_WEEK)
                        != targetDay ||
                        cal.getTimeInMillis()
                        <= System.currentTimeMillis()) {
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    Intent intent = new Intent(c, Reminder.class);
                    intent.putExtra("title",
                        task.optString("title", "Пора выполнить задачу"));

                    PendingIntent pi = PendingIntent.getBroadcast(
                        c, count++, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
                    );

                   if (Build.VERSION.SDK_INT < 31 ||
    am.canScheduleExactAlarms()) {
    am.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        cal.getTimeInMillis(), pi
    );
} else {
    am.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        cal.getTimeInMillis(), pi
    );
}
                    
                }
            }
        } catch (Exception e) {
            android.util.Log.e("PashaLife", "Schedule error", e);
        }

        prefs.edit().putInt("alarmCount", count).apply();
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        NotificationManager nm = (NotificationManager)
            c.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                new NotificationChannel(
                    "tasks", "Напоминания",
                    NotificationManager.IMPORTANCE_HIGH
                )
            );
        }

        PendingIntent open = PendingIntent.getActivity(
            c, 0, new Intent(c, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT |
            PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder =
            Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(c, "tasks")
            : new Notification.Builder(c);

        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ПАША 2.3")
            .setContentText(intent.getStringExtra("title"))
            .setContentIntent(open)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true);

        try {
            nm.notify((int) System.currentTimeMillis(),
                builder.build());
        } catch (SecurityException ignored) {}

        scheduleAll(c);
    }
}
