package com.example.mcu_team25_voice_assistant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class FitnessMate extends AppCompatActivity {
    private static final String TAG = "FitnessMate: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fitnessmate);

        Log.d(TAG, "FitnessMate Started");

        Runnable r = new Runnable() {
            @Override
            public void run(){
                Intent intent = new Intent(FitnessMate.this, MainPage.class);
                startActivity(intent);
            }
        };

        Handler h = new Handler();
        h.postDelayed(r, 2000); // <-- the "2000" is the delay time in miliseconds.
    }
}