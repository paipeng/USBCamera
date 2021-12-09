package com.paipeng.usbcamera;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.paipeng.libauthclient.AuthClient;
import com.paipeng.libauthclient.base.HttpClientCallback;
import com.paipeng.libauthclient.model.User;
import com.paipeng.usbcamera.utils.ImageUtil;
import com.paipeng.usbcamera.widget.SimpleUVCCameraTextureView;
import com.paipeng.utschauth.AuthParam;
import com.paipeng.utschauth.AuthResult;
import com.paipeng.utschauth.CodeImage;
import com.paipeng.utschauth.UtschAuthApi;
import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;

import java.nio.ByteBuffer;

public final class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final Object mSync = new Object();
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private SimpleUVCCameraTextureView mUVCCameraView;
    private ImageButton mCameraButton;
    private Surface mPreviewSurface;

    private ImageView previewImageView;
    private ImageView registImageView;
    private TextView authResultTextView;


    private boolean registerSample;
    private static CodeImage sampleCodeImage;
    private AuthParam authParam;
    private AuthClient authClient;
    private static final String URL = "http://114.115.137.22";
    private User user;

    public static final int AUTH_IMAGE_SIZE = 298;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraButton = findViewById(R.id.camera_button);
        mCameraButton.setOnClickListener(mOnClickListener);

        mUVCCameraView = findViewById(R.id.UVCCameraTextureView1);
        mUVCCameraView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);

        previewImageView = findViewById(R.id.previewImageView);
        registImageView = findViewById(R.id.registImageView);
        authResultTextView = findViewById(R.id.authResultTextView);


        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);

        this.authParam = new AuthParam();


        authParam.corr_shift = 8;
        authParam.corr_size = 32;

        authParam.mode_col = 20;
        authParam.mode_row = 20;
        authParam.mode_size = 20;


        // QRCode
        authParam.mode_col = 21;
        authParam.mode_row = 21;
        authParam.mode_size = 22;


        authClient= new AuthClient(URL);


        try {
            User user1 = new User();
            user1.setEmail("sipaipv6@gmail.com");
            user1.setPassword("123456");
            authClient.login(user1, new HttpClientCallback() {
                @Override
                public void onSuccess(Object value) {
                    MainActivity.this.user = ((User)value);
                    Log.d(TAG, "user token: " + ((User)value).getToken());
                }

                @Override
                public void onFailure(int code, String message) {
                    Log.e(TAG,"onFailure: " + code + " message: " + message);
                    MainActivity.this.user = null;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUSBMonitor.register();
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.startPreview();
            }
        }
    }

    @Override
    protected void onStop() {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.stopPreview();
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.unregister();
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        synchronized (mSync) {
            releaseCamera();
            if (mToast != null) {
                mToast.cancel();
                mToast = null;
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }
        mUVCCameraView = null;
        mCameraButton = null;
        super.onDestroy();
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            synchronized (mSync) {
                if (mUVCCamera == null) {
                    CameraDialog.showDialog(MainActivity.this);
                } else {
                    //releaseCamera();

                    registerSample = true;
                }
            }
        }
    };

    private Toast mToast;

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            releaseCamera();
            queueEvent(() -> {
                final UVCCamera camera = new UVCCamera();
                camera.open(ctrlBlock);
                camera.setStatusCallback((statusClass, event, selector, statusAttribute, data) -> runOnUiThread(() -> {
                    final Toast toast = Toast.makeText(MainActivity.this, "onStatus(statusClass=" + statusClass
                            + "; " +
                            "event=" + event + "; " +
                            "selector=" + selector + "; " +
                            "statusAttribute=" + statusAttribute + "; " +
                            "data=...)", Toast.LENGTH_SHORT);
                    synchronized (mSync) {
                        if (mToast != null) {
                            mToast.cancel();
                        }
                        toast.show();
                        mToast = toast;
                    }
                }));
                camera.setButtonCallback((button, state) -> runOnUiThread(() -> {
                    final Toast toast = Toast.makeText(MainActivity.this, "onButton(button=" + button + "; " +
                            "state=" + state + ")", Toast.LENGTH_SHORT);
                    synchronized (mSync) {
                        if (mToast != null) {
                            mToast.cancel();
                        }
                        mToast = toast;
                        toast.show();
                    }
                }));
//					camera.setPreviewTexture(camera.getSurfaceTexture());
                if (mPreviewSurface != null) {
                    mPreviewSurface.release();
                    mPreviewSurface = null;
                }
                try {
                    camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
                } catch (final IllegalArgumentException e) {
                    // fallback to YUV mode
                    try {
                        camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                    } catch (final IllegalArgumentException e1) {
                        camera.destroy();
                        return;
                    }
                }
                final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                if (st != null) {
                    mPreviewSurface = new Surface(st);
                    camera.setPreviewDisplay(mPreviewSurface);
                    camera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGB565/*UVCCamera.PIXEL_FORMAT_NV21*/);
                    camera.startPreview();
                }
                synchronized (mSync) {
                    mUVCCamera = camera;
                }
            }, 0);
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            // XXX you should check whether the coming device equal to camera device that currently using
            releaseCamera();
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    private synchronized void releaseCamera() {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                try {
                    mUVCCamera.setStatusCallback(null);
                    mUVCCamera.setButtonCallback(null);
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCamera = null;
            }
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
        }
    }

    /**
     * to access from CameraDialog
     *
     * @return USBMonitor
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            runOnUiThread(() -> {
                // FIXME
            }, 0);
        }
    }

    // if you need frame data as byte array on Java side, you can use this callback method with UVCCamera#setFrameCallback
    // if you need to create Bitmap in IFrameCallback, please refer following snippet.
    final Bitmap bitmap = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.RGB_565);
    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            Log.i("MainActivity", "onFrame");
            //
            synchronized (bitmap) {
                bitmap.copyPixelsFromBuffer(frame);

                Rect cropRect = new Rect();
                cropRect.left = (bitmap.getWidth() - AUTH_IMAGE_SIZE)/2;
                cropRect.top = (bitmap.getHeight() - AUTH_IMAGE_SIZE)/2;

                cropRect.right = cropRect.left + AUTH_IMAGE_SIZE;
                cropRect.bottom = cropRect.top + AUTH_IMAGE_SIZE;

                Bitmap cropBitmap = ImageUtil.cropBitmap(bitmap, cropRect);
                Bitmap grayBitmap = ImageUtil.getGrayBitmap(cropBitmap);
                if (registerSample) {
                    registImageView.setImageBitmap(grayBitmap);
                    sampleCodeImage = com.paipeng.utschauth.ImageUtil.convertBitmapToCodeImage(grayBitmap);

                    UtschAuthApi.getInstance().utschRegister(sampleCodeImage, authParam);

                    registerSample = false;
                } else {
                    Bitmap paddingBitmap = ImageUtil.paddingBitmap(bitmap, 0, (bitmap.getWidth() - bitmap.getHeight())/2);
                    Bitmap resizeBitmap = ImageUtil.resizedBitmap(paddingBitmap, paddingBitmap.getWidth()/4, paddingBitmap.getHeight()/4);
                    Bitmap grayBitmap2 = ImageUtil.getGrayBitmap(resizeBitmap);
                    Bitmap blurBitmap = ImageUtil.blurImage(MainActivity.this, grayBitmap2);
                    // Log.d(TAG, "grayBitmap2 size: " + grayBitmap2.getWidth() + "-" + grayBitmap2.getHeight());
                    String qrData = ImageUtil.decodeWithZxing(blurBitmap);
                    Log.d(TAG, "decodeWithZxing: " + qrData);
                    if (qrData != null) {
                        Toast.makeText(MainActivity.this, String.format("decodeWithZxing: %d", qrData), Toast.LENGTH_SHORT);
                    }
                    previewImageView.setImageBitmap(blurBitmap);
                    if (sampleCodeImage != null) {
                        AuthResult authResult = new AuthResult();
                        int ret = UtschAuthApi.getInstance().utschAuth(com.paipeng.utschauth.ImageUtil.convertBitmapToCodeImage(grayBitmap),
                                null, authParam, authResult);
                        if (ret == 0) {
                            // Log.d("MainActivity", "utsch-auth result: " + authResult.accu + " score: " + authResult.authent_score);
                            authResultTextView.setText(String.format("Auth mean score: %.03f (modi: %.03f)", authResult.mean_authent_score, authResult.modi_authent_score) + " qr: " + qrData);
                        } else {
                            Toast.makeText(MainActivity.this, String.format("utschAuth err: %d", ret), Toast.LENGTH_SHORT);
                        }
                    }
                }
            }
            frame.clear();
        }
    };
	/*
	private final Runnable mUpdateImageTask = new Runnable() {
		@Override
		public void run() {
			synchronized (bitmap) {
				mImageView.setImageBitmap(bitmap);
			}
		}
	}; */
}
