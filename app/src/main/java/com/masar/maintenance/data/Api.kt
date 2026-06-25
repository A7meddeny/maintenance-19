package com.masar.maintenance.data

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/** واجهة Retrofit — كل الردود JsonObject تُحلَّل في Repository. */
interface Api {

    @FormUrlEncoded
    @POST("api/auth.php")
    suspend fun login(@FieldMap fields: Map<String, String>): Response<JsonObject>

    @GET("api/{file}")
    suspend fun get(
        @Path("file") file: String,
        @QueryMap params: Map<String, String>
    ): Response<JsonObject>

    @Multipart
    @POST("api/{file}")
    suspend fun postMultipart(
        @Path("file") file: String,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part files: List<MultipartBody.Part>
    ): Response<JsonObject>
}
