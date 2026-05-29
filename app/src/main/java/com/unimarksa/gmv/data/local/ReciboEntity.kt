package com.unimarksa.gmv.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recibos")
data class ReciboEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reciboNumero: String,
    val clienteCodigo: String,
    val clienteNombre: String,
    val montoTotal: Double = 0.0,
    val fecha: Long = System.currentTimeMillis(),
    val estado: String = "borrador",
    val fechaEnvio: Long? = null
)