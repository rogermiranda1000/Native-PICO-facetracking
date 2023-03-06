package com.pico.engine.ft_sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/* loaded from: classes.dex */
public class CameraProxyManager {
    public static final String TAG = "CameraProxyManager";
    private CameraCharacteristics cameraInfo;
    private ByteBuffer eyeLeft;
    private ByteBuffer eyeRight;
    private final CameraCallback mCameraCallback;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private final CameraManager mCameraManager;
    private final Handler mHandler;
    private HandlerThread mHandlerThread;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private ImageReader mRawPrivateImageReader;
    private ByteBuffer mouth;
    private Size outputSize;
    private final String mSetCameraId = "5";
    private final int mRawPrivateFormat = 36;
    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() { // from class: com.pico.engine.ft_sdk.CameraProxyManager.2
        @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
        }

        @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            CameraProxyManager.this.mCameraCaptureSession = cameraCaptureSession;
            try {
                CameraProxyManager.this.mCameraCaptureSession.setRepeatingRequest(CameraProxyManager.this.mPreviewRequestBuilder.build(), null, CameraProxyManager.this.mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    };
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() { // from class: com.pico.engine.ft_sdk.CameraProxyManager.3
        @Override // android.hardware.camera2.CameraDevice.StateCallback
        public void onOpened(CameraDevice cameraDevice) {
            CameraProxyManager.this.mCameraDevice = cameraDevice;
            try {
                CameraProxyManager cameraProxyManager = CameraProxyManager.this;
                cameraProxyManager.mPreviewRequestBuilder = cameraProxyManager.mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                CameraProxyManager.this.mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, 0);
                CameraProxyManager.this.mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 2000000L);
                CameraProxyManager.this.mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 600);
                CameraProxyManager cameraProxyManager2 = CameraProxyManager.this;
                cameraProxyManager2.setupImageReader(cameraProxyManager2.outputSize.getWidth(), CameraProxyManager.this.outputSize.getHeight());
                CameraProxyManager.this.mPreviewRequestBuilder.addTarget(CameraProxyManager.this.mRawPrivateImageReader.getSurface());
                CameraProxyManager.this.mCameraDevice.createCaptureSession(Arrays.asList(CameraProxyManager.this.mRawPrivateImageReader.getSurface()), CameraProxyManager.this.mSessionStateCallback, CameraProxyManager.this.mHandler);
                Log.e(CameraProxyManager.TAG, "onOpened !!");
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(CameraProxyManager.TAG, "onOpened return CameraAccessException !!");
            }
        }

        @Override // android.hardware.camera2.CameraDevice.StateCallback
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.e(CameraProxyManager.TAG, "onDisconnected !!");
        }

        @Override // android.hardware.camera2.CameraDevice.StateCallback
        public void onError(CameraDevice cameraDevice, int i) {
            Log.e(CameraProxyManager.TAG, "onError !!");
        }
    };

    /* loaded from: classes.dex */
    public interface CameraCallback {
        void onClose();

        void onFrameCallback(ArrayList<ByteBuffer> arrayList);

        void onOpen(boolean cameraAccess);
    }

    public CameraProxyManager(Context context, CameraCallback cameraCallback, Handler handler) {
        this.mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.mHandler = handler;
        this.mCameraCallback = cameraCallback;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setupImageReader(final int i, final int i2) {
        int i3 = (i * i2) / 4;
        this.eyeLeft = ByteBuffer.allocateDirect(i3);
        this.eyeRight = ByteBuffer.allocateDirect(i3);
        this.mouth = ByteBuffer.allocateDirect(i3);
        ImageReader newInstance = ImageReader.newInstance(i, i2, ImageFormat.RAW_PRIVATE, 2);
        this.mRawPrivateImageReader = newInstance;
        newInstance.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() { // from class: com.pico.engine.ft_sdk.CameraProxyManager.1
            @Override // android.media.ImageReader.OnImageAvailableListener
            public void onImageAvailable(ImageReader imageReader) {
                byte[] bArr;
                int i4;
                int i5;
                Image acquireLatestImage = imageReader.acquireLatestImage();
                if (acquireLatestImage == null) {
                    Log.e(CameraProxyManager.TAG, "Null image returned RAW");
                    return;
                }
                ByteBuffer buffer = acquireLatestImage.getPlanes()[0].getBuffer();
                if (buffer.hasArray()) {
                    bArr = buffer.array();
                } else {
                    byte[] bArr2 = new byte[buffer.capacity()];
                    buffer.get(bArr2);
                    bArr = bArr2;
                }
                Log.i(CameraProxyManager.TAG, "RAW private: onImageAvailable: width " + i + ", height " + i2 + ", rawBuf.length " + bArr.length);
                int i6 = 0;
                int i7 = 0;
                int i8 = 0;
                for (int i9 = 0; i9 < 640000; i9 += 1600) {
                    int i10 = i9;
                    while (true) {
                        i4 = i9 + 400;
                        if (i10 >= i4) {
                            break;
                        }
                        CameraProxyManager.this.eyeLeft.array()[i6] = bArr[i10];
                        i10++;
                        i6++;
                    }
                    while (true) {
                        i5 = i9 + 800;
                        if (i4 >= i5) {
                            break;
                        }
                        CameraProxyManager.this.eyeRight.array()[i7] = bArr[i4];
                        i4++;
                        i7++;
                    }
                    while (i5 < i9 + 1200) {
                        CameraProxyManager.this.mouth.array()[i8] = bArr[i5];
                        i5++;
                        i8++;
                    }
                }
                if (CameraProxyManager.this.mCameraCallback != null) {
                    ArrayList<ByteBuffer> arrayList = new ArrayList<>();
                    arrayList.add(CameraProxyManager.this.eyeLeft);
                    arrayList.add(CameraProxyManager.this.eyeRight);
                    arrayList.add(CameraProxyManager.this.mouth);
                    CameraProxyManager.this.mCameraCallback.onFrameCallback(arrayList);
                }
                acquireLatestImage.close();
            }
        }, null);
    }

    @SuppressLint("MissingPermission")
    public void openCamera(Context context) {
        Log.i(TAG, "openCamera...");
        boolean state;
        try {
            CameraCharacteristics cameraCharacteristics = this.mCameraManager.getCameraCharacteristics("5");
            this.cameraInfo = cameraCharacteristics;
            int intValue = ((Integer) this.cameraInfo.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
            Log.d(TAG, "cameraRotate = " + intValue);
            Size[] outputSizes = ((StreamConfigurationMap) cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(36);
            for (Size size : outputSizes) {
                Log.d(TAG, "outputSizes size: width =" + size.getWidth() + ",  height =" + size.getHeight());
            }
            if (intValue % 90 != 0) {
                this.outputSize = new Size(outputSizes[0].getHeight(), outputSizes[0].getWidth());
            } else {
                this.outputSize = outputSizes[0];
            }
            this.mCameraManager.openCamera("5", this.mCameraDeviceStateCallback, this.mHandler);
            state = true;
        } catch (CameraAccessException e) {
            state = false;
            e.printStackTrace();
        } catch (IllegalArgumentException e2) {
            state = false;
            e2.printStackTrace();
        }

        final boolean stateCpy = state;
        this.mHandler.post(() -> {
            CameraCallback cameraCallback = mCameraCallback;
            if (cameraCallback != null) {
                cameraCallback.onOpen(stateCpy);
            }
        });
    }

    public Size getOutputSize() {
        return this.outputSize;
    }

    public void close() {
        try {
            CameraDevice cameraDevice = this.mCameraDevice;
            if (cameraDevice != null) {
                cameraDevice.close();
                this.mCameraDevice = null;
            }
            this.mHandler.post(() -> {
                CameraCallback cameraCallback = mCameraCallback;
                if (cameraCallback != null) cameraCallback.onClose();
            });
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
