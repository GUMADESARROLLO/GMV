package com.unimarksa.gmv.models

import java.io.Serializable

data class ItemDetalleFactura(
    val articulo: String,
    val descripcion: String,
    val cantidad: String,
    val venta: String,
    val imagen: String = "",
    val observaciones: String = ""
) : Serializable {
    val esBonificado: Boolean get() = venta.replace(",", "").trim().toDoubleOrNull() == 0.0
    val ventaNumerica: Double get() = venta.replace(",", "").trim().toDoubleOrNull() ?: 0.0
}
