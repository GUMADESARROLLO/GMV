package com.unimarksa.gmv.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val codigo: String = "",
    val clienteCodigo: String,
    val clienteNombre: String,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val total: Double = 0.0,
    val estado: String = "pendiente",
    val observaciones: String = ""
)