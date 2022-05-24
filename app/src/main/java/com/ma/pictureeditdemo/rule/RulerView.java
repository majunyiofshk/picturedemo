package com.ma.pictureeditdemo.rule;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.OverScroller;

import com.ma.pictureeditdemo.R;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

/**
 * 仿薄荷卷尺
 */

public class RulerView extends View {
    public static final String TAG = "RulerView";

    private int mWidth;
    private int mHeight;
    private int mMinGraduation = 25; //最小刻度
    private int mMaxGraduation = 200; //最大刻度

    private OverScroller mScroller;
    private int mOverDistance; //超出的距离
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;
    private int mMinimumFlingVelocity;
    private int mMaximumFlingVelocity;

    private int mLastPointX;
    private static final int INVALID_POINTER = -1;
    private int mActivePointerId = INVALID_POINTER;

    private boolean mIsBeingDragged;

    private float mSmallGraduationHeight;
    private float mBigGraduationHeight;
    private float mMiddleGraduationHeight;
    private int mSpace = 29;
    private int mTotalGraduation;

    private Paint mSmallGraduationPaint;
    private Paint mBigGraduationPaint;
    private Paint mMiddleGraduationPaint;
    private Paint mTextPaint;
    private Paint.FontMetrics mFontMetrics;

    private float mTextPaddingBottom = 39;
    private ViewConfiguration configuration;

    public RulerView(Context context) {
        super(context);
    }

