package com.unimarksa.gmv.models

data class ItemPedidoCliente(
    val numero: String = "",
    val fecha: String = "",
    val monto: String = "0.00",
    val estado: String = "",
    val articulos: String = "0",
    val cliente: String = ""
)
