package cz.peptidovydenicek.app;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

/**
 * Plays (and reliably stops) the alarm sound directly, instead of relying
 * on a notification channel's own sound handling -- which turned out to be
 * unreliable for a continuous, cancel-on-demand alarm sound. A static
 * reference is used because AlarmReceiver and AlarmActionReceiver are
 * separate broadcast receivers that may each run as a fresh instance, but
 * they share the same app process.
 */
public class ActiveAlarmSound {

    private static Ringtone current;

    public static synchronized void play(Context context, Uri uri) {
        stop();
        if (uri == null) return;
        try {
            Ringtone r = RingtoneManager.getRingtone(context, uri);
            if (r == null) return;

            AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            r.setAudioAttributes(attrs);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                r.setLooping(true);
            }

            current = r;
            r.play();
        } catch (Exception ignored) {}
    }

    public static synchronized void stop() {
        if (current != null) {
            try { current.stop(); } catch (Exception ignored) {}
            current = null;
        }
    }
}
