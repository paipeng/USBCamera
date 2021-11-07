package com.paipeng.usbcamera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class OverlayView extends View {
    public OverlayView(Context context) {
        super(context);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d("TAG", "onDraw: " + getWidth() + "-" + getHeight());



        // 1080 -> 640
        // x -> 400;
        int frameWidth = getWidth()*400/640;
        int frameHeight = frameWidth;

        Rect focusFrame = new Rect();
        focusFrame.top = (getHeight() - frameHeight) / 2;
        focusFrame.left = (getWidth() - frameWidth)/ 2;
        Log.d("TAG", "onDraw: " + focusFrame.top + "  " + focusFrame.left);

        focusFrame.bottom = frameHeight + focusFrame.top;
        focusFrame.right = frameWidth + focusFrame.left;


        Log.d("TAG", "onDraw: " + focusFrame);
        Log.d("TAG", "onDraw: " + focusFrame.bottom + "  " + focusFrame.right);
        Log.d("TAG", "onDraw: " + focusFrame.top + "  " + focusFrame.left);
        Paint myPaint = new Paint();
        myPaint.setColor(Color.rgb(0, 0, 0));
        myPaint.setStrokeWidth(2);
        myPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(focusFrame, myPaint);

        focusFrame.left += 2;
        focusFrame.top += 2;
        focusFrame.right -= 2;
        focusFrame.bottom -= 2;
        myPaint.setColor(Color.rgb(255, 255, 255));
        canvas.drawRect(focusFrame, myPaint);
    }
}
