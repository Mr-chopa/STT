package com.kakao.sdk.newtone.custom;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Created by chopa on 2018. 1. 15..
 */

public class CorrectionValueListener implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable edit) {
        try {
            String s = edit.toString();

            // 포커스 상실 시
            if(s.length() > 0) {
                Printer.debug("보정값 변경 - [" + s + "]");

                Settings.set(Settings.Attribute.correctionValue, s);
                Settings.save();
            }
        } catch(Exception e) {
            Printer.error(e);

            throw new RuntimeException(e);
        }
    }
}
