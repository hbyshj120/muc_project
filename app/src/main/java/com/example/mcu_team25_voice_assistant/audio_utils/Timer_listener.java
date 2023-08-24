package com.example.mcu_team25_voice_assistant.audio_utils;
import static android.Manifest.permission.RECORD_AUDIO;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.mcu_team25_voice_assistant.CommandClasses;
import com.example.mcu_team25_voice_assistant.R;
import com.example.mcu_team25_voice_assistant.Timer;
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
import java.util.Objects;

// https://medium.com/mlearning-ai/integrating-custom-pytorch-models-into-an-android-app-a2cdfce14fe8
public class Timer_listener extends Activity {
    private static final String TAG = "listener: ";
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
    private boolean isRecord = false;// 设置正在录制的状态
    private Thread recordingThread;
    Module module;
    final private static int limit = 4;
    private Context mContext;
    private Button mButtonStartPause;
    private Button mButtonStop;
    AudioFileProcess audio = new AudioFileProcess();

    CommandClasses commandClass = new CommandClasses();
    public boolean CheckPermission(){
        int result = ContextCompat.checkSelfPermission(mContext.getApplicationContext(), RECORD_AUDIO);
        return  result == PackageManager.PERMISSION_GRANTED;
    }

    public void RequestPermission(){
        Log.d(TAG, "Request permissions.");
        ActivityCompat.requestPermissions(Timer_listener.this, new String[] {RECORD_AUDIO}, RequestPermisssionCode);
    }

    public Timer_listener(Context context, Button pause_start, Button button_stop){
        mContext = context;
        mButtonStartPause =  pause_start;
        mButtonStop = button_stop;
    }

    public void LoadModel() {
        try {
            String modelpath = assetFilePath("dataset_cnn_soundclassifier_quantized_lite.ptl");
            module = LiteModuleLoader.load(modelpath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void AudioRecorderReady() {
//        recordBufsize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        recordBufsize = 2048*2;

        Log.d(TAG, "AudioRecorderReady: size --> " + recordBufsize);
        try {
            if (ActivityCompat.checkSelfPermission(mContext, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
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

    public void startRecord(String filename) {
        if (isRecord) {
            return;
        }
        Log.i("audioRecordTest", "start 录音");
        audioRecord.startRecording();
        isRecord = true;
        recordingThread = new Thread(new Timer_listener.AudioRecordThread(mContext.getFilesDir().getAbsolutePath() + '/' + filename));
        recordingThread.start();
    }

    public void stopRecord() {
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
        String rawFilename = mContext.getFilesDir().getAbsolutePath() + "/temp.raw";
        AudioRecordThread(String s) {wavFilename = s; }
        @Override
        public void run() {
            try {
                writeDateTOFile(rawFilename, wavFilename);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void writeDateTOFile(String rawFilename, String wavFilename) throws Exception {

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

        double vad_threshold = 0;
        double class_probability = 0.5;  //0.5;


        // active listening --> active voice occurred before and past voice occurence never > 1 second
        boolean activeListening = false;
        int activeVoiceOccurence= 0;
        int activeVoiceDuration = 2; // have to be 2 for "six"
        int passiveVoiceOccurence = 0;
        double passiveVoiceDuration = 0.2;
        int win_length = recordBufsize / 2;
        int passVoiceMaxOccurence = (int) (sampleRateInHz * passiveVoiceDuration / win_length);
        boolean isPaused = false;

        int counter = 0;
        while (isRecord == true) {
            readsize = audioRecord.read(audiodata, 0, recordBufsize);
            short[] shorts = new short[audiodata.length/2];
            // https://stackoverflow.com/questions/5625573/byte-array-to-short-array-and-back-again-in-java
            // to turn bytes to shorts as either big endian or little endian.
            ByteBuffer.wrap(audiodata).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(mContext));
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

                            Long tsLong = System.currentTimeMillis() / 1000;
                            String ts = tsLong.toString();
                            String filename = wavFilename.substring(0, wavFilename.length() - 4) + "_" + ts + ".wav";
                            //   and create a wav file with header based on temp file
                            copyWaveFile(rawFilename, filename);//给裸数据加上头文件
                            Log.d(TAG, "save wav file: " + filename);

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
                            String detected_class = commandClass.getSoundClass(ms_ix);

                            float[] inputs2 =  audio.ReadingAudioFile(filename, 16000, 1);
                            short int16[] =  audio.float32ToInt16(inputs2); // suppose, the new wav file's each sample will be in int16 Format
                            audio.WriteCleanAudioWav(this,filename.substring(0, filename.length()-4) + "_" +  detected_class + ".wav", int16);

                            Log.d(TAG, detected_class + " is deteced with probobility: " + pred[ms_ix]);

                            if (pred[ms_ix] > class_probability) {
                                if (commandClass.isPause(ms_ix) &&  !isPaused) {
                                    isPaused = true;
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            mButtonStartPause.performClick();
                                        }
                                    });
                                }

                                if (commandClass.isContinue(ms_ix) && isPaused) {
                                    isPaused = false;
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            mButtonStartPause.performClick();
                                        }
                                    });
                                }

                                if (commandClass.isStop(ms_ix)) {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            mButtonStop.performClick();
                                        }
                                    });
                                }
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

    // add wave header to the original data
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

    // Given the name of the pytorch model, get the path for that model
    public String assetFilePath(String assetName) throws IOException {
        File file = new File(mContext.getFilesDir(), assetName);
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
}

