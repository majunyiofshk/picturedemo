package com.ma.pictureeditdemo.box

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.Matrix.ScaleToFit
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.ma.pictureeditdemo.extensions.dp2px
import kotlin.math.abs

/*
* 裁剪框
* */
class UFrameView constructor(var cropPhotoView: CropPhotoView) {
    companion object{
        //裁剪框最小长
        var mMinFrameWith: Float = 0f
        var mMinFrameHeight: Float = 0f
        //裁剪框最大宽高(原始)
        var mMaxFrameWith: Float = 0f
        var mMaxFrameHeight: Float = 0f
        //原始裁剪框的top和bottom
        var mOriginTop: Float = 0f
        var mOriginBottom: Float = 0f
    }
    private val mLinePaint: Paint = Paint() // 线框
    private val mAngleLinePaint: Paint = Paint() // 四个角
    private val mGuideLinePaint: Paint = Paint() // 辅助线

    private var mLineStrokeWidth: Float = cropPhotoView.dp2px(1).toFloat()
    private var mAngleLineStrokeWidth: Float = cropPhotoView.dp2px(3).toFloat()
    private var mAngleLineStrokeLength: Float = cropPhotoView.dp2px(26).toFloat()
    private var mGuideLineStrokeWidth: Float = cropPhotoView.dp2px(1).toFloat()

    private var mLineColor = Color.WHITE
    private var mAngleLineColor = Color.WHITE
    private var mGuideLineColor = Color.parseColor("#7FFFFFFF")

    private var mDrawRectF: RectF =  RectF()
    private var mCurrentRectF: RectF = RectF() //裁剪框当前区域(不包括线框宽)
    private var mCurrentInitRectF: RectF = RectF() //裁剪框当前比例区域

    private var mScale: Float = 0f // 等于0表示自由模式
    private val mMinFrameLength = cropPhotoView.dp2px(66).toFloat()

    init {
        // 初始化裁剪框最小最大宽高
        mMinFrameWith = mMinFrameLength
        mMinFrameHeight = mMinFrameLength
        mMaxFrameWith = cropPhotoView.dp2px(312).toFloat()
        mMaxFrameHeight = cropPhotoView.dp2px(501).toFloat()
        mOriginTop = cropPhotoView.dp2px(76).toFloat()
        mOriginBottom = cropPhotoView.dp2px(227).toFloat()

        mLinePaint.let {
            it.style = Paint.Style.STROKE
            it.color = mLineColor
            it.isAntiAlias = true
            it.isDither = true
            it.strokeWidth = mLineStrokeWidth
            it.strokeCap = Paint.Cap.SQUARE
        }
        mAngleLinePaint.let {
            it.style = Paint.Style.STROKE
            it.color = mAngleLineColor
            it.isAntiAlias = true
            it.isDither = true
            it.strokeWidth = mAngleLineStrokeWidth
            it.strokeCap = Paint.Cap.SQUARE
        }
        mGuideLinePaint.let {
            it.style = Paint.Style.STROKE
            it.color = mGuideLineColor
            it.isAntiAlias = true
            it.isDither = true
            it.strokeWidth = mGuideLineStrokeWidth
        }
    }

    /*
    * 自由模式
    * */
    fun adjustToFreedom(){
        if (mScale != 0f){
            mScale = 0f
            mMinFrameWith = mMinFrameLength
            mMinFrameHeight = mMinFrameLength
        }
    }

    /*
    * 原始模式
    * 裁剪框按照图片比例进行移动缩放
    * */
    fun adjustToOrigin(){
        val drawableWidth = cropPhotoView.drawable.intrinsicWidth.toFloat()
        val drawableHeight = cropPhotoView.drawable.intrinsicHeight.toFloat()
        val scale = drawableWidth / drawableHeight
        if (mScale != scale){
            mScale = scale
            if (scale > 1f) mMinFrameWith = mMinFrameLength * scale
            else mMinFrameHeight = mMinFrameLength / scale
        }
    }

