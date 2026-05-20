package com.unimarksa.gmv.models

data class ItemProducto(
    val nombre: String,
    val sku: String,
    val codArticulo: String,
    val cantidad: String,
    val precio: String,
    val oferta: String? = null,
    val bonificado: String? = null,
    val imagenUrl: String? = null,
    val imagenResource: Int? = null
)
