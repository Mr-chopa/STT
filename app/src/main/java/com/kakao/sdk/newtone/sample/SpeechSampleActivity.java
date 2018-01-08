package com.kakao.sdk.newtone.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.dialoid.speech.recognition.SpeechReader;
import com.dialoid.speech.recognition.SpeechRecognizer;
import com.kakao.sdk.newtoneapi.AndroidReaderImpl;
import com.kakao.sdk.newtoneapi.FileSpeechReaderImpl;
import com.kakao.sdk.newtoneapi.SpeechRecognizeListener;
import com.kakao.sdk.newtoneapi.SpeechRecognizerActivity;
import com.kakao.sdk.newtoneapi.SpeechRecognizerClient;
import com.kakao.sdk.newtoneapi.SpeechRecognizerClientImpl;
import com.kakao.sdk.newtoneapi.SpeechRecognizerManager;
import com.kakao.sdk.newtoneapi.impl.util.PermissionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

/**
 * 본 sample의 main activity.
 *
 * 직접 음성인식 기능을 제어하는 API를 호출하는 버튼과 기본으로 제공되는 UI를 통해 음성인식을 수행하는
 * 두가지 형태를 제공한다.
 *
 * 음성인식 API의 callback을 받기 위해 {@link com.kakao.sdk.newtoneapi.SpeechRecognizeListener} interface를 구현하였다.
 *
 * @author Daum Communications Corp.
 * @since 2013
 */
public class SpeechSampleActivity extends Activity implements View.OnClickListener, SpeechRecognizeListener {
    private SpeechRecognizerClientImpl client;
    private File file = null;
    private SpeechReader reader = null;
    private File resultFile = null;
    private FileOutputStream resultFos = null;
    private boolean[] progress = {false, false, false, false, false, false, false, false, false};
    private String serviceType;
    private int errCnt = 0;

    private Thread.UncaughtExceptionHandler mUncaughtExceptionHandler;

    class UncaughtExceptionHandlerApplication implements Thread.UncaughtExceptionHandler{
        private SpeechSampleActivity activity;

        public void setActivity(SpeechSampleActivity activity) {
            this.activity = activity;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
//            Toast.makeText(activity, getStackTrace(ex), Toast.LENGTH_LONG).show();

            String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/speech";
            File file = new File(sdPath+"/speecherror.txt");

            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(file, true);

                fos.write(getStackTrace(ex).getBytes());
                fos.flush();

                fos.close();
            } catch(Exception e) {
                try {if (fos != null) fos.close(); }catch (Exception ex1){}
            }

