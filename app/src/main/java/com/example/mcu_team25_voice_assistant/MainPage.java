package com.example.mcu_team25_voice_assistant;
import com.example.mcu_team25_voice_assistant.audio_utils.Listener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class MainPage extends Activity {
    Button voicecommand;
    TextView voice_content_library;
    TextView exit;
    private static final String TAG = "MainPage: ";
    private boolean isRecord = false;// 设置正在录制的状态
    private DBHelper dbHelper;
    final private static int limit = 4;
    TextView[] commandViews = new TextView[limit];
    private String[] audioNames = new String[limit];
    private ArrayList<VoiceCommand> voiceCommandList;
    private Listener audio_listener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainpage);

        Log.d(TAG, "Main Page Opened");

        dbHelper = new DBHelper(MainPage.this);

        voiceCommandList = dbHelper.getTopRows(limit);

        commandViews[0] = findViewById(R.id.mainPageName1);
        commandViews[1] = findViewById(R.id.mainPageName2);
        commandViews[2] = findViewById(R.id.mainPageName3);
        commandViews[3] = findViewById(R.id.mainPageName4);

        for (int i = 0; i < voiceCommandList.size(); ++i) {
            VoiceCommand command = voiceCommandList.get(i);

            Log.d("Command ", Integer.toString(i) + " : " + command.printCommand());

            audioNames[i] = command.getName();
            commandViews[i].setText(audioNames[i]);
        }

        audio_listener = new Listener(this);
        audio_listener.LoadModel();

        voice_content_library = findViewById(R.id.voice_content_library);
        voice_content_library.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audio_listener.stopRecord();
                Log.d(TAG, "Switch to Voice Content Library");

                Intent intent = new Intent(MainPage.this, VoiceCommandLibrary.class);
                startActivity(intent);
            }
        });

        exit = findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audio_listener.stopRecord();
                Log.d(TAG, "Switch to Statistics");
                Intent intent = new Intent(MainPage.this, Statistics.class);
                startActivity(intent);
            }
        });

        voicecommand = findViewById(R.id.voicecommand);
        voicecommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isRecord) {
                    audio_listener.stopRecord();
                    isRecord = false;
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                } else {
                    if (audio_listener.CheckPermission()) {
                        audio_listener.AudioRecorderReady();
                        try {
                            audio_listener.startRecord("mainpage.wav");
                            isRecord = true;
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(MainPage.this, "Recording Started", Toast.LENGTH_SHORT).show();
                    } else {
                        audio_listener.RequestPermission();
                    }
                    findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                }
            }
        });
        voicecommand.performClick();
    }

}

