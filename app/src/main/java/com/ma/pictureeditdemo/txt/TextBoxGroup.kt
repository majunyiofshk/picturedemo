package com.ma.pictureeditdemo.txt

import android.graphics.Canvas
import android.graphics.Matrix
import android.text.TextUtils
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.view.GestureDetectorCompat
import com.ma.pictureeditdemo.R
import com.ma.pictureeditdemo.photoview.CustomGestureDetector
import com.ma.pictureeditdemo.photoview.OnGestureListener

/*
* 文本编辑框容器
* */
class TextBoxGroup constructor(var imageView: ImageView): IMemoAction<TextMemo>{
    private val mList = mutableListOf<TextBox>()
    
    var mCaretaker = PhotoEditCaretaker<TextMemo>()
    
    private var mIsInside = false // 此次点击是否在编辑框
    private var mTouchTextBox: TextBox? = null // 此次被触摸的编辑框
    private var mSelectedTextBox: TextBox? = null
    
    private var mDownMatrix = Matrix() // 按下时文本框Matrix
    private var mUpMatrix = Matrix() // 抬手时文本框Matrix
    private val mHint = imageView.resources.getString(R.string.edit_default_text)
    
    private lateinit var mDetector: GestureDetectorCompat
    private lateinit var mScalerDetector: CustomGestureDetector
    
    private val mDialog: KeyBoardDialog by lazy {
        val dialog = KeyBoardDialog(imageView.context, R.style.notify_input_dialog)
        dialog.setTextChangedListener { s ->
            mSelectedTextBox?.let {
                val text = it.text
                if (text != s.toString()){
                    it.onTextChanged(s)
                }
                if (text != s.toString() && !TextUtils.isEmpty(s)){
                    save(createMemo(it))
                }
            }
        }
        dialog.setOnDismissListener {
            mSelectedTextBox?.setState(TextBox.State.SELECTED)
        }
        dialog
    }
    
    private fun openSoftInput(text: String?) {
        mDialog.show(text)
    }
    
    private fun closeSoftInput() {
        mDialog.dismiss()
    }
    
    private fun changeDialogText(text: String?){
        mDialog.changeText(text)
    }
    
    init {
        initDetector()
    }
    
