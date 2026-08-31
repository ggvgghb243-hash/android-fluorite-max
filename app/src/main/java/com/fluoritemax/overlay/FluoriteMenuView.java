package com.fluoritemax.overlay;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class FluoriteMenuView extends FrameLayout {

    public interface MenuListener {
        void onCloseRequested();
        void onDrag(int dx, int dy);
    }

    private final MenuListener listener;
    private final Context ctx;

    // Theme Colors
    private static final int COLOR_MAIN_BG      = 0xFA0A0D15;
    private static final int COLOR_SIDEBAR_BG   = 0xFF07090F;
    private static final int COLOR_TAB_ACTIVE   = 0xFF0F1422;
    private static final int COLOR_ACCENT       = 0xFF1852FF;
    private static final int COLOR_ACCENT_TEXT  = 0xFF2060FF;
    private static final int COLOR_TEXT_MAIN    = 0xFFEBF0FA;
    private static final int COLOR_TEXT_MUTED   = 0xFF7D8AA0;
    private static final int COLOR_BOX_BG       = 0xFF101522;
    private static final int COLOR_BOX_BORDER   = 0xFF1C263A;
    private static final int COLOR_HEADER_BG    = 0xFF0D111C;
    private static final int COLOR_HEADER_BORDER= 0xFF161E30;

    private int activeTab = 0;
    private final List<View> tabButtons = new ArrayList<>();
    private FrameLayout contentContainer;

    // Drag tracking
    private float lastTouchX, lastTouchY;

    public FluoriteMenuView(Context context, MenuListener listener) {
        super(context);
        this.ctx = context;
        this.listener = listener;
        initUI();
    }

    private void initUI() {
        // Main Container Frame with Dark Border & Glow
        LinearLayout mainLayout = new LinearLayout(ctx);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_MAIN_BG);
        bg.setCornerRadius(dp(9));
        bg.setStroke(dp(1), COLOR_BOX_BORDER);
        mainLayout.setBackground(bg);

        int menuWidth = dp(520);
        int menuHeight = dp(360);
        LayoutParams params = new LayoutParams(menuWidth, menuHeight);
        mainLayout.setLayoutParams(params);

        // 1. Header Bar (Draggable)
        View headerView = createHeader();
        mainLayout.addView(headerView);

        // 2. Body Area (Sidebar on Left + Dynamic Content on Right)
        LinearLayout bodyLayout = new LinearLayout(ctx);
        bodyLayout.setOrientation(LinearLayout.HORIZONTAL);
        bodyLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Sidebar
        View sidebar = createSidebar();
        bodyLayout.addView(sidebar);

        // Content Area Container
        contentContainer = new FrameLayout(ctx);
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f
        ));
        contentContainer.setPadding(dp(8), dp(8), dp(8), dp(8));
        bodyLayout.addView(contentContainer);

        mainLayout.addView(bodyLayout);
        addView(mainLayout);

        // Select default tab
        switchTab(0);
    }

    private View createHeader() {
        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(8), dp(12), dp(8));

        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(COLOR_HEADER_BG);
        headerBg.setCornerRadii(new float[]{dp(9), dp(9), dp(9), dp(9), 0, 0, 0, 0});
        headerBg.setStroke(dp(1), COLOR_HEADER_BORDER);
        header.setBackground(headerBg);

        // Logo Title: "BRAZILIX " (White) + "PRO" (Royal Blue)
        LinearLayout titleLayout = new LinearLayout(ctx);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleBrazilix = new TextView(ctx);
        titleBrazilix.setText("BRAZILIX ");
        titleBrazilix.setTextColor(COLOR_TEXT_MAIN);
        titleBrazilix.setTextSize(15);
        titleBrazilix.setTypeface(null, Typeface.BOLD);
        titleLayout.addView(titleBrazilix);

        TextView titlePro = new TextView(ctx);
        titlePro.setText("PRO");
        titlePro.setTextColor(COLOR_ACCENT);
        titlePro.setTextSize(15);
        titlePro.setTypeface(null, Typeface.BOLD);
        titleLayout.addView(titlePro);

        TextView subtitle = new TextView(ctx);
        subtitle.setText("  •  Fluorite Max");
        subtitle.setTextColor(COLOR_ACCENT_TEXT);
        subtitle.setTextSize(12);
        subtitle.setTypeface(null, Typeface.ITALIC);
        titleLayout.addView(subtitle);

        header.addView(titleLayout, new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
        ));

        // Close / Minimize Button (X)
        TextView closeBtn = new TextView(ctx);
        closeBtn.setText("✕");
        closeBtn.setTextColor(COLOR_TEXT_MUTED);
        closeBtn.setTextSize(14);
        closeBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        closeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onCloseRequested();
            }
        });
        header.addView(closeBtn);

        // Header Dragging
        header.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - lastTouchX;
                        float dy = event.getRawY() - lastTouchY;
                        if (listener != null) {
                            listener.onDrag((int) dx, (int) dy);
                        }
                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();
                        return true;
                }
                return false;
            }
        });

        return header;
    }

    private View createSidebar() {
        LinearLayout sidebar = new LinearLayout(ctx);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setLayoutParams(new LinearLayout.LayoutParams(dp(115), ViewGroup.LayoutParams.MATCH_PARENT));

        GradientDrawable sideBg = new GradientDrawable();
        sideBg.setColor(COLOR_SIDEBAR_BG);
        sideBg.setCornerRadii(new float[]{0, 0, 0, 0, 0, 0, dp(9), dp(9)});
        sidebar.setBackground(sideBg);
        sidebar.setPadding(dp(6), dp(8), dp(6), dp(8));

        String[] tabNames = {"Aimbot", "Visuals", "Misc", "Settings"};
        String[] icons = {"🎯", "👁", "📦", "⚙"};

        tabButtons.clear();
        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;
            LinearLayout tab = new LinearLayout(ctx);
            tab.setOrientation(LinearLayout.HORIZONTAL);
            tab.setGravity(Gravity.CENTER_VERTICAL);
            tab.setPadding(dp(10), dp(10), dp(10), dp(10));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            p.setMargins(0, 0, 0, dp(6));
            tab.setLayoutParams(p);

            TextView iconView = new TextView(ctx);
            iconView.setText(icons[i] + " ");
            iconView.setTextSize(13);
            tab.addView(iconView);

            TextView textView = new TextView(ctx);
            textView.setText(tabNames[i]);
            textView.setTextSize(12);
            textView.setTextColor(COLOR_TEXT_MUTED);
            textView.setTypeface(null, Typeface.BOLD);
            tab.addView(textView);

            tab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab(index);
                }
            });

            tabButtons.add(tab);
            sidebar.addView(tab);
        }

        return sidebar;
    }

    private void switchTab(int index) {
        activeTab = index;

        // Update tab styles
        for (int i = 0; i < tabButtons.size(); i++) {
            LinearLayout tab = (LinearLayout) tabButtons.get(i);
            TextView text = (TextView) tab.getChildAt(1);

            GradientDrawable tabBg = new GradientDrawable();
            if (i == index) {
                tabBg.setColor(COLOR_TAB_ACTIVE);
                tabBg.setCornerRadius(dp(6));
                tabBg.setStroke(dp(1), COLOR_ACCENT);
                tab.setBackground(tabBg);
                text.setTextColor(COLOR_TEXT_MAIN);
            } else {
                tabBg.setColor(Color.TRANSPARENT);
                tab.setBackground(tabBg);
                text.setTextColor(COLOR_TEXT_MUTED);
            }
        }

        // Render Active Tab Content
        contentContainer.removeAllViews();
        switch (index) {
            case 0: contentContainer.addView(createAimbotTab()); break;
            case 1: contentContainer.addView(createVisualsTab()); break;
            case 2: contentContainer.addView(createMiscTab()); break;
            case 3: contentContainer.addView(createSettingsTab()); break;
        }
    }

    // ===== TAB 1: AIMBOT =====
    private View createAimbotTab() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Column 1: Aimbot
        LinearLayout col1 = createCard("AIMBOT", 1.0f);
        addCheckbox(col1, "Enable Aimbot", true, null);
        addCheckbox(col1, "Silent Aim", false, null);
        addCheckbox(col1, "Scope Aim", true, null);
        addSlider(col1, "FOV Radius", 120, 0, 360, "°");
        addSlider(col1, "Smooth Speed", 8, 1, 30, "");
        row.addView(wrapInScroll(col1));

        // Column 2: Target Options
        LinearLayout col2 = createCard("TARGET OPTIONS", 1.0f);
        addDropdown(col2, "Target Bone", new String[]{"Head", "Neck", "Chest", "Auto-Lock"});
        addSlider(col2, "Hit Chance", 95, 0, 100, "%");
        addCheckbox(col2, "Trigger Bot", false, null);
        addCheckbox(col2, "Ignore Knocked", true, null);
        addCheckbox(col2, "Auto Fire Delay", false, null);
        row.addView(wrapInScroll(col2));

        return row;
    }

    // ===== TAB 2: VISUALS (ESP) =====
    private View createVisualsTab() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Column 1: Player ESP
        LinearLayout col1 = createCard("ESP PLAYER", 1.0f);
        addCheckbox(col1, "ESP Box", true, null);
        addCheckbox(col1, "ESP Line", true, null);
        addCheckbox(col1, "ESP Skeleton", true, null);
        addCheckbox(col1, "ESP Health Bar", true, null);
        addCheckbox(col1, "ESP Distance", true, null);
        addCheckbox(col1, "ESP Player Name", true, null);
        row.addView(wrapInScroll(col1));

        // Column 2: ESP Style
        LinearLayout col2 = createCard("ESP STYLE", 1.0f);
        addDropdown(col2, "Box Style", new String[]{"Corner Boxes", "2D Rectangles", "3D Wireframe"});
        addDropdown(col2, "Line Origin", new String[]{"Bottom Screen", "Crosshair Center", "Top Screen"});
        addDropdown(col2, "ESP Color", new String[]{"Vibrant Royal Blue", "Neon Cyan", "Toxic Green", "Crimson Red", "Bright Yellow", "Pure White"});
        addSlider(col2, "ESP Line Width", 2, 1, 5, " px");
        addCheckbox(col2, "Show Team ID", false, null);
        row.addView(wrapInScroll(col2));

        return row;
    }

    // ===== TAB 3: MISC =====
    private View createMiscTab() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Column 1: Weapon & Movement
        LinearLayout col1 = createCard("MOVEMENT & WEAPON", 1.0f);
        addSlider(col1, "Speed Multiplier", 1, 1, 5, "x");
        addCheckbox(col1, "Zero Recoil", true, null);
        addCheckbox(col1, "Fast Reload", false, null);
        addCheckbox(col1, "Instant Bullet Hit", true, null);
        addCheckbox(col1, "Wide View Angle", false, null);
        row.addView(wrapInScroll(col1));

        // Column 2: Utilities
        LinearLayout col2 = createCard("UTILITIES & STATUS", 1.0f);
        addCheckbox(col2, "Night Sky Mode", false, null);
        addCheckbox(col2, "High Damage Boost", false, null);
        addCheckbox(col2, "Wall Penetration", false, null);

        // Protection Badge
        TextView statusLabel = new TextView(ctx);
        statusLabel.setText("\n🛡️ Anti-Cheat Bypass: Active\n⚡ Emulator Status: Synced");
        statusLabel.setTextSize(11);
        statusLabel.setTextColor(0xFF00E5FF);
        statusLabel.setPadding(dp(4), dp(8), dp(4), dp(8));
        col2.addView(statusLabel);

        row.addView(wrapInScroll(col2));

        return row;
    }

    // ===== TAB 4: SETTINGS =====
    private View createSettingsTab() {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Column 1: Menu Config
        LinearLayout col1 = createCard("MENU CONFIG", 1.0f);
        addCheckbox(col1, "Enable Header Drag", true, null);
        addDropdown(col1, "Hotkey Keybind", new String[]{"PageUp / PageDown (PgUp/PgDn)", "Insert Key", "Home Key", "Floating Badge Only"});
        
        Button btnSave = createButton("SAVE CONFIG", COLOR_ACCENT, 0xFFFFFFFF);
        btnSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ctx, "Configuration Saved Successfully!", Toast.LENGTH_SHORT).show();
            }
        });
        col1.addView(btnSave);

        Button btnReset = createButton("RESET DEFAULTS", COLOR_HEADER_BG, COLOR_TEXT_MUTED);
        btnReset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ctx, "Settings Reset to Defaults", Toast.LENGTH_SHORT).show();
            }
        });
        col1.addView(btnReset);

        row.addView(wrapInScroll(col1));

        // Column 2: Emulator Guide & About
        LinearLayout col2 = createCard("EMULATOR CONTROLS", 1.0f);
        TextView guide = new TextView(ctx);
        guide.setText(
            "⌨️ Keyboard Shortcuts:\n" +
            "• Press [PgUp] or [PgDn] on PC keyboard to quickly toggle this menu.\n\n" +
            "🖱️ Mouse Controls:\n" +
            "• Click and drag top header bar to position overlay anywhere.\n\n" +
            "🏷️ Fluorite Max v1.0\n" +
            "Designed for LDPlayer, BlueStacks, Nox & MuMu"
        );
        guide.setTextSize(11);
        guide.setTextColor(COLOR_TEXT_MUTED);
        guide.setLineSpacing(4, 1.2f);
        col2.addView(guide);

        row.addView(wrapInScroll(col2));

        return row;
    }

    // ===== HELPER VIEW FACTORIES =====
    private LinearLayout createCard(String title, float weight) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        p.setMargins(dp(4), 0, dp(4), 0);
        card.setLayoutParams(p);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(COLOR_BOX_BG);
        cardBg.setCornerRadius(dp(7));
        cardBg.setStroke(dp(1), COLOR_BOX_BORDER);
        card.setBackground(cardBg);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));

        // Card Header Title
        TextView cardTitle = new TextView(ctx);
        cardTitle.setText(title);
        cardTitle.setTextSize(11);
        cardTitle.setTextColor(COLOR_ACCENT_TEXT);
        cardTitle.setTypeface(null, Typeface.BOLD);
        cardTitle.setPadding(0, 0, 0, dp(8));
        card.addView(cardTitle);

        return card;
    }

    private View wrapInScroll(View view) {
        ScrollView scroll = new ScrollView(ctx);
        scroll.setLayoutParams(view.getLayoutParams());
        scroll.addView(view);
        return scroll;
    }

    private void addCheckbox(LinearLayout parent, String title, boolean defaultChecked, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        CheckBox cb = new CheckBox(ctx);
        cb.setChecked(defaultChecked);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT));
        if (listener != null) cb.setOnCheckedChangeListener(listener);
        row.addView(cb);

        TextView label = new TextView(ctx);
        label.setText(title);
        label.setTextSize(12);
        label.setTextColor(COLOR_TEXT_MAIN);
        label.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cb.setChecked(!cb.isChecked());
            }
        });
        row.addView(label);

        parent.addView(row);
    }

    private void addSlider(LinearLayout parent, final String title, int defaultValue, final int min, int max, final String unit) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dp(4), 0, dp(4));

        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView titleView = new TextView(ctx);
        titleView.setText(title);
        titleView.setTextSize(11);
        titleView.setTextColor(COLOR_TEXT_MUTED);
        topRow.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        final TextView valView = new TextView(ctx);
        valView.setText(defaultValue + unit);
        valView.setTextSize(11);
        valView.setTextColor(COLOR_ACCENT_TEXT);
        valView.setTypeface(null, Typeface.BOLD);
        topRow.addView(valView);

        layout.addView(topRow);

        SeekBar seekBar = new SeekBar(ctx);
        seekBar.setMax(max - min);
        seekBar.setProgress(defaultValue - min);
        seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT));
        seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actual = min + progress;
                valView.setText(actual + unit);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        layout.addView(seekBar);
        parent.addView(layout);
    }

    private void addDropdown(LinearLayout parent, final String title, final String[] options) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dp(4), 0, dp(4));

        TextView titleView = new TextView(ctx);
        titleView.setText(title);
        titleView.setTextSize(11);
        titleView.setTextColor(COLOR_TEXT_MUTED);
        titleView.setPadding(0, 0, 0, dp(2));
        layout.addView(titleView);

        final Button btn = new Button(ctx);
        btn.setText(options[0] + "  ▾");
        btn.setTextSize(11);
        btn.setTextColor(COLOR_TEXT_MAIN);
        btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        btn.setPadding(dp(10), dp(6), dp(10), dp(6));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(COLOR_HEADER_BG);
        btnBg.setCornerRadius(dp(5));
        btnBg.setStroke(dp(1), COLOR_BOX_BORDER);
        btn.setBackground(btnBg);

        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctx, android.R.style.Theme_DeviceDefault_Dialog_Alert);
                builder.setTitle(title);
                builder.setItems(options, (dialog, which) -> {
                    btn.setText(options[which] + "  ▾");
                });
                builder.show();
            }
        });

        layout.addView(btn);
        parent.addView(layout);
    }

    private Button createButton(String text, int bgColor, int textColor) {
        Button btn = new Button(ctx);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setTextColor(textColor);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setPadding(0, dp(8), 0, dp(8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(6));
        btn.setBackground(bg);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(0, dp(6), 0, dp(2));
        btn.setLayoutParams(p);
        return btn;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