            //예외상황이 발행 되는 경우 작업
            Log.e("Error", getStackTrace(ex));

//            return;
            //예외처리를 하지 않고 DefaultUncaughtException으로 넘긴다.
            mUncaughtExceptionHandler.uncaughtException(thread, ex);
        }

    }

    private void printError(String str) {
        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/speech";
        File file = new File(sdPath+"/speecherror.txt");

        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file, true);

            Date date = new Date(System.currentTimeMillis());


            fos.write("\n\n\n".getBytes());
            fos.write((date.getHours()+":"+date.getMinutes()+":"+date.getSeconds()+"\n").getBytes());
            fos.write(str.getBytes());
            fos.flush();

            fos.close();
        } catch(Exception e) {
            try {if (fos != null) fos.close(); }catch (Exception ex1){}
        }
    }

    private String getStackTrace(Throwable th) {

        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);

        Throwable cause = th;
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        final String stacktraceAsString = result.toString();
        printWriter.close();

        return stacktraceAsString;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        mUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        UncaughtExceptionHandlerApplication exceptionHandlerApplication = new UncaughtExceptionHandlerApplication();
        exceptionHandlerApplication.setActivity(this);

        Thread.setDefaultUncaughtExceptionHandler(exceptionHandlerApplication);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // library를 초기화 합니다.
        // API를 사용할 시점이 되었을 때 initializeLibrary(Context)를 호출한다.
        // 사용을 마치면 finalizeLibrary()를 호출해야 한다.
        SpeechRecognizerManager.getInstance().initializeLibrary(this);

        findViewById(R.id.speechbutton).setOnClickListener(this);
        findViewById(R.id.cancelbutton).setOnClickListener(this);
        findViewById(R.id.restartbutton).setOnClickListener(this);
        findViewById(R.id.stopbutton).setOnClickListener(this);
        findViewById(R.id.uibutton).setOnClickListener(this);
        findViewById(R.id.ttsbutton).setOnClickListener(this);

        setButtonsStatus(true);

        Intent intent = getIntent();

        try {

            Bundle bundle = intent.getExtras();

//            Uri uri1 = intent.getData();
//            Iterator<String> iter = bundle.keySet().iterator();
//            String str = "";
//
//            while(iter.hasNext()) {
//                String key = iter.next();
//
//                str += key + " : " + bundle.get(key).getClass().getName() + "\n";
//            }
//
//            new AlertDialog.Builder(this).
//                    setMessage((uri1 == null ? "getData is null" : "getData is not null") + "\n" + str).
//                    setPositiveButton("확인", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
////                            finish();
//                        }
//                    }).
//                    show();

            File file3 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/speech/speecherror.txt");
            file3.deleteOnExit();

            File file4 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/speech/SpeechClient.txt");
            file4.deleteOnExit();

            File file5 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/speech/SpeechActivity.txt");
            file5.deleteOnExit();

            if(bundle != null && bundle.keySet() != null && bundle.keySet().contains(Intent.EXTRA_STREAM)) {
                Uri uri = (Uri) bundle.getParcelable(Intent.EXTRA_STREAM);

                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToNext();
                String path = cursor.getString(cursor.getColumnIndex("_data"));
                cursor.close();

                file = new File(path);
            }

            File file2 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/speech");
            file2.mkdirs();

//            Toast.makeText(this, Environment.getExternalStorageDirectory().getAbsolutePath(), Toast.LENGTH_LONG).show();

//            new AlertDialog.Builder(this).
//                    setMessage(Environment.getExternalStorageDirectory().getAbsolutePath()).
//                    setPositiveButton("확인", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            //dialog.dismiss();
//                            finish();
//                        }
//                    }).
//                    show();

            // 해시키 확인
//            try {
//                PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
//                for (android.content.pm.Signature signature : info.signatures) {
//                    MessageDigest md;
//                    md = MessageDigest.getInstance("SHA");
//                    md.update(signature.toByteArray());
//                    String something = new String(Base64.encode(md.digest(), 0));
//
//                    printLog("hash key : [" + something + "]");
//                }
//            } catch (Exception e) {
//                // TODO Auto-generated catch block
//                Log.e("name not found", e.toString());
//            }

            if(file != null) {
                FileSpeechReaderImpl fReader = new FileSpeechReaderImpl();
                fReader.setFile(file);
                reader = fReader;

                Date date = new Date();
                String postfix = "" + date.getYear() + (date.getMonth()+1) + date.getDate() + date.getHours() + date.getMinutes() + date.getSeconds();
                resultFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/speech/"+file.getName().substring(0, file.getName().lastIndexOf("."))+"_"+postfix+".txt");
                resultFos = new FileOutputStream(resultFile);
            }
            else {
                AndroidReaderImpl aReader = new AndroidReaderImpl();
                reader = aReader;
            }
        } catch(Exception e) {
            printError(getStackTrace(e));
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // API를 더이상 사용하지 않을 때 finalizeLibrary()를 호출한다.
        SpeechRecognizerManager.getInstance().finalizeLibrary();

        try{
            if(resultFos != null) resultFos.close();
        }catch (IOException ex) {}
    }

    private void setButtonsStatus(boolean enabled) {
        findViewById(R.id.speechbutton).setEnabled(enabled);
        findViewById(R.id.uibutton).setEnabled(enabled);
        findViewById(R.id.restartbutton).setEnabled(!enabled);
        findViewById(R.id.cancelbutton).setEnabled(!enabled);
        findViewById(R.id.stopbutton).setEnabled(!enabled);
    }

    @Override
    public void onClick(View v) {
        try {
            int id = v.getId();

            String serviceType = SpeechRecognizerClient.SERVICE_TYPE_WEB;

            RadioGroup serviceRadioGroup = (RadioGroup) this.findViewById(R.id.service_group);
            switch (serviceRadioGroup.getCheckedRadioButtonId()) {
                case R.id.web:
                    serviceType = SpeechRecognizerClient.SERVICE_TYPE_WEB;
                    break;
                case R.id.dictation:
                    serviceType = SpeechRecognizerClient.SERVICE_TYPE_DICTATION;
                    break;
                case R.id.local:
                    serviceType = SpeechRecognizerClient.SERVICE_TYPE_LOCAL;
                    break;
                case R.id.word:
                    serviceType = SpeechRecognizerClient.SERVICE_TYPE_WORD;
                    break;
            }

            Log.i("SpeechSampleActivity", "serviceType : " + serviceType);


            // 음성인식 버튼 listener
            if (id == R.id.speechbutton) {
                if (PermissionUtils.checkAudioRecordPermission(this)) {
                    this.serviceType = serviceType;

                    Toast.makeText(this, ((reader instanceof FileSpeechReaderImpl) ? "음성파일 변환 모드 시작" : "녹음 모드 시작"), Toast.LENGTH_LONG).show();

                    startRecording();

//                    SpeechRecognizerClientImpl.Builder builder = new SpeechRecognizerClientImpl.Builder().
//                            setServiceType(serviceType);
//
//                    this.serviceType = serviceType;
//
//                    if (serviceType.equals(SpeechRecognizerClient.SERVICE_TYPE_WORD)) {
//                        EditText words = (EditText) findViewById(R.id.words_edit);
//                        String wordList = words.getText().toString();
//                        builder.setUserDictionary(wordList);
//
//                        Log.i("SpeechSampleActivity", "word list : " + wordList.replace('\n', ','));
//                    }
//
//                    client = builder.build();
//
//                    client.setSpeechRecognizeListener(this);
//
//                    Toast.makeText(this, ((reader instanceof FileSpeechReaderImpl) ? "음성파일 변환 모드 시작" : "녹음 모드 시작"), Toast.LENGTH_LONG).show();
//
//                    client.startRecording(true, reader);
//
//                    setButtonsStatus(false);
                }
            }

            // 음성인식 취소버튼 listener
            else if (id == R.id.cancelbutton) {
                if (client != null) {
                    client.cancelRecording();
                }

                setButtonsStatus(true);
            }
            // 음성인식 재시작버튼 listener
            else if (id == R.id.restartbutton) {
                if (client != null) {
                    client.cancelRecording();
                    client.startRecording(true, reader);
                }
            }
            // 음성인식 중지버튼 listener
            else if (id == R.id.stopbutton) {
                if (client != null) {
                    client.stopRecording();
                }
            }
            // 음성인식 기본 UI 버튼 listener
            else if (id == R.id.uibutton) {
                Intent i = new Intent(getApplicationContext(), VoiceRecoActivity.class);

                if (serviceType.equals(SpeechRecognizerClient.SERVICE_TYPE_WORD)) {
                    EditText words = (EditText) findViewById(R.id.words_edit);
                    String wordList = words.getText().toString();

                    Log.i("SpeechSampleActivity", "word list : " + wordList.replace('\n', ','));

                    i.putExtra(SpeechRecognizerActivity.EXTRA_KEY_USER_DICTIONARY, wordList);
                }

                i.putExtra(SpeechRecognizerActivity.EXTRA_KEY_SERVICE_TYPE, serviceType);

                startActivityForResult(i, 0);
            }
            // 음성합성 sample activity 열기
            else if (id == R.id.ttsbutton) {
                Intent i = new Intent(getApplicationContext(), TextToSpeechActivity.class);
                startActivity(i);
            }
        } catch(Throwable e) {
            printError(getStackTrace(e));
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        try {
            if (resultCode == RESULT_OK) {
                ArrayList<String> results = data.getStringArrayListExtra(VoiceRecoActivity.EXTRA_KEY_RESULT_ARRAY);

                final StringBuilder builder = new StringBuilder();

                for (String result : results) {
                    builder.append(result);
                    builder.append("\n");
                }

                new AlertDialog.Builder(this).
                        setMessage(builder.toString()).
                        setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).
                        show();
            } else if (requestCode == RESULT_CANCELED) {
                // 음성인식의 오류 등이 아니라 activity의 취소가 발생했을 때.
                if (data == null) {
                    return;
                }

                int errorCode = data.getIntExtra(VoiceRecoActivity.EXTRA_KEY_ERROR_CODE, -1);
                String errorMsg = data.getStringExtra(VoiceRecoActivity.EXTRA_KEY_ERROR_MESSAGE);

                if (errorCode != -1 && !TextUtils.isEmpty(errorMsg)) {
                    new AlertDialog.Builder(this).
                            setMessage(errorMsg).
                            setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).
                            show();
                }
            }
        } catch(Throwable e) {
            printError(getStackTrace(e));
            finish();
        }
    }

    @Override
    public void onReady() {
//        printLog("[onReady]");
    }

    @Override
    public void onBeginningOfSpeech() {
//        printLog("[onBeginningOfSpeech]");
    }

    @Override
    public void onEndOfSpeech() {
//        printLog("[onEndOfSpeech]");
    }

    @Override
    public void onError(int errorCode, String errorMsg) {
        printLog("[onError] : " + errorCode + " msg : " + errorMsg);
        printError("errorCode : [" + errorCode + "]\nerrorMsg : [" + errorMsg + "]");

        Log.e("SpeechSampleActivity", "onError");

        if(reader instanceof FileSpeechReaderImpl) {
            if(errCnt < 3) {
                errCnt++;

                try {
                    FileSpeechReaderImpl fReader = (FileSpeechReaderImpl) reader;

                    final FileSpeechReaderImpl tmpReader = fReader;
                    final Activity activity = this;

                    tmpReader.setCurCycle(0);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(200);
                            } catch (Exception e) {
                            }

                            setButtonsStatus(true);
                            startRecording();
                        }
                    });

                    final String progressMsg = "에러 발생으로 인한 재시도 - " + errCnt;;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, progressMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception ex) {
                    printError(getStackTrace(ex));
                }
            }
        }
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setButtonsStatus(true);
                }
            });
        }

        client = null;

