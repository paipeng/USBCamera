package com.paipeng.usbcamera.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Base64;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

public class ImageUtil {
    private static final String TAG = ImageUtil.class.getSimpleName();

    public static Bitmap cropBitmap(Bitmap bitmap, Rect cropRect) {
        return Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
    }


    // 图片灰化处理
    public static Bitmap getGrayBitmap(Bitmap bmp) {
        // Bitmap mBitmap = BitmapFactory.decodeResource(getResources(),
        // R.drawable.android);
        Bitmap mGrayBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(mGrayBitmap);
        Paint mPaint = new Paint();

        // 创建颜色变换矩阵
        ColorMatrix mColorMatrix = new ColorMatrix();
        // 设置灰度影响范围
        mColorMatrix.setSaturation(0);
        // 创建颜色过滤矩阵
        ColorMatrixColorFilter mColorFilter = new ColorMatrixColorFilter(mColorMatrix);
        // 设置画笔的颜色过滤矩阵
        mPaint.setColorFilter(mColorFilter);
        // 使用处理后的画笔绘制图像
        mCanvas.drawBitmap(bmp, 0, 0, mPaint);

        return mGrayBitmap;
    }


    public static Bitmap resizedBitmap(Bitmap oldBt, int newWidth, int newHeight) {
        if (oldBt == null || oldBt.isRecycled()) {
            return null;
        }
        try {
            // 获取这个图片的宽和高
            int width = oldBt.getWidth();
            int height = oldBt.getHeight();

            // 计算缩放率，新尺寸除原始尺寸
            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;

            // 创建操作图片用的matrix对象
            Matrix matrix = new Matrix();

            // 缩放图片动作
            matrix.postScale(scaleWidth, scaleHeight);

            // 创建新的图片
            return Bitmap.createBitmap(oldBt, 0, 0, width, height, matrix, true);
        } catch (Exception | Error exception) {
            Log.e(TAG, "resizedBitmap " + exception.getMessage());
        }
        return null;
    }

    public static Bitmap paddingBitmap(Bitmap Src, int padding_x, int padding_y) {
        Bitmap outputimage = Bitmap.createBitmap(Src.getWidth() + padding_x * 2, Src.getHeight() + padding_y * 2, Bitmap.Config.ARGB_8888);
        Canvas can = new Canvas(outputimage);
        can.drawARGB(0xFF, 0xFF, 0xFF, 0xFF); //This represents White color
        can.drawBitmap(Src, padding_x, padding_y, null);
        return outputimage;
    }


    /**
     * 对 YUV格式的视频流图片通过Zxing进行二维码的识别
     *
     * @param arrData YUV格式像素数据
     * @param nWidth  图片宽
     * @param nHeight 图片高
     * @return 识别的二维码解码数据， null为没有识别到二维码
     */
    public static String decodeQR(byte[] arrData, int nWidth, int nHeight) {
        Log.i("", "decodeQR " + nWidth + "-" + nHeight);
        Result objRawResult;
        String strResult;

        try {
            PlanarYUVLuminanceSource objSource = new PlanarYUVLuminanceSource(arrData, nWidth, nHeight,
                    0,
                    0,
                    nWidth,
                    nHeight,
                    false);

            //Bitmap grayscaleBitmap = toBitmap(objSource.renderThumbnail(), objSource.getThumbnailWidth(), objSource.getThumbnailHeight());
//            FileUtil.saveImage(grayscaleBitmap);

            // BinaryBitmap是ZXing用来表示1 bit data位图的类
            BinaryBitmap objBitmap = new BinaryBitmap(new HybridBinarizer(objSource));


            //FileUtil.saveImage(convertBinaryBitmapToBitmap(objBitmap));
            Map<DecodeHintType, Object> hints = new EnumMap<>(
                    DecodeHintType.class);
            Vector<BarcodeFormat> decodeFormats;
            decodeFormats = new Vector<>();
            decodeFormats.add(BarcodeFormat.QR_CODE);
            decodeFormats.add(BarcodeFormat.PDF_417);
            decodeFormats.add(BarcodeFormat.DATA_MATRIX);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
            hints.put(DecodeHintType.TRY_HARDER, true);

            QRCodeReader reader = new QRCodeReader();
            objRawResult = reader.decode(objBitmap, hints);
            //mbReadQR = false;
            // TODO
            //mQrBmp = getBarCodeBitMap(objSource);
            strResult = objRawResult.getText();
        } catch (Exception e) {
            Log.e(TAG, "decodeQr exception: " + e.getMessage());
            return null;
        }
        Log.i(TAG, "decodeQR: " + strResult);
        return strResult;
    }

    public static Bitmap toBitmap(int[] pixels, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static String decodeWithZxing(Bitmap bitmap) {
        MultiFormatReader multiFormatReader = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new Hashtable<>();
        //hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);

        Vector<BarcodeFormat> decodeFormats;
        decodeFormats = new Vector<>();
        decodeFormats.add(BarcodeFormat.QR_CODE);
        decodeFormats.add(BarcodeFormat.PDF_417);
        decodeFormats.add(BarcodeFormat.DATA_MATRIX);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        hints.put(DecodeHintType.TRY_HARDER, true);

        multiFormatReader.setHints(hints);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        Result rawResult = null;
        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);

        if (source != null) {
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decodeWithState(binaryBitmap);
            } catch (ReaderException re) {
                re.printStackTrace();
            } finally {
                multiFormatReader.reset();
            }
        }
        return rawResult != null ? rawResult.getText() : null;
    }


    @SuppressLint("NewApi")
    public static Bitmap blurImage(Context context, Bitmap input) {
        try {
            RenderScript rsScript = RenderScript.create(context);
            Allocation alloc = Allocation.createFromBitmap(rsScript, input);

            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript));
            blur.setRadius(3);
            blur.setInput(alloc);

            Bitmap result = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
            Allocation outAlloc = Allocation.createFromBitmap(rsScript, result);

            blur.forEach(outAlloc);
            outAlloc.copyTo(result);

            rsScript.destroy();
            return result;
        } catch (Exception e) {
            // TODO: handle exception
            return input;
        }

    }

    public static byte[] bmp2JpegBytes(Bitmap objBmp, int nQuality) {
        if (objBmp == null)
            return null;
        ByteArrayOutputStream objArr = new ByteArrayOutputStream();
        objBmp.compress(Bitmap.CompressFormat.JPEG, nQuality, objArr);
        return objArr.toByteArray();
    }


    public static String bitmapToBase64(Bitmap bitmap) {
        String str = null;
        if (bitmap != null) {
            AndroidBmpUtil androidBmpUtil = new AndroidBmpUtil();
            byte[] arrData = androidBmpUtil.convertBytes(bitmap);
            str = Base64.encodeToString(arrData, Base64.NO_WRAP);
        }

        return str;
    }

    public static Bitmap convertByteArrayToBitmap(byte[] bytes) {
        return null;
    }
}
