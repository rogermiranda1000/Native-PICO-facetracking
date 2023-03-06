package com.rogermiranda1000.pico_facetracking;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.pico.engine.ft_sdk.FaceTrackor;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "pico_facetracking_sender";

    private final int REQUEST_CODE_CAMERA_PERMISSIONS = 1;
    private final int REQUEST_CODE_RECORD_AUDIO_PERMISSIONS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.startService(new Intent(this, FacetrackingSender.class));

        final TextView text = (TextView)findViewById(R.id.text);
        if (!checkPermissions()) text.setText("Permissions error");
        else {
            text.setText("Loading...");

            // setup
            FaceTrackor.initialize(this);

            // get the data
            new Thread(() -> {
                while (true) {
                    String data = Arrays.toString(FaceTrackor.getResults());

                    text.setText(data);
                    Log.v(TAG, data);

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignore) {}
                }
            }).start();
        }
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSIONS);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO_PERMISSIONS);
        }
        // TODO add read/write external files? (it seems that the FaceTraacking code moves .so files)
        // if so, include to Manifest:
        // <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
        // <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

        return (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
    }
}