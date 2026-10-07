package cz.peptidovydenicek.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmActionReceiver extends BroadcastReceiver {

    private static final long SNOOZE_MILLIS = 5 * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        int notifId = intent.getIntExtra("notifId", 0);
        String action = intent.getAction();

        // stopping the service is now the primary way to end the alarm --
        // its own onDestroy() stops the sound and vibration
        context.stopService(new Intent(context, AlarmService.class));

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(notifId);

        // belt-and-suspenders fallback in case the service wasn't running
        // for some reason
        ActiveAlarmSound.stop();

        if ("SNOOZE".equals(action)) {
            String title = intent.getStringExtra("title");
            String body = intent.getStringExtra("body");
            String soundUri = intent.getStringExtra("soundUri");
            String soundTitle = intent.getStringExtra("soundTitle");

            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            alarmIntent.putExtra("notifId", notifId);
            alarmIntent.putExtra("title", title);
            alarmIntent.putExtra("body", body);
            if (soundUri != null) alarmIntent.putExtra("soundUri", soundUri);
            if (soundTitle != null) alarmIntent.putExtra("soundTitle", soundTitle);

            PendingIntent pi = PendingIntent.getBroadcast(
                context, notifId, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            try {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + SNOOZE_MILLIS,
                    pi
                );
            } catch (SecurityException ignored) {}
        }
    }
}
