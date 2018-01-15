package com.kakao.sdk.newtone.custom;

/**
 * Created by chopa on 2018. 1. 8..
 * Thread exception 핸들러
 */

public class UncaughtExceptionHandlerApplication implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler handler;

    /**
     * 원본 핸들러 설정
     * @param handler 원본 핸들러
     */
    public void setHandler(Thread.UncaughtExceptionHandler handler) {
        this.handler = handler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable th) {
        Printer.error(thread, th);

        handler.uncaughtException(thread, th);
    }
}
