package com.example.mcu_team25_voice_assistant;

import static android.Manifest.permission.RECORD_AUDIO;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;


public class MainPage extends Activity {

    Button voicecommand;
    TextView voice_content_library;
    TextView exit;
    private static final String TAG = "MainPage: ";

    public static final int RequestPermisssionCode = 1;

    private int recordBufsize = 0;

    private AudioRecord audioRecord = null;

    private int audioSource = MediaRecorder.AudioSource.MIC;
    // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    private static int sampleRateInHz = 16000; //44100;
    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    String AudioName = null;

    //NewAudioName可播放的音频文件
    String NewAudioName = null;

    private boolean isRecord = false;// 设置正在录制的状态

    private Thread recordingThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainpage);

        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        Python python = Python.getInstance();
        String NewAudioName = getFilesDir().getAbsolutePath() + "/new.wav";
        PyObject pyObject = python.getModule("hello");
        PyObject obj = pyObject.callAttr("process", NewAudioName);

        Log.d(TAG, obj.toString());

        Log.d(TAG, "Main Page Opened");

        voice_content_library = findViewById(R.id.voice_content_library);
        voice_content_library.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Switch to Voice Content Library");

                Intent intent = new Intent(MainPage.this, VoiceCommandLibrary.class);
                startActivity(intent);
            }
        });

        exit = findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Exit the App");
                finishAffinity();
            }
        });

        voicecommand = findViewById(R.id.voicecommand);
        voicecommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isRecord) {
                    stopRecord();
                    isRecord = false;
                    Log.d(TAG, "Not recording");

                } else {
                    Log.d(TAG, "Is recording");
                    if (CheckPermission()) {
                        Log.d(TAG, "buttonStart.setOnClickListener: checked permission");

                        AudioRecorderReady();

                        try {
                            startRecord("mainpage.wav");
                            isRecord = true;
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(MainPage.this, "Recording Started", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "buttonStart.setOnClickListener: RequestPermission");
                        RequestPermission();
                    }

                }


            }
        });

    }



    public boolean CheckPermission(){
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return  result == PackageManager.PERMISSION_GRANTED;
    }

    public void RequestPermission(){
        ActivityCompat.requestPermissions(MainPage.this, new String[] {RECORD_AUDIO}, RequestPermisssionCode);
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
                return;
            }
            audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, recordBufsize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRecord(String filename) {
        if (isRecord) {
            return;
        }
        audioRecord.startRecording();
        // 让录制状态为true
        isRecord = true;
        // 开启音频文件写入线程
        recordingThread = new Thread(new MainPage.AudioRecordThread(getFilesDir().getAbsolutePath() + '/' + filename));
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
        String wavFilename;
        String rawFilename = getFilesDir().getAbsolutePath() + "/temp.raw";
        AudioRecordThread(String s) {wavFilename = s; }
        @Override
        public void run() {
            writeDateTOFile(rawFilename);//往文件中写入裸数据
            copyWaveFile(rawFilename, wavFilename);//给裸数据加上头文件
        }
    }

    /**
     * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
     * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
     * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
     */
    private void writeDateTOFile(String rawname) {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        Log.d(TAG, "writeDateTOFile: size --> " + recordBufsize);

        byte[] audiodata = new byte[recordBufsize];
        FileOutputStream fos = null;
        int readsize = 0;
        try {
            File file = new File( rawname);
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
        } catch (Exception e) {
            e.printStackTrace();
        }
        int counter = 0;
        boolean on = false;
        double threshold = 0.7;
        boolean start = true;
        int off_counter = 0;
        while (isRecord == true) {
            readsize = audioRecord.read(audiodata, 0, recordBufsize);
//            Log.d(TAG, "writeDateTOFile: readsize --> " + readsize);
            short[] shorts = new short[audiodata.length/2];
            // https://stackoverflow.com/questions/5625573/byte-array-to-short-array-and-back-again-in-java
            // to turn bytes to shorts as either big endian or little endian.
            ByteBuffer.wrap(audiodata).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);


            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(MainPage.this));
            }
            Python python = Python.getInstance();
            PyObject pyObject = python.getModule("hello");
            PyObject obj = pyObject.callAttr("speechratio", shorts, sampleRateInHz);
            double speechratio = obj.toDouble();

            if (speechratio >= threshold) {
                on = true;
                start = false;
                Log.d(TAG, "Triggered "  + speechratio + " --> " + off_counter);
            } else {
                if (!on && !start) {
                    off_counter++;
                } else {
                    on = false;
                    off_counter = 0;
                }
            }

            if (off_counter > 60) {
                threshold = 1.1;
                Log.d(TAG, "conversion " + counter + ": " + speechratio + " --> " + off_counter);
            }

            byte[] audiodata2 = new byte[recordBufsize];
            // to turn shorts back to bytes.
            ByteBuffer.wrap(audiodata2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);



            if (AudioRecord.ERROR_INVALID_OPERATION != readsize && !start && off_counter < 30) {
                try {
                    fos.write(audiodata2);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            counter = counter + 1;
        }
        try {
            fos.close();// 关闭写入流
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 这里得到可播放的音频文件
    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = sampleRateInHz;
        int channels = 1;
        long byteRate = 16 * sampleRateInHz * channels / 8;
        byte[] data = new byte[recordBufsize];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。
     * 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有
     * 自己特有的头文件。
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}

