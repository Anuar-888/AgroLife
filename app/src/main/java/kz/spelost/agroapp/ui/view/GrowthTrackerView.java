package kz.spelost.agroapp.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import kz.spelost.agroapp.R;

public class GrowthTrackerView extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private int progress = 0; // 0 to 100

    public GrowthTrackerView(Context context) {
        super(context);
        init();
    }

    public GrowthTrackerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(getResources().getColor(R.color.line));
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(12f);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(getResources().getColor(R.color.primary));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(12f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgress(int progress) {
        this.progress = Math.min(100, Math.max(0, progress));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float padding = 20f;
        
        float circleRadius = 15f;
        float bottomY = height - padding - circleRadius;
        float topY = padding + circleRadius;
        float centerX = width / 2;

        // Draw Circle at the bottom (seed)
        backgroundPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, bottomY + circleRadius, circleRadius, backgroundPaint);
        
        // Draw Background Line (stem)
        backgroundPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(centerX, bottomY, centerX, topY, backgroundPaint);

        // Draw Progress Line
        float progressY = bottomY - (bottomY - topY) * (progress / 100f);
        canvas.drawLine(centerX, bottomY, centerX, progressY, progressPaint);

        // Draw Progress Circle (head)
        progressPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, progressY, circleRadius, progressPaint);
    }
}
