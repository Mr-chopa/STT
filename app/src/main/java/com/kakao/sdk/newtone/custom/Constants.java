package com.kakao.sdk.newtone.custom;

import android.os.Environment;

/**
 * Created by chopa on 2018. 1. 8..
 * 상수 클래스
 */

public final class Constants {
    /** 어플리케이션 디렉토리 */
    public static final String APP_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath() + "/speech/";

    /** 설정파일 디렉토리 */
    public static final String CONFIG_DIRECTORY = APP_DIRECTORY + "setting/";

    /** 로그 디렉토리 */
    public static final String LOG_DIRECTORY = APP_DIRECTORY + "logs/";

    /** 설정 파일명 */
    public static final String PROP_FILE = "setting.properties";

    /** 로그 파일명 */
    public static final String LOG_FILE = ".log";

    /** 변환 파일 확장자 */
    public static final String EXTENSION = ".txt";

    /** 디버그 출력 여부 */
    public static final boolean DEFAULT_PRINT_DEBUG = false;

    /** 기본 보정값 */
    public static final int DEFAULT_CORRECTION_VALUE = 0;

    /** 기본 App key */
    public static final String DEFAULT_APP_KEY = "8338b6a7ca17e2e3ac9ce610aea67421";

    /** 실시간 모드 타이틀 */
    public static final String MODE_LIVE = "실시간 변환 모드";

    /** 파일 변환 모드 타이틀 */
    public static final String MODE_FILE = "파일 변환 모드";

    /** 공유 파일 접근 오류 메시지 */
    public static final String MSG_FILE_ACCESS_ERROR = "공유된 파일에 access 할 수 없습니다.\n어플을 종료합니다.";

    /** 받아쓰기에 특화된 서비스 타입 */
    public static final String SERVICE_TYPE_DICTATION = "DICTATION";

    /** 통합 검색에 특화된 서비스 타입 */
    public static final String SERVICE_TYPE_WEB = "WEB";

    /** 위치 검색에 특화된 서비스 타입 */
    public static final String SERVICE_TYPE_LOCAL = "LOCAL";

    /** 고립어 인식을 위한 서비스 타입 */
    public static final String SERVICE_TYPE_WORD = "WORD";

    /** 디폴트 서비스 타입 */
    public static final String SERVICE_TYPE = SERVICE_TYPE_DICTATION;

    /** 파일 postfix 날짜 포맷 */
    public static final String FILE_DATE_FORMAT = "yyyyMMddHHmm";

    /** 로그 출력 시간 포맷 */
    public static final String LOG_DATE_FORMAT = "HH:mm:ss.SSS";

    /** 변환 파일 prefix */
    public static final String TRANSFER_FILE_PREFIX = "";

    /** App 타이틀 */
    public static final String APP_TITLE = "SpeechToText";
}