    /*
    * 比例模式
    * 裁剪框按照给定比例进行移动缩放
    * */
    fun adjustByScale(scale: Float){
        if (mScale != scale){
            mScale = scale
            if (scale > 1f) mMinFrameWith = mMinFrameLength * scale
            else mMinFrameHeight = mMinFrameLength / scale
            updateForScale(getRectFByScale(scale))
            // 是否调整图片
            cropPhotoView.mAttach.adjustImageOfFrameScale(mCurrentRectF);
        }
    }
    /*
    * 重置
    * */
    fun reset(){
        mScale = 0f
        mMinFrameWith = mMinFrameLength
        mMinFrameHeight = mMinFrameLength
        updateForScale(getFreedomRectF())
    }

    fun rotateByVertical(){
        val r = RectF(0f, 0f, mCurrentRectF.height(), mCurrentRectF.width())
        val matrix = Matrix()
        matrix.setRectToRect(r, getOriginRectF(), ScaleToFit.CENTER)
        matrix.mapRect(r)
        cropPhotoView.mAttach.rotateOfFrame(mCurrentRectF, r)
        updateForScale(r)
    }

    private fun updateForScale(r: RectF, isSizeChanged: Boolean = false){
        mCurrentInitRectF.set(r)
        mCurrentRectF.set(r)
        // 更新触摸区域
        cropPhotoView.mAttach.adjustAttachRegion(r)
        if (!isSizeChanged) cropPhotoView.postInvalidate()
    }

    private fun  updateForTouch(r: RectF){
        mCurrentRectF.set(r)
        cropPhotoView.postInvalidate()
    }

    /*
    * 原始模式边框区域
    * */
    fun getOriginRectF(): RectF{
        val r = RectF(0f, 0f, mMaxFrameWith, mMaxFrameHeight)
        val dx = (cropPhotoView.width - mMaxFrameWith) / 2
        val dy = mOriginTop
        r.offset(dx, dy)
        return r
    }

    /*
    * 自由模式边框区域
    * */
    private fun getFreedomRectF(): RectF{
        val drawableWidth = cropPhotoView.drawable.intrinsicWidth.toFloat()
        val drawableHeight = cropPhotoView.drawable.intrinsicHeight.toFloat()
//        val matrix = Matrix()
//        val result = RectF(0f, 0f, drawableWidth, drawableHeight)
//        matrix.setRectToRect(result, getOriginRectF(), ScaleToFit.CENTER)
//        matrix.mapRect(result)
//        return result
        return getRectFByScale(drawableWidth / drawableHeight)
    }

    /*
    * 获取指定比例的边框区域, 以原始边框区域为目标进行缩放
    * */
    private fun getRectFByScale(scale: Float): RectF{
        var w = mMinFrameWith
        var h = mMaxFrameHeight
        if (w / h > scale) w = h * scale
        else h = w / scale
        val matrix = Matrix()
        val result = RectF(0f, 0f, w, h)
        matrix.setRectToRect(result, getOriginRectF(), ScaleToFit.CENTER)
        matrix.mapRect(result)
        return result
    }

    fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updateForScale(getFreedomRectF(), true)
        Log.e("onSizeChanged", "drawable : w = ${ cropPhotoView.drawable.intrinsicWidth}, " +
                "h = ${ cropPhotoView.drawable.intrinsicHeight}")
    }

    fun draw(canvas: Canvas?) {
        //绘制四个角, mCurrentRectF 表示内边框区域, 绘制时移动到外边框
        mDrawRectF.set(mCurrentRectF.left, mCurrentRectF.top, mCurrentRectF.right, mCurrentRectF.bottom)
        mDrawRectF.inset(-mLineStrokeWidth - 1f , -mLineStrokeWidth - 1f)
        canvas?.let {
            it.drawLine(
                mDrawRectF.left,
                mDrawRectF.top,
                mDrawRectF.left + mAngleLineStrokeLength,
                mDrawRectF.top,
                mAngleLinePaint
            )
            it.drawLine(
                mDrawRectF.left,
                mDrawRectF.top,
                mDrawRectF.left,
                mDrawRectF.top + mAngleLineStrokeLength,
                mAngleLinePaint
            )

            it.drawLine(
                mDrawRectF.right,
                mDrawRectF.top,
                mDrawRectF.right - mAngleLineStrokeLength,
                mDrawRectF.top,
                mAngleLinePaint
            )
            it.drawLine(
                mDrawRectF.right,
                mDrawRectF.top,
                mDrawRectF.right,
                mDrawRectF.top + mAngleLineStrokeLength,
                mAngleLinePaint
            )

            it.drawLine(
                mDrawRectF.left,
                mDrawRectF.bottom,
                mDrawRectF.left + mAngleLineStrokeLength,
                mDrawRectF.bottom,
                mAngleLinePaint
            )
            it.drawLine(
                mDrawRectF.left,
                mDrawRectF.bottom,
                mDrawRectF.left,
                mDrawRectF.bottom - mAngleLineStrokeLength,
                mAngleLinePaint
            )

            it.drawLine(
                mDrawRectF.right,
                mDrawRectF.bottom,
                mDrawRectF.right - mAngleLineStrokeLength,
                mDrawRectF.bottom,
                mAngleLinePaint
            )
            it.drawLine(
                mDrawRectF.right,
                mDrawRectF.bottom,
                mDrawRectF.right,
                mDrawRectF.bottom - mAngleLineStrokeLength,
                mAngleLinePaint
            )
        }
        // 4个角绘制后再移回去
        mDrawRectF.inset(mLineStrokeWidth + 1f , mLineStrokeWidth + 1f)
        // 绘制线框,考虑线宽影响
        mDrawRectF.inset(mLineStrokeWidth / 2, mLineStrokeWidth / 2)
        canvas?.drawRect(mDrawRectF, mLinePaint)
        mDrawRectF.inset(-mLineStrokeWidth / 2, -mLineStrokeWidth / 2)
        // 绘制辅助线
        val w = mDrawRectF.width()
        val h = mDrawRectF.height()
        mDrawRectF.inset(mGuideLineStrokeWidth / 2, mGuideLineStrokeWidth / 2)
        canvas?.let {
            it.drawLine(
                mDrawRectF.left,
                mDrawRectF.top + h / 3,
                mDrawRectF.right,
                mDrawRectF.top + h / 3,
                mGuideLinePaint
            )
            it.drawLine(
                mDrawRectF.left,
                mDrawRectF.top + 2 * h / 3,
                mDrawRectF.right,
                mDrawRectF.top + 2 * h / 3,
                mGuideLinePaint
            )
            it.drawLine(
                mDrawRectF.left + w / 3,
                mDrawRectF.top,
                mDrawRectF.left + w / 3,
                mDrawRectF.bottom,
                mGuideLinePaint
            )
            it.drawLine(
                mDrawRectF.left + 2 * w / 3,
                mDrawRectF.top,
                mDrawRectF.left + 2 * w / 3,
                mDrawRectF.bottom,
                mGuideLinePaint
            )
        }
        mDrawRectF.inset(-mGuideLineStrokeWidth / 2, -mGuideLineStrokeWidth / 2)
    }

    inner class Attach constructor(cropPhotoView: CropPhotoView) :
        PhotoViewAttach(cropPhotoView), GestureDetector.OnGestureListener {
        // 默认裁剪框四个角的触摸区域大小是以粗线框长为边长的矩形
        private val mTouchSlop = mAngleLineStrokeLength.toInt()

        //8个触摸区域
        private var mLeftTopRegion: Region = Region()
        private var mRightTopRegion: Region = Region()
        private var mLeftBottomRegion: Region = Region()
        private var mRightBottomRegion: Region = Region()
        private var mLeftRegion: Region = Region()
        private var mRightRegion: Region = Region()
        private var mTopRegion: Region = Region()
        private var mBottomRegion: Region = Region()

        private var mTouchType: TouchType = TouchType.NONE

        private var mGestureDetector: GestureDetector = GestureDetector(cropPhotoView.context, this)

        private val mMatrix = Matrix()
        private var mIsScrolled = false // 抬手时判断此次是否滑动过
        private var mIsInAnimation = false

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, ev: MotionEvent?): Boolean {
            if (ev?.action == MotionEvent.ACTION_DOWN) {
                val x = ev.x.toInt()
                val y = ev.y.toInt()
                determineWhichRegion(x, y)
            }
            // 裁剪框接管触摸事件
            if (mTouchType != TouchType.NONE) return onTouchEvent(ev)
            // 图片处理接管
            return super.onTouch(v, ev)
        }

        override fun getOriginFrameRectF(): RectF {
            return getOriginRectF()
        }

        override fun getCurrentFrameRectF(): RectF {
            return mCurrentRectF
        }

        private fun onTouchEvent(event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    cropPhotoView.parent?.requestDisallowInterceptTouchEvent(true)
                    if (mIsInAnimation) return false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 抬手后是否有动画
                    // 由于滑动需要重新计算触摸区域
                    if (mIsScrolled){
                        if (mCurrentRectF != mCurrentInitRectF){
                            adjustAttachRegion(mCurrentRectF)
                            regulateFramePosition()
                        }
                        mIsScrolled = false
                    }
                }
            }
            return mGestureDetector.onTouchEvent(event)
        }

        /*
        * 松手后调整边框的位置
        * */
        private fun regulateFramePosition(){
            val width = mCurrentRectF.width()
            val height = mCurrentRectF.height()
            val targetRectF = getRectFByScale(width / height)
            adjustImageOfFrameMove(mCurrentRectF, targetRectF, mTouchType)
            updateForScale(targetRectF)
        }

        private fun startAnimation(){
            val width = mCurrentRectF.width()
            val height = mCurrentRectF.height()
            val startRectF = RectF(mCurrentRectF)
            val endRectF = getRectFByScale(width / height)
            val anim = ValueAnimator.ofObject(RectFEvaluator(), startRectF, endRectF)
            anim.addUpdateListener {
                val result = it.animatedValue as RectF
                updateForTouch(result)
            }
            anim.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    mIsInAnimation = true
                }

                override fun onAnimationEnd(animation: Animator?) {
                    mIsInAnimation = false
                    adjustAttachRegion(mCurrentRectF)
                }

                override fun onAnimationCancel(animation: Animator?) {
                    mIsInAnimation = false
                }

                override fun onAnimationRepeat(animation: Animator?) {
                    // do nothing
                }
            })
            anim.duration = 500
            anim.start()
        }

        override fun adjustAttachRegion(r: RectF) {
            val left = r.left.toInt()
            val top = r.top.toInt()
            val right = r.right.toInt()
            val bottom = r.bottom.toInt()
            mLeftTopRegion.set(left, top, left + mTouchSlop, top + mTouchSlop)
            mRightTopRegion.set(right - mTouchSlop, top, right, top + mTouchSlop)
            mLeftBottomRegion.set(left, bottom - mTouchSlop, left + mTouchSlop, bottom)
            mRightBottomRegion.set(right - mTouchSlop, bottom - mTouchSlop, right, bottom)
            mLeftRegion.set(left, top + mTouchSlop + 1, left + mTouchSlop, bottom - mTouchSlop - 1)
            mRightRegion.set(
                right - mTouchSlop,
                top + mTouchSlop + 1,
                right,
                bottom - mTouchSlop - 1
            )
            mTopRegion.set(left + mTouchSlop + 1, top, right - mTouchSlop - 1, top + mTouchSlop)
            mBottomRegion.set(
                left + mTouchSlop + 1,
                bottom - mTouchSlop,
                right - mTouchSlop - 1,
                bottom
            )
        }

        private fun determineWhichRegion(x: Int, y: Int) {
            mTouchType = when {
                mLeftTopRegion.contains(x, y) -> TouchType.LT
                mRightTopRegion.contains(x, y) -> TouchType.RT
                mLeftBottomRegion.contains(x, y) -> TouchType.LB
                mRightBottomRegion.contains(x, y) -> TouchType.RB
                mLeftRegion.contains(x, y) -> TouchType.LEFT
                mRightRegion.contains(x, y) -> TouchType.RIGHT
                mTopRegion.contains(x, y) -> TouchType.TOP
                mBottomRegion.contains(x, y) -> TouchType.BOTTOM
                else -> TouchType.NONE
            }
        }

        override fun onDown(e: MotionEvent?): Boolean {
            Log.e("Attach", "onDown")
            return true
        }

        override fun onShowPress(e: MotionEvent?) {
            Log.e("Attach", "onShowPress")
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            Log.e("Attach", "onSingleTapUp")
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            mIsScrolled = true
            if (mScale != 0f) {
                scrollByScale(distanceX, distanceY)
                return true
            }
            var scrollX = distanceX
            var scrollY = distanceY
            val left = mCurrentInitRectF.left
            val top = mCurrentInitRectF.top
            val right = mCurrentInitRectF.right
            val bottom = mCurrentInitRectF.bottom
            when (mTouchType) {
                TouchType.LEFT -> {
                    // 边界检查,包括最小边框
                    if (mCurrentRectF.left - distanceX < left) {
                        scrollX = mCurrentRectF.left - left
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left + scrollX < mMinFrameWith){
                        scrollX = mMinFrameWith + mCurrentRectF.left - mCurrentRectF.right
                    }
                    mCurrentRectF.set(
                        mCurrentRectF.left - scrollX,
                        mCurrentRectF.top,
                        mCurrentRectF.right,
                        mCurrentRectF.bottom
                    )
                }
                TouchType.TOP -> {
                    if (mCurrentRectF.top - distanceY < top) {
                        scrollY = mCurrentRectF.top - top
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top + scrollY < mMinFrameHeight){
                        scrollY = mMinFrameHeight + mCurrentRectF.top - mCurrentRectF.bottom
                    }
                    mCurrentRectF.set(
                        mCurrentRectF.left,
                        mCurrentRectF.top - scrollY,
                        mCurrentRectF.right,
                        mCurrentRectF.bottom
                    )
                }
                TouchType.RIGHT -> {
                    if (mCurrentRectF.right - distanceX > right) {
                        scrollX = mCurrentRectF.right - right
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left - scrollX < mMinFrameWith){
                        scrollX = mCurrentRectF.right - mCurrentRectF.left - mMinFrameWith
                    }
                    mCurrentRectF.set(
                        mCurrentRectF.left,
                        mCurrentRectF.top,
                        mCurrentRectF.right - scrollX,
                        mCurrentRectF.bottom
                    )
                }
                TouchType.BOTTOM -> {
                    if (mCurrentRectF.bottom - distanceY > bottom) {
                        scrollY = mCurrentRectF.bottom - bottom
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top - scrollY < mMinFrameHeight){
                        scrollY = mCurrentRectF.bottom - mCurrentRectF.top - mMinFrameHeight
                    }
                    mCurrentRectF.set(
                        mCurrentRectF.left,
                        mCurrentRectF.top,
                        mCurrentRectF.right,
                        mCurrentRectF.bottom - scrollY
                    )
                }
                TouchType.LT -> {
                    if (mCurrentRectF.left - distanceX < left) {
                        scrollX = mCurrentRectF.left - left
                    }
                    if (mCurrentRectF.top - distanceY < top) {
                        scrollY = mCurrentRectF.top - top
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left + scrollX < mMinFrameWith){
                        scrollX = mMinFrameWith + mCurrentRectF.left - mCurrentRectF.right
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top + scrollY < mMinFrameHeight){
                        scrollY = mMinFrameHeight + mCurrentRectF.top - mCurrentRectF.bottom
                    }
                    mCurrentRectF.set(
                        mCurrentRectF.left - scrollX,
                        mCurrentRectF.top - scrollY,
                        mCurrentRectF.right,
                        mCurrentRectF.bottom
                    )
                }
                TouchType.RT -> {
                    if (mCurrentRectF.right - distanceX > right) {
                        scrollX = mCurrentRectF.right - right
                    }
                    if (mCurrentRectF.top - distanceY < top) {
                        scrollY = mCurrentRectF.top - top
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left - scrollX < mMinFrameWith){
                        scrollX = mCurrentRectF.right - mCurrentRectF.left - mMinFrameWith
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top + scrollY < mMinFrameHeight){
                        scrollY = mMinFrameHeight + mCurrentRectF.top - mCurrentRectF.bottom
                    }
                    mCurrentRectF.set(
                        mCurrentRectF.left,
                        mCurrentRectF.top - scrollY,
                        mCurrentRectF.right - scrollX,
                        mCurrentRectF.bottom
                    )
                }
                TouchType.LB -> {
                    if (mCurrentRectF.left - distanceX < left) {
                        scrollX = mCurrentRectF.left - left
                    }
                    if (mCurrentRectF.bottom - distanceY > bottom) {
                        scrollY = mCurrentRectF.bottom - bottom
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left + scrollX < mMinFrameWith){
                        scrollX = mMinFrameWith + mCurrentRectF.left - mCurrentRectF.right
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top - scrollY < mMinFrameHeight){
                        scrollY = mCurrentRectF.bottom - mCurrentRectF.top - mMinFrameHeight
                    }
                    mCurrentRectF.set(
                        mCurrentRectF.left - scrollX,
                        mCurrentRectF.top,
                        mCurrentRectF.right,
                        mCurrentRectF.bottom - scrollY
                    )
                }
                TouchType.RB -> {
                    if (mCurrentRectF.right - distanceX > right) {
                        scrollX = mCurrentRectF.right - right
                    }
                    if (mCurrentRectF.bottom - distanceY > bottom) {
                        scrollY = mCurrentRectF.bottom - bottom
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left - scrollX < mMinFrameWith){
                        scrollX = mCurrentRectF.right - mCurrentRectF.left - mMinFrameWith
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top - scrollY < mMinFrameHeight){
                        scrollY = mCurrentRectF.bottom - mCurrentRectF.top - mMinFrameHeight
                    }
                    mCurrentRectF.set(
                        mCurrentRectF.left,
                        mCurrentRectF.top,
                        mCurrentRectF.right - scrollX,
                        mCurrentRectF.bottom - scrollY
                    )
                }
                else -> {
                    mIsScrolled = false
                }
            }
//            Log.e("onScroll", "after scrollX = $scrollX, scrollY = $scrollY" +
//                    ", width = ${mCurrentRectF.width()}, height = ${mCurrentRectF.height()}")
            updateForTouch(mCurrentRectF)
            return true
        }

        private fun scrollByScale(distanceX: Float, distanceY: Float){
            var scrollX = distanceX
            var scrollY = distanceY
            val left = mCurrentInitRectF.left
            val top = mCurrentInitRectF.top
            val right = mCurrentInitRectF.right
            val bottom = mCurrentInitRectF.bottom
            when (mTouchType) {
                TouchType.LEFT -> {
                    // 边界检查,包括最小边框
                    if (mCurrentRectF.left - distanceX < left) {
                        scrollX = mCurrentRectF.left - left
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left + scrollX < mMinFrameWith){
                        scrollX = mMinFrameWith + mCurrentRectF.left - mCurrentRectF.right
                    }

                    mMatrix.setScale((mCurrentRectF.width() + scrollX) / mCurrentRectF.width(),
                        (mCurrentRectF.width() + scrollX) / mCurrentRectF.width(), mCurrentRectF.right,
                        (mCurrentRectF.top + mCurrentRectF.bottom) / 2)
                    mMatrix.mapRect(mCurrentRectF)
                }
                TouchType.TOP -> {
                    if (mCurrentRectF.top - distanceY < top) {
                        scrollY = mCurrentRectF.top - top
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top + scrollY < mMinFrameHeight){
                        scrollY = mMinFrameHeight + mCurrentRectF.top - mCurrentRectF.bottom
                    }

                    mMatrix.setScale((mCurrentRectF.height() + scrollY) / mCurrentRectF.height(),
                        (mCurrentRectF.height() + scrollY) / mCurrentRectF.height(),
                        (mCurrentRectF.left + mCurrentRectF.right) / 2, bottom)
                    mMatrix.mapRect(mCurrentRectF)
                }
                TouchType.RIGHT -> {
                    if (mCurrentRectF.right - distanceX > right) {
                        scrollX = mCurrentRectF.right - right
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left - scrollX < mMinFrameWith){
                        scrollX = mCurrentRectF.right - mCurrentRectF.left - mMinFrameWith
                    }

                    mMatrix.setScale((mCurrentRectF.width() - scrollX) / mCurrentRectF.width(),
                        (mCurrentRectF.width() - scrollX) / mCurrentRectF.width(), mCurrentRectF.left,
                        (mCurrentRectF.top + mCurrentRectF.bottom) / 2)
                    mMatrix.mapRect(mCurrentRectF)

                }
                TouchType.BOTTOM -> {
                    if (mCurrentRectF.bottom - distanceY > bottom) {
                        scrollY = mCurrentRectF.bottom - bottom
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top - scrollY < mMinFrameHeight){
                        scrollY = mCurrentRectF.bottom - mCurrentRectF.top - mMinFrameHeight
                    }

                    mMatrix.setScale((mCurrentRectF.height() - scrollY) / mCurrentRectF.height(),
                        (mCurrentRectF.height() - scrollY) / mCurrentRectF.height(),
                        (mCurrentRectF.left + mCurrentRectF.right) / 2, top)
                    mMatrix.mapRect(mCurrentRectF)
                }
                TouchType.LT -> {
                    if (mCurrentRectF.left - distanceX < left) {
                        scrollX = mCurrentRectF.left - left
                    }
                    if (mCurrentRectF.top - distanceY < top) {
                        scrollY = mCurrentRectF.top - top
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left + scrollX < mMinFrameWith){
                        scrollX = mMinFrameWith + mCurrentRectF.left - mCurrentRectF.right
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top + scrollY < mMinFrameHeight){
                        scrollY = mMinFrameHeight + mCurrentRectF.top - mCurrentRectF.bottom
                    }

                    if (abs(scrollX) > abs(scrollY)){
                        mMatrix.setScale((mCurrentRectF.width() + scrollX) / mCurrentRectF.width(),
                            (mCurrentRectF.width() + scrollX) / mCurrentRectF.width(), mCurrentRectF.right, mCurrentRectF.bottom)
                    }else{
                        mMatrix.setScale((mCurrentRectF.height() + scrollY) / mCurrentRectF.height(),
                            (mCurrentRectF.height() + scrollY) / mCurrentRectF.height(), mCurrentRectF.right, mCurrentRectF.bottom)
                    }
                    mMatrix.mapRect(mCurrentRectF)
                }
                TouchType.RT -> {
                    if (mCurrentRectF.right - distanceX > right) {
                        scrollX = mCurrentRectF.right - right
                    }
                    if (mCurrentRectF.top - distanceY < top) {
                        scrollY = mCurrentRectF.top - top
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left - scrollX < mMinFrameWith){
                        scrollX = mCurrentRectF.right - mCurrentRectF.left - mMinFrameWith
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top + scrollY < mMinFrameHeight){
                        scrollY = mMinFrameHeight + mCurrentRectF.top - mCurrentRectF.bottom
                    }

                    if (abs(scrollX) > abs(scrollY)){
                        mMatrix.setScale((mCurrentRectF.width() - scrollX) / mCurrentRectF.width(),
                            (mCurrentRectF.width() - scrollX) / mCurrentRectF.width(), mCurrentRectF.left, mCurrentRectF.bottom)
                    }else{
                        mMatrix.setScale((mCurrentRectF.height() + scrollY) / mCurrentRectF.height(),
                            (mCurrentRectF.height() + scrollY) / mCurrentRectF.height(), mCurrentRectF.left, mCurrentRectF.bottom)
                    }
                    mMatrix.mapRect(mCurrentRectF)
                }
                TouchType.LB -> {
                    if (mCurrentRectF.left - distanceX < left) {
                        scrollX = mCurrentRectF.left - left
                    }
                    if (mCurrentRectF.bottom - distanceY > bottom) {
                        scrollY = mCurrentRectF.bottom - bottom
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left + scrollX < mMinFrameWith){
                        scrollX = mMinFrameWith + mCurrentRectF.left - mCurrentRectF.right
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top - scrollY < mMinFrameHeight){
                        scrollY = mCurrentRectF.bottom - mCurrentRectF.top - mMinFrameHeight
                    }
                    if (abs(scrollX) > abs(scrollY)){
                        mMatrix.setScale((mCurrentRectF.width() + scrollX) / mCurrentRectF.width(),
                            (mCurrentRectF.width() + scrollX) / mCurrentRectF.width(), mCurrentRectF.right, mCurrentRectF.top)
                    }else{
                        mMatrix.setScale((mCurrentRectF.height() - scrollY) / mCurrentRectF.height(),
                            (mCurrentRectF.height() - scrollY) / mCurrentRectF.height(), mCurrentRectF.right, mCurrentRectF.top)
                    }
                    mMatrix.mapRect(mCurrentRectF)

                }
                TouchType.RB -> {
                    if (mCurrentRectF.right - distanceX > right) {
                        scrollX = mCurrentRectF.right - right
                    }
                    if (mCurrentRectF.bottom - distanceY > bottom) {
                        scrollY = mCurrentRectF.bottom - bottom
                    }
                    if (mCurrentRectF.right - mCurrentRectF.left - scrollX < mMinFrameWith) {
                        scrollX = mCurrentRectF.right - mCurrentRectF.left - mMinFrameWith
                    }
                    if (mCurrentRectF.bottom - mCurrentRectF.top - scrollY < mMinFrameHeight) {
                        scrollY = mCurrentRectF.bottom - mCurrentRectF.top - mMinFrameHeight
                    }
                    if (abs(scrollX) > abs(scrollY)){
                        mMatrix.setScale((mCurrentRectF.width() - scrollX) / mCurrentRectF.width(),
                            (mCurrentRectF.width() - scrollX) / mCurrentRectF.width(), mCurrentRectF.left, mCurrentRectF.top)
                    }else{
                        mMatrix.setScale((mCurrentRectF.height() - scrollY) / mCurrentRectF.height(),
                            (mCurrentRectF.height() - scrollY) / mCurrentRectF.height(), mCurrentRectF.left, mCurrentRectF.top)
                    }
                    mMatrix.mapRect(mCurrentRectF)
                }
                else -> {
                    mIsScrolled = false
                }
            }
            updateForTouch(mCurrentRectF)
        }

        override fun onLongPress(e: MotionEvent?) {
            Log.e("Attach", "onLongPress")
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            Log.e("Attach", "onFling")
            return true
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