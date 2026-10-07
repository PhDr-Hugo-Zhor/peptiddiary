package cz.peptidovydenicek.app;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Schedules dose reminders as real alarm-clock-style events (via AlarmManager
 * + a BroadcastReceiver), not ordinary dismissible notifications. See
 * AlarmService for what actually happens when one fires.
 */
@CapacitorPlugin(name = "AlarmReminder")
public class AlarmReminderPlugin extends Plugin {

    @PluginMethod
    public void scheduleAlarm(PluginCall call) {
        String id = call.getString("id");
        String title = call.getString("title");
        String body = call.getString("body");
        Long atMillis = call.getLong("atMillis");
        String soundUri = call.getString("soundUri");
        String soundTitle = call.getString("soundTitle");
        if (id == null || atMillis == null) {
            call.reject("Chybí id nebo atMillis");
            return;
        }

        int notifId = idToInt(id);

        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        intent.putExtra("notifId", notifId);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        if (soundUri != null) intent.putExtra("soundUri", soundUri);
        if (soundTitle != null) intent.putExtra("soundTitle", soundTitle);

        PendingIntent pi = PendingIntent.getBroadcast(
            getContext(), notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi);
        } catch (SecurityException e) {
            call.reject("NO_EXACT_ALARM_PERMISSION");
            return;
        }
        call.resolve();
    }

    @PluginMethod
    public void cancelAlarms(PluginCall call) {
        JSArray idsArr = call.getArray("ids");
        if (idsArr == null) { call.resolve(); return; }
        try {
            for (int i = 0; i < idsArr.length(); i++) {
                cancelOne(idsArr.getString(i));
            }
        } catch (org.json.JSONException ignored) {}
        call.resolve();
    }

    private void cancelOne(String id) {
        int notifId = idToInt(id);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
            getContext(), notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);

        NotificationManager nm = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(notifId);
    }

    @PluginMethod
    public void hasExactAlarmPermission(PluginCall call) {
        JSObject ret = new JSObject();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            ret.put("granted", am.canScheduleExactAlarms());
        } else {
            ret.put("granted", true);
        }
        call.resolve(ret);
    }

    @PluginMethod
    public void requestExactAlarmPermission(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getContext().getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        }
        call.resolve();
    }

    /** Self-contained on purpose -- does not depend on AlarmChannelUtil,
     *  which only deals with the (unrelated) notification channel now. */
    private static int idToInt(String str) {
        int hash = 0;
        for (int i = 0; i < str.length(); i++) {
            hash = (hash * 31 + str.charAt(i)) | 0;
        }
        int abs = Math.abs(hash);
        return abs == 0 ? 1 : abs;
    }
}
