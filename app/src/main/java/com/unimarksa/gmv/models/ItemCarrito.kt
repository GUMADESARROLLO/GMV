package com.unimarksa.gmv.models

import java.io.Serializable

data class ItemCarrito(
    val producto: ItemProducto,
    var cantidad: Double = 1.0,
    val selectedBonificado: String? = null
) : Serializable {
    val subtotal: Double
        get() = (producto.precio.replace(",", "").toDoubleOrNull() ?: 0.0) * cantidad

    val stockDisponible: Double
        get() = producto.cantidad.replace(",", "").toDoubleOrNull() ?: 999.0
}
