package com.fluoritemax.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatingOverlayService extends Service {

    private static final String CHANNEL_ID = "fluorite_max_overlay_channel";
    private static final int NOTIFICATION_ID = 9999;

    private WindowManager windowManager;
    private FluoriteMenuView menuView;
    private WindowManager.LayoutParams menuParams;

    private boolean isMenuVisible = true;
    private int savedMenuX = 0;
    private int savedMenuY = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        initViews();
    }

    private void initViews() {
        int overlayType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            overlayType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // Setup Main Menu Window Parameters (NO floating badge, clean overlay)
        menuParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        menuParams.gravity = Gravity.CENTER;
        menuParams.x = 0;
        menuParams.y = 0;

        menuView = new FluoriteMenuView(this, new FluoriteMenuView.MenuListener() {
            @Override
            public void onCloseRequested() {
                toggleMenuVisibility(false);
            }

            @Override
            public void onDrag(int dx, int dy) {
                if (isMenuVisible) {
                    savedMenuX += dx;
                    savedMenuY += dy;
                    menuParams.x = savedMenuX;
                    menuParams.y = savedMenuY;
                    try {
                        windowManager.updateViewLayout(menuView, menuParams);
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onStreamproofToggled(boolean enabled) {
                setStreamproof(enabled);
            }
        }) {
            // Override dispatchKeyEvent to guarantee key interception even when collapsed
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = event.getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                        keyCode == KeyEvent.KEYCODE_PAGE_UP ||
                        keyCode == KeyEvent.KEYCODE_INSERT) {
                        toggleMenuVisibility(!isMenuVisible);
                        return true;
                    }
                }
                return super.dispatchKeyEvent(event);
            }
        };

        menuView.setFocusable(true);
        menuView.setFocusableInTouchMode(true);

        // Add Menu to WindowManager
        try {
            windowManager.addView(menuView, menuParams);
            menuView.requestFocus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toggleMenuVisibility(boolean show) {
        this.isMenuVisible = show;
        if (menuView != null) {
            if (show) {
                // Restore full menu visibility and original coordinates
                menuView.setVisibility(View.VISIBLE);
                menuParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
                menuParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                menuParams.x = savedMenuX;
                menuParams.y = savedMenuY;
                menuParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                   WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
                try {
                    windowManager.updateViewLayout(menuView, menuParams);
                    menuView.requestFocus();
                } catch (Exception ignored) {}
            } else {
                // Hide menu completely while maintaining key focus so PgDn / PgUp still works
                menuView.setVisibility(View.INVISIBLE);
                menuParams.width = 1;
                menuParams.height = 1;
                menuParams.x = -5000;
                menuParams.y = -5000;
                menuParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                   WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
                try {
                    windowManager.updateViewLayout(menuView, menuParams);
                    menuView.requestFocus();
                } catch (Exception ignored) {}
            }
        }
    }

    public void setStreamproof(boolean enabled) {
        if (enabled) {
            menuParams.flags |= WindowManager.LayoutParams.FLAG_SECURE;
        } else {
            menuParams.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
        }
        try {
            if (menuView != null) windowManager.updateViewLayout(menuView, menuParams);
        } catch (Exception ignored) {}
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Fluorite Max Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background overlay & hotkey listener");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
            .setContentTitle("Fluorite Max • Active")
            .setContentText("Press PgDn / PgUp on keyboard to Show / Hide menu")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null && menuView != null) {
            try { windowManager.removeView(menuView); } catch (Exception ignored) {}
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
