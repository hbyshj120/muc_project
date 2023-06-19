package com.example.mcu_team25_voice_assistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;


public class MainPage extends Activity {

    TextView voice_content_library;
    TextView exit;
    private static final String TAG = "MainPage: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainpage);

        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        Python python = Python.getInstance();
        String NewAudioName = getFilesDir().getAbsolutePath() + "/new.wav";
        PyObject pyObject = python.getModule("hello");
        PyObject obj = pyObject.callAttr("process", NewAudioName);

        Log.d(TAG, obj.toString());

        Log.d(TAG, "Main Page Opened");

        voice_content_library = findViewById(R.id.voice_content_library);
        voice_content_library.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Switch to Voice Content Library");

                Intent intent = new Intent(MainPage.this, VoiceContentLibrary.class);
                startActivity(intent);
            }
        });

        exit = findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Exit the App");
                finishAffinity();
            }
        });

    }
}
