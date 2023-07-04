package com.example.mcu_team25_voice_assistant;

import static android.Manifest.permission.RECORD_AUDIO;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
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

import org.pytorch.Device;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


// https://medium.com/mlearning-ai/integrating-custom-pytorch-models-into-an-android-app-a2cdfce14fe8
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

    Module module;

    private DBHelper dbHelper;
    final private static int limit = 4;
    TextView[] commandViews = new TextView[limit];
    private String[] audioNames = new String[limit];
    private ArrayList<VoiceCommand> voiceCommandList;

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


        try {
            String modelpath = assetFilePath("soundclassifier_quantized_lite.ptl");
            module = LiteModuleLoader.load(modelpath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        voice_content_library = findViewById(R.id.voice_content_library);
        voice_content_library.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecord();
                Log.d(TAG, "Switch to Voice Content Library");


                Intent intent = new Intent(MainPage.this, VoiceCommandLibrary.class);
                startActivity(intent);
            }
        });

        exit = findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecord();
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
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                } else {
                    Log.d(TAG, "Is recording");
                    if (CheckPermission()) {
                        Log.d(TAG, "buttonStart.setOnClickListener: checked permission");
                        AudioRecorderReady();
                        try {
                            startRecord("mainpage.wav");
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(MainPage.this, "Recording Started", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "buttonStart.setOnClickListener: RequestPermission");
                        RequestPermission();
                    }
                    findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                }


            }
        });

        voicecommand.performClick();

    }



    public boolean CheckPermission(){
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return  result == PackageManager.PERMISSION_GRANTED;
    }

    public void RequestPermission(){
        ActivityCompat.requestPermissions(MainPage.this, new String[] {RECORD_AUDIO}, RequestPermisssionCode);
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
        Log.i("audioRecordTest", "start 录音");
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
            try {
                writeDateTOFile(rawFilename, wavFilename);//往文件中写入裸数据
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
     * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
     * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
     */
    private void writeDateTOFile(String rawFilename, String wavFilename) throws Exception {
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

        double vad_threshold = 45.0;
        double class_probability = 0.5;


        // active listening --> active voice occurred before and pass voice occurence never > 1 second
        boolean activeListening = false;
        int activeVoiceOccurence= 0;
        int activeVoiceDuration = 2;
        int passiveVoiceOccurence = 0;
        double passiveVoiceDuration = 1.0;
        int win_length = recordBufsize / 2;
        int passVoiceMaxOccurence = (int) (sampleRateInHz * passiveVoiceDuration / win_length);



        while (isRecord == true) {
            readsize = audioRecord.read(audiodata, 0, recordBufsize);
            short[] shorts = new short[audiodata.length/2];
            // https://stackoverflow.com/questions/5625573/byte-array-to-short-array-and-back-again-in-java
            // to turn bytes to shorts as either big endian or little endian.
            ByteBuffer.wrap(audiodata).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

//            Log.d(TAG, "writeDateTOFile: readsize --> " + readsize + " --> " + shorts);

            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(MainPage.this));
            }
            Python python = Python.getInstance();
            PyObject pyObject = python.getModule("voicerecognizer");
            PyObject obj = pyObject.callAttr("speechratio", shorts, sampleRateInHz);
            double speechratio = obj.toDouble();

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

                            Long tsLong = System.currentTimeMillis() / 1000;
                            String ts = tsLong.toString();
                            String filename = wavFilename.substring(0, wavFilename.length() - 4) + "_" + ts + ".wav";
                            //   and create a wav file with header based on temp file
                            copyWaveFile(rawFilename, filename);//给裸数据加上头文件
                            Log.d(TAG, "save wav file: " + filename);


                            AudioFileProcess audio = new AudioFileProcess();
//                            filename = getFilesDir().getAbsolutePath() + "/mainpage_1688365608.wav";
                            float[] inputs =  audio.ReadingAudioFile(filename, 16000, 32768);

                            Log.d(TAG, "inputs length: " + inputs.length);

                            long[] shape = new long[]{1, 1, 16000};
                            Tensor inputTensor = Tensor.fromBlob(inputs, shape);
                            Log.d(TAG, "inputTensor type " + inputTensor.dtype());

                            IValue input = IValue.from(inputTensor);
                            final IValue output = module.forward(input);

                            final IValue[] output2 = module.forward(IValue.from(inputTensor)).toTuple();
                            final float[] pred = output2[0].toTensor().getDataAsFloatArray();
                            for (int i = 0; i < pred.length; i++) {
                                Log.d(TAG, "pred: "+ pred[i]);
                            }
                            final float[] logit = output2[1].toTensor().getDataAsFloatArray();
                            for (int i = 0; i < logit.length; i++) {
                                Log.d(TAG, "logit: "+ logit[i]);
                            }

                            // Fetch the index of the value with maximum score
                            int ms_ix = 0;
                            for (int i = 0; i < pred.length; i++) {
                                ms_ix = pred[i] > pred[ms_ix] ? i : ms_ix;
                            }
                            Log.d(TAG, "mx_ix: " + ms_ix);
                            //Fetching the name from the list based on the index
                            String detected_class = ModelClasses.SoundClasses[ms_ix];

                            float[] inputs2 =  audio.ReadingAudioFile(filename, 16000, 1);
                            short int16[] =  audio.float32ToInt16(inputs2); // suppose, the new wav file's each sample will be in int16 Format
                            audio.WriteCleanAudioWav(this,filename.substring(0, filename.length()-4) + "_" +  detected_class + ".wav", int16);

                            //Writing the detected class in to the text view of the layout
//                            Toast.makeText(MainPage.this, detected_class + " is deteced.", Toast.LENGTH_SHORT).show();

                            Log.d(TAG, detected_class + " is deteced with probobility: " + pred[ms_ix]);

                            if (pred[ms_ix] > class_probability) {
//                            Intent intent = new Intent(MainPage.this, VoiceCommandLibrary.class);
//                            startActivity(intent);
                                isRecord = false;
                                // https://stackoverflow.com/questions/49738997/calling-a-new-activity-from-within-runnable
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Intent intent = new Intent(MainPage.this, Timer.class);
                                        intent.putExtra("commandName", detected_class);
                                        intent.putExtra("duration_in_seconds", 10);
                                        startActivity(intent);
                                    }
                                });
                            }
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
        header[32] = (byte) (1 * 16 / 8); // block align
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
    public static String fetchModelFile(Context context, String modelName) throws IOException {
        File file = new File(context.getFilesDir(), modelName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(modelName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    // Given the name of the pytorch model, get the path for that model
    public String assetFilePath(String assetName) throws IOException {
        File file = new File(this.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = this.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    // Generate a tensor of random numbers given the size of that tensor.
    public Tensor generateTensor(long[] Size) {
        // Create a random array of floats
        Random rand = new Random();
        float[] arr = new float[(int)(Size[0]*Size[1]*Size[2])];
        for (int i = 0; i < Size[0]*Size[1]*Size[2]; i++) {
            arr[i] = -10000 + rand.nextFloat() * (20000);
        }

        // Create the tensor and return it
        return Tensor.fromBlob(arr, Size);
    }

    // https://medium.com/mlearning-ai/integrating-custom-pytorch-models-into-an-android-app-a2cdfce14fe8
    public Tensor generateTensor3(long[] Size) {
        // Create a random array of floats
        Random rand = new Random();
        float[] arr = new float[(int)(Size[0]*Size[1]*Size[2]*Size[3])];
        for (int i = 0; i < Size[0]*Size[1]*Size[2]*Size[3]; i++) {
            arr[i] = -10000 + rand.nextFloat() * (20000);
        }

        // Create the tensor and return it
        return Tensor.fromBlob(arr, Size);
    }

}

