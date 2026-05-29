package com.unimarksa.gmv.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "carts")
data class CartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clienteCodigo: String,
    val clienteNombre: String,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val total: Double = 0.0
)