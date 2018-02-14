package com.insprout.okubo.mytool;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by okubo on 2018/02/08.
 */

public class RulerView extends View {
    private final static float LINE_LENGTH_SCALE_10 = 7.0f;     // 10の目盛りの長さ。ミリ単位
    private final static float LINE_WIDTH_SCALE_10 = 0.15f;     // 10の目盛りの太さ。ミリ単位
    private final static float LINE_LENGTH_SCALE_05 = 5.5f;     // 5の目盛りの長さ。ミリ単位
    private final static float LINE_WIDTH_SCALE_05 = 0.15f;     // 5の目盛りの太さ。ミリ単位
    private final static float LINE_LENGTH_SCALE_01 = 4.0f;     // 1の目盛りの長さ。ミリ単位
    private final static float LINE_WIDTH_SCALE_01 = 0.10f;     // 1の目盛りの太さ。ミリ単位

    private final static float SIZE_SCALE_LABEL = 3.5f;
    private final static float MARGIN_SCALE_LABEL = LINE_LENGTH_SCALE_10 + 2.0f;


    Context mContext;
    Paint mPaint;

    float mYDotsPer1Millimeter;
    float mAdjustRate;
    int mLineColor;
    int mTextColor;


    public RulerView(Context context) {
        super(context);
        initialize(context);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        mContext = context;
        mPaint = new Paint();

        mLineColor = Color.BLUE;
        mTextColor = Color.GRAY;
        mYDotsPer1Millimeter = context.getResources().getDisplayMetrics().ydpi / 25.4f;
        mAdjustRate = 1.0f;
    }

    public void setAdjustRate(float adjustRate) {
        if (adjustRate >= 0.5f && adjustRate <= 1.5f) mAdjustRate = adjustRate;
    }


    // 描画処理を記述
    @Override
    protected void onDraw(Canvas canvas) {
        int minX = getPaddingStart();
        int maxX = canvas.getWidth() - 1 - getPaddingEnd();
        int minY = getPaddingTop();
        int maxY = canvas.getHeight() - 1 - getPaddingBottom();
        float lineLength, lineWidth;
        String scaleLabel;

        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        float y = minY;
        int i = 0;
        while(y <= maxY) {
            y = i * mYDotsPer1Millimeter * mAdjustRate + minY;

            scaleLabel = null;
            if (mod(i, 10) == 0) {
                // 10の倍数の時
                lineWidth = LINE_WIDTH_SCALE_10;
                lineLength = LINE_LENGTH_SCALE_10;

                if (i > 0) {
                    scaleLabel = Integer.toString(i / 10);
                    mPaint.setColor(mTextColor);
                    mPaint.setStrokeWidth(1);
                    mPaint.setTextSize(SIZE_SCALE_LABEL * mYDotsPer1Millimeter);
                    float labelWidth = getTextWidth(mPaint, scaleLabel);
                    float labelHeight = getTextHeight(mPaint);

                    // 目盛りの 表記
                    canvas.drawText(scaleLabel, maxX - MARGIN_SCALE_LABEL * mYDotsPer1Millimeter - labelWidth, y + labelHeight / 3, mPaint);
                }

            } else if (mod(i, 5) == 0) {
                // 5の倍数の時
                lineWidth = LINE_WIDTH_SCALE_05;
                lineLength = LINE_LENGTH_SCALE_05;

            } else {
                // その他
                lineWidth = LINE_WIDTH_SCALE_01;
                lineLength = LINE_LENGTH_SCALE_01;
            }
            mPaint.setColor(mLineColor);
            mPaint.setStrokeWidth(round(lineWidth * mYDotsPer1Millimeter));
            canvas.drawLine(maxX - lineLength * mYDotsPer1Millimeter, y, maxX, y, mPaint);

            i++;
        }
    }

    private float getTextWidth(Paint p, String text) {
        return p.measureText(text);
    }

    private float getTextHeight(Paint p) {
        Paint.FontMetrics fontMetrics = p.getFontMetrics();
        return Math.abs(fontMetrics.ascent) + Math.abs(fontMetrics.descent) + Math.abs(fontMetrics.leading);
    }

    private int mod(int value1, int value2) {
        if (value2 == 0) return 0;
        return value1 - (value1 / value2) * value2;
    }

    private int round(float value) {
        return (int) (value + 0.5f);
    }

}
