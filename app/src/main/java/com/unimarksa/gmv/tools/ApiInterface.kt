package com.unimarksa.gmv.tools

import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {
    @GET("api/api.php")
    fun getRecent(
        @Query("get_recent") getRecent: String = "F10",
        @Query("Cliente") cliente: String = "01097",
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getClientes(
        @Query("clients_id") clientsId: String = "F10",
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>
}
