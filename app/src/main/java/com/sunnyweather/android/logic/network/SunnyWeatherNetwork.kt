package com.sunnyweather.android.logic.network

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 一个统一的网络数据源访问入口，队所有网络请求的API进行封装
 */
object SunnyWeatherNetwork {

    //PlaceService接口的动态代理对象
    private val placeService = ServiceCreator.create(PlaceService::class.java)

    //调用placeService中定义的searchPlaces方法，以发起搜索城市数据请求
    /*
        为了使代码简洁，使用了11.7.3中的技巧来简化Retrofit回调的写法
    由于需要借助协程技术来实现，因此又定义了一个await函数，并将searchPlaces函数
    也声明为挂起函数（await函数的实现在11.7.3有解析）
        当外部调用searchPlaces函数时，Retrofit会立即发起网络请求，同时当前的协程也会被阻塞
    直到服务器响应请求之后，await函数会将解析出来的数据模型对象取出并返回，同时回复当前协程的执行
    searchPlaces函数在得到await函数的返回值后再返回到上一层
     */
    suspend fun searchPlaces(query:String) = placeService.searchPlaces(query).await()

    private suspend fun <T> Call<T>.await():T {
        return suspendCoroutine { continuation ->
            enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    val body = response.body()
                    if(body != null) continuation.resume(body)
                    else continuation.resumeWithException(
                        RuntimeException("response body is null"))
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }

}