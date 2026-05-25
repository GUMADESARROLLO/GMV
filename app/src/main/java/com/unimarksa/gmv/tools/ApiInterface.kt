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

    @GET("api/api.php")
    fun getPerfilCliente(
        @Query("get_perfil_user") codCliente: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getDetalleFactura(
        @Query("get_detalle_factura") numeroFactura: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getReporteFacturacion(
        @Query("post_rpt_ruta") ruta: String = "F10",
        @Query("desde") desde: String,
        @Query("hasta") hasta: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getHistoricoFactura(
        @Query("post_historico_factura") codCliente: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getHistoryLotes(
        @Query("get_history_lotes") lote: String,
        @Query("Cliente") cliente: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getOrdenesCliente(
        @Query("get_ordenes_cliente") codCliente: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getOrdenes(
        @Query("get_ordenes") getOrdenes: String = "F10",
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>
}