    public RulerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initRulerView(context);
    }

    private void initRulerView(Context context) {
        mScroller = new OverScroller(context);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();

        mOverDistance = 2 * 10 * mSpace;

        mSmallGraduationPaint = new Paint();
        mSmallGraduationPaint.setStrokeWidth(2);
        mSmallGraduationPaint.setColor(context.getResources().getColor(R.color.rulerGraduation));

        mBigGraduationPaint = new Paint();
        mBigGraduationPaint.setStrokeWidth(4);
        mBigGraduationPaint.setColor(context.getResources().getColor(R.color.rulerGraduation));

        mMiddleGraduationPaint = new Paint();
        mMiddleGraduationPaint.setStrokeWidth(4);
        mMiddleGraduationPaint.setColor(context.getResources().getColor(R.color.rulerMiddleGraduation));

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(40);
        mTextPaint.setColor(context.getResources().getColor(R.color.rulerGraduationText));
        mFontMetrics = mTextPaint.getFontMetrics();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final float part = h / 5f;
        //计算小刻度高度
        mSmallGraduationHeight = part;
        //计算大刻度高度
        mBigGraduationHeight = 2 * part;
        //计算中间刻度高度
        mMiddleGraduationHeight = 3 * part;
        //一共多少个刻度
        mTotalGraduation = w / mSpace;

        mWidth = w;
        mHeight = h;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTrackerIfNotExists();

        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mIsBeingDragged = !mScroller.isFinished()) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                if (mIsBeingDragged) {
                    mScroller.abortAnimation();
                }

                mLastPointX = (int) event.getX();
                mActivePointerId = event.getPointerId(0);
                break;

            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = event.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    break;
                }

                final int x = (int) event.getX(activePointerIndex);
                int deltaX = mLastPointX - x;
                final int range = getScrollRange();
                if (!mIsBeingDragged && Math.abs(deltaX) > mTouchSlop) {
                    mIsBeingDragged = true;
                    if (deltaX > 0) {
                        deltaX -= mTouchSlop;
                    } else {
                        deltaX += mTouchSlop;
                    }
                }

                if (mIsBeingDragged) {
                    mVelocityTracker.addMovement(event);
                    mLastPointX = x;
                    overScrollBy(deltaX, 0, getScrollX(), 0, range, 0,
                            mOverDistance, 0, true);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                    int initialVelocity = (int) velocityTracker.getXVelocity(mActivePointerId);
                    if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
                        Log.e(TAG, "是否fling");
                        handleFling(-initialVelocity);
                    } else if (mScroller.springBack(getScrollX(), getScrollY(),
                            0, getScrollRange(), 0, 0)) {
                        postInvalidateOnAnimation();
                    } else {
                        handleAdsorb();
                    }

                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged) {
                    if (mScroller.springBack(getScrollX(), getScrollY(),
                            0, getScrollRange(), 0, 0)) {
                        postInvalidateOnAnimation();
                    } else {
                        handleAdsorb();
                    }

                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = event.getActionIndex();
                mLastPointX = (int) event.getX(index);
                mActivePointerId = event.getPointerId(index);
                Log.e(TAG, "index = " + index + ", mLastPointX = " + mLastPointX +
                        ", mActivePointerId = " + mActivePointerId);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                mLastPointX = (int) event.getX(event.findPointerIndex(mActivePointerId));
                break;
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent event) {
        final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = event.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastPointX = (int) event.getX(newPointerIndex);
            mActivePointerId = event.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void endDrag() {
        mIsBeingDragged = false;

        recycleVelocityTracker();
    }

    private void handleFling(int velocityX) {
        final int x = getScrollX();
        final boolean canFling = (x >= -mOverDistance || velocityX > 0) &&
                (x <= getScrollRange() + mOverDistance || velocityX < 0);
        if (canFling) {
            mScroller.fling(getScrollX(), getScrollY(), velocityX, 0, 0,
                    getScrollRange(), 0, 0, mOverDistance, 0);
            postInvalidateOnAnimation();
        }
    }

    private void handleAdsorb() {
        final int middle = mWidth / 2;
        final int originValue = middle % mSpace;
        final int currentValue = computeStartDistance();
        final boolean canAdsorb = currentValue > 0 ? (originValue != currentValue) :
                (mSpace != originValue - currentValue);
        if (canAdsorb) {
            int deltaX;
            //向左还是向右
            if (currentValue > 0) {
                deltaX = currentValue - originValue;
            } else {
                deltaX = mSpace + currentValue - originValue;
            }

            mScroller.startScroll(getScrollX(), getScrollY(), deltaX, 0);
            postInvalidateOnAnimation();
        }

    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            final int oldX = getScrollX();
            final int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            final int range = getScrollRange();
            //超过滚动范围会出现 oldX = x 或者 oldY = y 的情况,需要特殊处理
            if (oldX != x || oldY != y) {
                overScrollBy(x - oldX, y - oldY, oldX, oldY,
                        range, 0, mOverDistance, 0, false);
            } else if (mScroller.isOverScrolled()) {
                //跳过此次滚动,进行下一次的计算
                invalidate();
            } else {
                //判断是否是整刻度,不是需要吸附到整刻度上
                handleAdsorb();
            }
        }
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.scrollTo(scrollX, scrollY);
    }

    @Override
    protected int computeHorizontalScrollRange() {
        return getScrollRange();
    }

    private int getScrollRange() {
        //卷尺的总长加上控件的宽度
        return 10 * mSpace * (mMaxGraduation - mMinGraduation);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int x = getScrollX();
        final int saveCount = canvas.getSaveCount();
        canvas.save();
        canvas.translate(x, 0);

        final int total = mTotalGraduation + 1; //绘制的时候多绘制一个刻度,类似于ListView
        final int space = mSpace;
        final float smallGraduationHeight = mSmallGraduationHeight;
        final float bigGraduationHeight = mBigGraduationHeight;
        final int index = computeStartIndex();
        float startX = computeStartDistance();
        for (int i = index; i < total + index; i++) {
            if (i % 10 == 0) {
                //画大刻度
                canvas.drawLine(startX, 0, startX, bigGraduationHeight, mBigGraduationPaint);

                //画文字
                int textValue = i / 10 + mMinGraduation;
                if (textValue >= mMinGraduation && textValue <= mMaxGraduation) {
                    String text = String.valueOf(textValue);
                    float textWidth = mTextPaint.measureText(text);
                    final float textX = startX - textWidth / 2.0f;
                    final float textY = mHeight - mTextPaddingBottom + mFontMetrics.bottom;
                    canvas.drawText(text, textX, textY, mTextPaint);
                }
            } else {
                //画小刻度
                canvas.drawLine(startX, 0, startX, smallGraduationHeight, mSmallGraduationPaint);
            }
            startX += space;
        }

        //画中间刻度
        final float middleX = mWidth / 2.0f;
        final float middleGraduationHeight = mMiddleGraduationHeight;
        canvas.drawLine(middleX, 0, middleX, middleGraduationHeight, mMiddleGraduationPaint);

        canvas.restoreToCount(saveCount);
    }

    /**
     * 用于计算绘制大刻度的索引值，需要考虑开始和末尾的大刻度在控件中间
     *
     * @return 索引值
     */
    private int computeStartIndex() {
        final int scrollX = getScrollX();
        final int space = mSpace;
        final int middle = mWidth / 2;
        return (scrollX - middle) / space;
    }

    /**
     * 用于计算绘制刻度的起始距离
     *
     * @return 起始距离
     */
    private int computeStartDistance() {
        final int scrollX = getScrollX();
        final int space = mSpace;
        final int middle = mWidth / 2;
        return (middle - scrollX) % space;
    }
}

/*
* 整刻度的吸附效果
* 1.拖拽后不fling,up事件处理,处理条件--->不是整刻度
* 2.拖拽后fling,fling结束后处理,处理条件--->不是整刻度
* */
