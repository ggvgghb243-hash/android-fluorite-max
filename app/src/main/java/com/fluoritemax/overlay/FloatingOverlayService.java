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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatingOverlayService extends Service {

    private static final String CHANNEL_ID = "fluorite_max_overlay_channel";
    private static final int NOTIFICATION_ID = 9999;

    private WindowManager windowManager;
    private FluoriteMenuView menuView;
    private View floatingBadge;
    private WindowManager.LayoutParams menuParams;
    private WindowManager.LayoutParams badgeParams;

    private boolean isMenuVisible = true;

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

        // 1. Main Menu View Window Parameters (Normal flags: screenshots allowed, shows in recording)
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
                menuParams.x += dx;
                menuParams.y += dy;
                try {
                    windowManager.updateViewLayout(menuView, menuParams);
                } catch (Exception ignored) {}
            }

            @Override
            public void onStreamproofToggled(boolean enabled) {
                setStreamproof(enabled);
            }
        });

        // Key Listener for PgUp / PgDown hotkey
        menuView.setFocusableInTouchMode(true);
        menuView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_PAGE_UP ||
                        keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                        keyCode == KeyEvent.KEYCODE_INSERT ||
                        keyCode == KeyEvent.KEYCODE_MOVE_HOME ||
                        keyCode == KeyEvent.KEYCODE_F1) {
                        toggleMenuVisibility(!isMenuVisible);
                        return true;
                    }
                }
                return false;
            }
        });

        // 2. Floating Mini Badge Window Parameters with FLAG_SECURE (Invisible in Screen Recording / SS)
        badgeParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
            WindowManager.LayoutParams.FLAG_SECURE, // Completely hidden from screen recorders & capture
            PixelFormat.TRANSLUCENT
        );
        badgeParams.gravity = Gravity.TOP | Gravity.START;
        badgeParams.x = 40;
        badgeParams.y = 120;

        floatingBadge = new FloatingButtonView(this, new FloatingButtonView.BadgeListener() {
            @Override
            public void onClick() {
                toggleMenuVisibility(!isMenuVisible);
            }

            @Override
            public void onMove(int deltaX, int deltaY) {
                badgeParams.x += deltaX;
                badgeParams.y += deltaY;
                try {
                    windowManager.updateViewLayout(floatingBadge, badgeParams);
                } catch (Exception ignored) {}
            }
        });

        // Add Views to WindowManager
        try {
            windowManager.addView(floatingBadge, badgeParams);
            windowManager.addView(menuView, menuParams);
            menuView.requestFocus();
            // Automatically hide floating badge when menu is open so screen is ultra clean
            updateBadgeVisibility();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toggleMenuVisibility(boolean show) {
        isMenuVisible = show;
        if (menuView != null) {
            menuView.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                menuView.requestFocus();
            }
        }
        updateBadgeVisibility();
    }

    private void updateBadgeVisibility() {
        if (floatingBadge != null) {
            // When menu is OPEN, hide the floating round icon
            // When menu is CLOSED, show floating icon with FLAG_SECURE so it is invisible to screen recording
            floatingBadge.setVisibility(isMenuVisible ? View.GONE : View.VISIBLE);
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
            .setContentText("Press PgUp / PgDn on keyboard to Show / Hide menu")
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
        if (windowManager != null) {
            if (menuView != null) {
                try { windowManager.removeView(menuView); } catch (Exception ignored) {}
            }
            if (floatingBadge != null) {
                try { windowManager.removeView(floatingBadge); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
