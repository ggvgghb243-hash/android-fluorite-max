package com.fluoritemax.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class FloatingButtonView extends View {

    public interface BadgeListener {
        void onClick();
        void onMove(int deltaX, int deltaY);
    }

    private final BadgeListener listener;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;

    public FloatingButtonView(Context context, BadgeListener listener) {
        super(context);
        this.listener = listener;

        bgPaint.setColor(0xEE0A0D15); // Dark semi-transparent
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(0xFF1852FF); // Royal blue border
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3.5f);

        iconPaint.setColor(0xFF1852FF);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(3.0f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = dpToPx(48);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2.0f;
        float cy = getHeight() / 2.0f;
        float radius = Math.min(cx, cy) - 4.0f;

        // Background circle
        canvas.drawCircle(cx, cy, radius, bgPaint);
        // Border circle
        canvas.drawCircle(cx, cy, radius, borderPaint);

        // Vector Crosshair Icon in center
        float iconR = radius * 0.45f;
        canvas.drawCircle(cx, cy, iconR, iconPaint);
        
        // 4 ticks
        float tickLen = 4.0f;
        canvas.drawLine(cx, cy - iconR - tickLen, cx, cy - iconR + 2, iconPaint);
        canvas.drawLine(cx, cy + iconR - 2, cx, cy + iconR + tickLen, iconPaint);
        canvas.drawLine(cx - iconR - tickLen, cy, cx - iconR + 2, cy, iconPaint);
        canvas.drawLine(cx + iconR - 2, cy, cx + iconR + tickLen, cy, iconPaint);

        // Center dot
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(0xFFEBF0FA);
        dotPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, 2.5f, dotPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.hypot(dx, dy) > 8) {
                    isDragging = true;
                    if (listener != null) {
                        listener.onMove((int) dx, (int) dy);
                    }
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!isDragging && listener != null) {
                    listener.onClick();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
