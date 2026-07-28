package com.example.humandetection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    public static class Box {
        public final RectF rect;
        public final String label;

        public Box(RectF rect, String label) {
            this.rect = rect;
            this.label = label;
        }
    }

    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<Box> boxes = new ArrayList<>();
    private int sourceWidth = 1;
    private int sourceHeight = 1;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        boxPaint.setColor(Color.parseColor("#00E676"));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setFakeBoldText(true);

        textBgPaint.setColor(Color.parseColor("#CC00A652"));
        textBgPaint.setStyle(Paint.Style.FILL);
    }

    public void setResults(List<Box> boxes, int sourceWidth, int sourceHeight) {
        this.boxes = boxes;
        this.sourceWidth = Math.max(1, sourceWidth);
        this.sourceHeight = Math.max(1, sourceHeight);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (boxes.isEmpty()) return;

        float scaleX = (float) getWidth() / sourceWidth;
        float scaleY = (float) getHeight() / sourceHeight;
        float scale = Math.max(scaleX, scaleY);

        float offsetX = (getWidth() - sourceWidth * scale) / 2f;
        float offsetY = (getHeight() - sourceHeight * scale) / 2f;

        for (Box b : boxes) {
            RectF r = new RectF(
                    b.rect.left * scale + offsetX,
                    b.rect.top * scale + offsetY,
                    b.rect.right * scale + offsetX,
                    b.rect.bottom * scale + offsetY
            );
            canvas.drawRect(r, boxPaint);

            float textWidth = textPaint.measureText(b.label);
            RectF tag = new RectF(r.left, r.top - 46f, r.left + textWidth + 24f, r.top);
            canvas.drawRect(tag, textBgPaint);
            canvas.drawText(b.label, r.left + 12f, r.top - 12f, textPaint);
        }
    }
}
