package com.example.mcu_team25_voice_assistant;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
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

// reference: https://stackoverflow.com/questions/10032003/how-to-make-a-countdown-timer-in-android
public class Timer extends Activity {

    TextView timeClock;
    TextView commandClass;
    Button timerReturn;
    private static final String TAG = "Timer";

    private static final String FORMAT = "%02d:%02d:%02d";

    MediaPlayer mediaPlayer;

    private DBHelper dbHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.timer);

        int time_duration_in_seconds = getIntent().getIntExtra("duration_in_seconds", 10);
        String commandName = getIntent().getStringExtra("commandName");

        dbHelper = new DBHelper(Timer.this);

        assert dbHelper.getCommand(commandName).getCount() == 1 : "command name is not right in database";

        Cursor cursor = dbHelper.getCommand(commandName);
        cursor.moveToFirst(); // https://stackoverflow.com/questions/50525179/gdx-sqlite-android-database-cursorindexoutofboundsexception-index-1-requested

        VoiceCommand command = new VoiceCommand(cursor.getString(1),
                cursor.getString(2), cursor.getFloat(3), cursor.getInt(4), cursor.getFloat(5));

        dbHelper.updateUsage(commandName, (float) time_duration_in_seconds);

        timeClock =(TextView)findViewById(R.id.textClock);
        commandClass = (TextView) findViewById(R.id.commandclass);
        timerReturn = (Button) findViewById(R.id.timeReturns);

        commandClass.setText(commandName);

        int COUNTDWON_INTERVAL = 1000;
        int Total_millis_counts = COUNTDWON_INTERVAL * time_duration_in_seconds;
        new CountDownTimer(Total_millis_counts, COUNTDWON_INTERVAL) { // adjust the milli seconds here

            public void onTick(long millisUntilFinished) {

                timeClock.setText(""+String.format(FORMAT,
                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(
                                TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
                                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
            }

            public void onFinish() {
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

        timerReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Return to Main Page");
                Intent intent = new Intent(Timer.this, MainPage.class);
                startActivity(intent);
            }
        });

    }

}