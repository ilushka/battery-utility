package com.moskovko.myfirstapp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.icu.util.Measure;
import android.util.AttributeSet;
import android.view.View;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;

/**
 * Created by ilushka on 1/24/17.
 */

public class BatteryChargeView extends View {
    private String mTest;
    private Paint mPaint;

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

        mPaint = new Paint();
        mPaint.setColor(Color.parseColor("#000000"));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // MONKEY:
        Log.d("measurespec_width", String.valueOf(MeasureSpec.getSize(widthMeasureSpec)));
        Log.d("measurespec_height", String.valueOf(MeasureSpec.getSize(heightMeasureSpec)));


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
        int centerX = getMeasuredWidth() / 2;
        int centerY = getMeasuredHeight() / 2;
        canvas.drawColor(Color.parseColor("#FF0000"));
        canvas.drawCircle(centerX, centerY, 100, mPaint);
    }
}
