package com.ma.pictureeditdemo.txt

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StyleRes
import com.ma.pictureeditdemo.R

class KeyBoardDialog constructor(context: Context, @StyleRes themeResId: Int) : Dialog(context, themeResId){
    private lateinit var mEtDialog: EditText
    private lateinit var mTvSure: TextView
    private lateinit var mTvNumber: TextView
    private var mListener: ((s: CharSequence?) -> Unit)? = null
    private var mTextNumberString = context.getString(R.string.edit_text_number)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_edit_txt)
        window?.let {
            val params = it.attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            it.attributes = params
            it.setGravity(Gravity.BOTTOM)
            it.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            it.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        }
        initView()
    }
    
    private fun initView() {
        mEtDialog = findViewById(R.id.et_dialog_text)
        mTvSure = findViewById(R.id.tv_dialog_text_sure)
        mTvNumber = findViewById(R.id.tv_dialog_text_number)
        mTvNumber.text = String.format(mTextNumberString, 0)
        mEtDialog.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
    
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mListener?.invoke(s)
            }
    
            override fun afterTextChanged(s: Editable?) {
                if (TextUtils.isEmpty(mEtDialog.text)){
                    mTvSure.isEnabled = false
                    mTvNumber.text = String.format(mTextNumberString, 0)
                }else{
                    mTvSure.isEnabled = true
                    mTvNumber.text = String.format(mTextNumberString, mEtDialog.text.length)
                }
            }
        })
        mTvSure.setOnClickListener{
            dismiss()
        }
       
    }
    
    fun show(text: String?){
        show()
        if (mEtDialog.hint != text){
            mEtDialog.setText(text)
            text?.let {
                mEtDialog.setSelection(it.length)
            }
        }else{
            mEtDialog.setText("")
        }
    }
    
    fun changeText(text: String?){
        if (mEtDialog.hint != text){
            mEtDialog.setText(text)
            text?.let {
                mEtDialog.setSelection(it.length)
            }
        }else{
            mEtDialog.setText("")
        }
    }
    
    override fun show() {
        super.show()
        KeyboardUtils.showKeyboard(mEtDialog)
    }
    
    override fun dismiss() {
        super.dismiss()
        KeyboardUtils.hideKeyboard(mEtDialog)
    }
    
    fun setTextChangedListener(listener: ((s: CharSequence?) -> Unit)?){
        mListener = listener
    }
}