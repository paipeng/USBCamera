package com.paipeng.usbcamera.utils;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class ImageUtil {

    public static Bitmap cropBitmap(Bitmap bitmap, Rect cropRect) {
        return Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
    }
}
