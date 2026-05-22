package com.unimarksa.gmv.models

import java.io.Serializable

data class ItemReporteFactura(
    val numero: String,
    val fecha: String,
    val total: String,
    val saldo: String = "",
    val cliente: String = "",
    val vencimiento: String = ""
) : Serializable
