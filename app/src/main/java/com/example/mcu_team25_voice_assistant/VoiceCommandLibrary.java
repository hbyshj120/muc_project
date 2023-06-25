package com.example.mcu_team25_voice_assistant;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class VoiceCommandLibrary extends Activity {
    TextView popularvoicecommands;
    TextView allvoicecommands;
    TextView voicecommand1;
    Button voicecommand1play;
    Button voicecommand1modify;
    Button addnewvoicecommand;
    TextView voicecontentlibraryreturn;
    MediaPlayer mediaPlayer;
    private static final String TAG = "Voice Content Library: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voicecommandlibrary);

        Log.d(TAG, "Voice Content Library Opened");

        voicecommand1 = findViewById(R.id.voicecommand1);
        // list top 5 popular voice commands by visiting DB
        String voicecommand1name = "Plank 30 seconds";
        String voicecommand1filename = "beep.wav";

        voicecommand1.setText(voicecommand1name);

        voicecommand1play = findViewById(R.id.voicecommand1play);
        voicecommand1modify = findViewById(R.id.voicecommand1modify);

        voicecommand1play.setEnabled(true);
        voicecommand1modify.setEnabled(true);

        voicecommand1play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                voicecommand1modify.setEnabled(false);
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(getFilesDir().getAbsolutePath() + "/" +voicecommand1filename);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, getFilesDir().getAbsolutePath() + "/" +voicecommand1filename + " is loaded");

                mediaPlayer.start();
                Toast.makeText(VoiceCommandLibrary.this, voicecommand1name + " is playing", Toast.LENGTH_SHORT).show();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        voicecommand1modify.setEnabled(true);
                    }
                });
            }
        });

        voicecommand1modify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        addnewvoicecommand = findViewById(R.id.addnewvoicecommand);
        addnewvoicecommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Switch to Add New Voice Command");
                Intent intent = new Intent(VoiceCommandLibrary.this, AddNewVoiceCommand.class);
                startActivity(intent);
            }
        });

        voicecontentlibraryreturn = findViewById(R.id.voicecontentlibraryreturn);
        voicecontentlibraryreturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Return to Main Page");
                Intent intent = new Intent(VoiceCommandLibrary.this, MainPage.class);
                startActivity(intent);
            }
        });
    }
}
