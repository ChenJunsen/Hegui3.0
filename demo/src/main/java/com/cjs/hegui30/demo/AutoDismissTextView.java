package com.cjs.hegui30.demo;


import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;


/**
 * 没有文字时自动消失的TextView
 *
 * @author JasonChen
 * @email chenjunsen@outlook.com
 * @createTime 2021/7/13 10:58
 */
public class AutoDismissTextView extends AppCompatTextView {

    public AutoDismissTextView(@NonNull Context context) {
        super(context);
        setViewVisible(getText());
    }

    public AutoDismissTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setViewVisible(getText());
    }

    public AutoDismissTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setViewVisible(getText());
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        setViewVisible(text);
    }

    private void setViewVisible(CharSequence text){
        setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }
}
