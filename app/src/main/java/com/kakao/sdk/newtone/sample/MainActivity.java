package com.kakao.sdk.newtone.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dialoid.speech.recognition.SpeechReader;
import com.kakao.sdk.newtone.custom.ActivityListener;
import com.kakao.sdk.newtone.custom.AppKeyListener;
import com.kakao.sdk.newtone.custom.Constants;
import com.kakao.sdk.newtone.custom.CorrectionValueListener;
import com.kakao.sdk.newtone.custom.Printer;
import com.kakao.sdk.newtone.custom.Settings;
import com.kakao.sdk.newtone.custom.UncaughtExceptionHandlerApplication;
import com.kakao.sdk.newtoneapi.AndroidReaderImpl;
import com.kakao.sdk.newtoneapi.FileSpeechReaderImpl;
import com.kakao.sdk.newtoneapi.SpeechRecognizeListener;
import com.kakao.sdk.newtoneapi.SpeechRecognizerClientImpl;
import com.kakao.sdk.newtoneapi.SpeechRecognizerManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SpeechRecognizeListener {
    private SpeechRecognizerClientImpl client;
    private SpeechReader sReader = null;

    private File sourceFile = null;
    private File targetFile = null;
    private FileOutputStream targetFos = null;
    private StringBuilder resultSb = null;

    public static final int RETRY_CNT = 3;
    private int errCnt = 0;
    private int completeCnt = 0;

    private boolean isRecording = false;

    private long startTime = 0L;
    private long curTime = 0L;

    private boolean beforeError = false;

    public SpeechRecognizerClientImpl getClient() {
        return client;
    }

    public SpeechReader getsReader() {
        return sReader;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setTargetFile(File file) {
        targetFile = file;
    }

    public void setTargetFos(FileOutputStream fos) {
        targetFos = fos;
    }

    public void setResultSb(StringBuilder sb) {
        resultSb = sb;
    }

    public String getResult() {
        return resultSb.toString();
    }

    public void setSReader(SpeechReader sReader) {
        this.sReader = sReader;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 쓰레드 exception 핸들러 설정(쓰레드 오류 로깅)
        UncaughtExceptionHandlerApplication threadExceptionHandler = new UncaughtExceptionHandlerApplication();
        threadExceptionHandler.setHandler(Thread.getDefaultUncaughtExceptionHandler());
        Thread.setDefaultUncaughtExceptionHandler(threadExceptionHandler);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.custom_main);

        try {
            // library 초기화
            SpeechRecognizerManager.getInstance().initializeLibrary(this);

            // 컴포넌트 초기화
            findViewById(R.id.copyClipboard).setEnabled(false);
            initViewComponent();

            // 보정값 로딩
            ((EditText) findViewById(R.id.correctionValue)).setText(Settings.get(Settings.Attribute.correctionValue));

            // appKey 로딩
            ((EditText) findViewById(R.id.appKey)).setText(Settings.get(Settings.Attribute.appKey));

            // logMode(debug) 로딩
            int checkId = 0;

            if ("true".equals(Settings.get(Settings.Attribute.debug))) {
                checkId = ((RadioButton) findViewById(R.id.logOn)).getId();
            } else {
                checkId = ((RadioButton) findViewById(R.id.logOff)).getId();
            }

            ((RadioGroup) findViewById(R.id.logMode)).check(checkId);

            // listener 설정
            ActivityListener activityListener = new ActivityListener(this);
            findViewById(R.id.startButton).setOnClickListener(activityListener);
            findViewById(R.id.copyClipboard).setOnClickListener(activityListener);
            ((EditText)findViewById(R.id.correctionValue)).addTextChangedListener(new CorrectionValueListener());
            ((EditText)findViewById(R.id.appKey)).addTextChangedListener(new AppKeyListener());
            ((RadioGroup) findViewById(R.id.logMode)).setOnCheckedChangeListener(activityListener);

            // Intent 수신
            Intent intent = getIntent();

            Bundle bundle = intent.getExtras();

            // 음성 파일 Intent 수신
            if(bundle != null && bundle.keySet() != null && bundle.keySet().contains(Intent.EXTRA_STREAM)) {
                Uri uri = (Uri) bundle.getParcelable(Intent.EXTRA_STREAM);
                String path = null;

                try {
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    cursor.moveToNext();
                    path = cursor.getString(cursor.getColumnIndex("_data"));
                    cursor.close();
                } catch(Exception e) {
                    Printer.error(e);

                    Toast.makeText(this,Constants.MSG_FILE_ACCESS_ERROR, Toast.LENGTH_LONG).show();

                    Thread.sleep(3000);

                    finish();
                }

                sourceFile = new File(path);
            }

            if(sourceFile != null) {
                ((TextView)findViewById(R.id.mode)).setText(Constants.MODE_FILE);
            }
            else {
                ((TextView)findViewById(R.id.mode)).setText(Constants.MODE_LIVE);
            }
        } catch(Throwable th) {
            Printer.error(th);
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // library 종료
        SpeechRecognizerManager.getInstance().finalizeLibrary();

        // Printer 종료
        Printer.close();

        try {
            if(targetFos != null) targetFos.close();
        } catch(IOException e) {}
    }

    /**
     * 음성인식을 위한 초기화가 완료되었을 때 호출된다.
     */
    @Override
    public void onReady() {}

    /**
     * 음성입력이 시작된 것으로 판단될 때 호출된다.
     */
    @Override
    public void onBeginningOfSpeech() {}

    /**
     * 음성입력이 끝난 것으로 판단될 때 호출된다.
     */
    @Override
    public void onEndOfSpeech() {}

    /**
     * 녹음 또는 인식 과정에서 오류가 있는 경우 호출된다.
     *
     * @param errorCode 에러 코드.
     * @param errorMsg 에러 메시지.
     */
    @Override
    public void onError(int errorCode, String errorMsg) {
        StringBuilder sb = new StringBuilder();
        sb.append("errorCode:[").append(errorCode).append("], errorMsg:[").append(errorMsg).append("]");

        Printer.error(new RuntimeException(sb.toString()));

        try {

            final MainActivity activity = this;

            Printer.debug("isRecording : [" + isRecording + "], errCnt : [" + errCnt + "], retryCnt : [" + RETRY_CNT + "]");

            if (isRecording && errCnt < RETRY_CNT) {
                Printer.debug("재시도");

                errCnt++;

                if (sourceFile != null) {
                    FileSpeechReaderImpl fReader = (FileSpeechReaderImpl) sReader;
                    fReader.setCurCycle(0);
                    fReader.setRound(fReader.getRound() + 1);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //                        Thread.sleep(200);

                            beforeError = true;

                            activity.startRecording();
                        } catch (Exception e) {
                            Printer.error(e);

                            throw new RuntimeException(e);
                        }
                    }
                });
            } else {
                Printer.debug("재시도 중지 - 실패 종료");

                isRecording = false;

                try {
                    if (targetFos != null) targetFos.close();
                } catch (IOException e) {
                }

                final String toastMsg = sb.append("\n재시도 횟수 : ").append(errCnt + 1).toString();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initViewComponent();

                        Toast.makeText(activity, toastMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }

            client = null;
        } catch(Exception e) {
            Printer.error(e);

            throw new RuntimeException(e);
        }
    }

    /**
     * 인식이 진행되는 중에 중간 결과 text가 있는 경우 전달된다.
     *
     * @param partialResult 인식 중간 결과 text
     */
    @Override
    public void onPartialResult(String partialResult) {}

    /**
     * 인식이 완료된 경우에 호출된다.
     *
     * @param results 인식 최종 결과.
     */
    @Override
    public void onResults(android.os.Bundle results) {
        completeCnt++;

        Printer.debug("translation cycle complete (cycle complete count : " + completeCnt + ")");

        try {
            ArrayList<String> texts = results.getStringArrayList(SpeechRecognizerClientImpl.KEY_RECOGNITION_RESULTS);

            if(texts.size() > 0) {
                errCnt = 0;

                String result = texts.get(0)+" / ";

                if(result != null && result.length() > 0) {
                    targetFos.write(result.getBytes());
                    targetFos.flush();

                    resultSb.append(result);
                }
            }

            // 파일 모드 시 설정
            if(sourceFile != null) {
                FileSpeechReaderImpl fReader = (FileSpeechReaderImpl)sReader;

                // 파일 리더에서 더이상 읽을 데이터가 없을 경우 레코딩 종료
                if(fReader.isFinish()) {
                    isRecording = false;
                    fReader.close();
                }
                else {
                    fReader.setCurCycle(0);
                    fReader.setRound(fReader.getRound()+1);
                }
            }
        } catch(Exception e) {
            Printer.error(e);

            throw new RuntimeException(e);
        }
    }

    /**
     * 녹음한 음성에서 추출한 음성의 에너지 값을 전달할 때 호출된다.
     *
     * @param audioLevel Normalized average dB level (0.0f ~ 1.0f)
     */
    @Override
    public void onAudioLevel(float audioLevel) {}

    /**
     * 인식이 완료된 후 audio 하드웨어와 관련된 모든 리소스가 해제된 후에 호출된다.
     */
    @Override
    public void onFinished() {
        if(beforeError) {
            Printer.debug("에러 이벤트 발생으로 onFinished() skip");

            beforeError = false;
            return;
        }

        StringBuilder sb = new StringBuilder();

        // 중지 버튼이 눌리지 않은 경우 or 파일 읽기 미완료 시 반복 진행
        if(isRecording) {
            Printer.debug("레코딩 계속 실행");

            final MainActivity activity = this;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
//                        Thread.sleep(200);
                        updateProgress(false);

                        activity.startRecording();
                    } catch(Exception e) {
                        Printer.error(e);

                        throw new RuntimeException(e);
                    }
                }
            });
        }
        // 변환 완료 처리
        else {
            Printer.debug("레코딩 완료");

            sb = new StringBuilder();

            if(sourceFile != null) {
                sb.append("파일 변환 완료 - ").append(targetFile.getAbsolutePath());
            }
            else {
                sb.append("실시간 음성 변환 완료 - ").append(targetFile.getAbsolutePath());
            }

            int millsOfHour = 3600000;
            int millsOfMinute = 60000;
            int millsOfSecond = 1000;

            long totalTime = System.currentTimeMillis() - startTime;

            int hours = (int)(totalTime / millsOfHour);
            int minutes = (int)(totalTime % millsOfHour / millsOfMinute);
            int seconds = (int)(totalTime % millsOfMinute / millsOfSecond);

            sb.append("\n\n(처리 시간 - ").append(hours).append("시간 ").append(minutes).append("분 ").append(seconds).append("초)");

            final String resultMsg = sb.toString();
            final Activity activity = this;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // finishing일때는 처리하지 않는다.
                    if (activity.isFinishing()) return;

                    updateProgress(true);

                    AlertDialog.Builder dialog = new AlertDialog.Builder(activity).
                            setMessage(resultMsg).
                            setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    findViewById(R.id.copyClipboard).setEnabled(true);
                                    initViewComponent();
                                    dialog.dismiss();
                                }
                            });
                    dialog.show();
                }
            });
        }

        client = null;
    }

    /**
     * 진척 사항 업데이트
     * @param complete 완료 여부
     */
    private void updateProgress(boolean complete) {
        if(sourceFile != null) {
            updateFileProgress(complete);
        }
    }

    /**
     * 파일 모드 progress 처리
     * @param complete 완료 여부
     */
    private void updateFileProgress(boolean complete) {
        TextView progressLabel = (TextView)findViewById(R.id.progressLabel);
        ProgressBar progress = (ProgressBar)findViewById(R.id.fileProgress);

        StringBuilder sb = new StringBuilder();

        int cur;
        long usageTime = System.currentTimeMillis() - startTime;

        if(complete) {
            cur = progress.getMax();
        }
        else {
            FileSpeechReaderImpl fReader = (FileSpeechReaderImpl)sReader;
            cur = (int)(fReader.getCurSize() * progress.getMax() / fReader.getTotalSize());

            int percent = cur * 100 / progress.getMax();

            if(percent == 0) {
                sb.append("진행 상태 (남은 시간 계산중...)");
            }
            else {
                int millsOfHour = 3600000;
                int millsOfMinute = 60000;
                int millsOfSecond = 1000;

                long remaining = (100-percent) * usageTime / percent;

                int hours = (int)(remaining / millsOfHour);
                int minutes = (int)(remaining % millsOfHour / millsOfMinute);
                int seconds = (int)(remaining % millsOfMinute / millsOfSecond);

                sb.append("진행 상태 (남은 시간 - ").append(hours).append("시간 ").append(minutes).append("분 ").append(seconds).append("초)");
            }
        }

        progress.setProgress(cur);
        progressLabel.setText(sb.toString());
    }

    /**
     * view component 초기화
     */
    private void initViewComponent() {
        errCnt = 0;

        findViewById(R.id.startButton).setEnabled(true);

        findViewById(R.id.fileLayout).setVisibility(View.INVISIBLE);
        findViewById(R.id.liveLayout).setVisibility(View.INVISIBLE);

        ((ProgressBar)findViewById(R.id.fileProgress)).setProgress(0);

        ((Button)findViewById(R.id.startButton)).setText("변환 시작");
        ((TextView)findViewById(R.id.progressLabel)).setText("진행 상태");
    }

    /**
     * 레코딩 시작
     */
    public void startRecording() {
        Printer.debug("client startRecording 호출");

        SpeechRecognizerClientImpl.Builder builder = new SpeechRecognizerClientImpl.Builder().
                setServiceType(Constants.SERVICE_TYPE);

        client = builder.build();

        client.setSpeechRecognizeListener(this);
        client.startRecording(true, sReader);
    }
}
