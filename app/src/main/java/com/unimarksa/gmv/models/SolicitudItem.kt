package com.unimarksa.gmv.models

data class SolicitudItem(
    val id: String = "",
    val cliente: String = "",
    val codCliente: String = "",
    val monto: String = "0.00",
    val fecha: String = "",
    val estado: String = "Pendiente",
    val motivo: String = "",
    val saldoActual: String = "0.00",
    val limiteActual: String = "0.00",
    val disponibleActual: String = "0.00"
)