package com.ma.pictureeditdemo.box

import android.animation.TypeEvaluator
import android.graphics.RectF

class RectFEvaluator: TypeEvaluator<RectF> {

    override fun evaluate(fraction: Float, startValue: RectF, endValue: RectF): RectF {
        val left = startValue.left + fraction * (endValue.left - startValue.left)
        val top = startValue.top + fraction * (endValue.top - startValue.top)
        val right = startValue.right + fraction * (endValue.right - startValue.right)
        val bottom = startValue.bottom + fraction * (endValue.bottom - startValue.bottom)
        return RectF(left, top, right, bottom)
    }


}