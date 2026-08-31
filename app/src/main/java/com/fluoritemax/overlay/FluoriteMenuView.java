package com.fluoritemax.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
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
import android.widget.PopupWindow;
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
    private static final int COLOR_WINDOW_BG    = 0xF5050811; // #050811 Dark sleek translucent
    private static final int COLOR_SIDEBAR_BG   = 0xFF04060C; // #04060C Deep black-navy
    private static final int COLOR_CARD_HEADER  = 0xFF080D1A; // #080D1A Header banner
    private static final int COLOR_BORDER       = 0xFF141F38; // #141F38 Subtle crisp border
    private static final int COLOR_ACCENT_BLUE  = 0xFF1B64FF; // #1B64FF Vibrant royal blue
    private static final int COLOR_TEXT_WHITE   = 0xFFFFFFFF; // #FFFFFF Pure white
    private static final int COLOR_TEXT_LABEL   = 0xFFE2E8F4; // #E2E8F4 Clean text
    private static final int COLOR_TEXT_MUTED   = 0xFF7D8FA9; // #7D8FA9 Steel description text
    private static final int COLOR_DROPDOWN_BG  = 0xFF080D1A; // #080D1A Dropdown box
    private static final int COLOR_POPUP_BG     = 0xFF0A0F1E; // #0A0F1E Sleek dropdown menu bg

    private int activeTab = 0;
    private final List<SidebarItemView> tabViews = new ArrayList<>();
    private FrameLayout contentFrame;

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

        int menuWidth = dp(490);
        int menuHeight = dp(330);
        LayoutParams params = new LayoutParams(menuWidth, menuHeight);
        windowLayout.setLayoutParams(params);

        // 1. Sidebar (Left Column with Custom Vector Glyphs and Empty Space at bottom)
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

        // Window Dragging
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

        // Default tab (Aimbot)
        switchTab(0);
    }

    private View createSidebar() {
        LinearLayout sidebar = new LinearLayout(ctx);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setLayoutParams(new LinearLayout.LayoutParams(dp(76), ViewGroup.LayoutParams.MATCH_PARENT));

        GradientDrawable sideBg = new GradientDrawable();
        sideBg.setColor(COLOR_SIDEBAR_BG);
        sideBg.setCornerRadii(new float[]{dp(14), dp(14), 0, 0, 0, 0, dp(14), dp(14)});
        sidebar.setBackground(sideBg);
        sidebar.setPadding(0, dp(8), 0, dp(8));
        sidebar.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);

        String[] tabNames = {"Aimbot", "Visuals", "Misc", "Settings"};
        int[] iconTypes = {
            SidebarItemView.ICON_AIMBOT,
            SidebarItemView.ICON_VISUALS,
            SidebarItemView.ICON_MISC,
            SidebarItemView.ICON_SETTINGS
        };

        tabViews.clear();
        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;
            SidebarItemView item = new SidebarItemView(ctx, iconTypes[i], tabNames[i]);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)
            );
            p.setMargins(dp(5), dp(2), dp(5), dp(2));
            item.setLayoutParams(p);

            item.setOnClickListener(v -> switchTab(index));

            tabViews.add(item);
            sidebar.addView(item);
        }

        // Empty space below Settings
        View emptyBottomSpace = new View(ctx);
        LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        );
        emptyBottomSpace.setLayoutParams(emptyParams);
        sidebar.addView(emptyBottomSpace);

        return sidebar;
    }

    private void switchTab(int index) {
        activeTab = index;

        for (int i = 0; i < tabViews.size(); i++) {
            tabViews.get(i).setSelected(i == index);
        }

        contentFrame.removeAllViews();
        switch (index) {
            case 0: contentFrame.addView(createAimbotView()); break;
            case 1: contentFrame.addView(createVisualsView()); break;
            case 2: contentFrame.addView(createMiscView()); break;
            case 3: contentFrame.addView(createSettingsView()); break;
        }
    }

    // ==========================================
    // 1. AIMBOT TAB (Subtitle: Fluorite Max)
    // ==========================================
    private View createAimbotView() {
        LinearLayout layout = createContentBase("AIMBOT", "Fluorite Max", SidebarItemView.ICON_AIMBOT);
        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);

        // Checkbox: Master switch (default unchecked)
        addCheckbox(list, "Master switch", false, null);

        // Dropdown: Aiming method (Clean slim design)
        addDropdown(list, "Aiming method", "Silent aimbot", new String[]{
            "Silent aimbot", "Memory aimbot", "Bullet tracking", "FOV smooth snap"
        });

        // Checkbox: Show FOV circle with white square preview (default unchecked)
        addCheckboxWithColorBadge(list, "Show FOV circle", false, 0xFFFFFFFF, null);

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
    // 2. VISUALS TAB (Subtitle: Fluorite Max)
    // ==========================================
    private View createVisualsView() {
        LinearLayout layout = createContentBase("VISUALS", "Fluorite Max", SidebarItemView.ICON_VISUALS);
        ScrollView scroll = new ScrollView(ctx);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);

        // Checkbox: Enemy ESP (default unchecked)
        addCheckbox(list, "Enemy ESP", false, null);

        // Checkbox: Line with White Color Box (default unchecked)
        addCheckboxWithColorBadge(list, "Line", false, 0xFFFFFFFF, null);

        // Dropdown: Line origin
        addDropdown(list, "Line origin", "Bottom screen", new String[]{
            "Bottom screen", "Crosshair center", "Top screen"
        });

        // Checkbox: Line fire material (default unchecked)
        addCheckbox(list, "Line fire material", false, null);

        // Checkbox: Box with Red & Green Color Boxes (default unchecked)
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
    // 3. MISC TAB (Subtitle: Fluorite Max)
    // ==========================================
    private View createMiscView() {
        LinearLayout layout = createContentBase("MISC", "Fluorite Max", SidebarItemView.ICON_MISC);
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

        // Checkboxes (default unchecked)
        addCheckbox(list, "No fog", false, null);
        addCheckbox(list, "No weapon spread", false, null);
        addCheckbox(list, "Instant loot", false, null);
        addCheckbox(list, "Inverted IceWall rotation", false, null);
        addCheckbox(list, "Aspect ratio (iPad View)", false, null);

        // Slider: Camera zoom scale (1.2x)
        addSlider(list, "Camera zoom scale", 1.2f, 1.0f, 3.0f, "x", 1);

        // Checkbox: Auto-fire (default unchecked)
        addCheckbox(list, "Auto-fire", false, null);

        scroll.addView(list);
        layout.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return layout;
    }

    // ==========================================
    // 4. SETTINGS TAB (Subtitle: Fluorite Max)
    // ==========================================
    private View createSettingsView() {
        LinearLayout layout = createContentBase("SETTINGS", "Fluorite Max", SidebarItemView.ICON_SETTINGS);
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

        // Subscription time (Blue highlighted values)
        TextView subInfo = new TextView(ctx);
        subInfo.setText("Subscription time left: 16 days, 23 hours, 52 minutes, 26 seconds");
        subInfo.setTextColor(COLOR_ACCENT_BLUE);
        subInfo.setTextSize(11);
        subInfo.setPadding(0, dp(2), 0, 0);
        list.addView(subInfo);

        TextView buildInfo = new TextView(ctx);
        buildInfo.setText("Build at Jan 20 2026 22:05:22 - 1.7.1 for game version 1.120.X");
        buildInfo.setTextColor(COLOR_ACCENT_BLUE);
        buildInfo.setTextSize(11);
        buildInfo.setPadding(0, dp(2), 0, dp(6));
        list.addView(buildInfo);

        // Checkbox: Streamproof (default unchecked)
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
    private LinearLayout createContentBase(String title, String subtitle, int iconType) {
        LinearLayout base = new LinearLayout(ctx);
        base.setOrientation(LinearLayout.VERTICAL);
        base.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Top Banner Card Header matching screenshots
        LinearLayout topBanner = new LinearLayout(ctx);
        topBanner.setOrientation(LinearLayout.HORIZONTAL);
        topBanner.setGravity(Gravity.CENTER_VERTICAL);
        topBanner.setPadding(dp(12), dp(7), dp(12), dp(7));

        GradientDrawable bannerBg = new GradientDrawable();
        bannerBg.setColor(COLOR_CARD_HEADER);
        bannerBg.setCornerRadius(dp(6));
        bannerBg.setStroke(dp(1), COLOR_BORDER);
        topBanner.setBackground(bannerBg);

        // Vector Icon in header
        VectorIconView headerIcon = new VectorIconView(ctx, iconType, COLOR_ACCENT_BLUE);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(15), dp(15));
        iconParams.setMargins(0, 0, dp(8), 0);
        headerIcon.setLayoutParams(iconParams);
        topBanner.addView(headerIcon);

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

        // Subtitle text: Fluorite Max
        TextView subtitleText = new TextView(ctx);
        subtitleText.setText(subtitle);
        subtitleText.setTextColor(COLOR_TEXT_MUTED);
        subtitleText.setTextSize(11.5f);
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

    // Slim, elegant dropdown input with smooth anchored popup
    private void addDropdown(LinearLayout parent, final String label, final String defaultValue, final String[] options) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dp(2), 0, dp(4));

        TextView labelView = new TextView(ctx);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(COLOR_TEXT_LABEL);
        labelView.setPadding(dp(2), 0, 0, dp(2));
        layout.addView(labelView);

        // Slim dropdown box container
        final LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(10), dp(6), dp(10), dp(6));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(COLOR_DROPDOWN_BG);
        btnBg.setCornerRadius(dp(5));
        btnBg.setStroke(dp(1), COLOR_BORDER);
        box.setBackground(btnBg);

        final TextView valText = new TextView(ctx);
        valText.setText(defaultValue);
        valText.setTextSize(11.5f);
        valText.setTextColor(COLOR_TEXT_WHITE);
        box.addView(valText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView arrow = new TextView(ctx);
        arrow.setText("▾");
        arrow.setTextSize(11.5f);
        arrow.setTextColor(COLOR_TEXT_MUTED);
        box.addView(arrow);

        box.setOnClickListener(v -> {
            showDropdownPopup(box, valText, options);
        });

        layout.addView(box);
        parent.addView(layout);
    }

    private void showDropdownPopup(View anchor, final TextView valueView, final String[] options) {
        final PopupWindow popup = new PopupWindow(ctx);

        LinearLayout popupLayout = new LinearLayout(ctx);
        popupLayout.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable popupBg = new GradientDrawable();
        popupBg.setColor(COLOR_POPUP_BG);
        popupBg.setCornerRadius(dp(6));
        popupBg.setStroke(dp(1.2f), COLOR_ACCENT_BLUE);
        popupLayout.setBackground(popupBg);
        popupLayout.setPadding(dp(6), dp(4), dp(6), dp(4));

        for (final String opt : options) {
            TextView optView = new TextView(ctx);
            optView.setText(opt);
            optView.setTextSize(11.5f);
            optView.setTextColor(COLOR_TEXT_LABEL);
            optView.setPadding(dp(10), dp(7), dp(10), dp(7));

            optView.setOnClickListener(v -> {
                valueView.setText(opt);
                popup.dismiss();
            });
            popupLayout.addView(optView);
        }

        popup.setContentView(popupLayout);
        popup.setWidth(anchor.getWidth());
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new GradientDrawable()); // Transparent
        popup.showAsDropDown(anchor, 0, dp(3));
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
        valView.setTextSize(11.5f);
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

    private int dp(float value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    // =========================================================
    // CUSTOM VECTOR ICON DRAWING VIEW (NO EMOJIS - EXACT GLYPHS)
    // =========================================================
    public static class VectorIconView extends View {
        private final int iconType;
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public VectorIconView(Context context, int iconType, int color) {
            super(context);
            this.iconType = iconType;
            strokePaint.setColor(color);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(2.2f);
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            strokePaint.setStrokeJoin(Paint.Join.ROUND);

            fillPaint.setColor(color);
            fillPaint.setStyle(Paint.Style.FILL);
        }

        public void setColor(int color) {
            strokePaint.setColor(color);
            fillPaint.setColor(color);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2.0f;
            float cy = h / 2.0f;
            float r = (Math.min(w, h) / 2.0f) - 3.0f;

            switch (iconType) {
                case SidebarItemView.ICON_AIMBOT: {
                    // Crosshair Circle
                    canvas.drawCircle(cx, cy, r, strokePaint);
                    // 4 Crosshair Ticks
                    float tick = 3.5f;
                    canvas.drawLine(cx, cy - r - tick, cx, cy - r + 2.0f, strokePaint);
                    canvas.drawLine(cx, cy + r - 2.0f, cx, cy + r + tick, strokePaint);
                    canvas.drawLine(cx - r - tick, cy, cx - r + 2.0f, cy, strokePaint);
                    canvas.drawLine(cx + r - 2.0f, cy, cx + r + tick, cy, strokePaint);
                    // Center Dot
                    canvas.drawCircle(cx, cy, 2.0f, fillPaint);
                    break;
                }
                case SidebarItemView.ICON_VISUALS: {
                    // Eye Outline Curves
                    Path eyePath = new Path();
                    float ew = w - 6.0f;
                    float eh = h - 10.0f;
                    eyePath.moveTo(cx - ew / 2.0f, cy);
                    eyePath.quadTo(cx, cy - eh / 1.3f, cx + ew / 2.0f, cy);
                    eyePath.quadTo(cx, cy + eh / 1.3f, cx - ew / 2.0f, cy);
                    eyePath.close();
                    canvas.drawPath(eyePath, strokePaint);
                    // Center Pupil
                    canvas.drawCircle(cx, cy, 3.0f, fillPaint);
                    break;
                }
                case SidebarItemView.ICON_MISC: {
                    // Briefcase / Toolbox Outline
                    float bw = w - 8.0f;
                    float bh = h - 10.0f;
                    RectF boxRect = new RectF(cx - bw / 2.0f, cy - bh / 2.0f + 2.0f, cx + bw / 2.0f, cy + bh / 2.0f + 2.0f);
                    canvas.drawRoundRect(boxRect, 3.0f, 3.0f, strokePaint);
                    // Handle on top
                    Path handlePath = new Path();
                    handlePath.moveTo(cx - 4.0f, cy - bh / 2.0f + 2.0f);
                    handlePath.lineTo(cx - 4.0f, cy - bh / 2.0f - 2.0f);
                    handlePath.lineTo(cx + 4.0f, cy - bh / 2.0f - 2.0f);
                    handlePath.lineTo(cx + 4.0f, cy - bh / 2.0f + 2.0f);
                    canvas.drawPath(handlePath, strokePaint);
                    // Horizontal divider slot
                    canvas.drawLine(cx - bw / 2.0f, cy, cx + bw / 2.0f, cy, strokePaint);
                    break;
                }
                case SidebarItemView.ICON_SETTINGS: {
                    // Gear / Cogwheel
                    int spokes = 6;
                    float rOuter = r;
                    float rInner = r - 3.0f;
                    Path gear = new Path();
                    for (int i = 0; i < spokes; i++) {
                        double a1 = (i * 2.0 * Math.PI / spokes) - 0.25;
                        double a2 = (i * 2.0 * Math.PI / spokes) + 0.25;
                        double a3 = ((i + 1) * 2.0 * Math.PI / spokes) - 0.25;

                        float p1x = (float) (cx + rOuter * Math.cos(a1));
                        float p1y = (float) (cy + rOuter * Math.sin(a1));
                        float p2x = (float) (cx + rOuter * Math.cos(a2));
                        float p2y = (float) (cy + rOuter * Math.sin(a2));
                        float p3x = (float) (cx + rInner * Math.cos(a2 + 0.15));
                        float p3y = (float) (cy + rInner * Math.sin(a2 + 0.15));
                        float p4x = (float) (cx + rInner * Math.cos(a3 - 0.15));
                        float p4y = (float) (cy + rInner * Math.sin(a3 - 0.15));

                        if (i == 0) gear.moveTo(p1x, p1y);
                        else gear.lineTo(p1x, p1y);
                        gear.lineTo(p2x, p2y);
                        gear.lineTo(p3x, p3y);
                        gear.lineTo(p4x, p4y);
                    }
                    gear.close();
                    canvas.drawPath(gear, strokePaint);
                    // Center hole
                    canvas.drawCircle(cx, cy, 3.0f, strokePaint);
                    break;
                }
            }
        }
    }

    // =========================================================
    // SIDEBAR BUTTON VIEW (ICON + TEXT + VERTICAL BLUE INDICATOR)
    // =========================================================
    public static class SidebarItemView extends LinearLayout {
        public static final int ICON_AIMBOT   = 1;
        public static final int ICON_VISUALS  = 2;
        public static final int ICON_MISC     = 3;
        public static final int ICON_SETTINGS = 4;

        private final VectorIconView iconView;
        private final TextView labelView;
        private final View indicator;
        private boolean isSelected = false;

        public SidebarItemView(Context context, int iconType, String title) {
            super(context);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);

            // Container for icon + label
            LinearLayout inner = new LinearLayout(context);
            inner.setOrientation(VERTICAL);
            inner.setGravity(Gravity.CENTER);
            LayoutParams innerParams = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            inner.setLayoutParams(innerParams);

            iconView = new VectorIconView(context, iconType, COLOR_TEXT_MUTED);
            int iconSize = Math.round(17 * getResources().getDisplayMetrics().density);
            iconView.setLayoutParams(new LayoutParams(iconSize, iconSize));
            inner.addView(iconView);

            labelView = new TextView(context);
            labelView.setText(title);
            labelView.setTextSize(10);
            labelView.setTextColor(COLOR_TEXT_MUTED);
            labelView.setGravity(Gravity.CENTER);
            labelView.setPadding(0, Math.round(2 * getResources().getDisplayMetrics().density), 0, 0);
            inner.addView(labelView);

            addView(inner);

            // Vertical Right Indicator Bar
            indicator = new View(context);
            int barW = Math.round(3.0f * getResources().getDisplayMetrics().density);
            LayoutParams barParams = new LayoutParams(barW, Math.round(22 * getResources().getDisplayMetrics().density));
            indicator.setLayoutParams(barParams);
            indicator.setVisibility(GONE);

            GradientDrawable indBg = new GradientDrawable();
            indBg.setColor(COLOR_ACCENT_BLUE);
            indBg.setCornerRadii(new float[]{
                4, 4, 0, 0, 0, 0, 4, 4
            });
            indicator.setBackground(indBg);
            addView(indicator);
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
            float d = getResources().getDisplayMetrics().density;
            if (selected) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0x221B64FF);
                bg.setCornerRadius(6 * d);
                setBackground(bg);
                iconView.setColor(COLOR_TEXT_WHITE);
                labelView.setTextColor(COLOR_TEXT_WHITE);
                indicator.setVisibility(VISIBLE);
            } else {
                setBackgroundColor(Color.TRANSPARENT);
                iconView.setColor(COLOR_TEXT_MUTED);
                labelView.setTextColor(COLOR_TEXT_MUTED);
                indicator.setVisibility(GONE);
            }
        }
    }
}
