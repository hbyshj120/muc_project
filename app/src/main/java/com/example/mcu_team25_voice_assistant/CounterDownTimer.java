package com.example.mcu_team25_voice_assistant;

import static android.Manifest.permission.RECORD_AUDIO;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
        import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
        import android.os.CountDownTimer;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

// reference: https://stackoverflow.com/questions/10032003/how-to-make-a-countdown-timer-in-android
public class CounterDownTimer extends Activity {

    TextView text1;
    private static final String TAG = "CounterDownTimer";

    private static final String FORMAT = "%02d:%02d:%02d";

    MediaPlayer mediaPlayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.countdown_timer);

        text1=(TextView)findViewById(R.id.textView1);

        int time_duration_in_seconds = getIntent().getIntExtra("time_duration_in_seconds", 0);
        int COUNTDWON_INTERVAL = 1000;
        int Total_millis_counts = COUNTDWON_INTERVAL * time_duration_in_seconds;
        new CountDownTimer(Total_millis_counts, COUNTDWON_INTERVAL) { // adjust the milli seconds here

            public void onTick(long millisUntilFinished) {

                text1.setText(""+String.format(FORMAT,
                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(
                                TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
                                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
            }

            public void onFinish() {
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(getFilesDir().getAbsolutePath()+"/countdown_finished.wav");
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, getFilesDir().getAbsolutePath()+"/countdown_finished.wav");

                mediaPlayer.start();
                Toast.makeText(CounterDownTimer.this, "Countdown Finished!", Toast.LENGTH_SHORT).show();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Intent intent = new Intent(CounterDownTimer.this, MainActivity.class);
                        startActivity(intent);
                    }
                });
            }
        }.start();

    }

}