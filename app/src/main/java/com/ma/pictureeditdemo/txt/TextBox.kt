package com.ma.pictureeditdemo.txt

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import com.ma.pictureeditdemo.R
import com.ma.pictureeditdemo.extensions.dp2px
import com.ma.pictureeditdemo.extensions.sp2px
import kotlin.math.abs
import kotlin.math.atan2

class TextBox constructor(var imageView: ImageView, var text: String) {
    private val mBoxStrokeWidth = imageView.dp2px(1).toFloat()
    private val mHint = imageView.resources.getString(R.string.edit_default_text)
    
    private var mBoxPaint: Paint = Paint()
    val mBoxRectF: RectF = RectF()
    private var mTextPaint: Paint = Paint()
    private var mTextColor = Color.WHITE
    private val mTextSize = imageView.sp2px(18)
    private val mTextPadding = imageView.dp2px(8).toFloat()
    
    private var mCopyDrawable: Drawable?
    private var mDeleteDrawable: Drawable?
    private var mStretchDrawable: Drawable?
    
    var mRotateMatrix: Matrix = Matrix()
    var mMatrix: Matrix = Matrix() // 缩放加上平移
    private val mTempMatrix: Matrix = Matrix()
    private val mTempRect: RectF = RectF()
    
    var mTouchType = TouchType.OUTSIDE
    var mState = State.SELECTED
    
    init {
        mBoxPaint.let {
            it.style = Paint.Style.STROKE
            it.isAntiAlias = true
            it.isDither = true
            it.strokeWidth = mBoxStrokeWidth
            it.color = Color.WHITE
        }
        mTextPaint.let {
            it.style = Paint.Style.FILL
            it.isAntiAlias = true
            it.isDither = true
            it.color = mTextColor
            it.textSize = mTextSize.toFloat()
        }
        //计算编辑框大小
        val bounds = Rect()
        mTextPaint.getTextBounds(text, 0, text.length, bounds)
        val width = bounds.width().toFloat() + 2 * mTextPadding
        val height = bounds.height().toFloat() + 2 * mTextPadding
        mBoxRectF.set(0f, 0f, width, height)
        
        mCopyDrawable = ResourcesCompat.getDrawable(imageView.resources, R.drawable.ic_edit_txt_copy, null)
        mDeleteDrawable = ResourcesCompat.getDrawable(imageView.resources, R.drawable.ic_edit_txt_delete, null)
        mStretchDrawable = ResourcesCompat.getDrawable(imageView.resources, R.drawable.ic_edit_txt_stretch, null)
    }
    
    fun configure(matrix: Matrix?, rotateMatrix: Matrix?){
        mMatrix.set(matrix)
        mRotateMatrix.set(rotateMatrix)
    }
    
    fun configure(text: String, matrix: Matrix?, rotateMatrix: Matrix?){
        val bounds = Rect()
        mTextPaint.getTextBounds(text, 0, text.length, bounds)
        val newWidth = bounds.width() + 2 * mTextPadding
        val newHeight = bounds.height() + 2 * mTextPadding
        mBoxRectF.set(0f, 0f, newWidth, newHeight)
        
        this.text = text
        mMatrix.set(matrix)
        mRotateMatrix.set(rotateMatrix)
    }
    
    private fun determineTouchType(x: Float, y: Float) {
        val point = floatArrayOf(x, y)
        // 计算旋转矩阵的逆矩阵,抵消canvas旋转
        mTempMatrix.reset()
        mRotateMatrix.invert(mTempMatrix)
        mTempMatrix.mapPoints(point)
        val eventX = point[0]
        val eventY = point[1]
        val rectF = mTempRect
        rectF.set(mBoxRectF)
        mMatrix.mapRect(rectF)
        mCopyDrawable?.let { // 左上
            if (abs(rectF.left - eventX) < it.intrinsicWidth
                && abs(rectF.top - eventY) < it.intrinsicHeight
            ) {
                mTouchType = TouchType.COPY
                return
            }
        }
        mDeleteDrawable?.let { //右上
            if (abs(rectF.right - eventX) < it.intrinsicWidth
                && abs(rectF.top - eventY) < it.intrinsicHeight
            ) {
                mTouchType = TouchType.DELETE
                return
            }
        }
        mDeleteDrawable?.let { //右下
            if (abs(rectF.right - eventX) < it.intrinsicWidth
                && abs(rectF.bottom - eventY) < it.intrinsicHeight
            ) {
                mTouchType = TouchType.ROTATE_AND_SCALE
                return
            }
        }
        if (rectF.contains(eventX, eventY)) {
            mTouchType = TouchType.INSIDE
            return
        }
        mTouchType = TouchType.OUTSIDE
        return
    }
    
