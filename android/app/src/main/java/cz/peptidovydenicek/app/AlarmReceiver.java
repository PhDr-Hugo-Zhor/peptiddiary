package cz.peptidovydenicek.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * The alarm itself (AlarmManager) still wakes this receiver up at the
 * right time, but all the actual work -- notification, sound, vibration --
 * now happens in AlarmService, so it survives the app being swiped away.
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, AlarmService.class);
        if (intent.getExtras() != null) {
            serviceIntent.putExtras(intent.getExtras());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
