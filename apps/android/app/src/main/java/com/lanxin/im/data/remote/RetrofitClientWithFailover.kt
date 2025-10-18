package com.lanxin.im.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * 支持故障转移的Retrofit客户端
 * 当主服务器失败时自动切换到副服务器
 */
object RetrofitClientWithFailover {
    
    private const val TAG = "RetrofitFailover"
    private const val CONNECT_TIMEOUT = 10L
    private const val READ_TIMEOUT = 15L
    private const val WRITE_TIMEOUT = 15L
    private const val MAX_RETRY_COUNT = 2
    
    private var token: String? = null
    
    fun setToken(token: String?) {
        this.token = token
    }
    
    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    /**
     * 认证拦截器
     */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        
        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }
        
        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.addHeader("Accept", "application/json")
        
        chain.proceed(requestBuilder.build())
    }
    
    /**
     * 故障转移拦截器
     * 当请求失败时自动切换服务器并重试
     */
    private val failoverInterceptor = Interceptor { chain ->
        var request = chain.request()
        var response: okhttp3.Response? = null
        var lastException: IOException? = null
        var retryCount = 0
        
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                // 更新请求URL为当前服务器
                val currentBaseUrl = ServerConfig.getApiBaseUrl()
                val newUrl = request.url.toString()
                    .replace(Regex("http://[^/]+/"), currentBaseUrl)
                
                val updatedRequest = request.newBuilder()
                    .url(newUrl)
                    .build()
                
                response = chain.proceed(updatedRequest)
                
                // 如果请求成功，返回响应
                if (response.isSuccessful) {
                    Log.d(TAG, "Request successful on ${ServerConfig.getCurrentServer().name}")
                    return@Interceptor response
                }
                
                // 服务器返回错误，但不是网络问题
                if (response.code in 400..499) {
                    // 客户端错误，不重试
                    return@Interceptor response
                }
                
                // 5xx错误，尝试切换服务器
                Log.w(TAG, "Server error ${response.code}, trying next server")
                
            } catch (e: IOException) {
                lastException = e
                Log.e(TAG, "Request failed: ${e.message}")
                
                when (e) {
                    is SocketTimeoutException,
                    is UnknownHostException -> {
                        // 网络问题，切换服务器
                        Log.w(TAG, "Network error, switching server")
                    }
                    else -> {
                        // 其他IO异常
                        Log.e(TAG, "IO exception: ${e.javaClass.simpleName}")
                    }
                }
            }
            
            // 切换到下一个服务器
            if (ServerConfig.switchToNextServer()) {
                retryCount++
                Log.i(TAG, "Switched to ${ServerConfig.getCurrentServer().name}, retry $retryCount/$MAX_RETRY_COUNT")
                
                // 重建Retrofit实例（使用新的服务器地址）
                rebuildRetrofit()
            } else {
                // 切换失败（可能因为切换太频繁）
                break
            }
        }
        
        // 所有服务器都失败了
        if (response != null && !response.isSuccessful) {
            return@Interceptor response
        }
        
        throw lastException ?: IOException("All servers failed after $retryCount retries")
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(failoverInterceptor)
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
        .build()
    
    private var retrofit: Retrofit = buildRetrofit()
    
    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ServerConfig.getApiBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * 重建Retrofit实例（当服务器地址改变时）
     */
    private fun rebuildRetrofit() {
        retrofit = buildRetrofit()
    }
    
    /**
     * 获取API服务实例
     */
    fun getApiService(): ApiService {
        return retrofit.create(ApiService::class.java)
    }
    
    /**
     * 手动重置连接（例如登录后）
     */
    fun reset() {
        ServerConfig.reset()
        rebuildRetrofit()
    }
}
