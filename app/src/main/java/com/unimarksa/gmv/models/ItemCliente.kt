package com.unimarksa.gmv.models

data class ItemCliente(
    val nombre: String,
    val codCliente: String,
    val telefono: String,
    val direccion: String,
    val disponible: String = "",
    val saldo: String = "",
    val limite: String = "",
    val moroso: String = "N",
    val badge: Int = 0,
    val avatarBgColor: Int = 0
)
