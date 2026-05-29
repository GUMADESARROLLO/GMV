package com.unimarksa.gmv.tools

import com.google.gson.JsonElement
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiInterface {
    @GET("api/api.php")
    fun getRecent(
        @Query("get_recent") getRecent: String,
        @Query("Cliente") cliente: String = "01097",
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getClientes(
        @Query("clients_id") clientsId: String,
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
        @Query("post_rpt_ruta") ruta: String,
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
        @Query("get_ordenes") getOrdenes: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getStatArticulo(
        @Query("get_stat_articulo") ruta: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getStatRuta(
        @Query("get_stat_ruta") ruta: String,
        @Query("sMes") mes: Int,
        @Query("sAnno") anno: Int,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getStatRecup(
        @Query("stat_recup") ruta: String,
        @Query("sMes") mes: Int,
        @Query("sAnno") anno: Int,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getRecibos(
        @Query("get_recibos") ruta: String,
        @Query("desde") desde: String,
        @Query("hasta") hasta: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getRecibosColector(
        @Query("get_recibos_colector") ruta: String,
        @Query("OrderBy") orderBy: String = "Desc",
        @Query("Desde") desde: String,
        @Query("Hasta") hasta: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @GET("api/api.php")
    fun getLogin(
        @Query("post_usuario") usuario: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @FormUrlEncoded
    @POST("api/api.php")
    fun postOrder(
        @Query("post_order") postOrder: String = "1",
        @Field("code") code: String,
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("phone") phone: String,
        @Field("address") address: String,
        @Field("shipping") shipping: String,
        @Field("order_list") orderList: String,
        @Field("order_total") orderTotal: String,
        @Field("comment") comment: String,
        @Field("player_id") playerId: String,
        @Field("date") date: String,
        @Field("server_url") serverUrl: String,
        @Field("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<ResponseBody>

    @GET("api/api.php")
    fun getSolicitudes(
        @Query("request_incremento") ruta: String,
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1"
    ): Call<JsonElement>

    @POST("api/api.php")
    fun postIncremento(
        @Query("request_incremento") action: String = "crear",
        @Query("APP_KEY") appKey: String = "C969C2E4E287B7A877FE193F785BF3D1",
        @Body body: okhttp3.RequestBody? = null
    ): Call<ResponseBody>
}
