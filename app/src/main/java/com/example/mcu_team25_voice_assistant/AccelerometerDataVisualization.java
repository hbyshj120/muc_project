package com.example.mcu_team25_voice_assistant;

        import androidx.appcompat.app.AppCompatActivity;

        import android.content.Intent;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.Button;

public class AccelerometerDataVisualization extends AppCompatActivity {
    //    public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private static final String TAG = "MainActivity";

//    private SensorManager sensorManager;
//    Sensor accelerometer;
//    TextView xValue, yValue, zValue;

    Button button_microphone_data_collection;
    Button button_countdown_timer;
//    , buttonStopRecording, buttonPlay, buttonStopPlaying;

//    Thread runner;
//    private static double mEMA = 0.0;
//    static final private double EMA_FILTER = 0.5;
//    public static final int RequestPermisssionCode = 1;
//
//    String AudioSavePathInDevice = null;
//
//    MediaRecorder mediaRecorder;
//    MediaPlayer mediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fitnessmate);

//        xValue = (TextView) findViewById(R.id.xValue);
//        yValue = (TextView) findViewById(R.id.yValue);
//        zValue = (TextView) findViewById(R.id.zValue);


        Log.d(TAG, "onCreate: Initializing Sensor Services");
//        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

//        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

//        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Log.d(TAG, "onCreate: Registered accelerometer listener");

        button_microphone_data_collection = (Button) findViewById(R.id.microphone_data_collection);

        button_microphone_data_collection.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(AccelerometerDataVisualization.this,AudioDataCollection.class);
                        startActivity(intent);
                    }
                }
        );


//        button_countdown_timer = (Button) findViewById(R.id.countdown_timer);
//
//        button_countdown_timer.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(AccelerometerDataVisualization.this,CounterDownTimer.class);
//                intent.putExtra("time_duration_in_seconds",5);
//                startActivity(intent);
//            }
//        });

//        buttonStopRecording.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mediaRecorder.stop();
//                buttonStopRecording.setEnabled(false);
//                buttonPlay.setEnabled(true);
//                buttonStart.setEnabled(true);
//                buttonStopPlaying.setEnabled(false);
//
//                Toast.makeText(MainActivity.this, "Recording Completed", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        buttonPlay.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) throws IllegalArgumentException, SecurityException, IllegalStateException{
//                buttonStopRecording.setEnabled(false);
//                buttonStart.setEnabled(false);
//                buttonStopPlaying.setEnabled(true);
//
//                mediaPlayer = new MediaPlayer();
//                try{
//                    mediaPlayer.setDataSource(AudioSavePathInDevice);
//                    mediaPlayer.prepare();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                mediaPlayer.start();
//                Toast.makeText(MainActivity.this, "Last Recording Playing", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        buttonStopPlaying.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                buttonStopRecording.setEnabled(false);
//                buttonStart.setEnabled(true);
//                buttonStopPlaying.setEnabled(true);
//                buttonPlay.setEnabled(false);
//            }
//        });

    }

//    public void MediaRecorderReady() {
//        mediaRecorder = new MediaRecorder();
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//        mediaRecorder.setOutputFile(AudioSavePathInDevice);
//    }
//    public void RequestPermission(){
//        ActivityCompat.requestPermissions(MainActivity.this, new String[] {RECORD_AUDIO}, RequestPermisssionCode);
//    }
//
//    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
//        switch(requestCode) {
//            case RequestPermisssionCode:
//                if (grantResults.length > 0) {
//                    boolean StoragePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
//                    boolean RecordPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
//
//                    if (StoragePermission && RecordPermission) {
//                        Toast.makeText(this, "Permission is Granted", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(this, "Permission is Denied", Toast.LENGTH_SHORT).show();
//                    }
//                }
//                break;
//        }
//    }
//
//    public boolean CheckPermission(){
//        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
//        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
//        boolean comp = result == PackageManager.PERMISSION_GRANTED;
//        boolean comp1 = result1 == PackageManager.PERMISSION_GRANTED;
//
//        Log.d(TAG, "buttonStart.setOnClickListener: CheckPermission: " + comp + " - : "  + comp1);
//
//        return  result1 == PackageManager.PERMISSION_GRANTED;
//    }
//
//
//    @Override
//    public void onAccuracyChanged(Sensor sensor, int i) {
//
//    }
//
//    @Override
//    public void onSensorChanged(SensorEvent sensorEvent) {
////        Log.d(TAG, "onSensorChanged: X: " + sensorEvent.values[0] + "Y: " + + sensorEvent.values[1] + "Z: " + + sensorEvent.values[2]);
//        xValue.setText("xValue: " + sensorEvent.values[0]);
//        yValue.setText("yValue: " + sensorEvent.values[1]);
//        zValue.setText("zValue: " + sensorEvent.values[2]);
//
//        String entry = sensorEvent.values[0] + "," + sensorEvent.values[1] + "," + sensorEvent.values[2] + ",";
//
//        try {
//
//            Log.v("Accel: ", entry);
//
//            File file = new File(getFilesDir().getAbsolutePath(), "/output.csv");
//            FileOutputStream f = new FileOutputStream(file, true);
//
//
//            try {
//                f.write(entry.getBytes());
//                f.flush();
//                f.close();
//                Toast.makeText(getBaseContext(), "Data saved", Toast.LENGTH_LONG).show();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//    }

}