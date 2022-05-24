package com.ma.pictureeditdemo.editdialog;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.ma.pictureeditdemo.R;

public class KeyBoardDialog extends Dialog implements View.OnClickListener,
    CustumerEditext.OnKeyBoardHideListener {

  private CustumerEditext mEdit_content;
  private Button mBt_send;

  public KeyBoardDialog(@NonNull Context context, int themeResId) {
    super(context, themeResId);
    init(context);
  }



  public Context mContext;
  public View mRootView;

  public void init(Context context) {
    mContext = context;
    mRootView = LayoutInflater.from(context).inflate(R.layout.notify_dialog_input, null);
    setContentView(mRootView);
    Window window = getWindow();
    WindowManager.LayoutParams params = window.getAttributes();
    params.width = WindowManager.LayoutParams.MATCH_PARENT;
    window.setAttributes(params);
    window.setGravity(Gravity.BOTTOM);

    mEdit_content = mRootView.findViewById(R.id.edit_content);
    mBt_send = mRootView.findViewById(R.id.bt_send);

    mBt_send.setOnClickListener(this);
    mEdit_content.setOnKeyBoardHideListener(this);
  }

  @Override public void onClick(View v) {
    if (v.getId() == R.id.bt_send) {
      if (mListenter != null) {
        mListenter.setSend(mEdit_content.getText().toString().trim());
        dismiss();
      }
    }
  }

  @Override public boolean onTouchEvent(@NonNull MotionEvent event) {
    if (isOutOfBounds(getContext(),event)) {
      if (mListenter != null) {
        mListenter.setContent(mEdit_content.getText().toString().trim());
        dismiss();
      }
      return true;
    }
    return super.onTouchEvent(event);
  }

  private boolean isOutOfBounds(Context context, MotionEvent event) {
    final int x = (int) event.getX();//相对弹窗左上角的x坐标
    final int y = (int) event.getY();//相对弹窗左上角的y坐标
    final int slop = ViewConfiguration.get(context).getScaledWindowTouchSlop();//最小识别距离
    final View decorView = getWindow().getDecorView();//弹窗的根View
    return (x < -slop) || (y < -slop) || (x > (decorView.getWidth() + slop))
        || (y > (decorView.getHeight() + slop));
  }



  private InputViewListenter mListenter;

  public void setListenter(InputViewListenter listenter) {
    mListenter = listenter;
  }

  public void setInitData(String content) {
    mEdit_content.setHint("回复 " +content +" :");
  }

  @Override public void onKeyHide(int keyCode, KeyEvent event) {
    if (mListenter != null) {
      mListenter.setContent(mEdit_content.getText().toString().trim());
      dismiss();
    }
  }

  public interface InputViewListenter {
    void setContent(String content);

    void setSend(String content);
  }
}
