package com.moskovko.myfirstapp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.animation.ValueAnimator;

/**
 * Created by ilushka on 1/24/17.
 */

public class BatteryChargeView extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final int MAX_BAR_HEIGHT = 660;
    private static final int FULL_BAR_ANIMATION_DURATION = 2500;     // milliseconds

    private String mTest;
    private Paint mBarStrokePaint;
    private Paint mBarFillPaint;
    private int mCurrentBarHeight;
    private float mCurrentChargeLevel;

    public BatteryChargeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BatteryChargeView,
                0, 0);
        try {
            mTest = a.getString(R.styleable.BatteryChargeView_testAttr);
        } finally {
            a.recycle();
        }

        mBarStrokePaint = new Paint();
        mBarStrokePaint.setColor(Color.parseColor("#000000"));
        mBarStrokePaint.setStyle(Paint.Style.STROKE);
        mBarStrokePaint.setStrokeWidth(15);

        mBarFillPaint = new Paint();
        mBarFillPaint.setColor(Color.parseColor("#000000"));
        mBarFillPaint.setStyle(Paint.Style.FILL);

        mCurrentBarHeight = MAX_BAR_HEIGHT;
    }

    public float getCurrentChargeLevel() {
        return mCurrentChargeLevel;
    }

    public void setCurrentChargeLevel(float level) {
        ValueAnimator animation = ValueAnimator.ofFloat(mCurrentChargeLevel, level);
        // scale animation time to how much movement the bar needs to make  
        animation.setDuration((int)(FULL_BAR_ANIMATION_DURATION *
                Math.abs((mCurrentChargeLevel - level))));
        animation.addUpdateListener(this);
        animation.start();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        mCurrentChargeLevel = ((Float)animation.getAnimatedValue()).floatValue();
        mCurrentBarHeight = (int)(MAX_BAR_HEIGHT * mCurrentChargeLevel);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int size = 0;
        if (width < height) {
            size = width;
        } else {
            size = height;
        }
/*
        int minimumWidth = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int width = resolveSizeAndState(minimumWidth, widthMeasureSpec, 0);
        int minHeight = MeasureSpec.getSize(width)  + getPaddingBottom() + getPaddingTop();
        int height = resolveSizeAndState(MeasureSpec.getSize(width), heightMeasureSpec, 0);
*/
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.parseColor("#FF0000"));
        /*
        int centerX = getMeasuredWidth() / 2;
        int centerY = getMeasuredHeight() / 2;
        canvas.drawCircle(centerX, centerY, 100, mPaint);
        */

        canvas.drawRect(100, 100, 400, 800, mBarStrokePaint);
        // canvas.drawRect(120, 120, 380, 780, mBarFillPaint);
        canvas.drawRect(120, 780 - mCurrentBarHeight, 380, 780, mBarFillPaint);
    }
}
