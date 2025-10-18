package com.lanxin.im.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.lanxin.im.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit客户端配置
 * 使用BuildConfig进行多环境配置
 */
object RetrofitClient {
    
    private val BASE_URL = BuildConfig.API_BASE_URL
    private const val CONNECT_TIMEOUT = 15L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L
    
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
    
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        
        // 添加JWT Token
        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }
        
        // 添加通用Header
        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.addHeader("Accept", "application/json")
        
        chain.proceed(requestBuilder.build())
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
        .build()
    
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

