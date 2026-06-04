package com.unimarksa.gmv.tools

import com.google.gson.JsonElement
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MedicalApiInterface {
    @POST("auth/login")
    fun login(@Body body: RequestBody): Call<JsonElement>

    @GET("medicos")
    fun getDoctors(): Call<JsonElement>

    @GET("medicos/productos/{id}")
    fun getDoctorProductos(@Path("id") doctorId: String): Call<JsonElement>

    @GET("productos")
    fun getProductos(): Call<JsonElement>
}
