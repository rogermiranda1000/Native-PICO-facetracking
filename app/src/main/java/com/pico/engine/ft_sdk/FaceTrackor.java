package com.pico.engine.ft_sdk;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * All the functions required to setup eye and face tracking.
 * Note: Retrieved from FT demo, `FaceTrackor.java`
 */
public class FaceTrackor {
    public static final String TAG = "ft_social";

    private static final String VERSION_STR = "0.1.3";

    /**
     * Specifies if `results` data is valid.
     * Also has the purpose of synchronizing `results` and `inited`.
     */
    private static final AtomicBoolean inited = new AtomicBoolean(false);

    private static AudioRecorder mAudioRecorder;
    private static CameraProxyManager mCameraManager;

    private static String modelDir;

    private static Handler mHandler;
    private static HandlerThread mHandlerThread;

    private static volatile ByteBuffer eyeLeft;
    private static volatile ByteBuffer eyeRight;
    private static volatile ByteBuffer mouth;

    private static float[] videoResults;
    private static float[] audioResults;

    /**
     * Combined `videoResults` and `audioResults` data.
     */
    private static volatile float[] results;



    public static native int destroy();

    public static native int getAudioDelayCount();

    private static native int init(String str, String str2);

    public static native float[] processAudioFrame(float[] fArr, int i);

    public static native float[] processVideoFrame(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ByteBuffer byteBuffer3, int i, int i2);



    static {
        System.loadLibrary("ft_sdk");
        results = new float[0]; // TODO it gets overridden immediately and never cached; leave as null?
        videoResults = new float[0];
        audioResults = new float[0];
    }

    public static void initSDK() {
        Log.e(TAG, "SDK version =" + VERSION_STR);
        int init = /*init(FaceTracking.modelDir, FaceTracking.modelDir)*/ 0;

        synchronized (FaceTrackor.inited) {
            FaceTrackor.inited.set(init == 0); // if 0, succeed
        }

        if (FaceTrackor.isInited()) Log.e(TAG, "initSDK success  in thread =" + Thread.currentThread().getId());
        else Log.e(TAG, "intSDK fail ret =" + init);
    }

    public static int release() {
        if (FaceTrackor.isInited()) {
            CameraProxyManager cameraProxyManager = mCameraManager;
            if (cameraProxyManager != null) {
                cameraProxyManager.close();
                mCameraManager = null;
                mHandlerThread.quit();
                mHandler = null;
            }
            AudioRecorder audioRecorder = mAudioRecorder;
            if (audioRecorder != null) {
                audioRecorder.stop();
                mAudioRecorder = null;
            }

            synchronized (FaceTrackor.inited) {
                FaceTrackor.inited.set(false);
            }
        }
        return 0;
    }

