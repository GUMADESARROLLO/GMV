package com.unimarksa.gmv.models

import java.io.Serializable

data class ItemFacturaPendiente(
    val numero: String,
    val saldo: String,
    val vencimiento: String,
    val saldoActual: String
) : Serializable
