package com.ma.pictureeditdemo.box

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.Visibility
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class CropPhotoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    // 裁剪框边框
    private var mFrameView: UFrameView = UFrameView(this)
    var mAttach: PhotoViewAttach = mFrameView.Attach(this)
    private var mFrameVisibility = true

    // 镜像动画
    private var mMirrorAnimator: ObjectAnimator? = null

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        super.setScaleType(ScaleType.MATRIX)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mFrameView.onSizeChanged(w, h, oldw, oldh)
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
    }

    private fun setFrameVisibility(visibility: Boolean){
        if (mFrameVisibility != visibility){
            mFrameVisibility = visibility
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (mFrameVisibility) mFrameView.draw(canvas)
    }
    
    fun correctVertical(value: Float ){
        mAttach.correctVertical(value * 5)
    }

    fun adjustToFreedom(){
       mFrameView.adjustToFreedom()
    }

    /*
    * 原始模式
    * */
    fun adjustToOrigin(){
        mFrameView.adjustToOrigin()
    }

    /*
    * 比例模式
    * */
    fun adjustByScale(scale: Float){
        mFrameView.adjustByScale(scale)
    }

    /*
    * 垂直旋转(逆时针)
    * */
    fun rotateByVertical(){
        mFrameView.rotateByVertical()
    }

    fun setRotateType(type: PhotoViewAttach.RotateType){
        mAttach.rotateType = type
    }

    /*
    * 指定角度旋转
    * */
    fun rotate(degree: Float){
        mAttach.setRotation(degree)
    }

    fun mirror(){
        if (mMirrorAnimator?.isRunning != true){
            mMirrorAnimator = if (scaleX == 1f) ObjectAnimator.ofFloat(this, "scaleX", 1f, -1f)
            else ObjectAnimator.ofFloat(this, "scaleX", -1f, 1f)
            mMirrorAnimator?.duration = 300
            mMirrorAnimator?.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    setFrameVisibility(false)
                }

                override fun onAnimationEnd(animation: Animator?) {
                    setFrameVisibility(true)
                }

                override fun onAnimationCancel(animation: Animator?) {
                    setFrameVisibility(true)
                }

                override fun onAnimationRepeat(animation: Animator?) {

                }
            })
            mMirrorAnimator?.start()
        }
    }

    /*
    * 重置
    * */
    fun reset(){
       mFrameView.reset()
    }

    fun obtainRotation(): Float{
        return mAttach.rotation
    }
}