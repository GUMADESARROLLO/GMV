package com.unimarksa.gmv.tools

import com.unimarksa.gmv.models.ItemProducto
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun fetchProductos(onResult: (List<ItemProducto>) -> Unit, onError: (String) -> Unit) {
        val url = Config.BASE_URL + ApiConstants.GET_RECENT
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onError("Error de conexión: ${e.message}")
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onError("Error del servidor: ${response.code}")
                    }
                    return
                }

                try {
                    val productos = parseProductos(body)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onResult(productos)
                    }
                } catch (e: Exception) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onError("Error al procesar datos: ${e.message}")
                    }
                }
            }
        })
    }

    private fun parseProductos(json: String): List<ItemProducto> {
        val lista = mutableListOf<ItemProducto>()

        val array: JSONArray = if (json.trimStart().startsWith("[")) {
            JSONArray(json)
        } else {
            val obj = JSONObject(json)
            obj.optJSONArray("data") ?: obj.optJSONArray("productos") ?: JSONArray()
        }

        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            lista.add(
                ItemProducto(
                    nombre = item.optString("nombre", item.optString("name", "")),
                    sku = item.optString("sku", ""),
                    codArticulo = item.optString("cod_articulo", item.optString("codigo", "")),
                    cantidad = item.optString("cantidad", item.optString("stock", "0")),
                    precio = item.optString("precio", item.optString("price", "0.00")),
                    oferta = item.optString("oferta", null)
                )
            )
        }
        return lista
    }
}
