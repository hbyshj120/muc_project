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
import java.util.ArrayList;

public class VoiceCommandLibrary extends Activity {
    TextView popularvoicecommands;
    TextView allvoicecommands;
    final private static int limit = 4;
    TextView[] commandViews = new TextView[limit];
    Button[] playButtons = new Button[limit];
    Button[] modifyButtons = new Button[limit];
    Button addnewvoicecommand;
    TextView voicecontentlibraryreturn;
    MediaPlayer mediaPlayer;

    private DBHelper dbHelper;

    private ArrayList<VoiceCommand> voiceCommandList;
    private Boolean[] isActive = new Boolean[limit];
    private String[] audioNames = new String[limit];
    private String[] audioPaths = new String[limit];

    private static final String TAG = "Voice Content Library: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voicecommandlibrary);

        Log.d(TAG, "Voice Content Library Opened");

        dbHelper = new DBHelper(VoiceCommandLibrary.this);

        voiceCommandList = dbHelper.getTopRows(limit);

        commandViews[0] = findViewById(R.id.name1);
        commandViews[1] = findViewById(R.id.name2);
        commandViews[2] = findViewById(R.id.name3);
        commandViews[3] = findViewById(R.id.name4);
        playButtons[0] = findViewById(R.id.play1);
        playButtons[1] = findViewById(R.id.play2);
        playButtons[2]= findViewById(R.id.play3);
        playButtons[3] = findViewById(R.id.play4);
        modifyButtons[0] = findViewById(R.id.modify1);
        modifyButtons[1]= findViewById(R.id.modify2);
        modifyButtons[2] = findViewById(R.id.modify3);
        modifyButtons[3] = findViewById(R.id.modify4);

        for (int i = 0; i < playButtons.length; ++i) {
            playButtons[i].setEnabled(false);
            modifyButtons[i].setEnabled(false);
        }

        for (int i = 0; i < voiceCommandList.size(); ++i) {
            VoiceCommand command = voiceCommandList.get(i);

            Log.d("Command ", Integer.toString(i) + " : " + command.printCommand());

            isActive[i] = Boolean.TRUE;
            audioNames[i] = command.getName();
            audioPaths[i] = command.getPath();
            commandViews[i].setText(audioNames[i]);
            playButtons[i].setEnabled(true);
            modifyButtons[i].setEnabled(true);
        }

        for (int i = 0; i < voiceCommandList.size(); ++i) {
            int finalI = i;
            playButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    modifyButtons[finalI].setEnabled(false);
                    mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(audioPaths[finalI]);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Log.d(TAG, audioPaths[finalI] + " is loaded");

                    mediaPlayer.start();
                    Toast.makeText(VoiceCommandLibrary.this, audioPaths[finalI] + " is playing", Toast.LENGTH_SHORT).show();
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            modifyButtons[finalI].setEnabled(true);
                        }
                    });
                }
            });

            modifyButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Switch to Modify Voice Command");
                    Intent intent = new Intent(VoiceCommandLibrary.this, AddNewVoiceCommand.class);
                    intent.putExtra("commandName", audioNames[finalI]);
                    intent.putExtra("isModify", true);
                    startActivity(intent);
                }
            });
        }


        addnewvoicecommand = findViewById(R.id.addnewcommands);
        addnewvoicecommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Switch to Add New Voice Command");
                Intent intent = new Intent(VoiceCommandLibrary.this, AddNewVoiceCommand.class);
                startActivity(intent);
            }
        });

        voicecontentlibraryreturn = findViewById(R.id.returnbutton);
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