    fun isInside(x: Float, y: Float): Boolean{
        determineTouchType(x, y)
        return mTouchType != TouchType.OUTSIDE
    }
    
    fun onTouchScroll(triggerX: Float, triggerY: Float, dx: Float, dy: Float) {
        when (mTouchType) {
            TouchType.ROTATE_AND_SCALE -> {
                onRotateAndScale(triggerX, triggerY, dx)
            }
            else -> {
                onMove(dx, dy)
            }
        }
    }
    
    private fun onMove(dx: Float, dy: Float) {
        if (checkCanMove(dx, dy)) {
            mMatrix.postTranslate(-dx, -dy)
            imageView.invalidate()
        }
    }
    
    private fun onRotateAndScale(triggerX: Float, triggerY: Float, dx: Float) {
        val rectF = mTempRect
        rectF.set(mBoxRectF)
        mMatrix.mapRect(rectF)
        
        val point1 = PointF(rectF.centerX(), rectF.centerY())
        val point2 = PointF(rectF.right, rectF.bottom)
        val point3 = PointF(triggerX, triggerY)
        val angle1 = calculateAngleBetweenPoints(point2, point1)
        val angle2 = calculateAngleBetweenPoints(point3, point1)
        val rotation = angle1 - angle2
        mRotateMatrix.reset()
        mRotateMatrix.postRotate(rotation, rectF.centerX(), rectF.centerY())
        
        val oldW = rectF.width()
        val w = oldW - dx
        val scale = w / oldW
        if (checkCanScale(scale)) {
            mMatrix.postScale(scale, scale, rectF.centerX(), rectF.centerY())
        }
        imageView.invalidate()
    }
    
    /*
    * 右下角旋转和缩放
    * */
    private fun calculateAngleBetweenPoints(pointA: PointF, pointB: PointF): Float {
        val dx = pointB.x - pointA.x
        val dy = pointB.y - pointA.y
        if (dx == 0f && dy == 0f) {
            return 0f
        }
        var angle = Math.toDegrees(atan2(dx.toDouble(), dy.toDouble()))
        if (angle < 0) {
            angle = 360.0 + angle % -360.0
        } else {
            angle %= 360.0
        }
        return angle.toFloat()
    }
    
    /*
    * 双指缩放
    * */
    fun onScale(scaleFactor: Float) {
        if (checkCanScale(scaleFactor)) {
            val rectF = mTempRect
            rectF.set(mBoxRectF)
            mMatrix.mapRect(rectF)
            
            mMatrix.postScale(scaleFactor, scaleFactor, rectF.centerX(), rectF.centerY())
            imageView.invalidate()
        }
    }
    
    private fun checkCanScale(scaleFactor: Float): Boolean {
        val rectF = mTempRect
        rectF.set(mBoxRectF)
        mMatrix.mapRect(rectF)
        mTempMatrix.reset()
        mTempMatrix.postScale(scaleFactor, scaleFactor, rectF.centerX(), rectF.centerY())
        mTempMatrix.mapRect(rectF)
        val minWidth = mBoxRectF.width() * 0.5f
        val maxWidth = imageView.width + mBoxRectF.width() * 2f
        return rectF.width() in minWidth..maxWidth
    }
    
