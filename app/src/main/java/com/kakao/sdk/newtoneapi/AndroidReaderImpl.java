package com.kakao.sdk.newtoneapi;

import android.media.AudioRecord;
import android.os.Environment;

import com.dialoid.speech.recognition.SpeechReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by chopa on 2018. 1. 4..
 */

public class AndroidReaderImpl implements SpeechReader {
    private static final String TAG = "AndroidSpeechReader";
    private static final int SAMPLING_RATE_8K = 8000;
    private static final int SAMPLING_RATE = 16000;
    private static final int SAMPLING_MAX_SECS = 5;
    private static final int SAMPLING_BUFFER_SIZE_IN_BYTES = 160000;
    private int mSamplingRate = 16000;
    private int mSamplingBufferSize = 160000;
    private int mAudioFormat = 2;
    private AudioRecord mAudioRecord;

    private FileOutputStream fos = null;
    private File mFile;
    private long round = 0L;

    public AndroidReaderImpl() {
    }

    public boolean doInitialize(int samplingRate) {
        if(this.mSamplingRate == 8000) {
            this.mSamplingBufferSize = 80000;
            this.mAudioFormat = 3;
        }

        this.mAudioRecord = new AudioRecord(6, this.mSamplingRate, 16, this.mAudioFormat, this.mSamplingBufferSize);

//        try {
//            fos = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/speech/reader_file_android.txt");
//        } catch(IOException e) {}

        if(this.mAudioRecord.getState() == 0) {
            return false;
        } else {
            this.mAudioRecord.startRecording();
            return true;
        }
    }

    public int doRead(short[] speeches, int size) {
        Calendar cal = Calendar.getInstance();

        int numRead;

        numRead = this.mAudioRecord != null ? this.mAudioRecord.read(speeches, 0, size) : 0;

//        if(round == 0) {
//            StringBuilder sb = new StringBuilder();
//
//            try {
//                for(int i=0; i<speeches.length; i++) {
//                    sb.append(Short.toString(speeches[i]));
//                }
//
//                sb.append("\n\n");
//
//                fos.write(sb.toString().getBytes());
//            } catch(IOException e) {}
//        }
//
//        round++;
//
//        try {
//            fos.write(("["+round+"][" + cal.get(Calendar.SECOND) + "." + cal.get(Calendar.MILLISECOND) + "] readBytes : " + numRead + ", speeches.length : " + speeches.length + "\n").getBytes());
//        } catch(IOException e) {}

        return numRead;
    }

    public boolean doFinalize() {
        if(this.mAudioRecord != null) {
            if(this.mAudioRecord.getRecordingState() == 3) {
                this.mAudioRecord.stop();
            }

            if(this.mAudioRecord.getState() == 1) {
                this.mAudioRecord.release();
            }

//            try {
//                fos.flush();
//                fos.close();
//            } catch(IOException e){}
        }

        return true;
    }
}
