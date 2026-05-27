package com.unimarksa.gmv.models

import java.io.Serializable

data class ItemReporteRecibo(
    val id: String,
    val ruta: String,
    val recibo: String,
    val codCliente: String,
    val nombreCliente: String,
    val fecha: String,
    val beneficiario: String,
    val orderTotal: String,
    val comentario: String,
    val status: String,
    val detalles: List<ItemReciboDetalle> = emptyList(),
    val commentAnul: String = ""
) : Serializable
