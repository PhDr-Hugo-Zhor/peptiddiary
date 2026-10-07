package cz.peptidovydenicek.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * The channel is now only responsible for making the notification
 * high-priority (heads-up, eligible for a full-screen intent). Sound and
 * vibration are handled entirely by the app itself (see ActiveAlarmSound
 * and the Vibrator calls in AlarmReceiver/AlarmActionReceiver), because
 * letting the channel own them proved unreliable to reliably stop on
 * demand. The channel therefore never needs sound/vibration settings, so
 * there's nothing about it that can ever get permanently "stuck" the way
 * a channel's sound can.
 */
public class AlarmChannelUtil {

    private static final String CHANNEL_ID = "dose_alarm_silent_v3";

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;

        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID, "Připomínky dávek (budík)", NotificationManager.IMPORTANCE_HIGH
        );
        channel.enableVibration(false);
        channel.setSound(null, null);
        channel.setDescription("Upozornění na dávky ve stylu budíku. Zvuk a vibrace řídí appka sama.");
        nm.createNotificationChannel(channel);
    }

    public static String channelId() {
        return CHANNEL_ID;
    }
}
