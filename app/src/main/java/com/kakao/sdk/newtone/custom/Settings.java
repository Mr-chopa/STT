package com.kakao.sdk.newtone.custom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Properties;

/**
 * Created by chopa on 2018. 1. 9..
 * 설정 클래스
 */

public class Settings {
    private static Properties prop;

    private static boolean debugEnabled;

    static {
        prop = new Properties();

        FileInputStream fis = null;

        try {
            File file = new File(Constants.CONFIG_DIRECTORY + Constants.PROP_FILE);

            if(file.exists()) {
                fis = new FileInputStream(file);
                prop.load(fis);

                debugEnabled = Boolean.valueOf(prop.getProperty(Attribute.debug.name()));
            }
        } catch(IOException e) {
            Printer.error(e);
        } finally {
            try { if(fis != null) fis.close(); } catch(IOException e){}
        }
    }

    /**
     * 설정 항목
     */
    public enum Attribute {
        debug("디버그 출력 여부", Boolean.toString(Constants.DEFAULT_PRINT_DEBUG)),
        correctionValue("보정값", Integer.toString(Constants.DEFAULT_CORRECTION_VALUE)),
        appKey("AppKey", Constants.DEFAULT_APP_KEY);

        private final String description;
        private final String defaultValue;

        Attribute(String description, String defaultValue) {
            this.description = description;
            this.defaultValue = defaultValue;
        }

        /**
         * 항목별 description 반환
         * @return attribute description
         */
        public String getDescription() {
            return description;
        }

        /**
         * 항목별 기본 설정값 반환
         * @return 기본 설정값
         */
        public String getDefaultValue() {
            return defaultValue;
        }
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * 설정 조회
     * @param attribute 설정 항목
     * @return 설정 값
     */
    public static String get(Attribute attribute) {
        if(!prop.containsKey(attribute.name())) {
            prop.setProperty(attribute.name(), attribute.getDefaultValue());
        }

        return prop.getProperty(attribute.name());
    }

    /**
     * 설정 update
     * @param attribute 설정 항목
     * @param value 설정 값
     */
    public static void set(Attribute attribute, String value) {
        if(attribute.equals(Attribute.debug)) {
            debugEnabled = Boolean.valueOf(value);
        }

        prop.setProperty(attribute.name(), value);
    }

    /** 설정파일 저장 */
    public static void save() {
        FileOutputStream fos = null;

        try {
            File file = new File(Constants.CONFIG_DIRECTORY + Constants.PROP_FILE);
            new File(Constants.CONFIG_DIRECTORY).mkdirs();

            fos = new FileOutputStream(file);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH24:mm:ss");

            StringBuilder sb = new StringBuilder(sdf.format(System.currentTimeMillis()))
                    .append(" updated");

            prop.store(fos, sb.toString());
        } catch(IOException e) {
            Printer.error(e);
        } finally {
            try { if(fos != null) fos.close(); } catch(IOException e) {}
        }
    }
}
