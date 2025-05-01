package com.example.expensetracker

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.Date
import java.util.concurrent.TimeUnit

object ApiClient {
    // Use 10.0.2.2 for Android Emulator to connect to localhost
    //private const val BASE_URL = "http://10.0.2.2:8000/"

    // using Tailscale android ip
    private const val BASE_URL = "http://100.90.114.28:8000/"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, object : JsonDeserializer<Date> {
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): Date {
                try {
                    return Date(json.asString.replace('T', ' ')
                        .replace('Z', ' ').trim()
                        .let { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(it).time })
                } catch (e: Exception) {
                    try {
                        return Date(json.asLong)
                    } catch (e: Exception) {
                        return Date()
                    }
                }
            }
        })
        .create()

    // Create logging interceptor
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Create OkHttpClient with the interceptor
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)  // Add the OkHttpClient with logging
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
