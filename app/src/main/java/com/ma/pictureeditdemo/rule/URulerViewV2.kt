package com.ma.pictureeditdemo.rule

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.annotation.ColorInt
import com.ma.pictureeditdemo.extensions.dp
import kotlin.math.abs
import kotlin.math.min

/**
 * @Description: 标尺, 0刻度居中显示, 当有负刻度时,最小与最大刻度必须是相反数
 * @Author: JunYi.Ma
 * @Date: 2022/5/21 0021-14:03
 * @Email:  junyi.ma@upuphone.com
 */
class URulerViewV2 @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    
    private var mTotalGridNumber = 40 // 总格数
    private var mMinScale = 0 // 最小刻度值
    private var mMaxScale = 100 // 最大刻度值
    private var mGap = 8f.dp() // 每格宽度
    private var mScaleLineWidth = 1f.dp() // 刻度宽
    
    @ColorInt
    private var mMinScaleLineColor = Color.parseColor("#4DFFFFFF") // 小刻度线颜色
    private var mMinScaleLineHeight = 6f.dp() // 小刻度线高
    
    @ColorInt
    private var mMaxScaleLineColor = Color.WHITE // 大刻度线颜色
    private var mMaxScaleLineHeight = 6f.dp() // 大刻度线高
    
    @ColorInt
    private var mIndicatorLineColor = Color.parseColor("#FFBA01")
    private var mIndicatorLineHeight = 36f.dp()
    
    private var mMinScaleLinePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        isDither = true
        color = mMinScaleLineColor
        strokeWidth = mScaleLineWidth
    }
    
    private var mMaxScaleLinePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        isDither = true
        color = mMaxScaleLineColor
        strokeWidth = mScaleLineWidth
    }
    
    private var mIndicatorLinePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        isDither = true
        color = mIndicatorLineColor
        strokeWidth = mScaleLineWidth
    }
    
    private val mTouchSlop: Int
    private var mScroller: OverScroller
    private var mIsBeingDragged = false
    private var mLastPointX = 0
    private val mOverDistance = 200 // 回弹距离
    
    private var mListener: ((value: Float) -> Unit)? = null // 刻度变化监听
    private var mUpListener: ((value: Float) -> Unit)? = null // 抬手变化监听
    
    init {
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledTouchSlop
        mScroller = OverScroller(context)
    }
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val range = getScrollRange()
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!mScroller.isFinished) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    mScroller.abortAnimation()
                }
                
                mLastPointX = event.x.toInt()
            }
            
            MotionEvent.ACTION_MOVE -> {
                var deltaX = mLastPointX - event.x
                
                if (!mIsBeingDragged && abs(deltaX) > mTouchSlop) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    mIsBeingDragged = true
                    if (deltaX > 0) {
                        deltaX -= mTouchSlop
                    } else {
                        deltaX += mTouchSlop
                    }
                }
                
                if (mIsBeingDragged) {
                    mLastPointX = event.x.toInt()
                    overScrollBy(deltaX.toInt(), 0, scrollX, 0, range, 0, mOverDistance, 0, true)
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 回弹
                if (mIsBeingDragged) {
                    if (mScroller.springBack(scrollX, scrollY, computeLeftEdge(),
                                getScrollRange(), 0, 0)
                    ) {
                        postInvalidateOnAnimation()
                    }
                    endDrag()
                }
                onUp()
            }
        }
        return true
    }
    
    private fun onUp() {
        var value = getCurrentScaleValue()
        if (value < mMinScale) {
            value = mMinScale.toFloat()
        } else if (value > mMaxScale) {
            value = mMaxScale.toFloat()
        }
        mUpListener?.invoke(value)
    }
    
    /**
     * 重写,主要是调整左右边侧移动临界值
     */
    override fun overScrollBy(
            deltaX: Int,
            deltaY: Int,
            scrollX: Int,
            scrollY: Int,
            scrollRangeX: Int,
            scrollRangeY: Int,
            maxOverScrollX: Int,
            maxOverScrollY: Int,
            isTouchEvent: Boolean,
    ): Boolean {
        // 去掉滚动条相关
        var newScrollX = scrollX + deltaX
        var newScrollY = scrollY + deltaY
        
        // this is different with native View
        val left = -maxOverScrollX + computeLeftEdge()
        val right = maxOverScrollX + scrollRangeX
        
        val top = -maxOverScrollY
        val bottom = maxOverScrollY + scrollRangeY
        var clampedX = false
        if (newScrollX > right) {
            newScrollX = right
            clampedX = true
        } else if (newScrollX < left) {
            newScrollX = left
            clampedX = true
        }
        
        var clampedY = false
        if (newScrollY > bottom) {
            newScrollY = bottom
            clampedY = true
        } else if (newScrollY < top) {
            newScrollY = top
            clampedY = true
        }
        
        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY)
        
        return clampedX || clampedY
    }
    
    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            val oldX = scrollX
            val oldY = scrollY
            val x = mScroller.currX
            val y = mScroller.currY
            if (oldX != x || oldY != y) {
                overScrollBy(
                        x - oldX, y - oldY, oldX, oldY,
                        getScrollRange(), 0, mOverDistance, 0, false
                )
            } else if (mScroller.isOverScrolled) {
                //跳过此次滚动,进行下一次的计算
                postInvalidateOnAnimation()
            }
        }
    }
    
    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.scrollTo(scrollX, scrollY)
    }
    
    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (l < computeLeftEdge() || l > getScrollRange()) return
        // 计算回调刻度
        mListener?.invoke(getCurrentScaleValue())
    }
    
    /**
     * 刻度变化监听
     */
    fun setScaleChangedListener(listener: ((value: Float) -> Unit)?) {
        this.mListener = listener
    }
    
    /**
     * 刻度抬手监听
     */
    fun setScaleUpListener(listener: ((value: Float) -> Unit)?) {
        this.mUpListener = listener
    }
    
    /**
     * 设置刻度范围
     */
    fun setScaleLimit(min: Int, max: Int) {
        this.mMinScale = min
        this.mMaxScale = max
        scrollX = 0
        invalidate()
    }
    
    /**
     * 设置当前居中刻度
     */
    fun setCenterScale(scale: Int) {
        if (scale in mMinScale..mMaxScale) {
            var length = getLengthBetweenScales(0, scale).toInt()
            val left = computeLeftEdge()
            val right = getScrollRange()
            if (length < left) {
                length = left
            }
            if (length > right) {
                length = right
            }
            scrollTo(length, 0)
        }
    }
    
    private fun getCurrentScaleValue(): Float {
        val gridNum = scrollX / (mGap + mScaleLineWidth)
        return gridNum * getPerGridScaleValue()
    }
    
    private fun endDrag() {
        mIsBeingDragged = false
    }
    
    private fun getScrollRange(): Int {
        val range = if (mMinScale < 0) (getLengthBetweenScales() - mScaleLineWidth) / 2
        else getLengthBetweenScales() - mScaleLineWidth
        return range.toInt()
    }
    
    // 左边界
    private fun computeLeftEdge(): Int {
        return if (mMinScale < 0) -getScrollRange() else 0
    }
    
    /**
     * 每格表示多少刻度
     * */
    private fun getPerGridScaleValue(): Float = (mMaxScale - mMinScale).toFloat() / mTotalGridNumber
    
    /**
     * 两刻度之间长度,默认刻度总长
     */
    private fun getLengthBetweenScales(before: Int = mMinScale, after: Int = mMaxScale): Float {
        if (before >= after) return 0f
        val total = mMaxScale - mMinScale
        val current = after - before
        val gapNum = current.toFloat() / total * mTotalGridNumber
        return gapNum * mGap + (gapNum + 1) * mScaleLineWidth
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val start = if (mMinScale < 0) -mTotalGridNumber / 2 else 0
        val end = if (mMinScale < 0) mTotalGridNumber / 2 else mTotalGridNumber
        val base = width / 2f
        var h: Float
        val i = scrollX / (mGap + mScaleLineWidth).toInt()
        val j = scrollX % (mGap + mScaleLineWidth) / (mGap + mScaleLineWidth)
        
        for (index in start..end) {
            val x = base + index * (mScaleLineWidth + mGap)
            h = if (index in -4 + i..4 + i) {
                if (index < i) min(obtainHeight(index - i - j, true), mIndicatorLineHeight)
                else if (index > i) min(obtainHeight(index - i - j, false), mIndicatorLineHeight)
                else min(obtainHeight(abs(j), false), mIndicatorLineHeight)
            } else {
                mMinScaleLineHeight
            }
            if (index % 5 == 0) {
                canvas.drawLine(x, height.toFloat(), x, height - h, mMaxScaleLinePaint)
            } else {
                canvas.drawLine(x, height.toFloat(), x, height - h, mMinScaleLinePaint)
            }
        }
        
        canvas.save()
        canvas.translate(scrollX.toFloat(), 0f)
        canvas.drawLine(width / 2f, height.toFloat(), width / 2f, height - mIndicatorLineHeight, mIndicatorLinePaint)
        canvas.restore()
    }
    
    private fun obtainHeight(x: Float, isPositive: Boolean): Float {
        val a = if (isPositive) (mIndicatorLineHeight - mMaxScaleLineHeight) / 5 else (mMaxScaleLineHeight - mIndicatorLineHeight) / 5
        val b = mIndicatorLineHeight
        return a * x + b
    }
}