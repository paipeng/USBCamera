package com.paipeng.usbcamera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

import com.paipeng.usbcamera.R;

public class SimpleUVCCameraTextureView extends TextureView    // API >= 14
        implements AspectRatioViewInterface {

    private double mRequestedAspect = -1.0;

    public SimpleUVCCameraTextureView(final Context context) {
        this(context, null, 0);
        drawFocusFrame();
    }

    public SimpleUVCCameraTextureView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
        drawFocusFrame();
    }

    public SimpleUVCCameraTextureView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        drawFocusFrame();
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void setAspectRatio(final double aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        if (mRequestedAspect != aspectRatio) {
            mRequestedAspect = aspectRatio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (mRequestedAspect > 0) {
            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);

            final int horizPadding = getPaddingLeft() + getPaddingRight();
            final int vertPadding = getPaddingTop() + getPaddingBottom();
            initialWidth -= horizPadding;
            initialHeight -= vertPadding;

            final double viewAspectRatio = (double)initialWidth / initialHeight;
            final double aspectDiff = mRequestedAspect / viewAspectRatio - 1;

            if (Math.abs(aspectDiff) > 0.01) {
                if (aspectDiff > 0) {
                    // width priority decision
                    initialHeight = (int) (initialWidth / mRequestedAspect);
                } else {
                    // height priority decison
                    initialWidth = (int) (initialHeight * mRequestedAspect);
                }
                initialWidth += horizPadding;
                initialHeight += vertPadding;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    private void drawFocusFrame() {
        //this.getOverlay().add(getResources().getDrawable(R.drawable.ic_launcher));

        /*
        SurfaceTexture surfaceTexture = getSurfaceTexture();

        assert surfaceTexture != null;
        //surfaceTexture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
        Surface surface = new Surface(surfaceTexture);

        Rect rect = new Rect();
        rect.left = 0;
        rect.right = 200;
        rect.top = 0;
        rect.bottom = 200;
        Canvas canvas = surface.lockCanvas(rect);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        canvas.drawColor(Color.TRANSPARENT);

        Paint myPaint = new Paint();
        myPaint.setColor(Color.rgb(100, 20, 50));
        myPaint.setStrokeWidth(10);
        myPaint.setStyle(Paint.Style.STROKE);

        canvas.drawCircle(100, 100, 100, myPaint);

        surface.unlockCanvasAndPost(canvas);


         */
    }
}
