package com.kakao.sdk.newtone.custom;

import android.annotation.SuppressLint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import static com.kakao.sdk.newtone.custom.Constants.LOG_DIRECTORY;
import static com.kakao.sdk.newtone.custom.Constants.LOG_DATE_FORMAT;
import static com.kakao.sdk.newtone.custom.Constants.FILE_DATE_FORMAT;

/**
 * Created by chopa on 2018. 1. 9..
 * 파일 출력 UTIL 클래스
 */

public class Printer {
    private static final File logFile;

    private static FileOutputStream logFos;

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat logTimeFormat = new SimpleDateFormat(LOG_DATE_FORMAT);

    static {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat(FILE_DATE_FORMAT);
        String curPrefix = sdf.format(System.currentTimeMillis());

        logFile = new File(LOG_DIRECTORY + curPrefix + Constants.LOG_FILE);

        new File(LOG_DIRECTORY).mkdirs();
    }

    /**
     * Printer 사용 완료 시 자원 해제를 위해 호출
     */
    public static void close() {
        try {
            if (logFos != null) {
                logFos.flush();
                logFos.close();
            }
        } catch(IOException e){}
    }

    /**
     * 에러 로그 출력
     * @param th throwable 객체
     */
    public static void error(Throwable th) {
        error(Thread.currentThread(), th);
    }

    /**
     * 에러 로그 출력
     * @param thread 쓰레드
     * @param th throwable 객체
     */
    public static void error(Thread thread, Throwable th) {
        if(logFos == null) {
            try {
                logFos = new FileOutputStream(logFile);
            } catch(FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("[ERROR] [").append(thread.getName()).append("] ")
                .append(logTimeFormat.format(System.currentTimeMillis())).append("\n");

        getStackTrace(sb, th).append("\n\n");

        try {
            logFos.write(sb.toString().getBytes());
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static StringBuilder getStackTrace(final StringBuilder sb, Throwable th) {
        StackTraceElement[] els = th.getStackTrace();

        sb.append(th.getClass().getName()).append(": ").append(th.getMessage()).append("\n");

        for(StackTraceElement el : els) {
            sb.append("\tat ")
                    .append(el.getClassName()).append(".")
                    .append(el.getMethodName()).append("(")
                    .append(el.getFileName()).append(":")
                    .append(el.getLineNumber()).append(")\n");
        }

        if(th.getCause() != null) {
            sb.append("\tcaused by: ");

            getStackTrace(sb, th.getCause());
        }

        return sb;
    }

    /**
     * 디버그 로그 출력
     * @param msg 디버깅 메시지
     */
    public static void debug(String msg) {
        if(Settings.isDebugEnabled()) {
            if(logFos == null) {
                try {
                    logFos = new FileOutputStream(logFile);
                } catch(FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            StringBuilder sb = new StringBuilder();

            StackTraceElement el = Thread.currentThread().getStackTrace()[3];

            sb.append("[DEBUG] ")
                    .append(logTimeFormat.format(System.currentTimeMillis()))
                    .append(" at ")
                    .append(el.getClassName()).append(".")
                    .append(el.getMethodName()).append("(")
                    .append(el.getFileName()).append(":")
                    .append(el.getLineNumber()).append(")\n")
                    .append(msg).append("\n\n");

            try {
                logFos.write(sb.toString().getBytes());
            } catch(IOException e) {
                error(e);
            }
        }
    }
}
