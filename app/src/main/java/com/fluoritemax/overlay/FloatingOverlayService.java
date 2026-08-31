package com.fluoritemax.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatingOverlayService extends Service {

    public static final String ACTION_TOGGLE_MENU = "com.fluoritemax.TOGGLE_MENU";
    private static final String CHANNEL_ID = "fluorite_max_overlay_channel";
    private static final int NOTIFICATION_ID = 9999;

    private WindowManager windowManager;
    private FluoriteMenuView menuView;
    private View invisibleTrigger;
    private WindowManager.LayoutParams menuParams;
    private WindowManager.LayoutParams triggerParams;

    private boolean isMenuVisible = true;
    private int savedMenuX = 0;
    private int savedMenuY = 0;

    private final BroadcastReceiver hotkeyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_TOGGLE_MENU.equals(intent.getAction())) {
                toggleMenuVisibility(!isMenuVisible);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // Register Global Hotkey Broadcast Receiver
        IntentFilter filter = new IntentFilter(ACTION_TOGGLE_MENU);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(hotkeyReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(hotkeyReceiver, filter);
        }

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

        // 1. Main Menu View Window Parameters
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

        // 2. Invisible 1x1 Keymap Trigger at top-left corner (0, 0)
        // This allows emulator users to map any key (e.g. PgDn) directly to coordinate (0,0) in keymapper
        triggerParams = new WindowManager.LayoutParams(
            1, 1,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        triggerParams.gravity = Gravity.TOP | Gravity.START;
        triggerParams.x = 0;
        triggerParams.y = 0;

        invisibleTrigger = new View(this);
        invisibleTrigger.setOnClickListener(v -> toggleMenuVisibility(!isMenuVisible));

        // Add Views to WindowManager
        try {
            windowManager.addView(invisibleTrigger, triggerParams);
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
                // Hide menu completely so game controls are 100% free and responsive
                menuView.setVisibility(View.GONE);
                menuParams.width = 1;
                menuParams.height = 1;
                menuParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                   WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                                   WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
                try {
                    windowManager.updateViewLayout(menuView, menuParams);
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
            .setContentText("Press PgDn to Show / Hide menu anytime")
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
        try {
            unregisterReceiver(hotkeyReceiver);
        } catch (Exception ignored) {}

        if (windowManager != null) {
            if (menuView != null) {
                try { windowManager.removeView(menuView); } catch (Exception ignored) {}
            }
            if (invisibleTrigger != null) {
                try { windowManager.removeView(invisibleTrigger); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
