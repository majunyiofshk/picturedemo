package com.ma.pictureeditdemo.surface

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.ma.pictureeditdemo.ScreenUtils
import com.ma.pictureeditdemo.box.RectFEvaluator
import com.ma.pictureeditdemo.extensions.dp
import kotlin.math.abs

/**
 * @Description: 裁剪框
 * @Author: JunYi.Ma
 * @Date: 2022/6/2 13:41
 * @Email:  junyi.ma@upuphone.com
 */
class CropFrame @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    
    private val mInnerLineStroke = 1f.dp()
    private val mOutLineStroke = 3f.dp()
    private val mGuideLineStroke = 1f.dp()
    
    // 内边框
    private val mInnerPaint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        isAntiAlias = true
        isDither = true
        strokeWidth = mInnerLineStroke
        strokeCap = Paint.Cap.SQUARE
    }
    
    // 外边框
    private val mOutPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        isAntiAlias = true
        isDither = true
        strokeCap = Paint.Cap.SQUARE
    }
    
    // 辅助线
    private val mGuideLinePaint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#7FFFFFFF")
        isAntiAlias = true
        isDither = true
        strokeWidth = mGuideLineStroke
    }
    
    private val mOutLineLength = 26f.dp()
    private val mMinCropFrameLength = 66f.dp() // 边框最小长度
    private var mBaseRectF: RectF = RectF() // 裁剪框初始化区域
    private var mDrawRectF: RectF = RectF()
    private var mCurrentRectF: RectF = RectF() //裁剪框当前区域(不包括线框宽)
    private var mTouchType = TouchType.NONE // 触摸类型
    private var mRatio: Float = 0f // 等于0表示自由模式
    private val mTouchSlop = 24f.dp() // 默认裁剪框四个角的触摸区域大小
    private var mMinWidth = 0f
    private var mMinHeight = 0f
    private var mLeftEdge = 0f
    private var mRightEdge = 0f
    private var mTopEdge = 0f
    private var mBottomEdge = 0f
    private val mTempMatrix = Matrix()
    private var mDownRectF = RectF()
    private val mGestureDetector by lazy {
        GestureDetector(context, onGestureListener)
    }
    private val onGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        /**
         * 每次按下时都判断手指位置是否在裁剪框四周
         * */
        override fun onDown(e: MotionEvent): Boolean {
            return obtainTouchType(e.x, e.y) != TouchType.NONE
        }
        
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            handleScroll(mTouchType, distanceX, distanceY)
            return true
        }
    }
    
    init {
        mBaseRectF.set(72.0f, 319.5f, 1008.0f, 1567.5f)
        mCurrentRectF.set(mBaseRectF)
        updateEdge(0f)
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownRectF.set(mCurrentRectF)
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                // 是否动画
                if (mDownRectF != mCurrentRectF) {
                    springBack()
                }
            }
        }
        return mGestureDetector.onTouchEvent(event)
    }
    
    /**
     * 抬手后回弹动画
     * */
    private fun springBack() {
        val ratio = if (mRatio == 0f) {
            mCurrentRectF.width() / mCurrentRectF.height()
        } else {
            mRatio
        }
        val startRectF = RectF(mCurrentRectF)
        val endRectF = getCropFrameRectFByScale(ratio)
        val anim = ValueAnimator.ofObject(RectFEvaluator(), startRectF, endRectF)
        anim.addUpdateListener {
            mCurrentRectF.set(it.animatedValue as RectF)
            invalidate()
        }
        anim.duration = 300
        anim.start()
    }
    
    private fun obtainTouchType(x: Float, y: Float): TouchType {
        val left = mCurrentRectF.left
        val top = mCurrentRectF.top
        val right = mCurrentRectF.right
        val bottom = mCurrentRectF.bottom
        val ltRectF = RectF(left - mTouchSlop, top - mTouchSlop, left + mTouchSlop, top + mTouchSlop)
        val rtRectF = RectF(right - mTouchSlop, top - mTouchSlop, right + mTouchSlop, top + mTouchSlop)
        val lbRectF = RectF(left - mTouchSlop, bottom - mTouchSlop, left + mTouchSlop, bottom + mTouchSlop)
        val rbRectF = RectF(right - mTouchSlop, bottom - mTouchSlop, right + mTouchSlop, bottom + mTouchSlop)
        val lRectF = RectF(left - mTouchSlop, top + mTouchSlop, left + mTouchSlop, bottom - mTouchSlop)
        val tRectF = RectF(left + mTouchSlop, top - mTouchSlop, right - mTouchSlop, top + mTouchSlop)
        val rRectF = RectF(right - mTouchSlop, top + mTouchSlop, right + mTouchSlop, bottom - mTouchSlop)
        val bRectF = RectF(left + mTouchSlop, bottom - mTouchSlop, right - mTouchSlop, bottom + mTouchSlop)
        
        mTouchType = when {
            ltRectF.contains(x, y) -> TouchType.LT
            rtRectF.contains(x, y) -> TouchType.RT
            lbRectF.contains(x, y) -> TouchType.LB
            rbRectF.contains(x, y) -> TouchType.RB
            lRectF.contains(x, y) -> TouchType.LEFT
            rRectF.contains(x, y) -> TouchType.RIGHT
            tRectF.contains(x, y) -> TouchType.TOP
            bRectF.contains(x, y) -> TouchType.BOTTOM
            else -> TouchType.NONE
        }
        return mTouchType
    }
    
    private fun handleScroll(touchType: TouchType, distanceX: Float, distanceY: Float) {
        when (touchType) {
            TouchType.LT -> handleScrollWhenLT(distanceX, distanceY)
            TouchType.RT -> handleScrollWhenRT(distanceX, distanceY)
            TouchType.LB -> handleScrollWhenLB(distanceX, distanceY)
            TouchType.RB -> handleScrollWhenRB(distanceX, distanceY)
            TouchType.LEFT -> handleScrollWhenL(distanceX)
            TouchType.TOP -> handleScrollWhenT(distanceY)
            TouchType.RIGHT -> handleScrollWhenR(distanceX)
            TouchType.BOTTOM -> handleScrollWhenB(distanceY)
            else -> {}
        }
    }
    
    private fun handleScrollWhenLT(distanceX: Float, distanceY: Float) {
        val minWidth = mMinWidth
        val minHeight = mMinHeight
        val left = mLeftEdge
        val top = mTopEdge
        var dx = distanceX
        var dy = distanceY
        if (mCurrentRectF.left - distanceX < left) {
            dx = mCurrentRectF.left - left
        }
        if (mCurrentRectF.width() + distanceX < minWidth) {
            dx = minWidth - mCurrentRectF.width()
        }
        if (mCurrentRectF.top - distanceY < top) {
            dy = mCurrentRectF.top - top
        }
        if (mCurrentRectF.height() + distanceY < minHeight) {
            dy = minHeight - mCurrentRectF.height()
        }
        if (dx == 0f && dy == 0f) {
            return
        }
        if (mRatio == 0f) {
            mCurrentRectF.set(mCurrentRectF.left - dx, mCurrentRectF.top - dy, mCurrentRectF.right, mCurrentRectF.bottom)
        } else {
            if (abs(dx) > abs(dy)) {
                mTempMatrix.setScale((mCurrentRectF.width() + dx) / mCurrentRectF.width(),
                        (mCurrentRectF.width() + dx) / mCurrentRectF.width(), mCurrentRectF.right, mCurrentRectF.bottom)
            } else {
                mTempMatrix.setScale((mCurrentRectF.height() + dy) / mCurrentRectF.height(),
                        (mCurrentRectF.height() + dy) / mCurrentRectF.height(), mCurrentRectF.right, mCurrentRectF.bottom)
            }
            mTempMatrix.mapRect(mCurrentRectF)
        }
        invalidate()
    }
    
    private fun handleScrollWhenRT(distanceX: Float, distanceY: Float) {
        val minWidth = mMinWidth
        val minHeight = mMinHeight
        val top = mTopEdge
        val right = mRightEdge
        var dx = distanceX
        var dy = distanceY
        if (mCurrentRectF.right - distanceX > right) {
            dx = mCurrentRectF.right - right
        }
        if (mCurrentRectF.width() - distanceX < minWidth) {
            dx = mCurrentRectF.width() - minWidth
        }
        if (mCurrentRectF.top - distanceY < top) {
            dy = mCurrentRectF.top - top
        }
        if (mCurrentRectF.height() + distanceY < minHeight) {
            dy = minHeight - mCurrentRectF.height()
        }
        if (dx == 0f && dy == 0f) {
            return
        }
        if (mRatio == 0f) {
            mCurrentRectF.set(mCurrentRectF.left, mCurrentRectF.top - dy, mCurrentRectF.right - dx, mCurrentRectF.bottom)
        } else {
            if (abs(dx) > abs(dy)) {
                mTempMatrix.setScale((mCurrentRectF.width() - dx) / mCurrentRectF.width(),
                        (mCurrentRectF.width() - dx) / mCurrentRectF.width(), mCurrentRectF.left, mCurrentRectF.bottom)
            } else {
                mTempMatrix.setScale((mCurrentRectF.height() + dy) / mCurrentRectF.height(),
                        (mCurrentRectF.height() + dy) / mCurrentRectF.height(), mCurrentRectF.left, mCurrentRectF.bottom)
            }
            mTempMatrix.mapRect(mCurrentRectF)
        }
        invalidate()
    }
    
    private fun handleScrollWhenLB(distanceX: Float, distanceY: Float) {
        val minWidth = mMinWidth
        val minHeight = mMinHeight
        val left = mLeftEdge
        val bottom = mBottomEdge
        var dx = distanceX
        var dy = distanceY
        if (mCurrentRectF.left - distanceX < left) {
            dx = mCurrentRectF.left - left
        }
        if (mCurrentRectF.width() + distanceX < minWidth) {
            dx = minWidth - mCurrentRectF.width()
        }
        if (mCurrentRectF.bottom - distanceY > bottom) {
            dy = mCurrentRectF.bottom - bottom
        }
        if (mCurrentRectF.height() - distanceY < minHeight) {
            dy = mCurrentRectF.height() - minHeight
        }
        if (dx == 0f && dy == 0f) {
            return
        }
        if (mRatio == 0f) {
            mCurrentRectF.set(mCurrentRectF.left - dx, mCurrentRectF.top, mCurrentRectF.right, mCurrentRectF.bottom - dy)
        } else {
            if (abs(dx) > abs(dy)) {
                mTempMatrix.setScale((mCurrentRectF.width() + dx) / mCurrentRectF.width(),
                        (mCurrentRectF.width() + dx) / mCurrentRectF.width(), mCurrentRectF.right, mCurrentRectF.top)
            } else {
                mTempMatrix.setScale((mCurrentRectF.height() - dy) / mCurrentRectF.height(),
                        (mCurrentRectF.height() - dy) / mCurrentRectF.height(), mCurrentRectF.right, mCurrentRectF.top)
            }
            mTempMatrix.mapRect(mCurrentRectF)
        }
        invalidate()
    }
    
    private fun handleScrollWhenRB(distanceX: Float, distanceY: Float) {
        val minWidth = mMinWidth
        val minHeight = mMinHeight
        val right = mRightEdge
        val bottom = mBottomEdge
        var dx = distanceX
        var dy = distanceY
        if (mCurrentRectF.right - distanceX > right) {
            dx = mCurrentRectF.right - right
        }
        if (mCurrentRectF.width() - distanceX < minWidth) {
            dx = mCurrentRectF.width() - minWidth
        }
        if (mCurrentRectF.bottom - distanceY > bottom) {
            dy = mCurrentRectF.bottom - bottom
        }
        if (mCurrentRectF.height() - distanceY < minHeight) {
            dy = mCurrentRectF.height() - minHeight
        }
        if (dx == 0f && dy == 0f) {
            return
        }
        if (mRatio == 0f) {
            mCurrentRectF.set(mCurrentRectF.left, mCurrentRectF.top, mCurrentRectF.right - dx, mCurrentRectF.bottom - dy)
        } else {
            if (abs(dx) > abs(dy)) {
                mTempMatrix.setScale((mCurrentRectF.width() - dx) / mCurrentRectF.width(),
                        (mCurrentRectF.width() - dx) / mCurrentRectF.width(), mCurrentRectF.left, mCurrentRectF.top)
            } else {
                mTempMatrix.setScale((mCurrentRectF.height() - dy) / mCurrentRectF.height(),
                        (mCurrentRectF.height() - dy) / mCurrentRectF.height(), mCurrentRectF.left, mCurrentRectF.top)
            }
            mTempMatrix.mapRect(mCurrentRectF)
        }
        invalidate()
    }
    
    private fun handleScrollWhenL(distanceX: Float) {
        val minWidth = mMinWidth
        val left = mLeftEdge
        var dx = distanceX
        if (mCurrentRectF.left - distanceX < left) {
            dx = mCurrentRectF.left - left
        }
        if (mCurrentRectF.width() + distanceX < minWidth) {
            dx = minWidth - mCurrentRectF.width()
        }
        if (dx == 0f) {
            return
        }
        if (mRatio == 0f) {
            mCurrentRectF.set(mCurrentRectF.left - dx, mCurrentRectF.top, mCurrentRectF.right, mCurrentRectF.bottom)
        } else {
            mTempMatrix.setScale((mCurrentRectF.width() + dx) / mCurrentRectF.width(),
                    (mCurrentRectF.width() + dx) / mCurrentRectF.width(), mCurrentRectF.right, mCurrentRectF.centerY())
            mTempMatrix.mapRect(mCurrentRectF)
        }
        invalidate()
    }
    
    private fun handleScrollWhenT(distanceY: Float) {
        val minHeight = mMinHeight
        val top = mTopEdge
        var dy = distanceY
        if (mCurrentRectF.top - distanceY < top) {
            dy = mCurrentRectF.top - top
        }
        if (mCurrentRectF.height() + distanceY < minHeight) {
            dy = minHeight - mCurrentRectF.height()
        }
        if (dy == 0f) {
            return
        }
        if (mRatio == 0f) {
            mCurrentRectF.set(mCurrentRectF.left, mCurrentRectF.top - dy, mCurrentRectF.right, mCurrentRectF.bottom)
        } else {
            mTempMatrix.setScale((mCurrentRectF.height() + dy) / mCurrentRectF.height(),
                    (mCurrentRectF.height() + dy) / mCurrentRectF.height(), mCurrentRectF.centerX(), mCurrentRectF.bottom)
            mTempMatrix.mapRect(mCurrentRectF)
        }
        invalidate()
    }
    
    private fun handleScrollWhenR(distanceX: Float) {
        val minWidth = mMinWidth
        val right = mRightEdge
        var dx = distanceX
        if (mCurrentRectF.right - distanceX > right) {
            dx = mCurrentRectF.right - right
        }
        if (mCurrentRectF.width() - distanceX < minWidth) {
            dx = mCurrentRectF.width() - minWidth
        }
        if (dx == 0f) {
            return
        }
        if (mRatio == 0f) {
            mCurrentRectF.set(mCurrentRectF.left, mCurrentRectF.top, mCurrentRectF.right - dx, mCurrentRectF.bottom)
        } else {
            mTempMatrix.setScale((mCurrentRectF.width() - dx) / mCurrentRectF.width(),
                    (mCurrentRectF.width() - dx) / mCurrentRectF.width(), mCurrentRectF.left, mCurrentRectF.centerY())
            mTempMatrix.mapRect(mCurrentRectF)
        }
        invalidate()
    }
    
    private fun handleScrollWhenB(distanceY: Float) {
        val minHeight = mMinHeight
        val bottom = mBottomEdge
        var dy = distanceY
        if (mCurrentRectF.bottom - distanceY > bottom) {
            dy = mCurrentRectF.bottom - bottom
        }
        if (mCurrentRectF.height() - distanceY < minHeight) {
            dy = mCurrentRectF.height() - minHeight
        }
        if (dy == 0f) {
            return
        }
        if (mRatio == 0f) {
            mCurrentRectF.set(mCurrentRectF.left, mCurrentRectF.top, mCurrentRectF.right, mCurrentRectF.bottom - dy)
        } else {
            mTempMatrix.setScale((mCurrentRectF.height() - dy) / mCurrentRectF.height(),
                    (mCurrentRectF.height() - dy) / mCurrentRectF.height(), mCurrentRectF.centerX(), mCurrentRectF.top)
            mTempMatrix.mapRect(mCurrentRectF)
        }
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mDrawRectF.set(mCurrentRectF.left, mCurrentRectF.top, mCurrentRectF.right, mCurrentRectF.bottom)
        // 绘制线框,考虑线宽影响
        mDrawRectF.inset(-mInnerLineStroke / 2, -mInnerLineStroke / 2)
        canvas.drawRect(mDrawRectF, mInnerPaint)
        mDrawRectF.inset(mInnerLineStroke / 2, mInnerLineStroke / 2)
        
        // 绘制辅助线
        val w = mDrawRectF.width()
        val h = mDrawRectF.height()
        canvas.drawLine(mDrawRectF.left, mDrawRectF.top + h / 3, mDrawRectF.right, mDrawRectF.top + h / 3, mGuideLinePaint)
        canvas.drawLine(mDrawRectF.left, mDrawRectF.bottom - h / 3, mDrawRectF.right, mDrawRectF.bottom - h / 3, mGuideLinePaint)
        canvas.drawLine(mDrawRectF.left + w / 3, mDrawRectF.top, mDrawRectF.left + w / 3, mDrawRectF.bottom, mGuideLinePaint)
        canvas.drawLine(mDrawRectF.right - w / 3, mDrawRectF.top, mDrawRectF.right - w / 3, mDrawRectF.bottom, mGuideLinePaint)
        
        // 绘制外边框
        mDrawRectF.inset(-mInnerLineStroke - mOutLineStroke + 1f, -mInnerLineStroke - mOutLineStroke + 1f)
        canvas.drawRect(mDrawRectF.left, mDrawRectF.top, mDrawRectF.left + mOutLineLength, mDrawRectF.top + mOutLineStroke, mOutPaint)
        canvas.drawRect(mDrawRectF.left, mDrawRectF.top, mDrawRectF.left + mOutLineStroke, mDrawRectF.top + mOutLineLength, mOutPaint)
        canvas.drawRect(mDrawRectF.right - mOutLineLength, mDrawRectF.top, mDrawRectF.right, mDrawRectF.top + mOutLineStroke, mOutPaint)
        canvas.drawRect(mDrawRectF.right - mOutLineStroke, mDrawRectF.top, mDrawRectF.right, mDrawRectF.top + mOutLineLength, mOutPaint)
        canvas.drawRect(mDrawRectF.left, mDrawRectF.bottom - mOutLineStroke, mDrawRectF.left + mOutLineLength, mDrawRectF.bottom, mOutPaint)
        canvas.drawRect(mDrawRectF.left, mDrawRectF.bottom - mOutLineLength, mDrawRectF.left + mOutLineStroke, mDrawRectF.bottom, mOutPaint)
        canvas.drawRect(mDrawRectF.right - mOutLineLength, mDrawRectF.bottom - mOutLineStroke, mDrawRectF.right, mDrawRectF.bottom, mOutPaint)
        canvas.drawRect(mDrawRectF.right - mOutLineStroke, mDrawRectF.bottom - mOutLineLength, mDrawRectF.right, mDrawRectF.bottom, mOutPaint)
        canvas.drawRect(mDrawRectF.centerX() - mOutLineLength / 2, mDrawRectF.top, mDrawRectF.centerX() + mOutLineLength / 2,
                mDrawRectF.top + mOutLineStroke, mOutPaint)
        canvas.drawRect(mDrawRectF.left, mDrawRectF.centerY() - mOutLineLength / 2, mDrawRectF.left + mOutLineStroke,
                mDrawRectF.centerY() + mOutLineLength / 2, mOutPaint)
        canvas.drawRect(mDrawRectF.centerX() - mOutLineLength / 2, mDrawRectF.bottom - mOutLineStroke,
                mDrawRectF.centerX() + mOutLineLength / 2, mDrawRectF.bottom, mOutPaint)
        canvas.drawRect(mDrawRectF.right - mOutLineStroke, mDrawRectF.centerY() - mOutLineLength / 2,
                mDrawRectF.right, mDrawRectF.centerY() + mOutLineLength / 2, mOutPaint)
        mDrawRectF.inset(mInnerLineStroke + mOutLineStroke - 1f, mInnerLineStroke + mOutLineStroke - 1f)
    }
    
    /**
     * 改变裁剪框比例
     * */
    fun changeCropFrameByRatio(ratio: Float) {
        if (mRatio != ratio) {
            mRatio = ratio
            val rectF = getCropFrameRectFByScale(ratio)
            mCurrentRectF.set(rectF)
            invalidate()
            
            updateEdge(ratio)
        }
    }
    
    /**
     * 原始模式, 裁剪框按照图片比例进行缩放
     * */
    fun changeCropFrameToOrigin() {
        val ratio = 1920f / 2560f
        if (mRatio != ratio) {
            mRatio = ratio
            if (mCurrentRectF != mBaseRectF) {
                mCurrentRectF.set(mBaseRectF)
                invalidate()
            }
            updateEdge(ratio)
        }
    }
    
    /**
     * 自由模式
     * */
    fun changeCropFrameToFreedom() {
        if (mRatio != 0f) {
            mRatio = 0f
            if (mCurrentRectF != mBaseRectF) {
                mCurrentRectF.set(mBaseRectF)
                invalidate()
            }
            updateEdge(0f)
        }
    }
    
    private fun updateEdge(ratio: Float) {
        // mMaxWidth = mCurrentRectF.width()
        // mMaxHeight = mCurrentRectF.height()
        mLeftEdge = mCurrentRectF.left
        mTopEdge = mCurrentRectF.top
        mRightEdge = mCurrentRectF.right
        mBottomEdge = mCurrentRectF.bottom
        if (ratio >= 1f) {
            mMinWidth = mMinCropFrameLength * ratio
            mMinHeight = minCropFrameLength
        } else if (ratio < 0f && ratio < 1f) {
            mMinWidth = mMinCropFrameLength
            mMinHeight = mMinCropFrameLength / ratio
        } else {
            mMinWidth = mMinCropFrameLength
            mMinHeight = mMinCropFrameLength
        }
    }
    
    companion object {
        private val minCropFrameLength = 66f.dp() // 裁剪框最小长度
        
        // 原始裁剪框大小
        val origin by lazy {
            val left = 24f.dp()
            val top = 76f.dp()
            val bottom = 227f.dp()
            val width = ScreenUtils.realScreenWidth - left - left
            val height = ScreenUtils.realScreenHeight - top - bottom
            RectF(left, top, left + width, top + height)
        }
        
        fun getCropFrameRectFByScale(scale: Float): RectF {
            val w: Float
            val h: Float
            if (scale > 1f) {
                w = scale
                h = 1f
            } else {
                w = 1f
                h = 1f / scale
            }
            val matrix = Matrix()
            val result = RectF(0f, 0f, w, h)
            matrix.setRectToRect(result, origin, Matrix.ScaleToFit.CENTER)
            matrix.mapRect(result)
            return result
        }
    }
    
    enum class TouchType(var meaning: String) {
        NONE("ignore"),
        LT("top left corner"),
        RT("right top corner"),
        LB("left bottom corner"),
        RB("right bottom corner"),
        LEFT("left edge"),
        RIGHT("right edge"),
        TOP("top edge"),
        BOTTOM("bottom edge")
    }
}