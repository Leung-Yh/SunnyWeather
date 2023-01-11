package com.sunnyweather.android.logic

import androidx.lifecycle.liveData
import com.sunnyweather.android.logic.dao.PlaceDao
import com.sunnyweather.android.logic.model.Place
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.network.SunnyWeatherNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext


/**
 * 仓库层统一封装入口
 */
object Repository {

    /**
     * 一般在仓库层中定义的方法，为了能将异步获取的数据以响应式编程的方式通知给上一层，通常会返回一个LiveData对象
     * 下面的liveData函数时lifecycle-livedata-ktx库提供的，它可以自动构建并返回一个LiveData对象
     * 然后在它的代码块中提供一个挂起函数的上下文，使我们可以在liveData函数的代码块中调用任意的挂起函数
     *
     * 这里调用了SunnyWeatherNetwork的searchPlaces函数来搜索城市数据，然后判断
     * 如果响应ok，就是要Kotlin内置的Result.success方法来包装获取的城市数据列表
     * 否则，使用Result.failure方法来包装一个异常信息
     * 最后使用一个emit方法将包装的结构发射出去
     * 这个emit方法其实类似于调用LiveData的setValue方法来通知数据变化，
     * 只不过这里我们无法直接取得返回的LiveData对象，
     * 所以lifecycle-livedata-ktx库提供了一个这样的替代方法
     *
     * 注意：
     * 下面代码中我们还将liveData()函数的线程参数类型指定成了Dispatchers.IO，
     * 这样代码块中的所有代码就都运行在子线程中了
     * 众所周知，Android是不允许在主线程中进行网络请求的，诸如读写数据库之类的本地数据操作也是
     * 不建议在主线程中进行的，因此非常有必要在仓库层进行一次线程转换
     */
    fun searchPlace(query:String) = fire(Dispatchers.IO) {
        val placeResponse = SunnyWeatherNetwork.searchPlaces(query)
        if (placeResponse.status == "ok") {
            val place = placeResponse.places
            Result.success(place)
        } else {
            Result.failure(RuntimeException("response status is ${placeResponse.status}"))
        }
    }

    fun refreshWeather(lng: String, lat: String) = fire(Dispatchers.IO) {
        coroutineScope {
            val deferredRealtime = async {
                SunnyWeatherNetwork.getRealtimeWeather(lng, lat)
            }
            val deferredDaily = async {
                SunnyWeatherNetwork.getDailyWeather(lng, lat)
            }
            val realtimeResponse = deferredRealtime.await()
            val dailyResponse = deferredDaily.await()
            if (realtimeResponse.status == "ok" && dailyResponse.status == "ok") {
                val weather = Weather(realtimeResponse.result.realtime,
                    dailyResponse.result.daily)
                Result.success(weather)
            } else {
                Result.failure(
                    RuntimeException(
                        "realtime response status is ${realtimeResponse.status}" +
                                "daily response status is ${dailyResponse.status}"
                    )
                )
            }
        }
    }

    private fun <T> fire(context: CoroutineContext, block: suspend () -> Result<T>) =
        liveData<Result<T>>(context) {
            val result = try {
                block()
            } catch (e: Exception) {
                Result.failure<T>(e)
            }
            emit(result)
        }

    fun savePlace(place: Place) = PlaceDao.savePlace(place)

    fun getSavedPlace() = PlaceDao.getSavedPlace()

    fun isPlaceSaved() = PlaceDao.isPlaceSaved()

}