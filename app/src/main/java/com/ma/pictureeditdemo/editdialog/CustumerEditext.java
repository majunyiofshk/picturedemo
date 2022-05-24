package com.ma.pictureeditdemo.editdialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.appcompat.widget.AppCompatEditText;


public class CustumerEditext extends AppCompatEditText {

    public CustumerEditext(Context context) {
      super(context);
    }

    public CustumerEditext(Context context, AttributeSet attrs) {
      super(context, attrs);
    }

    public CustumerEditext(Context context, AttributeSet attrs, int defStyleAttr) {
      super(context, attrs, defStyleAttr);
    }


    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == 1) {
        super.onKeyPreIme(keyCode, event);
        onKeyBoardHideListener.onKeyHide(keyCode, event);
        return false;
      }
      return super.onKeyPreIme(keyCode, event);
    }


    OnKeyBoardHideListener onKeyBoardHideListener;
    public void setOnKeyBoardHideListener(OnKeyBoardHideListener onKeyBoardHideListener) {
      this.onKeyBoardHideListener = onKeyBoardHideListener;
    }

    public interface OnKeyBoardHideListener{
      void onKeyHide(int keyCode, KeyEvent event);
    }

}
