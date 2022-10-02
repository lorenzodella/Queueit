package com.spotify.queueit.utils;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

public class KeyboardEditText extends androidx.appcompat.widget.AppCompatEditText {

    public KeyboardEditText(Context context) {
        super(context);
    }

    public KeyboardEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KeyboardEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (keyboardListener != null)
            keyboardListener.onStateChanged(this, focused);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, @NonNull KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            if (keyboardListener != null)
                keyboardListener.onStateChanged(this, false);
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void reset(int gravity, String hint){
        clearFocus();
        setText("");
        setGravity(gravity);
        setHint(hint);
    }

    /**
     * Keyboard Listener
     */
    KeyboardListener keyboardListener;

    public void setOnKeyboardListener(KeyboardListener listener) {
        this.keyboardListener = listener;
    }

    public interface KeyboardListener {
        void onStateChanged(KeyboardEditText keyboardEditText, boolean showing);
    }
}
