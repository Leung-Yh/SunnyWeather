package com.sunnyweather.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * 由于是哦那个MVVM的分层架构设计，从ViewModel层开始就不再持有Activity的引用
 * 为了解决“缺Context”的问题，给SunnyWeather项目提供一个全局获取Context的方式
 */
class SunnyWeatherApplication:Application() {

    companion object {

        const val TOKEN = "xc4eFLeGr0WwB6Tz"  //彩云天气令牌值

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    /* 注意：
        还要在AndroidManifest.xml的<application>标签下指定SunnyWeatherApplication
     */
}