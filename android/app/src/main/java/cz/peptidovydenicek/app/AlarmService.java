package cz.peptidovydenicek.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;

/**
 * Runs the alarm sound + vibration as a foreground service, not just a
 * plain broadcast-triggered process. A bare process (like a
 * BroadcastReceiver's onReceive) can be killed by Android the moment the
 * user swipes the app away from Recents -- a foreground service, backed
 * by its own visible notification, is what keeps things like music apps
 * (and this alarm) alive through that.
 */
public class AlarmService extends Service {

    private static final long[] VIBRATION_PATTERN = {0, 800, 400};

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int notifId = intent.getIntExtra("notifId", (int) System.currentTimeMillis());
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        String soundUri = intent.getStringExtra("soundUri");
        String soundTitle = intent.getStringExtra("soundTitle");

        AlarmChannelUtil.ensureChannel(this);
        String channelId = AlarmChannelUtil.channelId();

        ensureAlarmVolumeAudible();

        Intent dismissIntent = new Intent(this, AlarmActionReceiver.class);
        dismissIntent.setAction("DISMISS");
        dismissIntent.putExtra("notifId", notifId);
        PendingIntent dismissPi = PendingIntent.getBroadcast(
            this, notifId * 10 + 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent snoozeIntent = new Intent(this, AlarmActionReceiver.class);
        snoozeIntent.setAction("SNOOZE");
        snoozeIntent.putExtra("notifId", notifId);
        snoozeIntent.putExtra("title", title);
        snoozeIntent.putExtra("body", body);
        if (soundUri != null) snoozeIntent.putExtra("soundUri", soundUri);
        if (soundTitle != null) snoozeIntent.putExtra("soundTitle", soundTitle);
        PendingIntent snoozePi = PendingIntent.getBroadcast(
            this, notifId * 10 + 2, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent fullScreenIntent = new Intent(this, AlarmActivity.class);
        fullScreenIntent.putExtra("notifId", notifId);
        fullScreenIntent.putExtra("title", title);
        fullScreenIntent.putExtra("body", body);
        if (soundUri != null) fullScreenIntent.putExtra("soundUri", soundUri);
        if (soundTitle != null) fullScreenIntent.putExtra("soundTitle", soundTitle);
        fullScreenIntent.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_CLEAR_TOP
            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            | Intent.FLAG_ACTIVITY_NO_USER_ACTION
        );
        PendingIntent fullScreenPi = PendingIntent.getActivity(
            this, notifId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(getApplicationInfo().icon)
            .setContentTitle(title != null ? title : "Peptidový deníček")
            .setContentText(body != null ? body : "Čas na dávku")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPi, true)
            .addAction(0, "Odložit (5 min)", snoozePi)
            .addAction(0, "Zrušit", dismissPi)
            .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(notifId, notification);
        }

        try {
            startActivity(fullScreenIntent);
        } catch (Exception ignored) {}

        Uri resolvedSound = (soundUri != null) ? Uri.parse(soundUri) : resolveDefaultSound(this);
        ActiveAlarmSound.play(this, resolvedSound);
        vibrate();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ActiveAlarmSound.stop();
        stopVibration();
    }

    private static Uri resolveDefaultSound(android.content.Context context) {
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        return uri;
    }

    private void ensureAlarmVolumeAudible() {
        try {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            int current = am.getStreamVolume(AudioManager.STREAM_ALARM);
            if (current < max / 2) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, Math.max(1, (max * 2) / 3), 0);
            }
        } catch (Exception ignored) {}
    }

    private void vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                vm.getDefaultVibrator().vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                v.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, 0));
            } else {
                Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                v.vibrate(VIBRATION_PATTERN, 0);
            }
        } catch (Exception ignored) {}
    }

    private void stopVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    try { vm.getDefaultVibrator().cancel(); } catch (Exception ignored) {}
                    try { vm.cancel(); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.cancel();
        } catch (Exception ignored) {}
    }
}
