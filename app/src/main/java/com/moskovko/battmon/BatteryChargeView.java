package com.moskovko.battmon;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
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
    private static final String FULL_CHARGE_STR = "100";
    private static final String PERCENTAGE = "%";
    private static final String AMPEREHOUR = "Ah";

    private Paint mBarBackgroundPaint;
    private Paint mBarForegroundPaint;
    private Paint mBackgroundTextPaint;
    private Paint mForegroundTextPaint;
    private float mCurrentChargeLevel;      // 0.0 to 1.0
    private int mForegroundColor;
    private int mBackgroundColor;
    private Rect mBackgroundBarRect;        // rectangle for the background (maximum value bar)
    private Rect mForegroundBarRect;        // rectangle for the bar representing the fraction
    private boolean mIsHorizontal;
    private int mTextX;
    private int mTextY;
    private String mText;                   // text that is printed on top of bar (ie. percentage)
    private String mTexUnit;                // suffix to tox
    private int mScaledValue;               // maximum value that is multiplied by mCurrentChargeLevel

    public BatteryChargeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // load custom attributes from the xml file
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BatteryChargeView,
                0, 0);
        try {
            mForegroundColor = a.getColor(R.styleable.BatteryChargeView_foregroundColor,
                    Color.parseColor("#000000"));
            mBackgroundColor = a.getColor(R.styleable.BatteryChargeView_backgroundColor,
                    Color.parseColor("#FFFFFF"));

            String orientation = a.getString(R.styleable.BatteryChargeView_orientation);
            if (orientation.equals("horizontal")) {
                mIsHorizontal = true;
            } else {
                mIsHorizontal = false;
            }

            String units = a.getString(R.styleable.BatteryChargeView_units);
            if (units.equals("amperehour")) {
                mTexUnit = AMPEREHOUR;
            } else if (units.equals("percentage")) {
                mTexUnit = PERCENTAGE;
            }

            mScaledValue = a.getInt(R.styleable.BatteryChargeView_scaledValue, 100);
        } finally {
            a.recycle();
        }

        // paint
        mBarBackgroundPaint = new Paint();
        mBarBackgroundPaint.setColor(mBackgroundColor);
        mBarBackgroundPaint.setStyle(Paint.Style.FILL);
        mBarForegroundPaint = new Paint();
        mBarForegroundPaint.setColor(mForegroundColor);
        mBarForegroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundTextPaint = new Paint();
        mBackgroundTextPaint.setColor(ContextCompat.getColor(context, R.color.backgroundText));
        mBackgroundTextPaint.setStyle(Paint.Style.FILL);
        mBackgroundTextPaint.setTextAlign(Paint.Align.CENTER);
        mBackgroundTextPaint.setTextSize(getResources().getDisplayMetrics().scaledDensity * 20);
        mForegroundTextPaint = new Paint();
        mForegroundTextPaint.setColor(ContextCompat.getColor(context, R.color.foregroundText));
        mForegroundTextPaint.setStyle(Paint.Style.FILL);
        mForegroundTextPaint.setTextAlign(Paint.Align.CENTER);
        mForegroundTextPaint.setTextSize(getResources().getDisplayMetrics().scaledDensity * 20);

        mCurrentChargeLevel = FULL_CHARGE;
        mText = mScaledValue + mTexUnit;
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

        // calculate offset that is subtracted from size representing 100%
        int widthOffset = 0, heightOffset = 0;
        if (mIsHorizontal) {
            widthOffset = (int)(mBackgroundBarRect.width() * (1 - mCurrentChargeLevel));
        } else {
            heightOffset = (int)(mBackgroundBarRect.height() * (1 - mCurrentChargeLevel));
        }
        // update the rectangle of the bar
        mForegroundBarRect.set(0, 0, mBackgroundBarRect.width() - widthOffset,
                mBackgroundBarRect.height() - heightOffset);

        mText = Integer.toString((int)(mScaledValue * mCurrentChargeLevel)) + mTexUnit;

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        mBackgroundBarRect = new Rect(0, 0, width, height);
        mForegroundBarRect = new Rect(0, 0, width, height);

        // get center coordinates of percentage text
        mTextX = mBackgroundBarRect.width() / 2;
        // offset by height of text bounds to center it on y axis
        Rect textBounds = new Rect();
        String text = FULL_CHARGE_STR;
        mBackgroundTextPaint.getTextBounds(text, 0, text.length(), textBounds);
        mTextY = (mBackgroundBarRect.height() / 2) + (textBounds.height() / 2);

        setMeasuredDimension(mBackgroundBarRect.width(), mBackgroundBarRect.height());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(mBackgroundBarRect, mBarBackgroundPaint);
        canvas.drawRect(mForegroundBarRect, mBarForegroundPaint);
        canvas.drawText(mText, mTextX, mTextY, mBackgroundTextPaint);
        canvas.clipRect(mForegroundBarRect);
        canvas.drawText(mText, mTextX, mTextY, mForegroundTextPaint);
    }
}
