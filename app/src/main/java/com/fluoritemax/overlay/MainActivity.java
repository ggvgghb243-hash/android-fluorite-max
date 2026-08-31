package com.fluoritemax.overlay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dark modern launcher UI
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF0A0D15);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(40, 50, 40, 50);

        // Header Title
        TextView title = new TextView(this);
        title.setText("BRAZILIX PRO");
        title.setTextSize(26);
        title.setTextColor(0xFFEBF0FA);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Fluorite Max • Android Emulator Edition");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF1852FF);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 8, 0, 30);
        layout.addView(subtitle);

        // Instruction Card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFF101522);
        card.setPadding(30, 25, 30, 25);

        TextView cardTitle = new TextView(this);
        cardTitle.setText("⌨️ PgDn Keybind Show / Hide Instructions");
        cardTitle.setTextSize(15);
        cardTitle.setTextColor(0xFFEBF0FA);
        cardTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(cardTitle);

        TextView cardDesc = new TextView(this);
        cardDesc.setText(
            "1. Press [START OVERLAY SERVICE] below.\n" +
            "2. Click [ENABLE PGDN GLOBAL HOTKEY] to allow PgDn detection during gameplay.\n" +
            "3. Press PageDown (PgDn) on keyboard anytime to Show / Hide menu seamlessly!"
        );
        cardDesc.setTextSize(12.5f);
        cardDesc.setTextColor(0xFF7D8AA0);
        cardDesc.setLineSpacing(8, 1.2f);
        cardDesc.setPadding(0, 12, 0, 0);
        card.addView(cardDesc);

        layout.addView(card);

        // 1. Launch Button
        Button btnStart = new Button(this);
        btnStart.setText("START OVERLAY SERVICE");
        btnStart.setTextColor(0xFFFFFFFF);
        btnStart.setBackgroundColor(0xFF1852FF);
        btnStart.setTextSize(14);
        btnStart.setTypeface(null, android.graphics.Typeface.BOLD);
        btnStart.setPadding(0, 22, 0, 22);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 30, 0, 12);
        btnStart.setLayoutParams(btnParams);
        btnStart.setOnClickListener(v -> checkOverlayPermissionAndStart());
        layout.addView(btnStart);

        // 2. Enable Accessibility Hotkey Button
        Button btnAccess = new Button(this);
        btnAccess.setText("ENABLE PGDN GLOBAL HOTKEY");
        btnAccess.setTextColor(0xFFEBF0FA);
        btnAccess.setBackgroundColor(0xFF1C263A);
        btnAccess.setTextSize(13);
        btnAccess.setPadding(0, 20, 0, 20);
        LinearLayout.LayoutParams accessParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        accessParams.setMargins(0, 0, 0, 12);
        btnAccess.setLayoutParams(accessParams);
        btnAccess.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "Turn ON 'Fluorite Max' in Accessibility to enable PgDn in-game", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Please open Settings > Accessibility", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnAccess);

        // 3. Stop Button
        Button btnStop = new Button(this);
        btnStop.setText("STOP SERVICE");
        btnStop.setTextColor(0xFF7D8AA0);
        btnStop.setBackgroundColor(0xFF0D111C);
        btnStop.setTextSize(13);
        btnStop.setPadding(0, 18, 0, 18);
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        stopParams.setMargins(0, 0, 0, 20);
        btnStop.setLayoutParams(stopParams);
        btnStop.setOnClickListener(v -> {
            stopService(new Intent(MainActivity.this, FloatingOverlayService.class));
            Toast.makeText(MainActivity.this, "Overlay Service Stopped", Toast.LENGTH_SHORT).show();
        });
        layout.addView(btnStop);

        scrollView.addView(layout);
        setContentView(scrollView);

        // Auto request permission on launch
        checkOverlayPermissionAndStart();
    }

    private void checkOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
                Toast.makeText(this, "Please allow 'Display over other apps' permission", Toast.LENGTH_LONG).show();
                return;
            }
        }
        startFloatingService();
    }

    private void startFloatingService() {
        Intent intent = new Intent(this, FloatingOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "Fluorite Max Started! Press PgDn to Show / Hide", Toast.LENGTH_SHORT).show();
        moveTaskToBack(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                Toast.makeText(this, "Overlay permission is required for emulator overlay", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
