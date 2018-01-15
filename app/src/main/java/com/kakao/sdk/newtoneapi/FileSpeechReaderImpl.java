package com.kakao.sdk.newtoneapi;

import android.os.Environment;

import com.dialoid.speech.recognition.SpeechReader;
import com.kakao.sdk.newtone.custom.Printer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

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

    /** 현재 읽어들인 전문 사이즈(횟수) */
    private int curCycle = 0;

    /** 파일 전체 전송 완료 여부 */
    private boolean finish = false;

    /** 이전에 전송한 버퍼 */
    private short[] preSpeeches = null;

    /** 이전에 전송한 길이 */
    private int preNum = 0;

    /** 전송 재개 시 재전송할 이전 데이터 개수(10ms = 0.01초 단위) */
    private int reSendCount = 20;

    private LinkedList<short[]> preList = new LinkedList<short[]>();

    public FileSpeechReaderImpl() {
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
        return round;
    }

    public void setRound(long round) {
        this.round = round;
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
            }

            return true;
        } catch (FileNotFoundException var3) {
            return false;
        }
    }

    public int doRead(short[] speeches, int size) {
        int byteSize = speeches.length * 2;
        byte[] buffer = null;
        int numRead;

        if(round == 0 && curCycle == 0) {
            // 음성 데이터 시작 지점
            int index = 0;
            int findSize = 100;
            byte[] findBuffer = new byte[findSize];
            buffer = new byte[byteSize];

            try {
                numRead = this.mFileInputStream.read(findBuffer);

                curSize += numRead;

                StringBuilder sb = new StringBuilder();

                for(int i=0; i<findBuffer.length; i++) {
                    sb.append(Integer.toHexString(findBuffer[i])).append(" ");
                }

                for(int i=36; i<findSize-4; i++) {
                    // 'DATA' 문자열 탐색
                    if(findBuffer[i] == 0x64
                            && findBuffer[i+1] == 0x61
                            && findBuffer[i+2] == 0x74
                            && findBuffer[i+3] == 0x61) {
                        // 음성 데이터 시작 지점
                        index = i+8;

                        Printer.debug("'data' 문자열 index : [" + i + "], 음성 데이터 시작 index : [" + index + "]");

                        int cnt = 0;

                        // 탐색 후 남은 데이터 buffer 에 삽입
                        for(int j=index; j<findSize; j++) {
                            buffer[cnt++] = findBuffer[j];
                        }

                        byte[] tmpBuffer = new byte[byteSize - cnt];

                        numRead = this.mFileInputStream.read(tmpBuffer);

                        // byteSize 만큼 나머지 데이터 buffer 에 채우기
                        for(int j=0; j<tmpBuffer.length; j++) {
                            buffer[cnt++] = tmpBuffer[j];
                        }

                        curSize += numRead;

                        numRead = cnt;
                    }
                }

                if(index < 36) {
                    throw new RuntimeException("wav 파일 parsing 오류 : \n" + sb.toString());
                }
            } catch(IOException ex) {
                return -1;
            }
        }
        else {
            // 2라운드 이상에서 첫번째 호출일 경우 이전 라운드의 마지막 데이터를 한번 더 전송
            if (round > 0 && curCycle < reSendCount && preNum > 0) {
                Printer.debug("이전 round 마지막 데이터 한번 더 전송 - " + curCycle);

                preSpeeches = preList.pop();

                int cnt = preSpeeches.length;

                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < cnt; i++) {
                    speeches[i] = preSpeeches[i];
                    sb.append(speeches[i]);
                }

                curCycle++;

                return preNum;
            }

            buffer = new byte[byteSize];

            try {
                numRead = this.mFileInputStream.read(buffer);

                curSize += numRead;
            } catch (IOException ex) {
                return -1;
            }
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.asShortBuffer().get(speeches);

        preSpeeches = new short[speeches.length];
        System.arraycopy(speeches, 0, preSpeeches, 0, speeches.length);

        preList.push(preSpeeches);

        if(preList.size() > reSendCount) {
            preList.pop();
        }

        preNum = numRead;

        // 읽어 들인 바이트가 버퍼 사이즈보다 적은 경우 파일의 최종 단이라 판단하여 파일 전송 완료 처리
        if(numRead < buffer.length) {
            finish = true;
        }

        curCycle++;

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

            return true;
        } catch (IOException var2) {
            return false;
        }
    }
}
