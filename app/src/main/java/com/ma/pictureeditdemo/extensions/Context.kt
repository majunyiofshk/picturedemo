package com.ma.pictureeditdemo.extensions

import android.app.Application
import android.content.Context
import android.view.View
import com.ma.pictureeditdemo.GalleryApplication

/**
 * dp值转换为px
 */

val application: Application get() = GalleryApplication.instance

fun View.dp2px(dp: Int): Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

fun View.sp2px(sp: Int): Int{
    val scale = resources.displayMetrics.scaledDensity
    return (sp * scale + 0.5f).toInt()
}

fun View.dp2px(dp: Float): Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

fun Context.dp2px(dp: Int): Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}