package com.kakao.sdk.newtone.custom;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.kakao.sdk.newtone.sample.MainActivity;
import com.kakao.sdk.newtone.sample.R;
import com.kakao.sdk.newtoneapi.AndroidReaderImpl;
import com.kakao.sdk.newtoneapi.FileSpeechReaderImpl;
import com.kakao.sdk.newtoneapi.impl.util.PermissionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

/**
 * Created by chopa on 2018. 1. 9..
 */

public class ActivityListener implements View.OnClickListener, View.OnFocusChangeListener, RadioGroup.OnCheckedChangeListener {
    private MainActivity activity;

    private SimpleDateFormat sdf;

    private FileOutputStream fos = null;

    public ActivityListener(MainActivity activity) {
        this.activity = activity;
        sdf = new SimpleDateFormat(Constants.FILE_DATE_FORMAT);
    }

    @Override
    public void onClick(View view) {
        try {
            int id = view.getId();

            // 변환 시작 버튼
            if(id == R.id.startButton) {
                processStartButton();
            }
            // 클립보드 복사 버튼
            else if(id == R.id.copyClipboard) {
                processCopyClipboard();
            }
        } catch(Exception e) {
            Printer.error(e);

            throw new RuntimeException(e);
        }
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        try {
            int id = view.getId();

            // 포커스 상실 시
            if(!hasFocus) {
                Printer.debug("app key 값 변경");

                // app key 값 변경 이벤트 처리
                if (id == R.id.appKey) {
                    Settings.set(
                            Settings.Attribute.appKey,
                            ((EditText)activity.findViewById(R.id.appKey)).getText().toString()
                    );

                    Settings.save();
                }
            }
        } catch(Exception e) {
            Printer.error(e);

            throw new RuntimeException(e);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // 로그 모드(디버그) 변경 시 이벤트 처리
        if(group.getId() == R.id.logMode) {
            Printer.debug("로그 모드 변경");

            String value = null;

            switch (checkedId) {
                case R.id.logOn:
                    value = "true";
                    break;
                case R.id.logOff:
                    value = "false";
                    break;
                default:
                    new RuntimeException("디버그 모드 변경 오류");
            }

            Settings.set(Settings.Attribute.debug, value);

            Settings.save();
        }
    }

    /**
     * 시작 버튼 이벤트 처리
     * @throws Exception
     */
    private void processStartButton() throws Exception {
        String fileName = null;
        String msg = null;
        LinearLayout layout = null;

        // 중지처리
        if(activity.isRecording()) {
            Printer.debug("중지 버튼 클릭");

            if (activity.getSourceFile() != null) {
                layout = (LinearLayout) activity.findViewById(R.id.fileLayout);
            } else {
                layout = (LinearLayout) activity.findViewById(R.id.liveLayout);
            }

            activity.findViewById(R.id.startButton).setEnabled(false);

            activity.setRecording(false);

            if(activity.getClient() != null) {
                activity.getClient().stopRecording();
            }
        }
        // 시작처리
        else {
            Printer.debug("시작 버튼 클릭");

            if (activity.getSourceFile() != null) {
                FileSpeechReaderImpl fReader = new FileSpeechReaderImpl();
                fReader.setFile(activity.getSourceFile());
                activity.setSReader(fReader);

                fileName = activity.getSourceFile().getName()
                        .substring(
                                0, activity.getSourceFile().getName().lastIndexOf(".")
                        );
                msg = "음성파일 변환 시작";
                layout = (LinearLayout) activity.findViewById(R.id.fileLayout);
            } else {
                if (PermissionUtils.checkAudioRecordPermission(activity)) {
                    AndroidReaderImpl aReader = new AndroidReaderImpl();
                    activity.setSReader(aReader);

                    fileName = "live";
                    msg = "녹음 변환 시작";
                    layout = (LinearLayout) activity.findViewById(R.id.liveLayout);
                } else {
                    throw new RuntimeException("No audio recording rights.");
                }
            }

            File file = new File(Constants.APP_DIRECTORY
                    + Constants.TRANSFER_FILE_PREFIX
                    + fileName
                    + "_"
                    + sdf.format(System.currentTimeMillis())
                    + ".txt"
            );

            if(fos != null) fos.close();
            fos = new FileOutputStream(file);

            activity.setTargetFile(file);
            activity.setTargetFos(fos);
            activity.setResultSb(new StringBuilder());

            activity.setStartTime(System.currentTimeMillis());
            activity.setRecording(true);
            activity.startRecording();

            activity.findViewById(R.id.copyClipboard).setEnabled(false);
            ((Button) activity.findViewById(R.id.startButton)).setText("변환 중지");
            layout.setVisibility(View.VISIBLE);

            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 클립보드 복사 버튼 이벤트 처리
     * @throws Exception
     */
    private void processCopyClipboard() throws Exception {
        Printer.debug("클립보드 복사 버튼 클릭");

        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(activity.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(Constants.APP_TITLE, activity.getResult());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(activity, "변환 결과가 클립보드에 복사되었습니다.", Toast.LENGTH_LONG).show();
    }
}
