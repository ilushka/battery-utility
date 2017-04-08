package com.moskovko.battmon;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.content.Context;
import android.animation.ValueAnimator;

/**
 * Created by ilushka on 1/24/17.
 */

public class BatteryChargeView extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final int    FULL_BAR_ANIMATION_DURATION = 2500;     // milliseconds
    private static final float  FULL_CHARGE = 1.0f;                     // percentage

    private Paint mBarBackgroundPaint;
    private Paint mBarForegroundPaint;
    private float mCurrentChargeLevel;
    private int mForegroundColor;
    private int mBackgroundColor;
    private int mWidth;
    private int mHeight;
    private boolean mIsHorizontal;
    private int mWidthOffset;
    private int mHeightOffset;

    public BatteryChargeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // load custom attributes from the xml file
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BatteryChargeView,
                0, 0);
        try {
            // mTest = a.getString(R.styleable.BatteryChargeView_testAttr);
            mForegroundColor = a.getColor(R.styleable.BatteryChargeView_foregroundColor,
                    Color.parseColor("#000000"));
            mBackgroundColor = a.getColor(R.styleable.BatteryChargeView_backgroundColor,
                    Color.parseColor("#FFFFFF"));
            String orientation = a.getString(R.styleable.BatteryChargeView_orientation);
            if (orientation.equals("horizontal")) {
                mWidth = 700;
                mHeight = 300;
                mIsHorizontal = true;
            } else {
                mWidth = 300;
                mHeight = 700;
                mIsHorizontal = false;
            }
        } finally {
            a.recycle();
        }

        mBarBackgroundPaint = new Paint();
        mBarBackgroundPaint.setColor(mBackgroundColor);
        mBarBackgroundPaint.setStyle(Paint.Style.STROKE);
        mBarForegroundPaint = new Paint();
        mBarForegroundPaint.setColor(mForegroundColor);
        mBarForegroundPaint.setStyle(Paint.Style.FILL);

        mCurrentChargeLevel = FULL_CHARGE;
        mWidthOffset = 0;
        mHeightOffset = 0;
    }

    public float getCurrentChargeLevel() {
        return mCurrentChargeLevel;
    }

    public void setCurrentChargeLevel(float level) {
        // TODO: cancel old animation
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
        if (mIsHorizontal) {
            mWidthOffset = (int)(mWidth * mCurrentChargeLevel);
        } else {
            mHeightOffset = (int)(mHeight * mCurrentChargeLevel);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, mWidth, mHeight, mBarBackgroundPaint);
        canvas.drawRect(0, 0, mWidth - mWidthOffset, mHeight - mHeightOffset, mBarForegroundPaint);
    }
}
