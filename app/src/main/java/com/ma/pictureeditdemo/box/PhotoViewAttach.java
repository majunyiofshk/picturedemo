package com.ma.pictureeditdemo.box;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;

import com.ma.pictureeditdemo.photoview.CustomGestureDetector;
import com.ma.pictureeditdemo.photoview.OnGestureListener;

public abstract class PhotoViewAttach implements View.OnTouchListener,
        View.OnLayoutChangeListener {

    private static final float DEFAULT_MAX_SCALE = 3.0f;
    private static final float DEFAULT_MIN_SCALE = 0.5f;

    private float mMinScale = DEFAULT_MIN_SCALE;
    private float mMaxScale = DEFAULT_MAX_SCALE;

    private final ImageView mImageView;
    private CustomGestureDetector mScaleDragDetector;

    // 图片操作矩阵
    private final Matrix mBaseMatrix = new Matrix();
    private final Matrix mDrawMatrix = new Matrix();
    private final Matrix mSuppMatrix = new Matrix();
    private final Matrix mPolyMatrix = new Matrix();
    private final RectF mDisplayRect = new RectF();
    private final float[] mMatrixValues = new float[9];

    // 垂直旋转角度
    private float mBaseRotation;
    //旋转四个顶点
    private float[] mVertexArray = new float[8];

    // 旋转类型
    private RotateType mRotateType = RotateType.NORMAL;


    private final OnGestureListener onGestureListener = new OnGestureListener() {
        @Override
        public void onDrag(float dx, float dy) {
            Log.e("PhotoViewAttach", "onDrag : dx = " + dx + ", dy = " + dy);
            if (mScaleDragDetector.isScaling()) {
                return; // Do not drag if we are already scaling
            }
            mSuppMatrix.postTranslate(dx, dy);
            setImageViewMatrix(getDrawMatrix());

            if (mScaleDragDetector.isScaling()) {
                ViewParent parent = mImageView.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
            }
        }

        @Override
        public void onFling(float startX, float startY, float velocityX, float velocityY) {
            // 不处理fling事件
            Log.e("PhotoViewAttach", "onFling : startX = " + startX + ", startY = " + startY);
        }

        @Override
        public void onScale(float scaleFactor, float focusX, float focusY) {
            // 缩放事件
            if ((getScale() < mMaxScale || scaleFactor < 1f) && (getScale() > mMinScale || scaleFactor > 1f)) {
                Log.e("PhotoViewAttach", "onScale : scaleFactor =  " + scaleFactor + ", focusX = " + focusX + ", focusY = " + focusY);
                mSuppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
                setImageViewMatrix(getDrawMatrix());
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    public PhotoViewAttach(ImageView imageView) {
        mImageView = imageView;
        imageView.setOnTouchListener(this);
        imageView.addOnLayoutChangeListener(this);

        if (imageView.isInEditMode()) return;

        mBaseRotation = 0.0f;

        mScaleDragDetector = new CustomGestureDetector(imageView.getContext(), onGestureListener);
    }

    /*
     * 获取原始裁剪框
     * */
    public abstract RectF getOriginFrameRectF();

    /*
     * 获取当前裁剪框
     * */
    public abstract RectF getCurrentFrameRectF();

    /*
     * 调整裁剪框触摸区域
     * */
    public abstract void adjustAttachRegion(RectF r);

    public void update() {
        updateBaseMatrix(mImageView.getDrawable());
    }

    /**
     * 只考虑 ScaleType 是 FIT_CENTER 情况
     *
     * @param drawable - Drawable being displayed
     */
    private void updateBaseMatrix(Drawable drawable) {
        if (drawable == null) return;
        /*
         * 图片初次显示时要以裁剪框最大宽高进行缩放居中
         * */
        final int drawableWidth = drawable.getIntrinsicWidth();
        final int drawableHeight = drawable.getIntrinsicHeight();

        mBaseMatrix.reset();

        RectF mTempSrc = new RectF(0, 0, drawableWidth, drawableHeight);
        RectF mTempDst = getOriginFrameRectF();
        mBaseMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.CENTER);
        resetMatrix();
    }

    private void resetMatrix() {
        mSuppMatrix.reset();
        mBaseRotation = 0f;
        setImageViewMatrix(getDrawMatrix());
    }

    private Matrix getDrawMatrix() {
        mDrawMatrix.set(mBaseMatrix);
        mDrawMatrix.postConcat(mSuppMatrix);
        return mDrawMatrix;
    }

    private float mLastAngle = 0f;

    public void setRotation(float angle) {
        if (angle == 0) {
            return;
        }
        float result = angle - mLastAngle;
        RectF currentFrameRectF = getCurrentFrameRectF();
        mSuppMatrix.postRotate(result, currentFrameRectF.centerX(), currentFrameRectF.centerY());
        if (Math.abs(angle) > Math.abs(mLastAngle)) {
            //判断是否放大
            if (isNeedZoomIn()) {
                final float scale = calcScale(currentFrameRectF.width(), currentFrameRectF.height(),
                        Math.toRadians(mLastAngle), Math.toRadians(angle));
                Log.e("setRotation", "放大 : scale = " + scale);
                mSuppMatrix.postScale(scale, scale, currentFrameRectF.centerX(), currentFrameRectF.centerY());
            }
        }
        setImageViewMatrix(getDrawMatrix());
        mLastAngle = angle;
    }

    private float calcScale(float w, float h, double before, double after) {
        if (w < h) {
            double beforeW = h * Math.abs(Math.sin(before)) + w * Math.abs(Math.cos(before));
            double afterW = h * Math.abs(Math.sin(after)) + w * Math.abs(Math.cos(after));
            return (float) (afterW / beforeW);
        } else {
            double beforeH = h * Math.abs(Math.cos(before)) + w * Math.abs(Math.sin(before));
            double afterH = h * Math.abs(Math.cos(after)) + w * Math.abs(Math.sin(after));
            return (float) (afterH / beforeH);
        }
    }

    private boolean isNeedZoomIn() {
        RectF currentFrameRectF = getCurrentFrameRectF();
        float[] vertex = getVertex();
        // Log.e("setRotation", "after = " + Arrays.toString(vertex));
        Path path = new Path();
        path.moveTo(vertex[0], vertex[1]);
        path.lineTo(vertex[2], vertex[3]);
        path.lineTo(vertex[4], vertex[5]);
        path.lineTo(vertex[6], vertex[7]);
        path.close();
        Region globalRegion = new Region(-10000, -10000, 10000, 10000);
        Region region = new Region();
        region.setPath(path, globalRegion);
        boolean lt = region.contains((int) currentFrameRectF.left, (int) currentFrameRectF.top);
        boolean rt = region.contains((int) currentFrameRectF.right, (int) currentFrameRectF.top);
        boolean rb = region.contains((int) currentFrameRectF.right, (int) currentFrameRectF.bottom);
        boolean lb = region.contains((int) currentFrameRectF.left, (int) currentFrameRectF.bottom);
        // Log.e("setRotation", "lt = " + lt + ", rt = " + rt + ", rb = " + rb + ", lb = " + lb);
        return !(lt && rt && rb && lb);
    }

    /*
    * 水平矫正
    * */
    public void correctHorizontal(float value){

    }

    /*
    * 垂直矫正
    * */
    public void correctVertical(float value){
        RectF displayRect = getDisplayRect(getDrawMatrix());
        float[] src = new float[]{displayRect.left, displayRect.top,  // 左上
                displayRect.right, displayRect.top,  // 右上
                displayRect.left, displayRect.bottom, //左下
                displayRect.right, displayRect.bottom}; // 右下
        float[] dst;
        if (value < 0) {
            dst = new float[]{displayRect.left + value, displayRect.top,  // 左上
                    displayRect.right - value, displayRect.top,  // 右上
                    displayRect.left, displayRect.bottom, //左下
                    displayRect.right, displayRect.bottom}; // 右下
        } else {
            dst = new float[]{displayRect.left, displayRect.top,  // 左上
                    displayRect.right, displayRect.top,  // 右上
                    displayRect.left - value, displayRect.bottom, //左下
                    displayRect.right + value, displayRect.bottom}; // 右下
        }
        mSuppMatrix.setPolyToPoly(src, 0, dst, 0, 4);
        setImageViewMatrix(getDrawMatrix());
    }

    public void setRotationTo(float angle) {
        if (angle == 0) return;
        float result = angle - mLastAngle;
        RectF currentFrameRectF = getCurrentFrameRectF();

        RectF displayRect = getDisplayRect(getDrawMatrix());
        float[] src = new float[]{displayRect.left, displayRect.top,  // 左上
                displayRect.right, displayRect.top,  // 右上
                displayRect.left, displayRect.bottom, //左下
                displayRect.right, displayRect.bottom}; // 右下
        float[] dst;
        switch (mRotateType) {
            case NORMAL:
                mSuppMatrix.postRotate(result % 360, currentFrameRectF.centerX(), currentFrameRectF.centerY());
                break;
            case HORIZONTAL_POLY:
                if (angle < 0) {
                    dst = new float[]{displayRect.left, displayRect.top - result,  // 左上
                            displayRect.right, displayRect.top,  // 右上
                            displayRect.left, displayRect.bottom - result, //左下
                            displayRect.right, displayRect.bottom}; // 右下
                } else {
                    dst = new float[]{displayRect.left, displayRect.top,  // 左上
                            displayRect.right, displayRect.top + result,  // 右上
                            displayRect.left, displayRect.bottom, //左下
                            displayRect.right, displayRect.bottom + result}; // 右下
                }
                mSuppMatrix.setPolyToPoly(src, 0, dst, 0, 4);
            case VERTICAL_POLY:
                if (angle < 0) {
                    dst = new float[]{displayRect.left - result, displayRect.top,  // 左上
                            displayRect.right - result, displayRect.top,  // 右上
                            displayRect.left, displayRect.bottom, //左下
                            displayRect.right, displayRect.bottom}; // 右下
                } else {
                    dst = new float[]{displayRect.left, displayRect.top,  // 左上
                            displayRect.right, displayRect.top,  // 右上
                            displayRect.left + result, displayRect.bottom, //左下
                            displayRect.right + result, displayRect.bottom}; // 右下
                }
                mSuppMatrix.setPolyToPoly(src, 0, dst, 0, 4);
        }
        setImageViewMatrix(getDrawMatrix());
        mLastAngle = angle;
    }

    /**
     * 图片跟随裁剪框旋转,旋转前裁剪框图片内容要等比例缩放填充到旋转后的裁剪框
     *
     * @param before 旋转前裁剪框
     * @param after  旋转后裁剪框
     */
    public void rotateOfFrame(RectF before, RectF after) {
        final float scale = after.height() / before.width();
        mSuppMatrix.postRotate(-90f, after.centerX(), after.centerY());
        mSuppMatrix.postScale(scale, scale, after.centerX(), after.centerY());
        setImageViewMatrix(getDrawMatrix());
        // 记录这个角度
        mBaseRotation -= 90f;
    }

    /*
     * 获取图片四个顶点坐标,按照左上、右上、右下、左下
     * */
    private float[] getVertex() {
        Drawable d = mImageView.getDrawable();
        if (d != null) {
            Matrix matrix = getDrawMatrix();
            mVertexArray[0] = 0f;
            mVertexArray[1] = 0f;
            mVertexArray[2] = d.getIntrinsicWidth();
            mVertexArray[3] = 0f;
            mVertexArray[4] = d.getIntrinsicWidth();
            mVertexArray[5] = d.getIntrinsicHeight();
            mVertexArray[6] = 0f;
            mVertexArray[7] = d.getIntrinsicHeight();
            matrix.mapPoints(mVertexArray);
        }
        return mVertexArray;
    }

    private RectF getDisplayRect(Matrix matrix) {
        Drawable d = mImageView.getDrawable();
        if (d != null) {
            mDisplayRect.set(0, 0, d.getIntrinsicWidth(),
                    d.getIntrinsicHeight());
            matrix.mapRect(mDisplayRect);
            return mDisplayRect;
        }
        return new RectF();
    }

    private void setImageViewMatrix(Matrix matrix) {
        mImageView.setImageMatrix(matrix);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            updateBaseMatrix(mImageView.getDrawable());
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean handled = false;
        if (((ImageView) v).getDrawable() == null) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                ViewParent parent = v.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                regulateImagePosition();
                break;
        }

        if (mScaleDragDetector != null) {
            handled = mScaleDragDetector.onTouchEvent(event);
        }
        return handled;
    }

    /**
     * 移动或缩放图片后需要校正图片位置
     */
    public void regulateImagePosition() {
        //TODO: 图片角度对于校正的影响?
        final float scale = getScale();
        final float radian = getRotation();
        if (scale < 1f) {
            // 要归位
            mSuppMatrix.reset();
            setImageViewMatrix(getDrawMatrix());
        } else {
            // 比较边界
            float deltaX = 0, deltaY = 0;
            final RectF currentFrameRectF = getCurrentFrameRectF();
            RectF displayRect = getDisplayRect(getDrawMatrix());
            final float rate = (float) Math.abs(Math.sin(radian) * Math.cos(radian));
            final float maxLeft = currentFrameRectF.left - currentFrameRectF.height() * rate;
            final float maxTop = currentFrameRectF.top - currentFrameRectF.width() * rate;
            final float minRight = currentFrameRectF.right + currentFrameRectF.height() * rate;
            final float minBottom = currentFrameRectF.bottom + currentFrameRectF.width() * rate;
            if (displayRect.left > maxLeft) {
                deltaX = displayRect.left - maxLeft;
            }
            if (displayRect.top > maxTop) {
                deltaY = displayRect.top - maxTop;
            }
            if (displayRect.right < minRight) {
                deltaX = displayRect.right - minRight;
            }
            if (displayRect.bottom < minBottom) {
                deltaY = displayRect.bottom - minBottom;
            }
            Log.e("regulateImagePosition", "disLeft = " + displayRect.left + ", maxLeft = " + maxLeft);
            Log.e("regulateImagePosition", "disRight = " + displayRect.right + ", minRight = " + minRight);
            Log.e("regulateImagePosition", "disTop = " + displayRect.top + ", maxTop = " + maxTop);
            Log.e("regulateImagePosition", "disBottom = " + displayRect.bottom + ", minBottom = " + minBottom);

            if (deltaX != 0 || deltaY != 0) {
                mSuppMatrix.postTranslate(-deltaX, -deltaY);
                setImageViewMatrix(getDrawMatrix());
                Log.e("regulateImagePosition", "DisplayRect = " + getDisplayRect(getDrawMatrix()).toString());
            }
        }
    }

    /**
     * 获取当前图片缩放率
     *
     * @return 图片缩放率
     */
    public float getScale() {
        return (float) Math.sqrt((float) Math.pow(getValue(mSuppMatrix, Matrix.MSCALE_X), 2) + (float) Math.pow(getValue(mSuppMatrix, Matrix.MSKEW_Y), 2));
    }

    /*
     * 获取旋转的弧度
     * */
    public float getRotation() {
        double radian = Math.atan2(getValue(mSuppMatrix, Matrix.MSKEW_X), getValue(mSuppMatrix, Matrix.MSCALE_X));
        return (float) radian;
    }

    /**
     * 操作裁剪框松手后,裁剪框会居中缩放, 原裁剪框中图片内容随着裁剪框等比例缩放
     *
     * @param current   当前裁剪框
     * @param target    操作后裁剪框
     * @param touchType 触摸类型,边框的那个位置
     */
    protected void adjustImageOfFrameMove(RectF current, RectF target, UFrameView.TouchType touchType) {
        final float scale = target.width() / current.width();
        final float focalX = (current.left + current.right) / 2;
        final float focalY = (current.top + current.bottom) / 2;
        // 计算当前边框图片四个顶点缩放后的位置, 缩放后要对齐
        final Matrix matrix = new Matrix();
        float[] pts = new float[]{current.left, current.top,  // 左上点
                current.right, current.top,  // 右上点
                current.left, current.bottom, //左下点
                current.right, current.bottom}; // 右下点
        matrix.postScale(scale, scale, focalX, focalY);
        matrix.mapPoints(pts);
        float deltaX = 0, deltaY = 0;
        switch (touchType) {
            case LT:
            case LEFT:
                deltaX = target.right - pts[6];
                deltaY = target.bottom - pts[7];
                break;
            case RT:
            case TOP:
                deltaX = target.left - pts[4];
                deltaY = target.bottom - pts[5];
                break;
            case LB:
            case BOTTOM:
                deltaX = target.right - pts[2];
                deltaY = target.top - pts[3];
                break;
            case RB:
            case RIGHT:
                deltaX = target.left - pts[0];
                deltaY = target.top - pts[1];
                break;
        }
        mSuppMatrix.postScale(scale, scale, focalX, focalY);
        mSuppMatrix.postTranslate(deltaX, deltaY);
        setImageViewMatrix(getDrawMatrix());
    }

    /**
     * 改变裁剪框比例时需要调整图片
     *
     * @param after 改变后裁剪框
     */
    protected void adjustImageOfFrameScale(RectF after) {
        RectF displayRect = getDisplayRect(getDrawMatrix());
        float scale = 1f;
        if (displayRect.width() < after.width()) scale = after.width() / displayRect.width();
        else if (displayRect.height() < after.height()) scale = after.height() / displayRect.height();
        if (scale != 1f) {
            final float focalX = (displayRect.left + displayRect.right) / 2;
            final float focalY = (displayRect.top + displayRect.bottom) / 2;
            mSuppMatrix.postScale(scale, scale, focalX, focalY);
            setImageViewMatrix(getDrawMatrix());
        }
    }

    public float getTranslateX() {
        return getValue(mSuppMatrix, Matrix.MTRANS_X);
    }

    public float getTranslateY() {
        return getValue(mSuppMatrix, Matrix.MTRANS_Y);
    }

    private float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    public float getMinScale() {
        return mMinScale;
    }

    public void setMinScale(float minScale) {
        this.mMinScale = minScale;
    }

    public float getMaxScale() {
        return mMaxScale;
    }

    public void setMaxScale(float maxScale) {
        this.mMaxScale = maxScale;
    }

    public RotateType getRotateType() {
        return mRotateType;
    }

    public void setRotateType(RotateType type) {
        mRotateType = type;
    }

    public enum RotateType {
        NORMAL,
        HORIZONTAL_POLY,
        VERTICAL_POLY
    }
}
