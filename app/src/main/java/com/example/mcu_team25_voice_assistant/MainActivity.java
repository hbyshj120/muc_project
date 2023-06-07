package com.example.mcu_team25_voice_assistant;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private SpeechRecognizer speechRecognizer;
    private Intent intentRecognizer;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PackageManager.PERMISSION_GRANTED);

        textView = findViewById(R.id.textView);
        intentRecognizer = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecognizer.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                String mError = "";
                String mStatus = "Error detected";
                switch (error) {
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        mError = " network timeout";
//                        SpeechRecognizer.startListening();
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        mError = " network" ;
                        //toast("Please check data bundle or network settings");
                        return;
                    case SpeechRecognizer.ERROR_AUDIO:
                        mError = " audio";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        mError = " server";
//                        this.startListening();
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        mError = " client";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        mError = " speech time out" ;
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        mError = " no match" ;
//                        startListening();
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        mError = " recogniser busy" ;
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        mError = " insufficient permissions" ;
                        break;
                }
                Log.i(TAG,  "Error: " +  error + " - " + mError);

            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String string = "";
                if (matches != null) {
                    string = matches.get(0);
                    textView.setText(string);
                } else {
                    textView.setText("bad");
                }
                Log.i(TAG,  "In the onResults: ");
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
    }

    public void StartButton(View view) {
        speechRecognizer.startListening(intentRecognizer);
    }

    public void StopButton(View view) {
        speechRecognizer.stopListening();
    }
}