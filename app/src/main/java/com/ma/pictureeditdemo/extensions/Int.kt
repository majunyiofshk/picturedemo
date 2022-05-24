package com.ma.pictureeditdemo.extensions

import android.content.res.Resources
import java.text.DecimalFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.pow

/**
 *
 * @date 2021/11/10
 * @author jianyu.zhang
 * @email jianyu.zhang@upuphone.com
 * @desc Int的扩展工具类
 */

fun Int.getFormattedDuration(forceShowHours: Boolean = false): String {
    val duration = TimeUnit.MILLISECONDS.toSeconds(this.toLong()).toInt()
    val sb = StringBuilder(8)
    val hours = duration / 3600
    val minutes = duration % 3600 / 60
    val seconds = duration % 60

    if (duration >= 3600) {
        sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":")
    } else if (forceShowHours) {
        sb.append("0:")
    }

    sb.append(String.format(Locale.getDefault(), "%02d", minutes))
    sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds))
    return sb.toString()
}

/**
 * Int扩展函数,dp转px
 */
fun Int.dp() = Resources.getSystem().displayMetrics.density * this

/**
 * Float扩展函数,dp转px
 */
fun Float.dp() = Resources.getSystem().displayMetrics.density * this

/**
 * Int扩展函数,sp转px
 */
fun Int.sp() = Resources.getSystem().displayMetrics.scaledDensity * this

/**
 * Float扩展函数,dp转px
 */
fun Float.sp() = Resources.getSystem().displayMetrics.scaledDensity * this

fun Int.formatSize(): String {
    if (this <= 0) {
        return "0 B"
    }

    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (log10(toDouble()) / log10(1024.0)).toInt()
    return "${
        DecimalFormat("#,##0.#").format(this / 1024.0.pow(digitGroups.toDouble()))
    } ${units[digitGroups]}"
}
