package com.example.mcu_team25_voice_assistant;
import com.example.mcu_team25_voice_assistant.audio_utils.AudioFileProcess;

import static android.Manifest.permission.RECORD_AUDIO;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import java.util.List;

public class AddNewVoiceCommand extends Activity {

    Button record, stop, play, save;
    Button record2, stop2, play2;

    Button check;

    public static final int RequestPermisssionCode = 1;

    private int recordBufsize = 0;

    private AudioRecord audioRecord = null;

    private int audioSource = MediaRecorder.AudioSource.MIC;
    // 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    private static int sampleRateInHz = 16000;
    // 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    // 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private boolean isRecord = false;// 设置正在录制的状态

    private Thread recordingThread;
    MediaPlayer mediaPlayer;

    private ImageView mImageView;

    private TextView pagereturn;


    private static final String TAG = "Add New Voice Command: ";

    EditText command_name;
    String commandName;
    String commandPath;

    DBHelper db_command;

    AudioFileProcess audio = new AudioFileProcess();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addnewvoicecommand);

        record = (Button) findViewById(R.id.newvoicecommandrecord);
        stop = (Button) findViewById(R.id.newvoicecommandstop);
        play = (Button) findViewById(R.id.newvoicecommandplay);
        record2 = (Button) findViewById(R.id.newvoicecommandrecord2);
        stop2 = (Button) findViewById(R.id.newvoicecommandstop2);
        play2 = (Button) findViewById(R.id.newvoicecommandplay2);
        check = (Button) findViewById(R.id.newvoicecommandcheck);
        save = (Button) findViewById(R.id.newvoicecommandsave);

        record.setEnabled(true);
        stop.setEnabled(false);
        play.setEnabled(false);
        save.setEnabled(false);
        record2.setEnabled(true);
        stop2.setEnabled(false);
        play2.setEnabled(false);
        check.setEnabled(true);

        commandName = getIntent().getStringExtra("commandName");
        Boolean isModify = getIntent().getBooleanExtra("isModify", false);

        command_name = findViewById(R.id.command_name);

        // DBHelper
        db_command = new DBHelper(AddNewVoiceCommand.this);

        if (isModify) {
            Log.d(TAG, "Modify Voice Command Opened for " + commandName);
            play.setEnabled(true);
            command_name.setText(commandName);
            assert db_command.getCommand(commandName).getCount() == 1 : "command name is not right in database";

            Cursor cursor = db_command.getCommand(commandName);
            cursor.moveToFirst(); // https://stackoverflow.com/questions/50525179/gdx-sqlite-android-database-cursorindexoutofboundsexception-index-1-requested

            VoiceCommand command = new VoiceCommand(cursor.getString(1),
                    cursor.getString(2), cursor.getFloat(3), cursor.getInt(4), cursor.getFloat(5));
            commandPath = command.getPath();

            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(AddNewVoiceCommand.this));
            }
            Python python = Python.getInstance();

            PyObject pyObject = python.getModule("voicerecognizer");
            List<PyObject> obj = pyObject.callAttr("process", commandPath).asList();

            mImageView = (ImageView) findViewById(R.id.image1);
            mImageView.setImageBitmap(BitmapFactory.decodeFile(commandPath + ".png"));
            
        } else {
            Log.d(TAG, "Add New Voice Command Opened");
        }

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (command_name.getText().toString().isEmpty()) {
                    Toast.makeText(AddNewVoiceCommand.this, "Enter Command Name First", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    commandName = command_name.getText().toString();
                    commandPath = getFilesDir().getAbsolutePath() + "/" + commandName + ".wav";
                }

                if (isRecord) {
                    stopRecord();
                    Log.d(TAG, "Not recording");

                } else {
                    Log.d(TAG, "Is recording");
                    if (CheckPermission()) {
                        Log.d(TAG, "buttonStart.setOnClickListener: checked permission");

                        AudioRecorderReady();

                        try {
                            startRecord(commandName + ".wav");
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }

                        record.setEnabled(false);
                        stop.setEnabled(true);
                        play.setEnabled(false);
                        save.setEnabled(false);
                        record2.setEnabled(false);
                        stop2.setEnabled(false);

                        Toast.makeText(AddNewVoiceCommand.this, "Recording Started", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "buttonStart.setOnClickListener: RequestPermission");
                        RequestPermission();
                    }

                }
            }
        });


        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecord();
                record.setEnabled(true);
                stop.setEnabled(false);
                play.setEnabled(true);
                save.setEnabled(true);
                record2.setEnabled(true);
                stop2.setEnabled(false);

                Toast.makeText(AddNewVoiceCommand.this, "Recording Completed", Toast.LENGTH_SHORT).show();

                Log.d(TAG, "Add New Voice Command Before Plotting");

                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(AddNewVoiceCommand.this));
                }
                Python python = Python.getInstance();

                PyObject pyObject = python.getModule("voicerecognizer");
                List<PyObject> obj = pyObject.callAttr("process", commandPath).asList();

                mImageView = (ImageView) findViewById(R.id.image1);
                mImageView.setImageBitmap(BitmapFactory.decodeFile(commandPath + ".png"));
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record.setEnabled(false);
                stop.setEnabled(false);
                play.setEnabled(false);
                save.setEnabled(false);
                record2.setEnabled(true);
                stop2.setEnabled(false);

                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(commandPath);
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, commandPath + " is loaded");

                mediaPlayer.start();
                Toast.makeText(AddNewVoiceCommand.this,   commandPath +" is playing", Toast.LENGTH_SHORT).show();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        record.setEnabled(true);
                        stop.setEnabled(false);
                        play.setEnabled(true);
                        save.setEnabled(true);
                    }
                });
            }
        });

        record2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (CheckPermission()) {
                    Log.d(TAG, "buttonStart.setOnClickListener: checked permission");

                    AudioRecorderReady();

                    try {
                        startRecord("record2.wav");
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    record.setEnabled(false);
                    stop.setEnabled(false);
                    play.setEnabled(false);
                    save.setEnabled(false);
                    record2.setEnabled(false);
                    stop2.setEnabled(true);

                    Toast.makeText(AddNewVoiceCommand.this, "Recording Started", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "buttonStart.setOnClickListener: RequestPermission");
                    RequestPermission();
                }
            }
        });

        stop2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecord();
                record.setEnabled(true);
                stop.setEnabled(false);
                play.setEnabled(true);
                save.setEnabled(true);
                record2.setEnabled(true);
                stop2.setEnabled(false);
                play2.setEnabled(true);
                check.setEnabled(true);

                Toast.makeText(AddNewVoiceCommand.this, "Recording2 Completed", Toast.LENGTH_SHORT).show();

                Log.d(TAG, "Add New Voice Command Before Plotting2");

                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(AddNewVoiceCommand.this));
                }
                Python python = Python.getInstance();
                String NewAudioName = getFilesDir().getAbsolutePath() + "/record2.wav";
                PyObject pyObject = python.getModule("voicerecognizer");
                List<PyObject> obj = pyObject.callAttr("process", NewAudioName).asList();

                mImageView = (ImageView) findViewById(R.id.image2);
                mImageView.setImageBitmap(BitmapFactory.decodeFile(getFilesDir().getAbsolutePath() + "/record2.wav.png"));
            }
        });

        play2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record.setEnabled(false);
                stop.setEnabled(false);
                play.setEnabled(false);
                save.setEnabled(false);
                record2.setEnabled(false);
                stop2.setEnabled(false);

                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(getFilesDir().getAbsolutePath() + "/record2.wav");
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, getFilesDir().getAbsolutePath() + "/record2.wav" + " is loaded");

                mediaPlayer.start();
                Toast.makeText(AddNewVoiceCommand.this,   getFilesDir().getAbsolutePath() + "/record2.wav is playing", Toast.LENGTH_SHORT).show();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        record.setEnabled(true);
                        stop.setEnabled(false);
                        play.setEnabled(true);
                        save.setEnabled(true);
                        record2.setEnabled(true);
                        stop.setEnabled(false);
                        play.setEnabled(true);
                    }
                });
            }
        });


        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Under the Check");

                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(AddNewVoiceCommand.this));
                }
                Python python = Python.getInstance();
                String filename2 = getFilesDir().getAbsolutePath() + "/record2.wav";
                PyObject pyObject = python.getModule("voicerecognizer");
                PyObject obj = pyObject.callAttr("speechcorrelation", commandPath, filename2);

                TextView text1=(TextView)findViewById(R.id.addnewvoicecommandcorrcoeff);
                text1.setText(String.valueOf(obj.toInt()));

                mImageView = (ImageView) findViewById(R.id.image3);
                mImageView.setImageBitmap(BitmapFactory.decodeFile(getFilesDir().getAbsolutePath() + "/record2.wav_aligned.png"));

            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Save Voice Command to Database");

                if (isModify) {
                    db_command.updateAudio(commandName, commandPath);
                } else {
                    db_command.addCommand(commandName, commandPath, 0, 0, 0);
                }

                // Indicate to the user that data was stored
                Toast.makeText(AddNewVoiceCommand.this, "Voice Command Saved", Toast.LENGTH_SHORT).show();


            }
        });

        pagereturn = findViewById(R.id.addnewvoicecommandreturn);
        pagereturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Return to Main Page");
                Intent intent = new Intent(AddNewVoiceCommand.this, VoiceCommandLibrary.class);
                startActivity(intent);
            }
        });


    }

    public boolean CheckPermission(){
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return  result == PackageManager.PERMISSION_GRANTED;
    }

    public void RequestPermission(){
        ActivityCompat.requestPermissions(AddNewVoiceCommand.this, new String[] {RECORD_AUDIO}, RequestPermisssionCode);
    }

    private void AudioRecorderReady() {
//        recordBufsize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        recordBufsize = 2048*2;
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
        recordingThread = new Thread(new AddNewVoiceCommand.AudioRecordThread(getFilesDir().getAbsolutePath() + '/' + filename));
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

//            Runnable r = new Runnable() {
//                @Override
//                public void run(){
//                    Intent intent = new Intent(AddNewVoiceCommand.this, FitnessMate.class);
//                    startActivity(intent);
//                }
//            };
//
//            Handler h = new Handler();
//            h.postDelayed(r, 20000); // <-- the "1000" is the delay time in miliseconds.
        }
    }

    class AudioRecordThread implements Runnable {
        String wavFilename;
        String rawFilename = getFilesDir().getAbsolutePath() + "/temp.raw";
        AudioRecordThread(String s) {wavFilename = s; }
        @Override
        public void run() {
            writeDateTOFile(rawFilename, wavFilename);//往文件中写入裸数据
        }
    }

    /**
     * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
     * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
     * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
     */
    private void writeDateTOFile(String rawFilename, String wavFilename) {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        Log.d(TAG, "writeDateTOFile: size --> " + recordBufsize);

        byte[] audiodata = new byte[recordBufsize];
        FileOutputStream fos = null;
        int readsize = 0;
        try {
            File file = new File( rawFilename);
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
        } catch (Exception e) {
            e.printStackTrace();
        }

        double vad_threshold = 0.0;

        // active listening --> active voice occurred before and pass voice occurence never > 1 second
        boolean activeListening = false;
        int activeVoiceOccurence= 0;
        int activeVoiceDuration = 3;
        int passiveVoiceOccurence = 0;
        double passiveVoiceDuration = 0.4;
        int win_length = recordBufsize / 2;
        int passVoiceMaxOccurence = (int) (sampleRateInHz * passiveVoiceDuration / win_length);


        int counter = 0;
        while (isRecord == true) {
            readsize = audioRecord.read(audiodata, 0, recordBufsize);
//            Log.d(TAG, "writeDateTOFile: readsize --> " + readsize);
            short[] shorts = new short[audiodata.length/2];
            // https://stackoverflow.com/questions/5625573/byte-array-to-short-array-and-back-again-in-java
            // to turn bytes to shorts as either big endian or little endian.
            ByteBuffer.wrap(audiodata).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);


            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(AddNewVoiceCommand.this));
            }
            Python python = Python.getInstance();
            PyObject pyObject = python.getModule("voicerecognizer");
            PyObject obj = pyObject.callAttr("speechratio", shorts, sampleRateInHz);
            double speechratio = obj.toDouble();
            if (counter < 5) {
                vad_threshold = vad_threshold + speechratio / 5.0;
                counter = counter + 1;
                continue;
            }
            if (counter == 5) {
                vad_threshold = vad_threshold + 10;
                counter = counter + 1;
                Log.d(TAG, "vad_threshold = " + vad_threshold);
                continue;
            }

            if (speechratio >= vad_threshold && passiveVoiceOccurence <= passVoiceMaxOccurence) {
                activeListening = true;
                passiveVoiceOccurence = 0;
                activeVoiceOccurence++;
            } else if (speechratio >= vad_threshold && passiveVoiceOccurence > passVoiceMaxOccurence) {
                throw new java.lang.Error("Program should not enter this branch!");
            } else if (speechratio < vad_threshold && passiveVoiceOccurence <= passVoiceMaxOccurence) {
                if (activeListening) {
                    if (passiveVoiceOccurence < passVoiceMaxOccurence) {
                        passiveVoiceOccurence++;
                    } else {
                        Log.d(TAG, "reaching passive Voice Occurence tolerence: " + passVoiceMaxOccurence);
                        Log.d(TAG, "=======================================================================");

                        activeListening = false;
                        passiveVoiceOccurence = 0;

                        // close current temp file
                        try {
                            fos.close();// 关闭写入流
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (activeVoiceOccurence >= activeVoiceDuration) {
                            copyWaveFile(rawFilename, wavFilename);//给裸数据加上头文件
                            Log.d(TAG, "save wav file: " + wavFilename);
                            isRecord = false;
                        }

                        // prepare a new temp file
                        try {
                            File file = new File(rawFilename);
                            if (file.exists()) {
                                file.delete();
                            }
                            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        activeVoiceOccurence = 0;
                    }
                }  else {
                    assert passiveVoiceOccurence == 0 : "Not active listening yet; so no passive occurence either";
                }
            } else if (speechratio < vad_threshold && passiveVoiceOccurence > passVoiceMaxOccurence) {
                throw new java.lang.Error("Program should not enter this branch!");
            }

            byte[] audiodata2 = new byte[recordBufsize];
            // to turn shorts back to bytes.
            ByteBuffer.wrap(audiodata2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);

            if (AudioRecord.ERROR_INVALID_OPERATION != readsize && activeListening) {
                Log.d(TAG, "ratio: " + speechratio +  ": active Listening: " + activeListening +  "; active Voice Occurence: " + activeVoiceOccurence + "; passive Voice Occurence: " + passiveVoiceOccurence );

                try {
                    fos.write(audiodata2);
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
            audio.WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
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

}
