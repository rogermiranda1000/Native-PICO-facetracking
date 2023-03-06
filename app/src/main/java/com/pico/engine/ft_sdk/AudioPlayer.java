package com.pico.engine.ft_sdk;

import android.content.Context;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* loaded from: classes.dex */
public class AudioPlayer {
    public static final int AUDIO_FORMAT = 4;
    private static final int BUFFER_SIZE = AudioTrack.getMinBufferSize(16000, 4, 4) * 1;
    private static final int BUFFER_SIZE_FACTOR = 1;
    public static final int CHANNEL = 4;
    private static final int READ_BUFFER_SIZE = 640;
    public static final int SAMPLE_RATE = 16000;
    private static final String TAG = "ft_social";
    private AudioTrack mAudioTrack;
    private Context mContext;
    private PlayerListener mListener;
    private HandlerThread mPlayThread;
    private File pcmFile;
    private boolean mStopFlag = false;
    private Handler mHandler = null;
    private final String AUDIO_FILE = "/model/record.pcm";

    /* loaded from: classes.dex */
    public interface PlayerListener {
        void onPlayData(float[] fArr);

        void onStartPlay();

        void onStopPlay();
    }

    public AudioPlayer(Context context, PlayerListener PlayerListener) {
        this.pcmFile = null;
        try {
            this.mContext = context;
            this.mListener = PlayerListener;
            int i = BUFFER_SIZE;
            AudioTrack audioTrack = new AudioTrack(3, 16000, 4, 4, i, 1);
            this.mAudioTrack = audioTrack;
            int state = audioTrack.getState();
            Log.d(TAG, "createAudioTrack: state=" + state + " bufferSize=" + i);
            if (state == 0) {
                Log.e(TAG, "mAudioTrack uninitialized!!!");
            }
            File file = new File(this.mContext.getExternalFilesDir(null), "/model/record.pcm");
            this.pcmFile = file;
            if (file.exists()) {
                return;
            }
            Log.e(TAG, this.pcmFile.getAbsolutePath() + " Offline voice file does not exist");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        HandlerThread handlerThread = new HandlerThread("RecordThread");
        this.mPlayThread = handlerThread;
        handlerThread.start();
        Handler handler = new Handler(this.mPlayThread.getLooper());
        this.mHandler = handler;
        handler.post(new Runnable() { // from class: com.pico.engine.ft_sdk.-$$Lambda$AudioPlayer$b2X3ejyEUauOgwypgP5AjJjy-04
            @Override // java.lang.Runnable
            public final void run() {
                AudioPlayer.this.lambda$start$0$AudioPlayer();
            }
        });
    }

    public /* synthetic */ void lambda$start$0$AudioPlayer() {
        PlayerListener PlayerListener = this.mListener;
        if (PlayerListener != null) {
            PlayerListener.onStartPlay();
        } else {
            Log.w(TAG, "start: mListener is null");
        }
        playAudio();
    }

    public void stop() {
        Log.d(TAG, "stop AudioPlayer");
        this.mStopFlag = true;
        this.mHandler.post(new Runnable() { // from class: com.pico.engine.ft_sdk.-$$Lambda$AudioPlayer$1v5BCo9tTEWDHO_w2Z1V2Sq5aVg
            @Override // java.lang.Runnable
            public final void run() {
                AudioPlayer.this.lambda$stop$1$AudioPlayer();
            }
        });
        this.mPlayThread.quitSafely();
        this.mAudioTrack.stop();
        this.mAudioTrack.release();
        this.mListener = null;
    }

    public /* synthetic */ void lambda$stop$1$AudioPlayer() {
        PlayerListener PlayerListener = this.mListener;
        if (PlayerListener != null) {
            PlayerListener.onStopPlay();
        } else {
            Log.d(TAG, "stop: AudioPlayer mListener is null");
        }
    }

    private void playAudio() {
        try {
            if (this.mAudioTrack.getState() != 1) {
                Log.e(TAG, " mAudioTrack.getState() not INITIALIZED");
                return;
            }
            this.mAudioTrack.play();
            FileInputStream fileInputStream = new FileInputStream(this.pcmFile);
            boolean endsWith = this.pcmFile.getAbsolutePath().endsWith("wav");
            byte[] bArr = new byte[640];
            float[] fArr = new float[160];
            int i = 0;
            while (!this.mStopFlag && fileInputStream.read(bArr) != -1) {
                ByteBuffer.wrap(bArr).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(fArr);
                int write = this.mAudioTrack.write(fArr, (endsWith && i == 0) ? 11 : 0, 160, 0);
                if (write < 0) {
                    throw new RuntimeException("Reading of audio buffer failed: " + getBufferReadFailureReason(write));
                }
                Log.e(TAG, "mAudioTrack.write return =" + write);
                PlayerListener PlayerListener = this.mListener;
                if (PlayerListener != null) {
                    PlayerListener.onPlayData(fArr);
                    i++;
                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getBufferReadFailureReason(int i) {
        return i != -6 ? i != -3 ? i != -2 ? i != -1 ? "Unknown (" + i + ")" : "ERROR" : "ERROR_BAD_VALUE" : "ERROR_INVALID_OPERATION" : "ERROR_DEAD_OBJECT";
    }
}
