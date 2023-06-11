package com.example.mcu_team25_voice_assistant;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
//    public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private static final String TAG = "MainActivity";

//    private SensorManager sensorManager;
//    Sensor accelerometer;
//    TextView xValue, yValue, zValue;

    Button button_microphone_data_collection;
    Button button_countdown_timer;
    Button button_offline;
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

    private int recordBufsize = 0;

    private AudioRecord audioRecord = null;
    private Thread recordingThread;
    public static final int RequestPermisssionCode = 1;
    String AudioSavePathInDevice = null;
    private boolean isRecord = false;// 设置正在录制的状态
    String AudioName = null;


    private int audioSource = MediaRecorder.AudioSource.MIC;
    // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    private static int sampleRateInHz = 44100;
    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    byte[] audiobuffer;

    double[] audio_ref;
    double ref_mean;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                        Intent intent = new Intent(MainActivity.this,AudioDataCollection.class);
                        startActivity(intent);
                    }
                }
        );


        button_countdown_timer = (Button) findViewById(R.id.countdown_timer);

        button_countdown_timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,CounterDownTimer.class);
                intent.putExtra("time_duration_in_seconds",5);
                startActivity(intent);
            }
        });

        String audioFile = getFilesDir().getAbsolutePath()
                + "/count_down_timer.wav";
        // https://stackoverflow.com/questions/25097651/reading-pcm-file-to-double-array
        try {
            File file = new File(audioFile); // absolute path of the audio file
            InputStream fileInputstream = new FileInputStream(file); // we will use InputStream to read the bytes

            int size = (int) (file.length()) / 2;

            int rounded_size = 60 * (2688/2);

            audio_ref = new double[rounded_size];

            DataInputStream is = new DataInputStream(fileInputstream);

            ref_mean = 0.0;
            for (int i = 0; i < size; i++) {
                if (i < 22) {
                    double temp = is.readShort() / 256;
//                    Log.d(TAG, "header: " + (i+1) + " --> " + temp);
                } else {
                    audio_ref[i-22] = is.readShort() / 32768.0 / 256.0;
                    ref_mean = ref_mean + audio_ref[i];
//                    Log.d(TAG, "value: " + (i-22) + " --> " + String.valueOf(audio_ref[i-22]));
                }
            }
            for (int i = size-22; i < rounded_size; i++) {
                audio_ref[i] = 0.0;
            }
            ref_mean = ref_mean / rounded_size;

        } catch (FileNotFoundException e) {
            Log.i("File not found", "" + e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "read audio reference file " + String.valueOf(audio_ref.length) + " vs " +String.valueOf(ref_mean));


        Log.d(TAG, "Listening to User");

        if (CheckPermission()) {
            Log.d(TAG, "buttonStart.setOnClickListener: checked permission");
        } else {
            Log.d(TAG, "buttonStart.setOnClickListener: RequestPermission");
            RequestPermission();
        }

        AudioSavePathInDevice = getFilesDir().getAbsolutePath()
                + "/user_voice.wav";
        AudioRecorderReady();

        try {
            startRecord();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        Toast.makeText(MainActivity.this, "Recording Started", Toast.LENGTH_SHORT).show();

        button_offline = (Button) findViewById(R.id.button_offline);

        button_offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecord();
            }
        });

    }

    private void AudioRecorderReady() {
        recordBufsize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        Log.d(TAG, "AudioRecorderReady: size --> " + recordBufsize);

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.d(TAG, "AudioRecorderReady: No permission granted");

                return;
            }
            Log.d(TAG, "AudioRecorderReady:  permission granted");

            audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, recordBufsize);

            audiobuffer = new byte[recordBufsize*60];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void RequestPermission(){
        ActivityCompat.requestPermissions(MainActivity.this, new String[] {RECORD_AUDIO}, RequestPermisssionCode);
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case RequestPermisssionCode:
                if (grantResults.length > 0) {
                    boolean RecordPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (RecordPermission) {
                        Toast.makeText(this, "Permission is Granted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Permission is Denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    public boolean CheckPermission(){
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        boolean permission = result == PackageManager.PERMISSION_GRANTED;

        Log.d(TAG, "buttonStart.setOnClickListener: CheckPermission: " + permission );
        return  permission;
    }


    private void startRecord() {
        if (isRecord) {
            return;
        }
        audioRecord.startRecording();
        // 让录制状态为true
        isRecord = true;
        // 开启音频文件写入线程
        Log.i("audioRecordTest", "录音1");

        recordingThread = new Thread(new MainActivity.AudioRecordThread());
        recordingThread.start();
    }


    private void stopRecord() {
        isRecord = false;
        if (audioRecord != null) {
            audioRecord.stop();
            Log.i("audioRecordTest", "停止录音");
            audioRecord.release();
            audioRecord = null;
            recordingThread = null;
        }
    }

    class AudioRecordThread implements Runnable {
        @Override
        public void run() {
            Log.i("audioRecordTest", "录音");

            writeDateTOFile();//往文件中写入裸数据
        }
    }

    /**
     * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
     * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
     * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
     */
    private void writeDateTOFile() {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小

        AudioName = getFilesDir().getAbsolutePath() + "/temp.raw";

        Log.d(TAG, "writeDateTOFile: size --> " + recordBufsize);

        byte[] audiodata = new byte[recordBufsize];

        FileOutputStream fos = null;
        int readsize = 0;
        try {
            File file = new File(AudioName);
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (isRecord == true) {
            readsize = audioRecord.read(audiodata, 0, recordBufsize);

            for (int i = 0; i < recordBufsize * 59; ++i) {
                audiobuffer[i] = audiobuffer[i+recordBufsize];
            }
            for (int i = 0; i < recordBufsize; ++i) {
                audiobuffer[i+recordBufsize*59] = audiodata[i];
            }

            double[] data = new double[audiobuffer.length/2]; // pcm 16bit is 2 bytes per sample
            ByteBuffer bb = ByteBuffer.wrap(audiobuffer);

            double data_mean = 0.0;
            for (int i = 0; i < data.length; i++) {
                data[i] = bb.getShort()/ 32768.0 / 256.0;
//                Log.d(TAG, "value: " + i + " --> " + String.valueOf(data[i]));

                data_mean = data_mean + data[i];
            }
            data_mean = data_mean / data.length;

//            Log.d(TAG, "data length: " + String.valueOf(data.length) + " - ref length: " + String.valueOf(audio_ref.length));

            double corr[] = new double[recordBufsize];
            for (int i = 0; i < corr.length; ++i) {
                corr[i] = 0.0;
            }
            double data_variance = 0.0;
            double ref_variance = 0.0;
            double max_data = 0.0;
            double max_ref = 0.0;
            for (int i = 0; i < data.length; ++i) {
                data_variance = data_variance + (data[i] - data_mean) * (data[i] - data_mean);
                ref_variance = ref_variance + (audio_ref[i] - ref_mean) * (audio_ref[i] - ref_mean);
                if (max_data < data[i]) max_data = data[i];
                if (max_ref < audio_ref[i]) max_ref = audio_ref[i];
            }


            double max_corr = -1.0;
            for (int j = 0; j < recordBufsize; ++j) {
                for (int i = 0; i < data.length - recordBufsize; ++i) {
                    corr[j] = corr[j] + (audio_ref[i] - ref_mean) * (data[i+j] - data_mean);
                }
                corr[j] = corr[j]/ (Math.sqrt(data_variance) * Math.sqrt(ref_variance));
                if (max_corr < corr[j]) {
                    max_corr = corr[j];
                }
            }

//            Log.d(TAG, "correlation is: " + max_corr + "data_mean: " + data_mean + " data_variance: " + data_variance +
//                       "data_max: " + max_data + "ref_mean: " + ref_mean + "ref_variance:" + ref_variance + "ref_max: " + max_ref);
//            if (corr > 0.01) {
//                Log.d(TAG, "           correlation is: " + String.valueOf(corr));
//            }


//            Log.d(TAG, "writeDateTOFile: readsize --> " + readsize);

            if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
                try {
                    fos.write(audiodata);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            fos.close();// 关闭写入流
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





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