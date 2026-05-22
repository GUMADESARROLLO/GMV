package com.unimarksa.gmv.models

data class ItemPagoFactura(
    val numero: String,
    val nc: Double,
    val retencion: Double,
    val descuento: Double,
    val valorRecibido: Double,
    val saldo: Double = 0.0,
    val action: String,
    val estado: String = "pendiente"
)