    private fun checkCanMove(dx: Float, dy: Float): Boolean {
        val rectF = mTempRect
        rectF.set(mBoxRectF)
        mMatrix.mapRect(rectF)
        rectF.offset(-dx, -dy)
        val leftCanMove = rectF.left >= -rectF.width() / 2f
        val rightCanMove = rectF.right <= imageView.width + rectF.width() / 2f
        val topCanMove = rectF.top >= -rectF.height() / 2f
        val bottomCanMove = rectF.bottom <= imageView.height + rectF.height() / 2f
        return leftCanMove && rightCanMove && topCanMove && bottomCanMove
    }
    
    fun onTextChanged(s: CharSequence?) {
        val newText = if (TextUtils.isEmpty(s)) {
            mHint
        } else {
            s.toString()
        }
        if (text == newText){
            return
        }
        //重新计算编辑框大小
        val bounds = Rect()
        mTextPaint.getTextBounds(newText, 0, newText.length, bounds)
        val newWidth = bounds.width() + 2 * mTextPadding
        val newHeight = bounds.height() + 2 * mTextPadding
        mBoxRectF.set(0f, 0f, newWidth, newHeight)
        imageView.invalidate()
        text = newText
    }
    
    fun setState(state: State){
        if (mState != state){
            mState = state
            imageView.invalidate()
        }
    }
    
    fun draw(canvas: Canvas) {
        if (mState != State.NORMAL){
            drawBox(canvas)
        }
        drawText(canvas)
    }
    
    private fun drawText(canvas: Canvas) {
        //把缩放和旋转都作用在画布上
        mTempMatrix.set(mMatrix)
        mTempMatrix.postConcat(mRotateMatrix)
        canvas.save()
        canvas.concat(mTempMatrix)
        canvas.drawText(text, mTextPadding, mBoxRectF.height() - mTextPadding, mTextPaint)
        canvas.restore()
    }
    
    private fun drawBox(canvas: Canvas) {
        canvas.save()
        canvas.concat(mRotateMatrix)
        
        //绘制编辑框
        mTempRect.set(mBoxRectF)
        mMatrix.mapRect(mTempRect) // 应用缩放加平移
        mBoxRectF.inset(-mBoxStrokeWidth, -mBoxStrokeWidth)
        canvas.drawRect(mTempRect, mBoxPaint)
        mBoxRectF.inset(mBoxStrokeWidth, mBoxStrokeWidth)
    
        //绘制角icon
        mCopyDrawable?.let {
            val left: Int = (mTempRect.left - it.intrinsicWidth / 2).toInt()
            val top: Int = (mTempRect.top - it.intrinsicHeight / 2).toInt()
            val right: Int = (mTempRect.left + it.intrinsicWidth / 2).toInt()
            val bottom: Int = (mTempRect.top + it.intrinsicHeight / 2).toInt()
            it.setBounds(left, top, right, bottom)
            it.draw(canvas)
        }
        mDeleteDrawable?.let {
            val left: Int = (mTempRect.right - it.intrinsicWidth / 2).toInt()
            val top: Int = (mTempRect.top - it.intrinsicHeight / 2).toInt()
            val right: Int = (mTempRect.right + it.intrinsicWidth / 2).toInt()
            val bottom: Int = (mTempRect.top + it.intrinsicHeight / 2).toInt()
            it.setBounds(left, top, right, bottom)
            it.draw(canvas)
        }
        mStretchDrawable?.let {
            val left: Int = (mTempRect.right - it.intrinsicWidth / 2).toInt()
            val top: Int = (mTempRect.bottom - it.intrinsicHeight / 2).toInt()
            val right: Int = (mTempRect.right + it.intrinsicWidth / 2).toInt()
            val bottom: Int = (mTempRect.bottom + it.intrinsicHeight / 2).toInt()
            it.setBounds(left, top, right, bottom)
            it.draw(canvas)
        }
        
        canvas.restore()
    }
    
    enum class TouchType {
        OUTSIDE,
        COPY, //复制
        DELETE, //删除
        ROTATE_AND_SCALE, // 旋转缩放
        INSIDE // 编辑框内
    }
    
    enum class State {
        NORMAL,
        SELECTED, // 选中
        FOCUS //焦点
    }
}