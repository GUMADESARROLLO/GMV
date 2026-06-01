package com.unimarksa.gmv.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recibo_cart")
data class ReciboCartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val numeroFactura: String,
    val nc: Double = 0.0,
    val retencion: Double = 0.0,
    val descuento: Double = 0.0,
    val valorRecibido: Double = 0.0,
    val saldo: Double = 0.0,
    val action: String,
    val estado: String = "pendiente"
)
