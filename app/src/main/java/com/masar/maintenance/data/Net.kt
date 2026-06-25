package com.masar.maintenance.data

import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** كائن شبكة عام يبني Retrofit حسب رابط الخادم المخزّن ويعيد بناءه عند تغيّره. */
object Net {
    lateinit var session: Session
        private set

    private var api: Api? = null
    private var builtFor: String = ""

    fun init(session: Session) { this.session = session }

    fun isInitialized(): Boolean = ::session.isInitialized

    private fun client(): OkHttpClient {
        val auth = Interceptor { chain ->
            val b = chain.request().newBuilder()
            session.token?.let { if (it.isNotBlank()) b.addHeader("Authorization", "Bearer $it") }
            b.addHeader("Accept", "application/json")
            chain.proceed(b.build())
        }
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(log)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /** يعيد واجهة API مبنية على الرابط الحالي (يعيد البناء إن تغيّر). */
    fun api(): Api {
        val base = session.baseUrl
        if (api == null || builtFor != base) {
            val effective = if (base.isBlank()) "https://localhost/" else base
            api = Retrofit.Builder()
                .baseUrl(effective)
                .client(client())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(Api::class.java)
            builtFor = base
        }
        return api!!
    }

    val repo: Repository by lazy { Repository() }
}
