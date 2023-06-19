package com.example.mcu_team25_voice_assistant;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

public class AddNewVoiceCommand extends Activity {

    private static final String TAG = "Add New Voice Command: ";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addnewvoicecommand);

        Log.d(TAG, "Add New Voice Command Opened");

    }
}
