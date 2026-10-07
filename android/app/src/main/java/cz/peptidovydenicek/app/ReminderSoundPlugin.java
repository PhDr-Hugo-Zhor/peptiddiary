package cz.peptidovydenicek.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Lets the user pick a notification sound from the phone's own system
 * sound picker (same dialog used for ringtones/alarms). Channel creation
 * itself now happens in AlarmChannelUtil at the moment an alarm actually
 * fires, so this plugin only needs to remember which sound was picked.
 */
@CapacitorPlugin(name = "ReminderSound")
public class ReminderSoundPlugin extends Plugin {

    private static final String PREFS = "reminder_sound_prefs";
    private static final String KEY_URI = "sound_uri";
    private static final String KEY_TITLE = "sound_title";

    @PluginMethod
    public void pickSound(PluginCall call) {
        saveCall(call);
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        String existing = prefs.getString(KEY_URI, null);

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        if (existing != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existing));
        }
        startActivityForResult(call, intent, "pickSoundResult");
    }

    @ActivityCallback
    private void pickSoundResult(PluginCall call, androidx.activity.result.ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
            call.reject("CANCELLED");
            return;
        }
        Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);

        if (uri == null) {
            prefs.edit().remove(KEY_URI).remove(KEY_TITLE).apply();
            JSObject ret = new JSObject();
            ret.put("uri", JSObject.NULL);
            ret.put("title", "Výchozí systémový zvuk");
            call.resolve(ret);
            return;
        }

        String title;
        try {
            title = RingtoneManager.getRingtone(getContext(), uri).getTitle(getContext());
        } catch (Exception e) {
            title = "Vybraný zvuk";
        }
        prefs.edit().putString(KEY_URI, uri.toString()).putString(KEY_TITLE, title).apply();

        JSObject ret = new JSObject();
        ret.put("uri", uri.toString());
        ret.put("title", title);
        call.resolve(ret);
    }

    @PluginMethod
    public void getSound(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        String uri = prefs.getString(KEY_URI, null);
        String title = prefs.getString(KEY_TITLE, null);
        JSObject ret = new JSObject();
        ret.put("uri", uri != null ? uri : JSObject.NULL);
        ret.put("title", title != null ? title : JSObject.NULL);
        call.resolve(ret);
    }
}
