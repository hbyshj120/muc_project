package com.example.mcu_team25_voice_assistant;

import java.io.IOException;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
        import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mcu_team25_voice_assistant.audio_utils.Listener;
import com.example.mcu_team25_voice_assistant.audio_utils.Timer_listener;

// reference: https://stackoverflow.com/questions/10032003/how-to-make-a-countdown-timer-in-android
public class Timer extends Activity {

    TextView commandClass;
    Button timerReturn;
    private static final String TAG = "Timer";

    private static final String FORMAT = "%02d:%02d:%02d";

    MediaPlayer mediaPlayer;

    private DBHelper dbHelper;

    private TextView mTextViewCountDown;
    private Button mButtonStartPause;
    private Button mButtonStop;

    private CountDownTimer mCountDownTimer;

    private boolean mTimerRunning;

    private long mTimeLeftInMillis;

    String commandName;

    int time_duration_in_seconds;

    boolean isRecord = false;

    boolean isStop = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.timer);

        commandName = getIntent().getStringExtra("commandName");
        time_duration_in_seconds = getIntent().getIntExtra("duration", 10);
        int duration_type = getIntent().getIntExtra("duration_type", 10);
        mTimeLeftInMillis = time_duration_in_seconds * 1000;

        dbHelper = new DBHelper(Timer.this);

        assert dbHelper.getCommand(commandName).getCount() == 1 : "command name is not right in database";

        Cursor cursor = dbHelper.getCommand(commandName);
        cursor.moveToFirst(); // https://stackoverflow.com/questions/50525179/gdx-sqlite-android-database-cursorindexoutofboundsexception-index-1-requested

        VoiceCommand command = new VoiceCommand(cursor.getString(1),
                cursor.getString(2), cursor.getFloat(3), cursor.getInt(4), cursor.getFloat(5));

        commandClass = findViewById(R.id.commandclass);
        commandClass.setText(commandName);

        mTextViewCountDown = findViewById(R.id.text_view_countdown);

        mButtonStartPause = findViewById(R.id.button_start_pause);
        mButtonStop = findViewById(R.id.button_stop);

        mButtonStartPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });

        mButtonStartPause.performClick();

        Timer_listener audio_listener;

        audio_listener = new Timer_listener(this, mButtonStartPause, mButtonStop);
        audio_listener.LoadModel();

        if (audio_listener.CheckPermission()) {
            audio_listener.AudioRecorderReady();
            try {
                audio_listener.startRecord("timer.wav");
                isRecord = true;
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            Toast.makeText(Timer.this, "Recording Started", Toast.LENGTH_SHORT).show();
        } else {
            audio_listener.RequestPermission();
        }


        mButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();
            }
        });

        updateCountDownText();

        timerReturn = findViewById(R.id.timeReturns);

        timerReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Return to Main Page");

                audio_listener.stopRecord();
                isRecord = false;

                Intent intent = new Intent(Timer.this, MainPage.class);
                startActivity(intent);
            }
        });

    }

    private void startTimer() {
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                mButtonStartPause.setText("Start");
                mButtonStartPause.setVisibility(View.INVISIBLE);
                mButtonStop.setVisibility(View.INVISIBLE);

                if (!isStop) {
                    dbHelper.updateUsage(commandName, (float) time_duration_in_seconds);
                }

                mediaPlayer = new MediaPlayer();
                Log.d(TAG, getFilesDir().getAbsolutePath()+"/beep.wav");

                try {
                    mediaPlayer.setDataSource(getFilesDir().getAbsolutePath()+"/beep.wav");
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    mediaPlayer.prepare();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mediaPlayer.start();
                Toast.makeText(Timer.this, "Timer Finished!", Toast.LENGTH_SHORT).show();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Intent intent = new Intent(Timer.this, MainPage.class);
                        startActivity(intent);
                    }
                });
            }
        }.start();

        mTimerRunning = true;
        mButtonStartPause.setText("pause");
        mButtonStop.setVisibility(View.VISIBLE);
    }

    private void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning = false;
        mButtonStartPause.setText("Continue");
        mButtonStop.setVisibility(View.VISIBLE);
    }

    private void stopTimer() {
        isStop = true;
        dbHelper.updateUsage(commandName, (float) time_duration_in_seconds - mTimeLeftInMillis/1000);
        mTimeLeftInMillis = 0;
        mCountDownTimer.cancel();
        updateCountDownText();
        mButtonStop.setVisibility(View.INVISIBLE);
        mButtonStartPause.setVisibility(View.INVISIBLE);
        timerReturn.performClick();
    }

    private void updateCountDownText() {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        mTextViewCountDown.setText(timeLeftFormatted);
    }

}