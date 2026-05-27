package com.unimarksa.gmv.models

import java.io.Serializable

data class ItemReciboDetalle(
    val numeroFactura: String,
    val total: Double,
    val nc: Double,
    val retencion: Double,
    val descuento: Double,
    val valorRecibido: Double,
    val saldoPendiente: Double,
    val codCliente: String,
    val tipoAccion: String
) : Serializable