    private fun initDetector() {
        mDetector = GestureDetectorCompat(imageView.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                makeSureTouchType(e.x, e.y)
                return true
            }
            
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onClick()
                return true
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleClick()
                return super.onDoubleTap(e)
            }
            
            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                // 只有被选中的才能移动
                mSelectedTextBox?.onTouchScroll(e2.x, e2.y, distanceX, distanceY)
                return true
            }
        })
        mScalerDetector = CustomGestureDetector(imageView.context, object : OnGestureListener {
            override fun onDrag(dx: Float, dy: Float) {
                // do nothing
            }
            
            override fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
                // do nothing
            }
            
            override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
                // 只有被选中的才能缩放
                mSelectedTextBox?.onScale(scaleFactor)
            }
        })
    }
    
    private fun makeSureTouchType(x: Float, y: Float){
        mIsInside = false
        for (value in mList){
            if (value.isInside(x, y)){
                mIsInside = true
                mTouchTextBox = value
            }
        }
    }
    
    private fun onClick(){
        //1.在 out,选中编辑框需要取消选中
        //2.在 in, 点击的是选中编辑框,选中编辑框若已有软键盘,则关闭,没有,则弹出
        //3.在 in, 点击的不是选中编辑框,选中编辑框若已有软键盘, 则更换选中编辑框并且改变软键盘text,没有,则更换选中编辑框
        if (mIsInside){
            if (mSelectedTextBox == mTouchTextBox){
                mSelectedTextBox?.let {
                    when(it.mState) {
                        TextBox.State.NORMAL -> {
                            it.setState(TextBox.State.SELECTED)
                        }
                        TextBox.State.SELECTED -> {
                            when(it.mTouchType){
                                TextBox.TouchType.COPY -> {
                                    copy(it)
                                }
                                TextBox.TouchType.DELETE -> {
                                    // 删除
                                    delete(it)
                                }
                                else -> {
                                    openSoftInput(it.text)
                                    it.setState(TextBox.State.FOCUS)
                                }
                            }
                        }
                        TextBox.State.FOCUS -> {
                            when(it.mTouchType){
                                TextBox.TouchType.COPY -> {
                                    copy(it)
                                }
                                TextBox.TouchType.DELETE -> {
                                    delete(it)
                                }
                                else -> {
                                    closeSoftInput()
                                    it.setState(TextBox.State.SELECTED)
                                }
                            }
                        }
                    }
                }
            }else{
               if (mSelectedTextBox is TextBox){
                   mSelectedTextBox?.let {
                       when(it.mState) {
                           TextBox.State.SELECTED -> {
                               it.setState(TextBox.State.NORMAL)
                               mTouchTextBox?.setState(TextBox.State.SELECTED)
                           }
                           TextBox.State.FOCUS -> {
                               it.setState(TextBox.State.NORMAL)
                               mTouchTextBox?.setState(TextBox.State.FOCUS)
                               mSelectedTextBox = mTouchTextBox
                               changeDialogText(mTouchTextBox?.text)
                           }
                           else ->{
                               mTouchTextBox?.setState(TextBox.State.FOCUS)
                               openSoftInput(mTouchTextBox?.text)
                           }
                       }
                   }
               }else{
                   mTouchTextBox?.setState(TextBox.State.SELECTED)
               }
                mSelectedTextBox = mTouchTextBox
            }
        }else{
            // 软键盘
            mSelectedTextBox?.let {
                when (it.mState) {
                    TextBox.State.FOCUS -> {
                        it.setState(TextBox.State.NORMAL)
                        mSelectedTextBox = null
                        closeSoftInput()
                    }
                    TextBox.State.SELECTED -> {
                        it.setState(TextBox.State.NORMAL)
                        mSelectedTextBox = null
                    }
                    else -> {
                        // do nothing
                    }
                }
            }
        }
    }
    
    private fun onDoubleClick() {
        if (mIsInside){
            if (mSelectedTextBox == mTouchTextBox){
                mSelectedTextBox?.let {
                    when(it.mState) {
                        TextBox.State.NORMAL -> {
                            it.setState(TextBox.State.FOCUS)
                            openSoftInput(it.text)
                        }
                        TextBox.State.SELECTED -> {
                            it.setState(TextBox.State.FOCUS)
                            openSoftInput(it.text)
                        }
                        TextBox.State.FOCUS -> {
                            it.setState(TextBox.State.SELECTED)
                            closeSoftInput()
                        }
                    }
                }
            }else{
                if (mSelectedTextBox is TextBox){
                    mSelectedTextBox?.let {
                        when(it.mState) {
                            TextBox.State.SELECTED -> {
                                it.setState(TextBox.State.NORMAL)
                                mTouchTextBox?.setState(TextBox.State.FOCUS)
                                openSoftInput(mTouchTextBox?.text)
                            }
                            TextBox.State.FOCUS -> {
                                it.setState(TextBox.State.NORMAL)
                                mTouchTextBox?.setState(TextBox.State.FOCUS)
                                mSelectedTextBox = mTouchTextBox
                                changeDialogText(mTouchTextBox?.text)
                            }
                            else ->{
                                mTouchTextBox?.setState(TextBox.State.FOCUS)
                                openSoftInput(mTouchTextBox?.text)
                            }
                        }
                    }
                }else{
                    mTouchTextBox?.setState(TextBox.State.FOCUS)
                    openSoftInput(mTouchTextBox?.text)
                }
                mSelectedTextBox = mTouchTextBox
            }
        }else{
            mSelectedTextBox?.let {
                when (it.mState) {
                    TextBox.State.FOCUS -> {
                        it.setState(TextBox.State.NORMAL)
                        mSelectedTextBox = null
                        closeSoftInput()
                    }
                    TextBox.State.SELECTED -> {
                        it.setState(TextBox.State.NORMAL)
                        mSelectedTextBox = null
                    }
                    else -> {
                        // do nothing
                    }
                }
            }
        }
    }
    
    fun onSizeChanged(w: Int, h: Int){
        if (mList.isEmpty()){
            val element = createTextBox(w, h)
            mList.add(element)
            mSelectedTextBox = element
            
            save(createMemo(mSelectedTextBox))
        }
    }
    
    fun onTouchEvent(event: MotionEvent): Boolean{
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                imageView.parent?.requestDisallowInterceptTouchEvent(true)
                recordDownMatrix()
            }
            MotionEvent.ACTION_UP -> {
                recordUpMatrix()
                if (mDownMatrix != mUpMatrix){
                    save(createMemo(mSelectedTextBox))
                }
            }
        }
        if (mList.isEmpty()) return false
        mScalerDetector.onTouchEvent(event)
        return mDetector.onTouchEvent(event)
    }
    
    private fun recordDownMatrix(){
        mSelectedTextBox?.let {
            mDownMatrix.set(it.mMatrix)
            mDownMatrix.postConcat(it.mRotateMatrix)
        }
    }
    
    private fun recordUpMatrix(){
        mSelectedTextBox?.let {
            mUpMatrix.set(it.mMatrix)
            mUpMatrix.postConcat(it.mRotateMatrix)
        }
    }
    
    fun draw(canvas: Canvas){
        mList.forEach { it.draw(canvas) }
    }
    
    private fun copy(textBox: TextBox){
        val element = copyTextBox(textBox)
        textBox.setState(TextBox.State.NORMAL)
        mSelectedTextBox = element
        mList.add(element)
        imageView.invalidate()
        
        save(createMemo(element))
    }
    
    private fun delete(textBox: TextBox){
        mList.remove(textBox)
        mSelectedTextBox = null
        imageView.invalidate()
    
        save(createMemo(textBox))
    }
    
    private fun createTextBox(w: Int, h: Int): TextBox{
        val textBoxV2 = TextBox(imageView, mHint)
        val matrix =  Matrix()
        val rotateMatrix = Matrix()
        val width = textBoxV2.mBoxRectF.width()
        val height = textBoxV2.mBoxRectF.height()
        matrix.postTranslate((w - width) / 2f, (h - height) / 2f)
        textBoxV2.configure(matrix, rotateMatrix)
        return textBoxV2
    }
    
    private fun copyTextBox(textBox: TextBox): TextBox{
        val text = textBox.text
        val matrix = Matrix(textBox.mMatrix)
        matrix.postTranslate(40f, 40f)
        val rotateMatrix = Matrix(textBox.mRotateMatrix)
        val newTextBoxV2 = TextBox(imageView, text)
        newTextBoxV2.configure(matrix, rotateMatrix)
        newTextBoxV2.setState(textBox.mState)
        return newTextBoxV2
    }
    
    private fun createMemo(textBox: TextBox?): TextMemo{
        val textMemo = TextMemo()
        textBox?.let {
            textMemo.index = mList.indexOf(it)
            textMemo.size = mList.size
            textMemo.text = it.text
            textMemo.matrix = Matrix(it.mMatrix)
            textMemo.rotateMatrix = Matrix(it.mRotateMatrix)
        }
        return textMemo
    }
    
    /*
    * 保存
    * */
    override fun save(memo: TextMemo) {
        mCaretaker.saveMemo(memo)
    }
    
    /*
    * 恢复
    * */
    override fun restore(memo: TextMemo) {
        when {
            memo.size > mList.size -> {
                val textBox = TextBox(imageView, memo.text)
                textBox.configure(memo.matrix, memo.rotateMatrix)
                mList.add(memo.index, textBox)
            }
            memo.size < mList.size -> {
                mList.removeAt(mList.size - 1)
            }
            else -> {
                mList[memo.index].configure(memo.text, memo.matrix, memo.rotateMatrix)
            }
        }
        imageView.invalidate()
    }
}