    /**
     * Initialize the face-tracking with hybrid mode (face & lipsync).
     * Note: Retrieved from `FaceTrackor.java`, 3rd case of `initialize` (LIP_RECORD)
     * @param context Android app context
     */
    public static void initialize(Context context) {
        if (FaceTrackor.isInited()) {
            Log.e(TAG, "already inited, ignore");
            return;
        }
        FaceTrackor.prepareData(context);

        FaceTrackor.mHandlerThread = new HandlerThread("FaceTracking");
        FaceTrackor.mHandlerThread.start();
        FaceTrackor.mHandler = new Handler(FaceTrackor.mHandlerThread.getLooper());

        CameraProxyManager mCameraManager = new CameraProxyManager(context, new CameraProxyManager.CameraCallback() { // from class: com.pico.engine.ft_sdk.FaceTrackor.2
            @Override // com.pico.engine.ft_sdk.CameraProxyManager.CameraCallback
            public void onOpen(boolean cameraAccess) {
                if (cameraAccess) FaceTrackor.initSDK();
                else Log.e(TAG, "camera open failed");
            }

            @Override // com.pico.engine.ft_sdk.CameraProxyManager.CameraCallback
            public void onFrameCallback(ArrayList<ByteBuffer> arrayList) {
                synchronized (FaceTrackor.inited) {
                    FaceTrackor.eyeLeft = arrayList.get(0);
                    FaceTrackor.eyeRight = arrayList.get(1);
                    FaceTrackor.mouth = arrayList.get(2);
                }
                Log.e(TAG, "onFrameCallback in thread =" + Thread.currentThread().getId());
                float []data = FaceTrackor.processVideoFrame(FaceTrackor.eyeLeft, FaceTrackor.eyeRight, FaceTrackor.mouth, 400, 400);
                synchronized (FaceTrackor.inited) {
                    FaceTrackor.videoResults = data;
                }
                FaceTrackor.mergeResults();
            }

            @Override // com.pico.engine.ft_sdk.CameraProxyManager.CameraCallback
            public void onClose() {
                FaceTrackor.destroy();
                Log.e(TAG, "destroy SDK  in thread =" + Thread.currentThread().getId());
            }
        }, FaceTrackor.mHandler);
        mCameraManager.openCamera(context);

        // TODO first face, then we'll worry about lips
        /*AudioRecorder mAudioRecorder = new AudioRecorder(context, false, new AudioRecorder.RecordListener() { // from class: com.pico.engine.ft_sdk.FaceTrackor.3
            @Override // com.pico.engine.ft_sdk.AudioRecorder.RecordListener
            public void onStartRecord() {
                FaceTrackor.initSDK();
            }

            @Override // com.pico.engine.ft_sdk.AudioRecorder.RecordListener
            public void onRecordData(float[] fArr) {
                Log.d(TAG, "onRecordData current thread is =" + Thread.currentThread().getId());
                float[] processAudioFrame = FaceTrackor.processAudioFrame(fArr, 1);
                Log.e(TAG, "processAudioFrame  length  =" + fArr.length);
                if (processAudioFrame.length > 0) {
                    synchronized (FaceTrackor.inited) {
                        FaceTrackor.audioResults = processAudioFrame;
                    }
                    FaceTrackor.mergeResults();
                    StringBuilder sb = new StringBuilder();
                    for (float f : FaceTrackor.results) {
                        sb.append(f).append(",");
                    }
                    Log.e(TAG,  "audioResults =" + ((Object) sb));
                    return;
                }
                Log.e(TAG, "invalid results length =" + processAudioFrame.length);
            }

            @Override // com.pico.engine.ft_sdk.AudioRecorder.RecordListener
            public void onStopRecord() {
                FaceTrackor.destroy();
                Log.e(FaceTrackor.TAG, "destroy SDK  in thread =" + Thread.currentThread().getId());
            }
        });
        mAudioRecorder.start();*/
    }

    /**
     * Get the face-tracking data
     * @return Face-tracking data; it will have length 0 if there's no data
     */
    public static float[] getResults() {
        float[] fArr;
        synchronized (FaceTrackor.inited) {
            fArr = FaceTrackor.isInited() ? FaceTrackor.results : new float[0];
        }
        return fArr;
    }

    /**
     * Safe way to check if `FaceTracking.inited` is true
     * @return `FaceTracking.inited` value
     */
    private static boolean isInited() {
        boolean val;
        synchronized (FaceTrackor.inited) {
            val = FaceTrackor.inited.get();
        }
        return val;
    }

    /**
     * Copy `videoResults` and `audioResults` into `FaceTracking.results`
     */
    private static void mergeResults() {
        synchronized (FaceTrackor.inited) {
            FaceTrackor.results = new float[72];
            if (FaceTrackor.videoResults.length > 0) System.arraycopy(FaceTrackor.videoResults, 0, FaceTrackor.results, 0, FaceTrackor.videoResults.length);
            if (FaceTrackor.audioResults.length > 0) System.arraycopy(FaceTrackor.audioResults, 52, FaceTrackor.results, 52, 20); // TODO it starts consulting `audioResults` from the index 52, shouldn't it start at 0?
        }
    }

    private static void prepareData(Context context) {
        File file = new File(context.getExternalFilesDir(null), "model/");
        if (!file.exists() && !file.mkdirs()) {
            Log.e(TAG, "mkdirs " + file.getAbsolutePath() + " fail!!!");
        }
        String absolutePath = file.getAbsolutePath();
        FaceTrackor.modelDir = absolutePath;
        Log.d(TAG, absolutePath);
        // TODO are the models required? We couldn't decompile `copyData`
        /*Log.d(TAG, "copy models to sdcard");
        copyData(context, FaceTracking.modelDir, C0379R.raw.eye, "eye");
        copyData(context, FaceTracking.modelDir, C0379R.raw.mouth, "mouth");
        copyData(context, FaceTracking.modelDir, C0379R.raw.tt_blink_ints, "tt_blink_ints.model");
        copyData(context, FaceTracking.modelDir, C0379R.raw.tt_blink_seqs, "tt_blink_seqs.model");
        copyData(context, FaceTracking.modelDir, C0379R.raw.tt_audio_avatar_aip_int8, "tt_audio_avatar_aip_int8");
        copyData(context, FaceTracking.modelDir, C0379R.raw.libsnpe_dsp_v66_domains_v2_skel, "libsnpe_dsp_v66_domains_v2_skel.so");*/
    }
}
