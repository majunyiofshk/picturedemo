package com.ma.pictureeditdemo.rule

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.annotation.ColorInt
import com.ma.pictureeditdemo.R
import com.ma.pictureeditdemo.extensions.dp2px
import kotlin.math.abs

class URulerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mListener: ((value: Float) -> Unit)? = null
    private var mMinScale: Int = -45 //最小刻度值
    private var mMaxScale: Int = 45 //最大刻度值
    private var mScalePerGap = 1 //每格表示几个刻度
    private var mCountForMaxScale = 5 // 多少格表示一个大刻度
    private var mCenterScale = 0 // 居中显示的刻度
    var mCurrentScale: Float = mCenterScale.toFloat() // 当前刻度
        private set

    private val mScaleWidth: Int = dp2px(1) //刻度的宽
    private var mSmallScaleHeight: Float = dp2px(10).toFloat()

    @ColorInt
    private var mSmallScaleColor: Int = Color.parseColor("#B2FFFFFF")

    @ColorInt
    private var mBigScaleColor: Int = Color.WHITE

    private var mMiddleScaleHeight: Float = dp2px(36).toFloat()

    @ColorInt
    private var mMiddleScaleColor: Int = Color.parseColor("#FFBA01")

    private val mGap: Int = dp2px(8) // 刻度间隔

    private var mScalePaint: Paint

    private var mWidth: Int = 0
    private var mHeight: Int = 0

    private val mTouchSlop: Int
    private var mScroller: OverScroller
    private var mIsBeingDragged = false
    private var mLastPointX = 0
    private val mOverDistance = 200 // 回弹距离

    init {
        // 自定义属性
        attrs?.let {
            val a = getContext().obtainStyledAttributes(it, R.styleable.URulerView)
            mMinScale = a.getInteger(R.styleable.URulerView_rulerMinScale, mMinScale)
            mMaxScale = a.getInteger(R.styleable.URulerView_rulerMaxScale, mMaxScale)
            mCenterScale = a.getInteger(R.styleable.URulerView_rulerCenterScale, mCenterScale)
            mScalePerGap = a.getInteger(R.styleable.URulerView_rulerScalePerGap, mScalePerGap)
            mCountForMaxScale = a.getInteger(R.styleable.URulerView_rulerCountForMaxScale, mCountForMaxScale)
            mSmallScaleHeight = a.getDimension(R.styleable.URulerView_rulerSmallScaleHeight, mSmallScaleHeight)
            mMiddleScaleHeight = a.getDimension(R.styleable.URulerView_rulerMiddleScaleHeight, mMiddleScaleHeight)
            mSmallScaleColor = a.getColor(R.styleable.URulerView_rulerSmallScaleColor, mSmallScaleColor)
            mMiddleScaleColor = a.getColor(R.styleable.URulerView_rulerMiddleScaleColor, mMiddleScaleColor)
            mBigScaleColor = a.getColor(R.styleable.URulerView_rulerBigScaleColor, mBigScaleColor)
            a.recycle()
        }
        
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledTouchSlop

        mScroller = OverScroller(context)
        mScalePaint = Paint()
        mScalePaint.let {
            it.style = Paint.Style.FILL
            it.isAntiAlias = true
            it.isDither = true
            it.strokeWidth = mScaleWidth.toFloat()
        }

        // 忽略padding影响
        setPadding(0, 0, 0, 0)
    }

    private fun getScrollRange(): Int {
        return getLengthBetweenScales() - mWidth
    }

    //左右
    private fun computeLeftEdge(): Int =  -(mWidth - mScaleWidth) / 2
    private fun computeRightEdge(): Int = (mWidth - mScaleWidth) / 2

    /**
     * 两刻度之间长度,默认刻度总长
     */
    private fun getLengthBetweenScales(before: Int = mMinScale, after: Int = mMaxScale): Int{
        val gapNum = (after - before) / mScalePerGap
        return if (gapNum == 0) 0 else gapNum * mGap + (gapNum + 1) * mScaleWidth
    }

    /**
     *
     * 绘制是从左最小刻度开始,初始化时要想居中显示某个刻度,需要做个偏移
     */
    private fun getBeginOffset() : Int{
        val total = getLengthBetweenScales()
        val middleScale = (mMinScale + mMaxScale) / 2
        val delta = getLengthBetweenScales(middleScale, mCenterScale)
        return (total - mWidth) / 2 + delta
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        // 超出边界不回调
        if (l < computeLeftEdge()) return
        if (l > getScrollRange() + computeRightEdge()) return

        val  firstScroll = getBeginOffset()
//        val toScale = (mCenterScale + (l - firstScroll).toFloat() / (mGap + mScaleWidth) * mScalePerGap).roundToInt()
//        if (mCurrentScale != toScale){
//            mListener?.invoke(toScale)
//            mCurrentScale = toScale
//        }
        val toScale = (mCenterScale + (l - firstScroll).toFloat() / (mGap + mScaleWidth) * mScalePerGap)
        mListener?.invoke(toScale)
        mCurrentScale = toScale
    }

    fun setScaleChangedListener(listener: ((value: Float) -> Unit)?){
        this.mListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mWidth = w
        mHeight = h
        scrollX = getBeginOffset()
        Log.e("onSizeChanged", "left = ${computeLeftEdge()}, right = ${computeRightEdge()}, mTouchSlop = $mTouchSlop")
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
                    overScrollBy(
                        deltaX.toInt(), 0, scrollX, 0, range, 0,
                        mOverDistance, 0, true
                    )
                }
            }

            MotionEvent.ACTION_UP -> {
                // 回弹
                if (mIsBeingDragged) {
                    if (mScroller.springBack(scrollX, scrollY, computeLeftEdge()  ,
                            getScrollRange() + computeRightEdge(), 0, 0)){
                        postInvalidateOnAnimation()
                    }
                    endDrag()
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                // 回弹
                if (mIsBeingDragged) {
                    if (mScroller.springBack(scrollX, scrollY, -mWidth / 2,
                            getScrollRange() + computeRightEdge(), 0, 0)){
                        postInvalidateOnAnimation()
                    }
                    endDrag()
                }
            }
        }
        return true
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
        isTouchEvent: Boolean
    ): Boolean {
        // 去掉滚动条相关
        var newScrollX = scrollX + deltaX
        var newScrollY = scrollY + deltaY

        // this is different with native View
        val left = -maxOverScrollX + computeLeftEdge()
        val right = maxOverScrollX + scrollRangeX + computeRightEdge()

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
            }else if (mScroller.isOverScrolled){
                //跳过此次滚动,进行下一次的计算
                postInvalidateOnAnimation()
            }
        }
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.scrollTo(scrollX, scrollY)
    }

    private fun endDrag() {
        mIsBeingDragged = false
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        //当前刻度所在的位置
        var index = 0
        //当前刻度的索引值
        var lineCount = 0

        val distance: Float = when{
            scrollX < computeLeftEdge()-> (computeLeftEdge() - getBeginOffset()).toFloat()
            scrollX > getScrollRange() + computeRightEdge() -> (getScrollRange() + computeRightEdge() - getBeginOffset()).toFloat()
            else -> (scrollX - getBeginOffset()).toFloat()
        }
        val v =if (distance < 0) distance / (mGap + mScaleWidth) -0.5f
            else distance / (mGap + mScaleWidth) + 0.5f
        // 绘制大刻度与小刻度
        while (index <= getLengthBetweenScales()) {
            var height: Float = if ((lineCount % mCountForMaxScale) == 0 || lineCount == 0 ) mSmallScaleHeight
            else mSmallScaleHeight
            // 波峰处理
            if (index == ((mMaxScale - mMinScale) / 2 - 4 + v.toInt()) * (mGap + mScaleWidth)) height = dp2px(8).toFloat()
            if (index == ((mMaxScale - mMinScale) / 2 - 3 + v.toInt()) * (mGap + mScaleWidth)) height = dp2px(10).toFloat()
            if (index == ((mMaxScale - mMinScale) / 2 - 2 + v.toInt()) * (mGap + mScaleWidth)) height = dp2px(16).toFloat()
            if (index == ((mMaxScale - mMinScale) / 2 - 1 + v.toInt()) * (mGap + mScaleWidth)) height = dp2px(24).toFloat()
            if (index == ((mMaxScale - mMinScale) / 2 + v.toInt()) * (mGap + mScaleWidth)) height = dp2px(30).toFloat()
            if (index == ((mMaxScale - mMinScale) / 2 + 1 + v.toInt()) * (mGap + mScaleWidth)) height = dp2px(24).toFloat()
            if (index == ((mMaxScale - mMinScale) / 2 + 2 + v.toInt()) * (mGap + mScaleWidth)) height = dp2px(16).toFloat()
            if (index == ((mMaxScale - mMinScale) / 2 + 3 + v.toInt()) * (mGap + mScaleWidth)) height = dp2px(10).toFloat()
            if (index == ((mMaxScale - mMinScale) / 2 + 4 + v.toInt()) * (mGap + mScaleWidth)) height = dp2px(8).toFloat()

            val color = when {
                (lineCount % mCountForMaxScale) == 0 || lineCount == 0 -> mBigScaleColor
                else -> mSmallScaleColor
            }
            mScalePaint.color = color
            canvas?.drawLine(
                index.toFloat(),
                mHeight.toFloat(),
                index.toFloat(),
                mHeight - height,
                mScalePaint
            )
            lineCount++
            index += (mGap + mScaleWidth)
        }

        //绘制中间刻度
        mScalePaint.color = mMiddleScaleColor
        canvas?.let {
            it.save()
            it.translate(scrollX.toFloat() , 0f)
            it.drawLine(((mWidth - mScaleWidth) / 2).toFloat(), mHeight.toFloat(),((mWidth - mScaleWidth) / 2).toFloat(),
                mHeight - mMiddleScaleHeight.toFloat(),mScalePaint)
            it.restore()
        }
    }
}