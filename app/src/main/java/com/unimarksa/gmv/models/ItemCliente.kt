package com.unimarksa.gmv.models

import java.io.Serializable

data class ItemCliente(
    val nombre: String,
    val codCliente: String,
    val telefono: String,
    val direccion: String,
    val disponible: String = "",
    val saldo: String = "",
    val limite: String = "",
    val moroso: String = "N",
    val condPago: String = "",
    val badge: Int = 0,
    val avatarBgColor: Int = 0
) : Serializable
