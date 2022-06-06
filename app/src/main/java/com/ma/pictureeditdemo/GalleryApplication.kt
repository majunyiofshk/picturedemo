package com.ma.pictureeditdemo

import android.app.Application

/**
 * @Description:
 * @Author: JunYi.Ma
 * @Date: 2022/6/2 0002 12:37
 * @Email:  junyi.ma@upuphone.com
 */
class GalleryApplication : Application() {
    companion object {
        lateinit var instance: GalleryApplication
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}