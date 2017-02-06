package com.moskovko.myfirstapp;

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

public class BatteryHealthView extends View {
    private Paint mBarStrokePaint;
    private Paint mBarFillPaint;
    private RectF mOvalRect;


    public BatteryHealthView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mBarStrokePaint = new Paint();
        mBarStrokePaint.setColor(Color.parseColor("#000000"));
        mBarStrokePaint.setStyle(Paint.Style.STROKE);
        mBarStrokePaint.setStrokeWidth(100);

        mBarFillPaint = new Paint();
        mBarFillPaint.setColor(Color.parseColor("#000000"));
        mBarFillPaint.setStyle(Paint.Style.FILL);

        mOvalRect = new RectF(50, 50, 1000, 1050);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.parseColor("#00FF00"));

        /*
        canvas.drawRect(100, 100, 400, 800, mBarStrokePaint);
        // canvas.drawRect(120, 120, 380, 780, mBarFillPaint);
        canvas.drawRect(120, 780 - mCurrentBarHeight, 380, 780, mBarFillPaint);
        */

        canvas.drawArc(mOvalRect, 200, 140, false, mBarStrokePaint);
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
        setMeasuredDimension((int)(mOvalRect.width() + 100), (int)((mOvalRect.height() / 2) - 100));
    }
}
