package com.unimarksa.gmv.tools

import android.util.Log
import com.unimarksa.gmv.models.Doctor
import com.unimarksa.gmv.models.DoctorStatus
import com.unimarksa.gmv.models.ItemProducto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

object MedicalApiService {
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val api: MedicalApiInterface

    init {
        val baseUrl = Config.VisitaMedica.trimEnd('/') + "/"
        Log.d("MedicalAPI", "Retrofit baseUrl: $baseUrl")
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                Log.d("MedicalAPI", "Request URL: ${req.url}")
                val token = SessionManager.getMedicalToken()
                val builder = req.newBuilder()
                if (token.isNotEmpty()) {
                    builder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(builder.build())
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .build()

        api = retrofit.create(MedicalApiInterface::class.java)
    }

    fun ensureAuth(onReady: () -> Unit, onError: (String) -> Unit) {
        var username = SessionManager.getUsername()
        var password = SessionManager.getPassword()
        if (username.isEmpty() || password.isEmpty()) {
            Log.d("MedicalAPI", "Usando credenciales hardcodeadas para pruebas")
            username = "admin"
            password = "admin123"
        }
        Log.d("MedicalAPI", "ensureAuth — login con usuario: '$username'")
        fetchLogin(username, password, onResult = {
            Log.d("MedicalAPI", "Token fresco obtenido, continuando...")
            onReady()
        }, onError = onError)
    }

    fun fetchLogin(username: String, password: String, onResult: () -> Unit, onError: (String) -> Unit) {
        Log.d("MedicalAPI", "Iniciando login a /api/auth/login con usuario: $username")
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }
        val body = json.toString().toRequestBody(JSON_MEDIA)
        api.login(body).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.d("MedicalAPI", "Login response code: ${response.code()}")
                if (!response.isSuccessful) {
                    val errorBody = try { response.errorBody()?.string() ?: "sin cuerpo" } catch (_: Exception) { "error al leer" }
                    Log.e("MedicalAPI", "Login falló: $errorBody")
                    onError("Error del servidor: ${response.code()} — $errorBody")
                    return
                }
                val body = response.body()
                if (body == null) { Log.e("MedicalAPI", "Login respuesta vacía"); onError("Respuesta vacía"); return }
                try {
                    val raw = body.string()
                    Log.d("MedicalAPI", "Login response body: $raw")
                    val obj = JSONObject(raw)
                    val token = obj.optString("token", "")
                    if (token.isEmpty()) { Log.e("MedicalAPI", "Token no encontrado en respuesta"); onError("Token no recibido"); return }
                    SessionManager.saveMedicalToken(token)
                    Log.d("MedicalAPI", "Token guardado exitosamente: ${token.take(20)}...")
                    onResult()
                } catch (e: Exception) {
                    Log.e("MedicalAPI", "Error parseando login: ${e.message}")
                    onError("Error al procesar datos: ${e.message}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("MedicalAPI", "Error de conexión en login: ${t.message}")
                onError("Error de conexión: ${t.message}")
            }
        })
    }

    fun fetchDoctors(onResult: (List<Doctor>) -> Unit, onError: (String) -> Unit) {
        ensureAuth(
            onReady = {
                Log.d("MedicalAPI", "Obteniendo médicos de /api/medicos")
                api.getDoctors().enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.d("MedicalAPI", "Médicos response code: ${response.code()}")
                        if (!response.isSuccessful) {
                            val errorBody = try { response.errorBody()?.string() ?: "sin cuerpo" } catch (_: Exception) { "error al leer" }
                            Log.e("MedicalAPI", "Médicos falló: $errorBody")
                            onError("Error del servidor: ${response.code()} — $errorBody")
                            return
                        }
                        val body = response.body()
                        if (body == null) { onError("Respuesta vacía"); return }
                        try {
                            val raw = body.string()
                            Log.d("MedicalAPI", "Médicos response: $raw")
                            onResult(parseDoctors(raw))
                        } catch (e: Exception) {
                            Log.e("MedicalAPI", "Error parseando médicos: ${e.message}")
                            onError("Error al procesar datos: ${e.message}")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("MedicalAPI", "Error de conexión en médicos: ${t.message}")
                        onError("Error de conexión: ${t.message}")
                    }
                })
            },
            onError = onError
        )
    }

    fun fetchDoctorProductos(
        doctorId: String,
        onResult: (List<ItemProducto>) -> Unit,
        onError: (String) -> Unit
    ) {
        ensureAuth(
            onReady = {
                Log.d("MedicalAPI", "Obteniendo productos del médico $doctorId")
                api.getDoctorProductos(doctorId).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.d("MedicalAPI", "Productos médico response code: ${response.code()}")
                        if (!response.isSuccessful) {
                            val errorBody = try { response.errorBody()?.string() ?: "sin cuerpo" } catch (_: Exception) { "error al leer" }
                            Log.e("MedicalAPI", "Productos médico falló: $errorBody")
                            onError("Error del servidor: ${response.code()} — $errorBody")
                            return
                        }
                        val body = response.body()
                        if (body == null) { onError("Respuesta vacía"); return }
                        try {
                            onResult(parseDoctorProductos(body.string()))
                        } catch (e: Exception) {
                            Log.e("MedicalAPI", "Error parseando productos médico: ${e.message}")
                            onError("Error al procesar datos: ${e.message}")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("MedicalAPI", "Error de conexión en productos: ${t.message}")
                        onError("Error de conexión: ${t.message}")
                    }
                })
            },
            onError = onError
        )
    }

    private fun parseDoctorProductos(json: String): List<ItemProducto> {
        val lista = mutableListOf<ItemProducto>()
        val obj = JSONObject(json)
        val baseImagen = Config.VisitaMedica.removeSuffix("/api").trimEnd('/')

        fun agregarDesdeArray(key: String) {
            val arr = obj.optJSONArray(key) ?: return
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val imagen = item.optString("imagenUrl", "")
                val imagenUrl = if (imagen.isNotEmpty()) "$baseImagen$imagen" else null
                lista.add(ItemProducto(
                    nombre = item.optString("nombre", ""),
                    sku = item.optString("codigo", ""),
                    codArticulo = item.optString("codigo", ""),
                    cantidad = "",
                    und = null,
                    precio = "",
                    oferta = null,
                    bonificado = null,
                    imagenUrl = imagenUrl
                ))
            }
        }

        agregarDesdeArray("asignados")
        return lista
    }

    fun fetchProductos(onResult: (List<ItemProducto>) -> Unit, onError: (String) -> Unit) {
        ensureAuth(
            onReady = {
                Log.d("MedicalAPI", "Obteniendo catálogo de productos")
                api.getProductos().enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.d("MedicalAPI", "Catálogo response code: ${response.code()}")
                        if (!response.isSuccessful) {
                            val errorBody = try { response.errorBody()?.string() ?: "sin cuerpo" } catch (_: Exception) { "error al leer" }
                            Log.e("MedicalAPI", "Catálogo falló: $errorBody")
                            onError("Error del servidor: ${response.code()} — $errorBody")
                            return
                        }
                        val body = response.body()
                        if (body == null) { onError("Respuesta vacía"); return }
                        try {
                            onResult(parseCatalogoProductos(body.string()))
                        } catch (e: Exception) {
                            Log.e("MedicalAPI", "Error parseando catálogo: ${e.message}")
                            onError("Error al procesar datos: ${e.message}")
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("MedicalAPI", "Error de conexión en catálogo: ${t.message}")
                        onError("Error de conexión: ${t.message}")
                    }
                })
            },
            onError = onError
        )
    }

    private fun parseCatalogoProductos(json: String): List<ItemProducto> {
        val lista = mutableListOf<ItemProducto>()
        val array = extractArray(json)
        val baseImagen = Config.VisitaMedica.removeSuffix("/api").trimEnd('/')
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val imagen = item.optString("imagenUrl", "")
            val imagenUrl = if (imagen.isNotEmpty()) "$baseImagen$imagen" else null
            lista.add(ItemProducto(
                nombre = item.optString("nombre", ""),
                sku = item.optString("codigo", ""),
                codArticulo = item.optString("codigo", ""),
                cantidad = "",
                und = null,
                precio = "",
                oferta = null,
                bonificado = null,
                imagenUrl = imagenUrl
            ))
        }
        return lista
    }

    private fun parseDoctors(json: String): List<Doctor> {
        val lista = mutableListOf<Doctor>()
        val array = extractArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val opt: (String) -> String? = { k -> item.optString(k, null) }

            val activoRaw = opt("activo") ?: "0"
            val estado = when (activoRaw) {
                "1" -> DoctorStatus.ACTIVO
                "0" -> DoctorStatus.PENDIENTE_BAJA
                else -> DoctorStatus.ACTIVO
            }

            val espObj = item.optJSONObject("especialidad")
            val especialidad = espObj?.optString("nombre", "")
                ?: opt("especialidad")
                ?: opt("ESPECIALIDAD")
                ?: opt("specialty")
                ?: ""

            lista.add(Doctor(
                id = opt("id") ?: opt("ID") ?: opt("doctor_id") ?: opt("codigo") ?: "",
                nombre = opt("nombre") ?: opt("NOMBRE") ?: opt("name") ?: opt("doctor_nombre") ?: "",
                especialidad = especialidad,
                telefono = opt("telefono") ?: opt("TELEFONO") ?: opt("phone") ?: opt("celular") ?: "",
                email = opt("email") ?: opt("EMAIL") ?: opt("correo") ?: "",
                estado = estado
            ))
        }
        return lista
    }

    private fun extractArray(json: String): JSONArray {
        if (json.trimStart().startsWith("[")) return JSONArray(json)
        val obj = JSONObject(json)
        return obj.optJSONArray("data")
            ?: obj.optJSONArray("medicos")
            ?: obj.optJSONArray("doctors")
            ?: obj.optJSONArray("items")
            ?: obj.optJSONArray("result")
            ?: obj.optJSONArray("results")
            ?: obj.optJSONArray("lista")
            ?: JSONArray()
    }
}