//        finish();
    }

    @Override
    public void onPartialResult(String text) {
//        printLog("[onPartialResult] text : " + text);
    }

    @Override
    public void onResults(Bundle results) {
//        printLog("[onResults] results : " + results.toString());

        final StringBuilder builder = new StringBuilder();
        Log.i("SpeechSampleActivity", "onResults");

        if(reader instanceof FileSpeechReaderImpl) {
            try {
                FileSpeechReaderImpl fReader = (FileSpeechReaderImpl) reader;

                ArrayList<String> texts = results.getStringArrayList(SpeechRecognizerClient.KEY_RECOGNITION_RESULTS);

                if (texts.size() > 0) {
                    errCnt = 0;

                    resultFos.write(texts.get(0).getBytes("UTF-8"));
                    resultFos.flush();
                }

                final FileSpeechReaderImpl tmpReader = fReader;
                final Activity activity = this;

                String msg = null;

                if(tmpReader.isFinish()) {
//                    printLog("[onResults] 리더 finished");

                    tmpReader.close();
                    msg = "파일 변환 완료 - " + resultFile.getAbsolutePath() + "/" + resultFile.getName();

                    setButtonsStatus(true);
                }
                else {
//                    printLog("[onResults] 리더 continue");

                    tmpReader.setCurCycle(0);
                    int curProgress = (int) (tmpReader.getCurSize() * 100 / tmpReader.getTotalSize() / 10 - 1);

                    if(curProgress > -1) {
                        if (!progress[curProgress]) {
                            msg = "파일 변환 - " + (curProgress * 10) + "% 진행중";
                            progress[curProgress] = true;
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(200);
                            } catch(Exception e){}

                            setButtonsStatus(true);
                            startRecording();
                        }
                    });
                }

                final String progressMsg = msg;

                if(msg != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, progressMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }catch(Exception ex){
                printError(getStackTrace(ex));
            }
        }
        else {
            ArrayList<String> texts = results.getStringArrayList(SpeechRecognizerClient.KEY_RECOGNITION_RESULTS);
            ArrayList<Integer> confs = results.getIntegerArrayList(SpeechRecognizerClient.KEY_CONFIDENCE_VALUES);

            for (int i = 0; i < texts.size(); i++) {
                builder.append(texts.get(i));
                builder.append(" (");
                builder.append(confs.get(i).intValue());
                builder.append(")\n");
            }

            final Activity activity = this;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // finishing일때는 처리하지 않는다.
                    if (activity.isFinishing()) return;

                    AlertDialog.Builder dialog = new AlertDialog.Builder(activity).
                            setMessage(builder.toString()).
                            setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    dialog.show();

                    setButtonsStatus(true);
                }
            });
        }

        client = null;
    }

    @Override
    public void onAudioLevel(float v) {
//        printLog("[onAudioLevel]");
    }

    @Override
    public void onFinished() {
//        printLog("[onFinished]");

        Log.i("SpeechSampleActivity", "onFinished");
    }

    private void startRecording() {
        SpeechRecognizerClientImpl.Builder builder = new SpeechRecognizerClientImpl.Builder().
                setServiceType(serviceType);

        if (serviceType.equals(SpeechRecognizerClient.SERVICE_TYPE_WORD)) {
            EditText words = (EditText) findViewById(R.id.words_edit);
            String wordList = words.getText().toString();
            builder.setUserDictionary(wordList);

            Log.i("SpeechSampleActivity", "word list : " + wordList.replace('\n', ','));
        }

        client = builder.build();

        client.setSpeechRecognizeListener(this);
        client.startRecording(true, reader);

        setButtonsStatus(false);
    }

    private void printLog(String str) {
        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/speech";
        File file = new File(sdPath+"/SpeechActivity.txt");

        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file, true);

            Calendar cal = Calendar.getInstance();

            StringBuilder sb = new StringBuilder();

            sb.append("["+(cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND)+"."+cal.get(Calendar.MILLISECOND)));
            sb.append("] ");
            sb.append(str);
            sb.append("\n");

            fos.write(sb.toString().getBytes("UTF-8"));
            fos.flush();

            fos.close();
        } catch(Exception e) {
            try {if (fos != null) fos.close(); }catch (Exception ex1){}
        }
    }
}
