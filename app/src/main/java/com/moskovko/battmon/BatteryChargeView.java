package com.moskovko.battmon;

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
    private static final int MAX_BAR_HEIGHT = 660;
    private static final int FULL_BAR_ANIMATION_DURATION = 2500;     // milliseconds

    private Paint mBarStrokePaint;
    private Paint mBarFillPaint;
    private int mCurrentBarHeight;
    private float mCurrentChargeLevel;

    public BatteryChargeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        /*
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BatteryChargeView,
                0, 0);
        try {
            mTest = a.getString(R.styleable.BatteryChargeView_testAttr);
        } finally {
            a.recycle();
        }
        */

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
        mCurrentBarHeight = (int)(MAX_BAR_HEIGHT * mCurrentChargeLevel);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // TODO: create constants
        setMeasuredDimension(300, 700);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.parseColor("#FF0000"));

        // TODO: create constants
        canvas.drawRect(0, 0, 300, 700, mBarStrokePaint);
        canvas.drawRect(20, 680 - mCurrentBarHeight, 280, 680, mBarFillPaint);
    }
}
