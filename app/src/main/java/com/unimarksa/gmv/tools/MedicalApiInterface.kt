package com.unimarksa.gmv.tools

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MedicalApiInterface {
    @POST("auth/login")
    fun login(@Body body: RequestBody): Call<ResponseBody>

    @GET("medicos")
    fun getDoctors(): Call<ResponseBody>

    @GET("medicos/productos/{id}")
    fun getDoctorProductos(@Path("id") doctorId: String): Call<ResponseBody>

    @GET("productos")
    fun getProductos(): Call<ResponseBody>
}
