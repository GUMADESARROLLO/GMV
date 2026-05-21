package com.unimarksa.gmv.tools

import com.google.gson.JsonElement
import com.unimarksa.gmv.models.ItemCliente
import com.unimarksa.gmv.models.ItemProducto
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiService {
    private val api: ApiInterface

    init {
        val baseUrl = Config.BASE_URL.trimEnd('/') + "/"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(ApiInterface::class.java)
    }

    fun fetchProductos(onResult: (List<ItemProducto>) -> Unit, onError: (String) -> Unit) {
        api.getRecent().enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    onResult(parseProductos(body.toString()))
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    fun fetchClientes(onResult: (List<ItemCliente>) -> Unit, onError: (String) -> Unit) {
        api.getClientes().enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    onResult(parseClientes(body.toString()))
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    private fun parseProductos(json: String): List<ItemProducto> {
        val lista = mutableListOf<ItemProducto>()
        val array = extractArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val opt: (String) -> String? = { k -> item.optString(k, null) }
            val name = opt("product_name") ?: ""
            val id = opt("product_id") ?: ""
            val qty = opt("product_quantity") ?: "0"
            val und = opt("product_und") ?: ""
            val price = opt("product_price") ?: "0.00"
            val status = opt("product_status") ?: ""
            val image = opt("product_image") ?: ""
            val bonificado = opt("product_bonificado") ?: opt("product_bonificacion") ?: ""
            val imageUrl = if (image.isNotEmpty()) "${Config.BASE_URL}/upload/product/$image" else null
            lista.add(
                ItemProducto(
                    nombre = name,
                    sku = id,
                    codArticulo = id,
                    cantidad = qty,
                    und = und.ifEmpty { null },
                    precio = price,
                    oferta = status,
                    bonificado = bonificado,
                    imagenUrl = imageUrl
                )
            )
        }
        return lista
    }

    private fun parseClientes(json: String): List<ItemCliente> {
        val lista = mutableListOf<ItemCliente>()
        val array = extractArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val opt: (String) -> String? = { k -> item.optString(k, null) }
            lista.add(
                ItemCliente(
                    nombre = opt("NOMBRE") ?: opt("nombre")
                        ?: opt("name") ?: opt("cliente")
                        ?: opt("razon_social") ?: "",
                    codCliente = opt("CLIENTE") ?: opt("cod_cliente")
                        ?: opt("codigo") ?: opt("id")
                        ?: opt("cliente_id") ?: "",
                    telefono = opt("TELE") ?: opt("telefono")
                        ?: opt("phone") ?: opt("celular")
                        ?: opt("contacto") ?: "",
                    direccion = opt("DIRECCION") ?: opt("direccion")
                        ?: opt("address") ?: opt("domicilio")
                        ?: "",
                    disponible = opt("DIPONIBLE") ?: opt("disponible")
                        ?: opt("available") ?: "",
                    saldo = opt("SALDO") ?: opt("saldo")
                        ?: opt("balance") ?: "",
                    limite = opt("LIMITE") ?: opt("limite")
                        ?: opt("limit") ?: "",
                    moroso = opt("MOROSO") ?: opt("moroso") ?: "N",
                    badge = item.optInt("badge", item.optInt("notificaciones", item.optInt("pedidos_pendientes", 0)))
                )
            )
        }
        return lista
    }

    private fun extractArray(json: String): JSONArray {
        if (json.trimStart().startsWith("[")) return JSONArray(json)
        val obj = JSONObject(json)
        return obj.optJSONArray("data")
            ?: obj.optJSONArray("clientes")
            ?: obj.optJSONArray("productos")
            ?: obj.optJSONArray("articulos")
            ?: obj.optJSONArray("items")
            ?: obj.optJSONArray("result")
            ?: obj.optJSONArray("results")
            ?: obj.optJSONArray("rows")
            ?: obj.optJSONArray("lista")
            ?: obj.optJSONArray("list")
            ?: JSONArray()
    }
}
