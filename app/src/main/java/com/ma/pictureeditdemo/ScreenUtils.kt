package com.ma.pictureeditdemo

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.ma.pictureeditdemo.extensions.application

/**
 * @Description:
 * @Author: JunYi.Ma
 * @Date: 2022/6/2 0002 12:54
 * @Email:  junyi.ma@upuphone.com
 */
object ScreenUtils {
    val realScreenWidth: Int
        get() {
            val wm = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wm.currentWindowMetrics.bounds.width()
            } else {
                val dm = DisplayMetrics()
                wm.defaultDisplay.getRealMetrics(dm)
                dm.widthPixels
            }
        }
    
    val realScreenHeight: Int
        get() {
            val wm = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return if (Build.VERSION.SDK_INT >= 30) {
                wm.currentWindowMetrics.bounds.height()
            } else {
                val dm = DisplayMetrics()
                wm.defaultDisplay.getRealMetrics(dm)
                dm.heightPixels
            }
        }
}