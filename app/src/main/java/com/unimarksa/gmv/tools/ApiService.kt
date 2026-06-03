package com.unimarksa.gmv.tools

import com.google.gson.JsonElement
import com.unimarksa.gmv.data.local.OrderItemEntity
import com.unimarksa.gmv.models.ItemCliente
import okhttp3.ResponseBody
import com.unimarksa.gmv.models.ItemDetalleFactura
import com.unimarksa.gmv.models.ItemFacturaPendiente
import com.unimarksa.gmv.models.ItemLoteHistorial
import com.unimarksa.gmv.models.ItemPedidoCliente
import com.unimarksa.gmv.models.ItemPerfilCliente
import com.unimarksa.gmv.models.ItemProducto
import com.unimarksa.gmv.models.ItemReciboDetalle
import com.unimarksa.gmv.models.ItemReporteFactura
import com.unimarksa.gmv.models.ItemReporteRecibo
import com.unimarksa.gmv.models.ItemStatArticulo
import com.unimarksa.gmv.models.ItemStatRecup
import com.unimarksa.gmv.models.ItemStatVenta
import com.unimarksa.gmv.models.SolicitudItem
import com.unimarksa.gmv.tools.SessionManager
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ApiService {
    private val currentUser: String get() = SessionManager.getUsername().ifEmpty { "F10" }
    private val api: ApiInterface
    private val apiAws: ApiInterface

    init {
        val baseUrl = Config.BASE_URL.trimEnd('/') + "/"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(ApiInterface::class.java)

        val awsUrl = Config.BASE_AWS.trimEnd('/') + "/"
        val retrofitAws = Retrofit.Builder()
            .baseUrl(awsUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiAws = retrofitAws.create(ApiInterface::class.java)
    }

    fun fetchProductos(onResult: (List<ItemProducto>) -> Unit, onError: (String) -> Unit) {
        api.getRecent(getRecent = currentUser).enqueue(object : Callback<JsonElement> {
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
        api.getClientes(clientsId = currentUser).enqueue(object : Callback<JsonElement> {
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
                    condPago = opt("CONDPA") ?: opt("COMDPA") ?: opt("COND_PAGO") ?: opt("condpago") ?: "",
                    badge = item.optInt("badge", item.optInt("notificaciones", item.optInt("pedidos_pendientes", 0)))
                )
            )
        }
        return lista
    }

    fun fetchPerfilCliente(codCliente: String, onResult: (ItemPerfilCliente) -> Unit, onError: (String) -> Unit) {
        api.getPerfilCliente(codCliente).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    onResult(parsePerfilCliente(body.toString()))
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    fun parseFacturasPendientes(factPend: String): List<ItemFacturaPendiente> {
        if (factPend.isBlank()) return emptyList()
        return factPend.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size >= 4) {
                ItemFacturaPendiente(
                    numero = parts[0].trim(),
                    saldo = parts[1].trim(),
                    vencimiento = parts[2].trim().removePrefix("Ven."),
                    saldoActual = parts[3].trim()
                )
            } else null
        }
    }

    fun fetchDetalleFactura(numeroFactura: String, onResult: (List<ItemDetalleFactura>) -> Unit, onError: (String) -> Unit) {
        api.getDetalleFactura(numeroFactura).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    onResult(parseDetalleFactura(body.toString()))
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    private fun parseDetalleFactura(json: String): List<ItemDetalleFactura> {
        val lista = mutableListOf<ItemDetalleFactura>()
        val array = extractArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            lista.add(
                ItemDetalleFactura(
                    articulo = item.optString("ARTICULO", ""),
                    descripcion = item.optString("DESCRIPCION", ""),
                    cantidad = item.optString("CANTIDAD", "0"),
                    venta = item.optString("VENTA", "0.00"),
                    imagen = item.optString("IMAGEN", ""),
                    observaciones = item.optString("OBSERVACIONES", "")
                )
            )
        }
        return lista
    }

    private fun parsePerfilCliente(json: String): ItemPerfilCliente {
        val array = extractArray(json)
        if (array.length() == 0) return ItemPerfilCliente()
        val item = array.getJSONObject(0)
        val opt: (String) -> String = { k -> item.optString(k, "0.00") }
        return ItemPerfilCliente(
            noVencidos = opt("NoVencidos"),
            dias30 = opt("Dias30"),
            dias60 = opt("Dias60"),
            dias90 = opt("Dias90"),
            dias120 = opt("Dias120"),
            mas120 = opt("Mas120"),
            factPend = item.optString("FACT_PEND", "")
        )
    }

    fun fetchReporteFacturacion(
        desde: String,
        hasta: String,
        onResult: (List<ItemReporteFactura>) -> Unit,
        onError: (String) -> Unit
    ) {
        api.getReporteFacturacion(ruta = currentUser, desde = desde, hasta = hasta).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    onResult(parseReporteFacturacion(body.toString()))
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    private fun parseReporteFacturacion(json: String): List<ItemReporteFactura> {
        val lista = mutableListOf<ItemReporteFactura>()
        val array = extractArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val opt: (String) -> String? = { k -> item.optString(k, null) }
            lista.add(
                ItemReporteFactura(
                    numero = opt("factura") ?: opt("numero")
                        ?: opt("NUMERO") ?: opt("FACTURA") ?: opt("documento") ?: "",
                    fecha = opt("fecha") ?: opt("FECHA") ?: opt("fecha_emision")
                        ?: opt("fecha_factura") ?: "",
                    total = opt("total") ?: opt("TOTAL") ?: opt("monto")
                        ?: opt("MONTO") ?: opt("saldo") ?: opt("SALDO") ?: "0.00",
                    saldo = opt("saldo") ?: opt("SALDO") ?: "",
                    cliente = opt("cliente") ?: opt("CLIENTE") ?: opt("nombre")
                        ?: opt("NOMBRE") ?: opt("razon_social") ?: "",
                    vencimiento = opt("vence") ?: opt("VENCE") ?: ""
                )
            )
        }
        return lista
    }

    fun fetchRecibos(
        desde: String,
        hasta: String,
        onResult: (List<ItemReporteRecibo>) -> Unit,
        onError: (String) -> Unit
    ) {
        api.getRecibosColector(ruta = currentUser, desde = desde, hasta = hasta).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    onResult(parseRecibos(body.toString()))
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    private fun parseRecibos(json: String): List<ItemReporteRecibo> {
        val lista = mutableListOf<ItemReporteRecibo>()
        val array = extractArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val opt: (String) -> String? = { k -> item.optString(k, null) }
            val orderTotal = opt("mOrderTotal") ?: "C$ 0.00"
            val detalles = parseOrderList(opt("mOrderList") ?: "")
            lista.add(
                ItemReporteRecibo(
                    id = opt("mId") ?: "",
                    ruta = opt("mRuta") ?: "",
                    recibo = opt("mRecibo") ?: "",
                    codCliente = opt("mCod_Cliente") ?: "",
                    nombreCliente = opt("mName_Cliente") ?: "",
                    fecha = opt("mFecha") ?: "",
                    beneficiario = opt("mBenificiario") ?: "",
                    orderTotal = orderTotal,
                    comentario = opt("mComentario") ?: "",
                    status = opt("mStatus") ?: "",
                    detalles = detalles,
                    commentAnul = opt("mComment_anul") ?: ""
                )
            )
        }
        return lista
    }

    private fun parseOrderList(orderListStr: String): List<ItemReciboDetalle> {
        val lista = mutableListOf<ItemReciboDetalle>()
        if (orderListStr.isBlank()) return lista
        val cleaned = orderListStr.trim('[', ']', ',')
        val orders = cleaned.split("],")
        for (orderStr in orders) {
            val parts = orderStr.trim('[', ']', ',').split(";")
            if (parts.size >= 8) {
                val actionRaw = if (parts.size >= 9) parts[8] else parts.getOrElse(7) { "" }
                val tipoAccion = actionRaw.split(" ").first().trim()
                lista.add(
                    ItemReciboDetalle(
                        numeroFactura = parts[0],
                        total = parts[1].toDoubleOrNull() ?: 0.0,
                        nc = parts[2].toDoubleOrNull() ?: 0.0,
                        retencion = parts[3].toDoubleOrNull() ?: 0.0,
                        descuento = parts[4].toDoubleOrNull() ?: 0.0,
                        valorRecibido = parts[5].toDoubleOrNull() ?: 0.0,
                        saldoPendiente = parts[6].toDoubleOrNull() ?: 0.0,
                        codCliente = if (parts.size > 7) parts[7] else "",
                        tipoAccion = tipoAccion
                    )
                )
            }
        }
        return lista
    }

    fun fetchHistoricoFactura(
        codCliente: String,
        onResult: (List<ItemReporteFactura>) -> Unit,
        onError: (String) -> Unit
    ) {
        api.getHistoricoFactura(codCliente).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    onResult(parseReporteFacturacion(body.toString()))
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    fun fetchHistoryLotes(
        lote: String,
        codCliente: String,
        onResult: (List<ItemLoteHistorial>) -> Unit,
        onError: (String) -> Unit
    ) {
        api.getHistoryLotes(lote, codCliente).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    onResult(parseHistoryLotes(body.toString()))
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    private fun parseHistoryLotes(json: String): List<ItemLoteHistorial> {
        val lista = mutableListOf<ItemLoteHistorial>()
        val array = extractArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val opt: (String) -> String? = { k -> item.optString(k, null) }
            lista.add(
                ItemLoteHistorial(
                    lote = opt("mLote") ?: opt("lote") ?: "",
                    factura = opt("mFactura") ?: opt("factura") ?: "",
                    fecha = opt("mDia") ?: opt("fecha") ?: "",
                    descripcion = opt("mDescripcion") ?: opt("descripcion") ?: "",
                    articulo = opt("mArticulo") ?: opt("articulo") ?: ""
                )
            )
        }
        return lista
    }

    fun fetchOrdenes(
        onResult: (List<ItemPedidoCliente>) -> Unit,
        onError: (String) -> Unit
    ) {
        api.getOrdenes(getOrdenes = currentUser).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    onResult(parseOrdenesCliente(body.toString()))
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    fun fetchOrdenesCliente(
        codCliente: String,
        onResult: (List<ItemPedidoCliente>) -> Unit,
        onError: (String) -> Unit
    ) {
        api.getOrdenesCliente(codCliente).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    onResult(parseOrdenesCliente(body.toString()))
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    private fun parseOrdenesCliente(json: String): List<ItemPedidoCliente> {
        val lista = mutableListOf<ItemPedidoCliente>()
        val array = extractArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val opt: (String) -> String? = { k -> item.optString(k, null) }
            lista.add(
                ItemPedidoCliente(
                    numero = opt("num_pedido") ?: opt("numero") ?: opt("pedido") ?: opt("NUMERO") ?: "",
                    fecha = opt("fecha") ?: opt("FECHA") ?: opt("fecha_pedido") ?: "",
                    monto = opt("monto") ?: opt("MONTO") ?: opt("total") ?: opt("TOTAL") ?: "0.00",
                    estado = opt("estado") ?: opt("ESTADO") ?: opt("status") ?: "",
                    articulos = opt("articulos") ?: opt("items") ?: opt("cantidad") ?: "0",
                    cliente = opt("cliente") ?: opt("CLIENTE") ?: opt("nombre") ?: ""
                )
            )
        }
        return lista
    }

    fun fetchStatArticulo(
        onResult: (ItemStatArticulo) -> Unit,
        onError: (String) -> Unit
    ) {
        api.getStatArticulo(ruta = currentUser).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) { onError("Error del servidor: ${response.code()}"); return }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try { onResult(parseStatArticulo(body.toString())) }
                catch (e: Exception) { onError("Error al procesar datos: ${e.message}") }
            }
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    fun fetchStatRuta(
        mes: Int, anno: Int,
        onResult: (ItemStatVenta) -> Unit,
        onError: (String) -> Unit
    ) {
        api.getStatRuta(ruta = currentUser, mes = mes, anno = anno).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) { onError("Error del servidor: ${response.code()}"); return }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try { onResult(parseStatVenta(body.toString())) }
                catch (e: Exception) { onError("Error al procesar datos: ${e.message}") }
            }
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    fun fetchStatRecup(
        mes: Int, anno: Int,
        onResult: (ItemStatRecup) -> Unit,
        onError: (String) -> Unit
    ) {
        api.getStatRecup(ruta = currentUser, mes = mes, anno = anno).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) { onError("Error del servidor: ${response.code()}"); return }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try { onResult(parseStatRecup(body.toString())) }
                catch (e: Exception) { onError("Error al procesar datos: ${e.message}") }
            }
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    private fun parseStatArticulo(json: String): ItemStatArticulo {
        val array = extractArray(json)
        if (array.length() == 0) return ItemStatArticulo()
        val item = array.getJSONObject(0)
        return ItemStatArticulo(
            noVencidos = item.optString("NoVencidos", item.optString("noVencidos", "0.00")),
            vencidos = item.optString("Vencidos", item.optString("vencidos", "0.00")),
            dias30 = item.optString("Dias30", item.optString("dias30", "0.00")),
            dias60 = item.optString("Dias60", item.optString("dias60", "0.00")),
            dias90 = item.optString("Dias90", item.optString("dias90", "0.00")),
            dias120 = item.optString("Dias120", item.optString("dias120", "0.00")),
            mas120 = item.optString("Mas120", item.optString("mas120", "0.00"))
        )
    }

    private fun parseStatVenta(json: String): ItemStatVenta {
        val array = extractArray(json)
        if (array.length() == 0) return ItemStatVenta()
        val item = array.getJSONObject(0)
        return ItemStatVenta(
            ventaReal = item.optDouble("mVentaReal", 0.0),
            metaVenta = item.optDouble("mMetaVenta", 0.0),
            ventaDif = item.optDouble("mVentaDif", 0.0),
            vntCanti = item.optInt("mVntCanti", 0),
            vntCantiReal = item.optInt("mVntCantiReal", 0),
            vntCantiDif = item.optInt("mVntCantiDif", 0)
        )
    }

    private fun parseStatRecup(json: String): ItemStatRecup {
        val array = extractArray(json)
        if (array.length() == 0) return ItemStatRecup()
        val item = array.getJSONObject(0)
        return ItemStatRecup(
            metaRecuperacion = item.optDouble("Meta_Recuperacion", 0.0),
            recupCredito = item.optDouble("Recup_Credito", 0.0),
            recupContado = item.optDouble("Recup_Contado", 0.0),
            recupTotal = item.optDouble("Recup_Total", 0.0),
            recupCumple = item.optDouble("Recup_cumple", 100.0)
        )
    }

    fun formatearOrderList(items: List<OrderItemEntity>, subtotal: Double, iva: Double, total: Double, currency: String = "C$"): String {
        val sb = StringBuilder()
        for (item in items) {
            val bonif = item.bonificado ?: ""
            sb.append("[${item.cantidad};${item.codArticulo};${item.nombre};$bonif;${"%.2f".format(item.lineaSubtotal)} $currency],")
        }
        val taxPct = if (subtotal > 0) (iva / subtotal * 100).toInt() else 0
        sb.append("\n[${"Subtotal %.2f".format(subtotal)} $currency;\nIVA $taxPct% : ${"%.2f".format(iva)} $currency;\nTotal ${"%.2f".format(total)} $currency]")
        return sb.toString()
    }

    fun enviarOrden(
        codigo: String,
        clienteNombre: String,
        clienteCodigo: String,
        clienteTelefono: String,
        clienteDireccion: String,
        items: List<OrderItemEntity>,
        total: Double,
        subtotal: Double,
        iva: Double,
        observaciones: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val orderList = formatearOrderList(items, subtotal, iva, total)
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val playerId = "N/A"

        api.postOrder(
            code = codigo,
            name = SessionManager.getUsername().uppercase(),
            email = "${clienteCodigo} - ",
            phone = clienteNombre,
            address = clienteDireccion,
            shipping = "str_shipping",
            orderList = orderList,
            orderTotal = "%.2f".format(total),
            comment = observaciones,
            playerId = playerId,
            date = date,
            serverUrl = Config.BASE_URL
        ).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Error del servidor: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onError(t.message ?: "Error de conexión")
            }
        })
    }

    fun fetchSolicitudes(
        onResult: (List<SolicitudItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        api.getSolicitudes(ruta = currentUser).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    onResult(parseSolicitudes(body.toString()))
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    private fun parseSolicitudes(json: String): List<SolicitudItem> {
        val lista = mutableListOf<SolicitudItem>()
        val array = extractArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val opt: (String) -> String? = { k -> item.optString(k, null) }
            lista.add(
                SolicitudItem(
                    id = opt("id") ?: "",
                    cliente = opt("cliente") ?: opt("nombre") ?: "",
                    codCliente = opt("cod_cliente") ?: opt("codCliente") ?: "",
                    monto = opt("monto") ?: "0.00",
                    fecha = opt("fecha") ?: "",
                    estado = opt("estado") ?: "Pendiente",
                    motivo = opt("motivo") ?: "",
                    saldoActual = opt("saldo_actual") ?: "0.00",
                    limiteActual = opt("limite_actual") ?: "0.00",
                    disponibleActual = opt("disponible_actual") ?: "0.00"
                )
            )
        }
        return lista
    }

    fun formatearOrderListRecibo(pagos: List<com.unimarksa.gmv.models.ItemPagoFactura>, currency: String = "C$"): String {
        val sb = StringBuilder()
        for (pago in pagos) {
            val saldoPendiente = pago.saldo - pago.valorRecibido
            sb.append("[${pago.numero};${"%.2f".format(pago.saldo)};${"%.2f".format(pago.nc)};${"%.2f".format(pago.retencion)};${"%.2f".format(pago.descuento)};${"%.2f".format(pago.valorRecibido)};${"%.2f".format(saldoPendiente)};;${pago.action}],")
        }
        return sb.toString().trimEnd(',')
    }

    fun enviarRecibo(
        ruta: String,
        codCliente: String,
        clienteNombre: String,
        fechaRecibo: String,
        reciboNumero: String,
        pagos: List<com.unimarksa.gmv.models.ItemPagoFactura>,
        total: Double,
        observaciones: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val orderList = formatearOrderListRecibo(pagos)
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val playerId = "N/A"

        api.postRecibo(
            ruta = ruta,
            codCliente = codCliente,
            fechaRecibo = fechaRecibo,
            recibo = reciboNumero,
            nameCliente = clienteNombre,
            orderList = orderList,
            orderTotal = "%.2f".format(total),
            comment = observaciones,
            playerId = playerId,
            date = date
        ).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val bodyStr = try { response.body()?.string() ?: "" } catch (_: Exception) { "" }
                if (response.isSuccessful) {
                    onResult(bodyStr.trim())
                } else {
                    onError("Error del servidor: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onError(t.message ?: "Error de conexión")
            }
        })
    }

    fun eliminarRecibo(
        reciboNumero: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        api.deleteRecibo(reciboNumero = reciboNumero).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val bodyStr = try { response.body()?.string() ?: "" } catch (_: Exception) { "" }
                if (response.isSuccessful) {
                    onResult(bodyStr.trim())
                } else {
                    onError("Error del servidor: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onError(t.message ?: "Error de conexión")
            }
        })
    }

    fun anularReciboPorNumero(
        recibo: String,
        fechaRecibo: String,
        ruta: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        api.anularRecibo(recibo = recibo, fechaRecibo = fechaRecibo, ruta = ruta).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val bodyStr = try { response.body()?.string() ?: "" } catch (_: Exception) { "" }
                if (response.isSuccessful) {
                    onResult(bodyStr.trim())
                } else {
                    onError("Error del servidor: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onError(t.message ?: "Error de conexión")
            }
        })
    }

    fun enviarAdjunto(
        idRecibo: String,
        nombre: String,
        imagenBase64: String,
        onResult: () -> Unit,
        onError: (String) -> Unit
    ) {
        apiAws.postAdjunto(nombre = nombre, imagenBase64 = imagenBase64, idRecibo = idRecibo).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val bodyStr = try { response.body()?.string() ?: "" } catch (_: Exception) { "" }
                if (response.isSuccessful && bodyStr.contains("Success", ignoreCase = true)) {
                    onResult()
                } else {
                    onError(bodyStr.ifEmpty { "Error ${response.code()}" })
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onError(t.message ?: "Error de conexión")
            }
        })
    }

    fun fetchAdjuntos(
        idRecibo: String,
        onResult: (List<com.unimarksa.gmv.models.ItemAdjunto>) -> Unit,
        onError: (String) -> Unit
    ) {
        apiAws.getAdjuntos(idRecibo = idRecibo).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) { onError("Error del servidor: ${response.code()}"); return }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    val lista = mutableListOf<com.unimarksa.gmv.models.ItemAdjunto>()
                    val array = body.asJsonArray
                    for (element in array) {
                        val obj = element.asJsonObject
                        lista.add(com.unimarksa.gmv.models.ItemAdjunto(
                            id = obj.get("mId")?.asString ?: "",
                            idRecibo = obj.get("mRecibo")?.asString ?: "",
                            nombreImagen = obj.get("mNombreImagen")?.asString ?: "",
                            imagenUrl = obj.get("imagen_url")?.asString ?: ""
                        ))
                    }
                    onResult(lista)
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError(t.message ?: "Error de conexión")
            }
        })
    }

    fun fetchLastRecibo(
        ruta: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        api.getLastRecibo(ruta = ruta).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) { onError("Error del servidor: ${response.code()}"); return }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    val json = body.toString()
                    val obj = org.json.JSONObject(json)
                    onResult(obj.optString("last_recibo", ""))
                } catch (e: Exception) {
                    onError("Error al procesar: ${e.message}")
                }
            }
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError(t.message ?: "Error de conexión")
            }
        })
    }

    fun enviarIncremento(
        monto: Double,
        disponible: Double,
        saldo: Double,
        limite: Double,
        motivo: String,
        clienteNombre: String,
        clienteCodigo: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val ruta = currentUser
        val codigo = "${ruta}-${System.currentTimeMillis().toString().takeLast(6)}"

        val json = JSONObject().apply {
            put("codigo", codigo)
            put("ruta", ruta)
            put("cliente", clienteNombre)
            put("cod_cliente", clienteCodigo)
            put("monto", "%.2f".format(monto))
            put("motivo", motivo)
            put("saldo_actual", "%.2f".format(saldo))
            put("limite_actual", "%.2f".format(limite))
            put("disponible", "%.2f".format(disponible))
        }
        val requestBody = okhttp3.RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            json.toString()
        )

        api.postIncremento(body = requestBody).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val bodyStr = try { response.body()?.string() ?: "" } catch (_: Exception) { "" }
                if (response.isSuccessful && bodyStr.contains("Success", ignoreCase = true)) {
                    onSuccess()
                } else {
                    onError("Servidor: ${bodyStr.ifEmpty { "Error ${response.code()}" }}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onError(t.message ?: "Error de conexión")
            }
        })
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
    fun fetchLogin(
        usuario: String,
        contrasena: String,
        onSuccess: (String, String, String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cred = "[$usuario@$contrasena]"
        api.getLogin(usuario = cred).enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (!response.isSuccessful) {
                    onError("Error del servidor: ${response.code()}")
                    return
                }
                val body = response.body()
                if (body == null) { onError("Respuesta vacía"); return }
                try {
                    val json = body.toString()
                    val obj = JSONObject(json)
                    val array = obj.optJSONArray("result") ?: JSONArray()
                    if (array.length() > 0) {
                        val item = array.getJSONObject(0)
                        val success = item.optString("success", "0")
                        if (success == "1") {
                            onSuccess(
                                item.optString("name", usuario),
                                item.optString("FullName", ""),
                                item.optString("Tele", ""),
                                item.optString("Correo", "")
                            )
                        } else {
                            onError("Credenciales incorrectas")
                        }
                    } else {
                        onError("Credenciales incorrectas")
                    }
                } catch (e: Exception) {
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                onError("Error de conexión: ${t.message}")
            }
        })
    }
}
