package com.fluoritemax.overlay;

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

    public interface DropdownSelectionListener {
        void onSelected(String option);
    }

    private final MenuListener listener;
    private final Context ctx;

    // Exact Theme Colors from Screenshots
    private static final int COLOR_WINDOW_BG    = 0xF0070B14; // #070B14 Dark translucent navy
    private static final int COLOR_SIDEBAR_BG   = 0xFF05080E; // #05080E Deep blackish navy
    private static final int COLOR_CARD_HEADER  = 0xFF0A0F1D; // #0A0F1D Top banner inside content
    private static final int COLOR_BORDER       = 0xFF141F36; // #141F36 Subtle dark blue border
    private static final int COLOR_ACCENT_BLUE  = 0xFF1B64FF; // #1B64FF Electric vibrant blue
    private static final int COLOR_TEXT_WHITE   = 0xFFFFFFFF; // #FFFFFF Crisp white
    private static final int COLOR_TEXT_LABEL   = 0xFFE1E7F5; // #E1E7F5 Off-white for labels
    private static final int COLOR_TEXT_MUTED   = 0xFF7D8FA9; // #7D8FA9 Steel gray for descriptions
    private static final int COLOR_DROPDOWN_BG  = 0xFF0B1020; // #0B1020 Dark input fields

    private int activeTab = 0;
    private final List<LinearLayout> tabLayouts = new ArrayList<>();
    private FrameLayout contentFrame;
    private FrameLayout modalOverlay;

    // Drag tracking
    private float lastTouchX, lastTouchY;

    public FluoriteMenuView(Context context, MenuListener listener) {
        super(context);
        this.ctx = context;
        this.listener = listener;
        initUI();
    }

    private void initUI() {
        // Outer Main Window Frame
        LinearLayout windowLayout = new LinearLayout(ctx);
        windowLayout.setOrientation(LinearLayout.HORIZONTAL);

        GradientDrawable winBg = new GradientDrawable();
        winBg.setColor(COLOR_WINDOW_BG);
        winBg.setCornerRadius(dp(14));
        winBg.setStroke(dp(1.2f), COLOR_BORDER);
        windowLayout.setBackground(winBg);

        int menuWidth = dp(460);
        int menuHeight = dp(310);
        LayoutParams params = new LayoutParams(menuWidth, menuHeight);
        windowLayout.setLayoutParams(params);

        // 1. Sidebar (Left Column)
        View sidebar = createSidebar();
        windowLayout.addView(sidebar);

        // 2. Main Content Area (Right Column)
        contentFrame = new FrameLayout(ctx);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f
        );
        contentFrame.setLayoutParams(contentParams);
        contentFrame.setPadding(dp(10), dp(10), dp(12), dp(10));
        windowLayout.addView(contentFrame);

        addView(windowLayout);

        // Modal Overlay for Dropdown selections
        modalOverlay = new FrameLayout(ctx);
        modalOverlay.setLayoutParams(new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        modalOverlay.setBackgroundColor(0xAA000000);
        modalOverlay.setVisibility(View.GONE);
        modalOverlay.setOnClickListener(v -> hideDropdownModal());
        addView(modalOverlay);

        // Enable Dragging on whole menu or blank areas
        windowLayout.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - lastTouchX;
                    float dy = event.getRawY() - lastTouchY;
                    if (listener != null) listener.onDrag((int) dx, (int) dy);
                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();
                    return true;
            }
            return false;
        });

        // Show default tab (Aimbot)
        switchTab(0);
    }

    private View createSidebar() {
        LinearLayout sidebar = new LinearLayout(ctx);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setLayoutParams(new LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.MATCH_PARENT));

        GradientDrawable sideBg = new GradientDrawable();
        sideBg.setColor(COLOR_SIDEBAR_BG);
        sideBg.setCornerRadii(new float[]{dp(14), dp(14), 0, 0, 0, 0, dp(14), dp(14)});
        sidebar.setBackground(sideBg);
        sidebar.setPadding(0, dp(10), 0, dp(10));
        sidebar.setGravity(Gravity.CENTER_HORIZONTAL);

        String[] tabNames = {"Aimbot", "Visuals", "Misc", "Settings"};
        String[] icons = {"🎯", "👁", "🧰", "⚙"};

        tabLayouts.clear();
        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;
            LinearLayout tab = new LinearLayout(ctx);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(0, dp(10), 0, dp(10));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
            );
            tab.setLayoutParams(p);

            // Icon
            TextView iconView = new TextView(ctx);
            iconView.setText(icons[i]);
            iconView.setTextSize(18);
            iconView.setGravity(Gravity.CENTER);
            tab.addView(iconView);

            // Label
            TextView label = new TextView(ctx);
            label.setText(tabNames[i]);
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            label.setTypeface(null, Typeface.NORMAL);
            label.setPadding(0, dp(3), 0, 0);
            tab.addView(label);

            tab.setOnClickListener(v -> switchTab(index));

            tabLayouts.add(tab);
            sidebar.addView(tab);
        }

        return sidebar;
    }

    private void switchTab(int index) {
        activeTab = index;

        // Update active sidebar pill highlight and vertical indicator bar
        for (int i = 0; i < tabLayouts.size(); i++) {
            LinearLayout tab = tabLayouts.get(i);
            TextView label = (TextView) tab.getChildAt(1);

            if (i == index) {
                GradientDrawable activeBg = new GradientDrawable();
                activeBg.setColor(0x331B64FF);
                activeBg.setCornerRadius(dp(8));
                activeBg.setStroke(dp(1.2f), COLOR_ACCENT_BLUE);
                tab.setBackground(activeBg);
                label.setTextColor(COLOR_TEXT_WHITE);
            } else {
                tab.setBackgroundColor(Color.TRANSPARENT);
                label.setTextColor(COLOR_TEXT_MUTED);
            }
        }

        // Render Active Tab Views
        contentFrame.removeAllViews();
        switch (index) {
            case 0: contentFrame.addView(createAimbotView()); break;
            case 1: contentFrame.addView(createVisualsView()); break;
            case 2: contentFrame.addView(createMiscView()); break;
            case 3: contentFrame.addView(createSettingsView()); break;
        }
    }

    // ==========================================
    // 1. AIMBOT TAB (Exact match to Screenshot 4)
    // ==========================================
    private View createAimbotView() {
        LinearLayout layout = createContentBase("AIMBOT", "Automatically aim at enemies.", "🎯");
        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);

        // Checkbox: Master switch
        addCheckbox(list, "Master switch", true, null);

        // Dropdown: Aiming method
        addDropdown(list, "Aiming method", "Silent aimbot", new String[]{
            "Silent aimbot", "Memory aimbot", "Bullet tracking", "FOV smooth snap"
        });

        // Checkbox: Show FOV circle with white color badge
        addCheckboxWithColorBadge(list, "Show FOV circle", true, 0xFFFFFFFF, null);

        // Slider: FOV radius (60.0°)
        addSlider(list, "FOV radius", 60.0f, 0.0f, 180.0f, "°", 1);

        // Slider: Lock-on speed (0.0)
        addSlider(list, "Lock-on speed", 0.0f, 0.0f, 10.0f, "", 1);

        // Slider: Max aim distance (120.0m)
        addSlider(list, "Max aim distance", 120.0f, 0.0f, 300.0f, "m", 1);

        scroll.addView(list);
        layout.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return layout;
    }

    // ==========================================
    // 2. VISUALS TAB (Exact match to Screenshot 3)
    // ==========================================
    private View createVisualsView() {
        LinearLayout layout = createContentBase("VISUALS", "Visual improvements.", "👁");
        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);

        // Checkbox: Enemy ESP
        addCheckbox(list, "Enemy ESP", false, null);

        // Checkbox: Line with White Color Box
        addCheckboxWithColorBadge(list, "Line", false, 0xFFFFFFFF, null);

        // Dropdown: Line origin
        addDropdown(list, "Line origin", "Bottom screen", new String[]{
            "Bottom screen", "Crosshair center", "Top screen"
        });

        // Checkbox: Line fire material
        addCheckbox(list, "Line fire material", false, null);

        // Checkbox: Box with Red & Green Color Boxes
        addCheckboxWithDualColorBadge(list, "Box", false, 0xFFFF2D55, 0xFF00E676, null);

        // Dropdown: Box style
        addDropdown(list, "Box style", "2D Full Box", new String[]{
            "2D Full Box", "2D Corner Box", "3D Box Wireframe"
        });

        // Checkbox: Health
        addCheckbox(list, "Health", false, null);

        // Checkbox: Skeleton
        addCheckbox(list, "Skeleton", false, null);

        // Checkbox: Distance
        addCheckbox(list, "Distance", false, null);

        scroll.addView(list);
        layout.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return layout;
    }

    // ==========================================
    // 3. MISC TAB (Exact match to Screenshot 2)
    // ==========================================
    private View createMiscView() {
        LinearLayout layout = createContentBase("MISC", "Game enhancements.", "🧰");
        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);

        // Warning Text matching screenshot 2
        TextView warningHeader = new TextView(ctx);
        warningHeader.setText("These features are only for fun and may be unsafe.\nUse them at your own risk!");
        warningHeader.setTextColor(COLOR_TEXT_WHITE);
        warningHeader.setTextSize(11);
        warningHeader.setLineSpacing(3, 1.15f);
        warningHeader.setPadding(0, 0, 0, dp(8));
        list.addView(warningHeader);

        // Checkboxes
        addCheckbox(list, "No fog", false, null);
        addCheckbox(list, "No weapon spread", false, null);
        addCheckbox(list, "Instant loot", false, null);
        addCheckbox(list, "Inverted IceWall rotation", false, null);
        addCheckbox(list, "Aspect ratio (iPad View)", false, null);

        // Slider: Camera zoom scale (1.2x)
        addSlider(list, "Camera zoom scale", 1.2f, 1.0f, 3.0f, "x", 1);

        // Checkbox: Auto-fire
        addCheckbox(list, "Auto-fire", false, null);

        scroll.addView(list);
        layout.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return layout;
    }

    // ==========================================
    // 4. SETTINGS TAB (Exact match to Screenshot 1)
    // ==========================================
    private View createSettingsView() {
        LinearLayout layout = createContentBase("SETTINGS", "Configure options.", "⚙");
        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);

        // Header Info text
        TextView buildMeta = new TextView(ctx);
        buildMeta.setText(
            "OB52 1.11 (14affab80a9c35e0) (null | 7fffffffffffffff)\n(0|0|0|0|0|0)"
        );
        buildMeta.setTextColor(COLOR_TEXT_WHITE);
        buildMeta.setTextSize(11);
        buildMeta.setTypeface(null, Typeface.BOLD);
        buildMeta.setPadding(0, 0, 0, dp(4));
        list.addView(buildMeta);

        // Accent color row
        LinearLayout accentRow = new LinearLayout(ctx);
        accentRow.setOrientation(LinearLayout.HORIZONTAL);
        accentRow.setGravity(Gravity.CENTER_VERTICAL);
        accentRow.setPadding(0, dp(3), 0, dp(3));

        TextView accentLabel = new TextView(ctx);
        accentLabel.setText("Accent color");
        accentLabel.setTextColor(COLOR_TEXT_LABEL);
        accentLabel.setTextSize(12);
        accentRow.addView(accentLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        View colorBadge = createColorPill(COLOR_ACCENT_BLUE);
        accentRow.addView(colorBadge);
        list.addView(accentRow);

        // Subscription time & build info (Blue highlight text)
        TextView subInfo = new TextView(ctx);
        subInfo.setText("Subscription time left: ");
        subInfo.setTextColor(COLOR_TEXT_WHITE);
        subInfo.setTextSize(11);

        TextView subTime = new TextView(ctx);
        subTime.setText("Subscription time left: 16 days, 23 hours, 52 minutes, 26 seconds");
        subTime.setTextColor(COLOR_ACCENT_BLUE);
        subTime.setTextSize(11);
        subTime.setPadding(0, dp(2), 0, 0);
        list.addView(subTime);

        TextView buildInfo = new TextView(ctx);
        buildInfo.setText("Build at Jan 20 2026 22:05:22 - 1.7.1 for game version 1.120.X");
        buildInfo.setTextColor(COLOR_ACCENT_BLUE);
        buildInfo.setTextSize(11);
        buildInfo.setPadding(0, dp(2), 0, dp(6));
        list.addView(buildInfo);

        // Checkbox: Streamproof
        addCheckbox(list, "Streamproof", false, null);

        // Dropdown: Language
        addDropdown(list, "Language", "English", new String[]{"English", "Spanish", "Portuguese", "Russian", "Arabic"});

        // Button: Enable silent mode
        Button btnSilent = createActionPillButton("Enable silent mode", COLOR_ACCENT_BLUE, COLOR_TEXT_WHITE);
        btnSilent.setOnClickListener(v -> Toast.makeText(ctx, "Silent mode activated", Toast.LENGTH_SHORT).show());
        list.addView(btnSilent);

        // Button: Save settings
        Button btnSave = createActionPillButton("Save settings", COLOR_ACCENT_BLUE, COLOR_TEXT_WHITE);
        btnSave.setOnClickListener(v -> Toast.makeText(ctx, "Settings Saved Successfully!", Toast.LENGTH_SHORT).show());
        list.addView(btnSave);

        scroll.addView(list);
        layout.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return layout;
    }

    // ==========================================
    // UI HELPER COMPONENTS
    // ==========================================
    private LinearLayout createContentBase(String title, String subtitle, String icon) {
        LinearLayout base = new LinearLayout(ctx);
        base.setOrientation(LinearLayout.VERTICAL);
        base.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Top Banner Card Header matching screenshots
        LinearLayout topBanner = new LinearLayout(ctx);
        topBanner.setOrientation(LinearLayout.HORIZONTAL);
        topBanner.setGravity(Gravity.CENTER_VERTICAL);
        topBanner.setPadding(dp(10), dp(8), dp(10), dp(8));

        GradientDrawable bannerBg = new GradientDrawable();
        bannerBg.setColor(COLOR_CARD_HEADER);
        bannerBg.setCornerRadius(dp(6));
        bannerBg.setStroke(dp(1), COLOR_BORDER);
        topBanner.setBackground(bannerBg);

        TextView iconText = new TextView(ctx);
        iconText.setText(icon + " ");
        iconText.setTextSize(12);
        topBanner.addView(iconText);

        TextView titleText = new TextView(ctx);
        titleText.setText(title + "  ");
        titleText.setTextColor(COLOR_ACCENT_BLUE);
        titleText.setTextSize(12);
        titleText.setTypeface(null, Typeface.BOLD);
        topBanner.addView(titleText);

        TextView divider = new TextView(ctx);
        divider.setText("|   ");
        divider.setTextColor(COLOR_TEXT_MUTED);
        divider.setTextSize(12);
        topBanner.addView(divider);

        TextView subtitleText = new TextView(ctx);
        subtitleText.setText(subtitle);
        subtitleText.setTextColor(COLOR_TEXT_MUTED);
        subtitleText.setTextSize(11);
        topBanner.addView(subtitleText);

        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bannerParams.setMargins(0, 0, 0, dp(8));
        topBanner.setLayoutParams(bannerParams);

        base.addView(topBanner);
        return base;
    }

    private void addCheckbox(LinearLayout parent, String title, boolean defaultChecked, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(2));

        CheckBox cb = new CheckBox(ctx);
        cb.setChecked(defaultChecked);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT_BLUE));
        if (listener != null) cb.setOnCheckedChangeListener(listener);
        row.addView(cb);

        TextView label = new TextView(ctx);
        label.setText(title);
        label.setTextSize(12);
        label.setTextColor(COLOR_TEXT_LABEL);
        label.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));
        row.addView(label);

        parent.addView(row);
    }

    private void addCheckboxWithColorBadge(LinearLayout parent, String title, boolean defaultChecked, int colorHex, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(2));

        CheckBox cb = new CheckBox(ctx);
        cb.setChecked(defaultChecked);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT_BLUE));
        if (listener != null) cb.setOnCheckedChangeListener(listener);
        row.addView(cb);

        TextView label = new TextView(ctx);
        label.setText(title);
        label.setTextSize(12);
        label.setTextColor(COLOR_TEXT_LABEL);
        label.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        View badge = createColorPill(colorHex);
        row.addView(badge);

        parent.addView(row);
    }

    private void addCheckboxWithDualColorBadge(LinearLayout parent, String title, boolean defaultChecked, int color1, int color2, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(2));

        CheckBox cb = new CheckBox(ctx);
        cb.setChecked(defaultChecked);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT_BLUE));
        if (listener != null) cb.setOnCheckedChangeListener(listener);
        row.addView(cb);

        TextView label = new TextView(ctx);
        label.setText(title);
        label.setTextSize(12);
        label.setTextColor(COLOR_TEXT_LABEL);
        label.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        View badge1 = createColorPill(color1);
        row.addView(badge1);

        View spacer = new View(ctx);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(4), 1));
        row.addView(spacer);

        View badge2 = createColorPill(color2);
        row.addView(badge2);

        parent.addView(row);
    }

    private View createColorPill(int color) {
        View pill = new View(ctx);
        int w = dp(16);
        int h = dp(14);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(2), 0, dp(4), 0);
        pill.setLayoutParams(p);

        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(3));
        pill.setBackground(d);
        return pill;
    }

    private void addDropdown(LinearLayout parent, final String label, String defaultValue, final String[] options) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dp(3), 0, dp(4));

        TextView labelView = new TextView(ctx);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(COLOR_TEXT_LABEL);
        labelView.setPadding(dp(2), 0, 0, dp(3));
        layout.addView(labelView);

        final Button btn = new Button(ctx);
        btn.setText(defaultValue + "                                    ▾");
        btn.setTextSize(11);
        btn.setTextColor(COLOR_TEXT_WHITE);
        btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        btn.setPadding(dp(12), dp(6), dp(12), dp(6));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(COLOR_DROPDOWN_BG);
        btnBg.setCornerRadius(dp(5));
        btnBg.setStroke(dp(1), COLOR_BORDER);
        btn.setBackground(btnBg);

        btn.setOnClickListener(v -> {
            showDropdownModal(label, options, selected -> {
                btn.setText(selected + "                                    ▾");
            });
        });

        layout.addView(btn);
        parent.addView(layout);
    }

    private void addSlider(LinearLayout parent, final String title, float defaultValue, final float min, final float max, final String unit, final int decimals) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dp(3), 0, dp(3));

        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView titleView = new TextView(ctx);
        titleView.setText(title);
        titleView.setTextSize(12);
        titleView.setTextColor(COLOR_TEXT_LABEL);
        topRow.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        final TextView valView = new TextView(ctx);
        valView.setText(formatVal(defaultValue, decimals) + unit);
        valView.setTextSize(11);
        valView.setTextColor(COLOR_ACCENT_BLUE);
        valView.setTypeface(null, Typeface.BOLD);
        topRow.addView(valView);

        layout.addView(topRow);

        SeekBar seekBar = new SeekBar(ctx);
        int steps = 1000;
        seekBar.setMax(steps);
        int initialProgress = Math.round(((defaultValue - min) / (max - min)) * steps);
        seekBar.setProgress(initialProgress);
        seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT_BLUE));
        seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT_BLUE));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float actual = min + ((float) progress / steps) * (max - min);
                valView.setText(formatVal(actual, decimals) + unit);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        layout.addView(seekBar);
        parent.addView(layout);
    }

    private String formatVal(float val, int decimals) {
        if (decimals == 0) return String.valueOf(Math.round(val));
        return String.format("%." + decimals + "f", val);
    }

    private Button createActionPillButton(String text, int bgColor, int textColor) {
        Button btn = new Button(ctx);
        btn.setText(text);
        btn.setTextSize(12);
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
        p.setMargins(0, dp(6), 0, dp(4));
        btn.setLayoutParams(p);
        return btn;
    }

    private void showDropdownModal(String title, String[] options, final DropdownSelectionListener callback) {
        modalOverlay.removeAllViews();

        LinearLayout modalCard = new LinearLayout(ctx);
        modalCard.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(COLOR_DROPDOWN_BG);
        cardBg.setCornerRadius(dp(8));
        cardBg.setStroke(dp(1.2f), COLOR_ACCENT_BLUE);
        modalCard.setBackground(cardBg);
        modalCard.setPadding(dp(16), dp(12), dp(16), dp(12));

        LayoutParams cardParams = new LayoutParams(dp(260), ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.CENTER;
        modalCard.setLayoutParams(cardParams);

        TextView titleView = new TextView(ctx);
        titleView.setText(title);
        titleView.setTextSize(13);
        titleView.setTextColor(COLOR_TEXT_WHITE);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(0, 0, 0, dp(8));
        modalCard.addView(titleView);

        for (final String opt : options) {
            TextView optView = new TextView(ctx);
            optView.setText(opt);
            optView.setTextSize(12);
            optView.setTextColor(COLOR_TEXT_LABEL);
            optView.setPadding(dp(6), dp(8), dp(6), dp(8));

            optView.setOnClickListener(v -> {
                if (callback != null) callback.onSelected(opt);
                hideDropdownModal();
            });
            modalCard.addView(optView);
        }

        modalOverlay.addView(modalCard);
        modalOverlay.setVisibility(View.VISIBLE);
    }

    private void hideDropdownModal() {
        modalOverlay.setVisibility(View.GONE);
        modalOverlay.removeAllViews();
    }

    private int dp(float value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
