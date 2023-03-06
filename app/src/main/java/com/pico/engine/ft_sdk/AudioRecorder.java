package com.pico.engine.ft_sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* loaded from: classes.dex */
public class AudioRecorder {
    public static final int AUDIO_FORMAT = 4;
    public static final int AUDIO_SOURCE = 0;
    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(16000, 16, 4) * 1;
    private static final int BUFFER_SIZE_FACTOR = 1;
    public static final int CHANNEL = 16;
    public static final int READ_BUFFER_SIZE = 640;
    public static final int SAMPLE_RATE = 16000;
    private static final String TAG = "ft_social";
    private boolean dumpPcm;
    private AudioRecord mAudioRecord;
    private Context mContext;
    private Handler mHandler;
    private RecordListener mListener;
    private HandlerThread mRecordThread;
    private boolean mStopFlag = false;
    private boolean mRecording = false;
    private FileOutputStream fos = null;

    /* loaded from: classes.dex */
    public interface RecordListener {
        void onRecordData(float[] fArr);

        void onStartRecord();

        void onStopRecord();
    }

    @SuppressLint("MissingPermission")
    public AudioRecorder(Context context, boolean z, RecordListener recordListener) {
        this.dumpPcm = false;
        try {
            this.dumpPcm = z;
            Log.d("ft_social", "dump audio " + z);
            this.mContext = context;
            this.mListener = recordListener;
            try {
                this.mAudioRecord = new AudioRecord(0, 16000, 16, 4, BUFFER_SIZE);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            int state = this.mAudioRecord.getState();
            Log.d("ft_social", "createAudioRecord: state=" + state + " bufferSize=" + BUFFER_SIZE);
            if (1 != state) {
                Log.e("ft_social", "AudioRecord无法初始化，请检查录制权限或者是否其他app没有释放录音器");
            }
            if (this.dumpPcm) {
                openFile();
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    public void openFile() throws IOException {
        File file = new File(this.mContext.getExternalFilesDir(null), "model/record.pcm");
        if (!file.exists()) {
            file.createNewFile();
        }
        this.fos = new FileOutputStream(file);
    }

    public void start() {
        try {
            Log.d("ft_social", "onStartRecord");
            HandlerThread handlerThread = new HandlerThread("RecordThread");
            this.mRecordThread = handlerThread;
            handlerThread.start();
            Handler handler = new Handler(this.mRecordThread.getLooper());
            this.mHandler = handler;
            handler.post(() -> {
                RecordListener recordListener = mListener;
                if (recordListener != null) recordListener.onStartRecord();
                else Log.w("ft_social", "start: mListener is null");
                recordAudio();
            });
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.e("ft_social", "exception caught while startRecording");
        }
    }

    public void stop() {
        Log.d("ft_social", "stop AuidoRecorder");
        this.mStopFlag = true;
        this.mHandler.post(new Runnable() { // from class: com.pico.engine.ft_sdk.-$$Lambda$AudioRecorder$bsv2E_JdbZzrMcH2Toh_y4Ec6FY
            @Override // java.lang.Runnable
            public final void run() {
                AudioRecorder.this.lambda$stop$1$AudioRecorder();
            }
        });
        this.mRecordThread.quitSafely();
    }

    public /* synthetic */ void lambda$stop$1$AudioRecorder() {
        try {
            FileOutputStream fileOutputStream = this.fos;
            if (fileOutputStream != null) {
                fileOutputStream.flush();
                this.fos.close();
                this.fos = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.mAudioRecord.stop();
        RecordListener recordListener = this.mListener;
        if (recordListener != null) {
            recordListener.onStopRecord();
        } else {
            Log.d("ft_social", "stop: AudioRecord mListener is null");
        }
        this.mListener = null;
        this.mAudioRecord.release();
    }

    public boolean isRecording() {
        return this.mRecording;
    }

    private String getBufferReadFailureReason(int i) {
        return i != -6 ? i != -3 ? i != -2 ? i != -1 ? "Unknown (" + i + ")" : "ERROR" : "ERROR_BAD_VALUE" : "ERROR_INVALID_OPERATION" : "ERROR_DEAD_OBJECT";
    }

    private void recordAudio() {
        if (this.mAudioRecord == null) {
            Log.e("ft_social", "mAudioRecord is null!!!");
        }
        if (this.mAudioRecord.getState() != 1) {
            Log.e("ft_social", "mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED!!!");
            return;
        }
        this.mAudioRecord.startRecording();
        Log.v("ft_social", "startRecording");
        Process.setThreadPriority(-19);
        if (this.mAudioRecord.getState() != 1) {
            Log.e("ft_social", "mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED  !!!");
            return;
        }
        byte[] bArr = new byte[READ_BUFFER_SIZE];
        float[] fArr = new float[160];
        while (!this.mStopFlag) {
            this.mRecording = true;
            int read = this.mAudioRecord.read(fArr, 0, 160, AudioRecord.READ_BLOCKING);
            ByteBuffer.wrap(bArr).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(fArr);
            Log.d("ft_social", "record read BUFFER_SIZE =640");
            if (read < 0) {
                throw new RuntimeException("Reading of audio buffer failed: " + getBufferReadFailureReason(read));
            }
            if (this.fos != null) {
                try {
                    Log.d("ft_social", "dump raw audio data to file");
                    this.fos.write(bArr, 0, READ_BUFFER_SIZE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            RecordListener recordListener = this.mListener;
            if (recordListener != null) {
                recordListener.onRecordData(fArr);
            }
        }
        this.mRecording = false;
        Log.v("ft_social", "recording  thread end");
    }
}
