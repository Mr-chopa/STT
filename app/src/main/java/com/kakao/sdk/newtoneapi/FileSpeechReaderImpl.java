package com.kakao.sdk.newtoneapi;

import android.os.Environment;

import com.dialoid.speech.recognition.SpeechReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by chopa on 2018. 1. 4..
 */

public class FileSpeechReaderImpl implements SpeechReader {
    private static final String TAG = "FileSpeechReader";
    private FileInputStream mFileInputStream = null;
    private FileOutputStream fos = null;
    private File mFile;
    private long round = 0L;
    private long curSize = 0L;
    private long totalSize = 0L;

    /** 한번에 서버로 전송할 수 있는 최대 전문 사이즈(횟수) */
    private final int cycleLimit = 200;

    /** 현재 읽어들인 전문 사이즈(횟수) */
    private int curCycle = 0;

    /** 파일 전체 전송 완료 여부 */
    private boolean finish = false;

    public FileSpeechReaderImpl() {
    }

    public void FileSpeechReader(File file) {
        this.mFile = file;
    }

    public void setFile(File file) {
        this.mFile = file;
    }

    public void setCurCycle(int curCycle) {
        this.curCycle = curCycle;
    }

    public long getCurCycle() {
        return curCycle;
    }

    public boolean isFinish() {
        return finish;
    }

    public int getCycleLimit() {
        return cycleLimit;
    }

    public long getCurSize() {
        return curSize;
    }

    public void setCurSize(long curSize) {
        this.curSize = curSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getRound() {
        return totalSize;
    }

    public void setRound(long round) {
        this.totalSize = totalSize;
    }

    /**
     * 전문 사이즈(횟수) 제한으로 인해 추가 전송을 위하여 파일 스트림을 취득 하였다가 추가 전송 시 reader에 set 해준 후 파일 전송 시작
     * @return 전송 중인 파일 스트림
     */
    public FileInputStream getMFileInputStream() {
        return mFileInputStream;
    }

    /**
     * 파일 전송이 완료 되지 않은 상태에서 추가 전송을 위해 기존 파일 스트림을 set 해준 후 파일 전송 시작 필요
     * @param fileInputStream
     */
    public void setMFileInputStream(FileInputStream fileInputStream) {
        mFileInputStream = fileInputStream;
    }

    public boolean doInitialize(int samplingRate) {
        try {
            if(mFileInputStream == null) {
                totalSize = mFile.length();
                this.mFileInputStream = new FileInputStream(this.mFile);
//                fos = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/speech/reader_file.txt");
            }

            return true;
        } catch (FileNotFoundException var3) {
            return false;
        }
    }

    public int doRead(short[] speeches, int size) {
        // 전송 제한 횟수 도달 시 서버 응답 후 재전송 위해 읽기 중지(pause)
//        if(curCycle >= cycleLimit) {
//            try {
//                fos.write("사이클 완료\n\n\n".getBytes());
//            } catch(IOException e) {}
//
//            curCycle = 0;
//            return 0;
//        }

        Calendar cal = Calendar.getInstance();

        int byteSize = speeches.length * 2;
        byte[] buffer = null;
        int numRead;

        if(round == 0) {
            buffer = new byte[652];

            try {
                numRead = this.mFileInputStream.read(buffer);

                curSize += numRead;
            } catch(IOException ex) {
                return -1;
            }
        }

//        if(round > 0 && curCycle < 1) {
//            int cnt = speeches.length;
//
//            for(int i=0; i<cnt; i++) {
//                speeches[i] = 0;
//            }
//
//            curCycle++;
//            round++;
//
//            try {
//                Thread.sleep(20);
//            } catch(Exception e) {}
//
//            return cnt;
//        }

        buffer = new byte[byteSize];

        try {
            numRead = this.mFileInputStream.read(buffer);

            curSize += numRead;
        } catch(IOException ex) {
            return -1;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.asShortBuffer().get(speeches);

        // 읽어 들인 바이트가 버퍼 사이즈보다 적은 경우 파일의 최종 단이라 판단하여 파일 전송 완료 처리
        if(numRead < buffer.length) {
            finish = true;
        }

        curCycle++;
        round++;

//        try {
//            fos.write(("[" + cal.get(Calendar.SECOND) + "." + cal.get(Calendar.MILLISECOND) + "] readBytes : " + numRead + ", speeches.length : " + speeches.length + "\n").getBytes());
//        } catch(IOException e) {}


        try {
            Thread.sleep(20);
        } catch(Exception e) {}

        return numRead;
    }

    public boolean doFinalize() {
        return true;
    }

    public boolean close() {
        try {
            this.mFileInputStream.close();
//            fos.close();

            return true;
        } catch (IOException var2) {
            return false;
        }
    }
}
