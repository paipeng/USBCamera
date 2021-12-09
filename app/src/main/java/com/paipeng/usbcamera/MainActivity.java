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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.paipeng.libauthclient.AuthClient;
import com.paipeng.libauthclient.base.HttpClientCallback;
import com.paipeng.libauthclient.model.Authorization;
import com.paipeng.libauthclient.model.Product;
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
import java.util.ArrayList;
import java.util.List;

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
    private Button registerButton;

    private boolean registerSample;
    private static CodeImage sampleCodeImage;
    private AuthParam authParam;
    private AuthClient authClient;
    private static final String URL = "http://114.115.137.22";
    private User user;
    private Product product;
    private String qrData;
    private boolean getProduct;
    private ImageView resultImageView;


    public static final int AUTH_IMAGE_SIZE = 462;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultImageView = findViewById(R.id.resultImageView);
        resultImageView.setVisibility(View.INVISIBLE);
        registerButton = findViewById(R.id.registerButton);
        registerButton.setVisibility(View.INVISIBLE);
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user != null && qrData != null) {
                    // register new product
                    product = new Product();
                    product.setUser(user);
                    product.setBarcode(qrData);
                    product.setName("Android test");
                    product.setDescription("OTG usb camera capture");
                    try {
                        authClient.postProduct(product, new HttpClientCallback() {
                            @Override
                            public void onSuccess(Object value) {
                                MainActivity.this.product = (Product)value;
                                Log.d(TAG, "postProduct onSuccess: " + product);
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, "new product registered successfully: " + product.getBarcode(), Toast.LENGTH_SHORT);
                                });
                            }

                            @Override
                            public void onFailure(int code, String message) {
                                Log.e(TAG, "postProduct onFailure: " + product);
                            }
                        });
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

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
                    authClient.setToken(MainActivity.this.user.getToken());
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
                    Log.d(TAG, "in registerSample: " + registerSample);
                    registerSample = false;
                    registImageView.setImageBitmap(grayBitmap);
                    sampleCodeImage = com.paipeng.utschauth.ImageUtil.convertBitmapToCodeImage(grayBitmap);
                    if (sampleCodeImage != null) {
                        int ret = UtschAuthApi.getInstance().utschRegister(sampleCodeImage, authParam);
                        Log.d(TAG, "utschRegister ret: " + ret);
                        postAuthorization(grayBitmap);
                    } else {
                        Log.d(TAG, "sampleCodeImage invalid");
                    }
                    Log.d(TAG, "out registerSample: " + registerSample);
                } else {
                    Bitmap paddingBitmap = ImageUtil.paddingBitmap(grayBitmap, 0, (grayBitmap.getWidth() - grayBitmap.getHeight())/2);
                    Bitmap resizeBitmap = ImageUtil.resizedBitmap(paddingBitmap, paddingBitmap.getWidth()/5, paddingBitmap.getHeight()/5);
                    Bitmap grayBitmap2 = ImageUtil.getGrayBitmap(resizeBitmap);
                    Bitmap blurBitmap = ImageUtil.blurImage(MainActivity.this, grayBitmap2);
                    // Log.d(TAG, "grayBitmap2 size: " + grayBitmap2.getWidth() + "-" + grayBitmap2.getHeight());
                    qrData = ImageUtil.decodeWithZxing(blurBitmap);
                    Log.d(TAG, "decodeWithZxing: " + qrData);
                    if (qrData != null) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, String.format("decodeWithZxing: %s", qrData), Toast.LENGTH_SHORT);
                        });
                        if ((product == null || !product.getBarcode().equals(qrData)) && user != null ) {
                            getProductByBarcode(qrData);

                        } else if (product != null && product.getBarcode().equals(qrData)) {
                            Log.d(TAG, "product already exists for this scanned barcode: " + qrData);
                        }
                    }

                    previewImageView.setImageBitmap(grayBitmap);
                    if (sampleCodeImage != null) {
                        Log.d(TAG, "compare utsch-auth sampleCodeImage: " + sampleCodeImage.width + "-" + sampleCodeImage.height + " sampleCodeImage: " + sampleCodeImage);
                        AuthResult authResult = new AuthResult();

                        //resizeBitmap = ImageUtil.resizedBitmap(grayBitmap, 462, 462);

                        CodeImage codeImage = com.paipeng.utschauth.ImageUtil.convertBitmapToCodeImage(grayBitmap);
                        if (codeImage != null) {
                            Log.d(TAG, "codeImage: " + codeImage.width + "-" + codeImage.height);
                            int ret = UtschAuthApi.getInstance().utschAuth(codeImage, sampleCodeImage, authParam, authResult);
                            Log.d(TAG, "utschAuth ret: " + ret);
                            if (ret == 0) {
                                runOnUiThread(() -> {
                                    authResultTextView.setText(String.format("Auth mean score: %.03f (modi: %.03f)", authResult.mean_authent_score, authResult.modi_authent_score) + " qr: " + qrData);
                                    if (authResult.mean_authent_score > 0.85) {
                                        resultImageView.setVisibility(View.VISIBLE);
                                    } else {
                                        resultImageView.setVisibility(View.INVISIBLE);
                                    }
                                });
                                // Log.d("MainActivity", "utsch-auth result: " + authResult.accu + " score: " + authResult.authent_score);
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, String.format("utschAuth err: %d", ret), Toast.LENGTH_SHORT);
                                });
                            }
                        } else {
                            Log.e(TAG, "codeImage invalid");
                        }
                    }
                }
            }
            frame.clear();
        }
    };

    private synchronized void getProductByBarcode(String qrData) {
        Log.d(TAG, "getProductByBarcode: " + qrData);
        if (!getProduct) {
            getProduct = true;
            Log.d(TAG, "do getProductByBarcode: " + qrData);
            authClient.getProductByBarcode(qrData, new HttpClientCallback() {
                @Override
                public void onSuccess(Object value) {
                    product = (Product) value;
                    Log.d(TAG, "getProductByBarcode onSuccess: " + product);
                    if (product != null) {
                        // TODO read authorizations of this product
                        getAuthorizationsByProduct();

                        runOnUiThread(() -> {
                            registerButton.setVisibility(View.INVISIBLE);
                        });
                    } else {

                    }

                    getProduct = false;
                }

                @Override
                public void onFailure(int code, String message) {
                    Log.e(TAG, "getProductByBarcode onFailure: " + code + " message: " + message);
                    registerSample = false;
                    if (code == 404) {
                        // TODO show register button
                        runOnUiThread(() -> {
                            registerButton.setVisibility(View.VISIBLE);
                        });
                    } else {
                        runOnUiThread(() -> {
                            registerButton.setVisibility(View.INVISIBLE);
                        });
                    }
                    getProduct = false;
                }
            });
        }
    }

    private synchronized void postAuthorization(Bitmap bitmap) {
        Log.d(TAG, "postAuthorization");
        Authorization authorization = new Authorization();
        String bitmapToBase64 = ImageUtil.bitmapToBase64(bitmap);
        authorization.setImageBase64("data:image/bmp;base64," + bitmapToBase64);
        authorization.setProduct(product);
        authorization.setActivate(true);
        try {

            Log.d(TAG, "do POST postAuthorization");
            authClient.postAuthorization(authorization, new HttpClientCallback() {
                @Override
                public void onSuccess(Object value) {
                    Log.d(TAG, "postAuthorization onSuccess");
                    Log.d(TAG, ((Authorization)value).toString());
                    List<Authorization> authorizations = product.getAuthorizations();
                    if (authorizations != null) {
                        authorizations.add(authorization);
                    } else {
                        authorizations = new ArrayList<>();
                        authorizations.add(authorization);
                        product.setAuthorizations(authorizations);
                    }
                }

                @Override
                public void onFailure(int code, String message) {
                    Log.e(TAG, "postAuthorization onFailure: " + code + " message: " + message);

                }
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private synchronized void getAuthorizationsByProduct() {
        Log.d(TAG, "getAuthorizationsByProduct: " + product.getId());
        authClient.getAuthorizationsByProductId(product.getId(), new HttpClientCallback() {
            @Override
            public void onSuccess(Object value) {
                Log.d(TAG, "getAuthorizationsByProduct onSuccess");
                List<Authorization> authorizations = (List<Authorization>)value;
                if (authorizations != null) {
                    product.setAuthorizations(authorizations);
                    if (authorizations.size() > 0) {
                        getAuthorizationImage(authorizations.get(authorizations.size()-1).getFilePath());
                    } else {

                    }
                } else {
                    Log.d(TAG, "no authorization for given product");
                    authorizations = new ArrayList<>();
                    product.setAuthorizations(authorizations);

                }
            }

            @Override
            public void onFailure(int code, String message) {
                Log.e(TAG, "getAuthorizationsByProduct onFailure: " + code + " messae: " + message);

            }
        });
        // show image of authorization of this product

        // begin utsch-auth comparing
    }

    private synchronized void getAuthorizationImage(String filePath) {
        Log.d(TAG, "getAuthorizationImage: " + filePath);
        authClient.getImageBytes(filePath, new HttpClientCallback() {
            @Override
            public void onSuccess(Object value) {
                if (value != null) {
                    byte[] data = (byte[]) value;
                    Log.d(TAG, "getAuthorizationImage onSuccess: " + data.length);
                    Bitmap bitmap = ImageUtil.convertByteArrayToBitmap(data);
                    if (bitmap != null) {
                        Log.d(TAG, "getAuthorizationImage to Bitmap valid");
                        // resize if needed

                        //bitmap = ImageUtil.resizedBitmap(bitmap, AUTH_IMAGE_SIZE, 462);

                        registImageView.setImageBitmap(bitmap);
                        sampleCodeImage = com.paipeng.utschauth.ImageUtil.convertBitmapToCodeImage(bitmap);
                        Log.d(TAG, "sampleCodeImage: " + sampleCodeImage);

                        //int ret = UtschAuthApi.getInstance().utschRegister(sampleCodeImage, authParam);
                        //Log.d(TAG, "utschRegister ret: " + ret);

                    } else {
                        Log.e(TAG, "bitmap invalid");
                    }
                } else {
                    Log.d(TAG, "onSuccess data is null");
                }
            }

            @Override
            public void onFailure(int code, String message) {
                Log.e(TAG, "getAuthorizationImage onFailure: " + code + " messae: " + message);
            }
        });

    }
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
