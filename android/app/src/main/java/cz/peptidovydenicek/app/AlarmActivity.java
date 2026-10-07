package cz.peptidovydenicek.app;

import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Takes over the whole screen (even over the lock screen) when a dose
 * alarm fires -- like a real alarm clock, not a dismissible notification.
 * Stays up until the user taps Snooze or Dismiss; the back button does
 * nothing on purpose.
 */
public class AlarmActivity extends AppCompatActivity {

    private int notifId;
    private String title, body, soundUri, soundTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        notifId = getIntent().getIntExtra("notifId", 0);
        title = getIntent().getStringExtra("title");
        body = getIntent().getStringExtra("body");
        soundUri = getIntent().getStringExtra("soundUri");
        soundTitle = getIntent().getStringExtra("soundTitle");

        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = (int) (32 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad * 4, pad, pad);
        root.setBackgroundColor(Color.parseColor("#15181A"));

        TextView titleView = new TextView(this);
        titleView.setText(title != null ? title : "Peptidový deníček");
        titleView.setTextSize(24);
        titleView.setTextColor(Color.WHITE);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView);

        TextView bodyView = new TextView(this);
        bodyView.setText(body != null ? body : "Čas na dávku");
        bodyView.setTextSize(18);
        bodyView.setTextColor(Color.parseColor("#CCCCCC"));
        bodyView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = pad;
        bodyLp.bottomMargin = pad * 3;
        bodyView.setLayoutParams(bodyLp);
        root.addView(bodyView);

        Button snoozeBtn = new Button(this);
        snoozeBtn.setText("Odložit (5 min)");
        snoozeBtn.setOnClickListener(v -> { sendAction("SNOOZE"); finish(); });
        LinearLayout.LayoutParams snoozeLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        root.addView(snoozeBtn, snoozeLp);

        Button dismissBtn = new Button(this);
        dismissBtn.setText("Zrušit");
        dismissBtn.setOnClickListener(v -> { sendAction("DISMISS"); finish(); });
        LinearLayout.LayoutParams dismissLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dismissLp.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        root.addView(dismissBtn, dismissLp);

        setContentView(root);
    }

    private void sendAction(String action) {
        Intent intent = new Intent(this, AlarmActionReceiver.class);
        intent.setAction(action);
        intent.putExtra("notifId", notifId);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        if (soundUri != null) intent.putExtra("soundUri", soundUri);
        if (soundTitle != null) intent.putExtra("soundTitle", soundTitle);
        sendBroadcast(intent);
    }

    @Override
    public void onBackPressed() {
        // intentionally does nothing -- Snooze/Dismiss are the only way out,
        // same as a real alarm clock
    }
}
