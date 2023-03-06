package com.rogermiranda1000.pico_facetracking;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.pico.engine.ft_sdk.FaceTracking;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.startService(new Intent(this, FacetrackingSender.class));

        FaceTracking.Pxr_EnableEyeTracking(true);
        // TODO PXR_EyeTracking.GetCombineEyeGazeVector(out v);
    }
}