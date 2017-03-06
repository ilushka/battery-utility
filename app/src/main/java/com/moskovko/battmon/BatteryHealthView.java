package com.moskovko.battmon;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by ilushka on 2/4/17.
 */

public class BatteryHealthView extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final int MAX_ARC_SWEEP = 140;

    private Paint mBackgroundStrokePaint;
    private Paint mForegroundStrokePaint;
    private Paint mBarFillPaint;
    private RectF mOvalRect;
    private float mCurrentHealthLevel;

    public BatteryHealthView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mBackgroundStrokePaint = new Paint();
        mBackgroundStrokePaint.setColor(Color.parseColor("#000000"));
        mBackgroundStrokePaint.setStyle(Paint.Style.STROKE);
        mBackgroundStrokePaint.setStrokeWidth(100);

        mForegroundStrokePaint = new Paint();
        mForegroundStrokePaint.setColor(Color.parseColor("#0000FF"));
        mForegroundStrokePaint.setStyle(Paint.Style.STROKE);
        mForegroundStrokePaint.setStrokeWidth(100);

        mBarFillPaint = new Paint();
        mBarFillPaint.setColor(Color.parseColor("#000000"));
        mBarFillPaint.setStyle(Paint.Style.FILL);

        mOvalRect = new RectF(50, 50, 1000, 1050);

        mCurrentHealthLevel = 0.65f;
    }

    public void setCurrentHealthLevel(float level) {
        ValueAnimator animation = ValueAnimator.ofFloat(mCurrentHealthLevel, level);
        // scale animation time to how much movement the bar needs to make
        animation.setDuration(1500);
        animation.addUpdateListener(this);
        animation.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.parseColor("#00FF00"));

        /*
        canvas.drawRect(100, 100, 400, 800, mBackgroundStrokePaint);
        // canvas.drawRect(120, 120, 380, 780, mBarFillPaint);
        canvas.drawRect(120, 780 - mCurrentBarHeight, 380, 780, mBarFillPaint);
        */

        canvas.drawArc(mOvalRect, 200, 140, false, mBackgroundStrokePaint);
        canvas.drawArc(mOvalRect, 200, (MAX_ARC_SWEEP * mCurrentHealthLevel), false, mForegroundStrokePaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

/*
        int minimumWidth = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int width = resolveSizeAndState(minimumWidth, widthMeasureSpec, 0);
        int minHeight = MeasureSpec.getSize(width)  + getPaddingBottom() + getPaddingTop();
        int height = resolveSizeAndState(MeasureSpec.getSize(width), heightMeasureSpec, 0);
*/
        setMeasuredDimension((int) (mOvalRect.width() + 100), (int) ((mOvalRect.height() / 2) - 100));
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        mCurrentHealthLevel = ((Float)animation.getAnimatedValue()).floatValue();
        // TODO: calculate foreground arc sweep angle here
        invalidate();
    }
}
