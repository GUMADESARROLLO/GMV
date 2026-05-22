package com.unimarksa.gmv.models

import java.io.Serializable

data class ItemPerfilCliente(
    val noVencidos: String = "0.00",
    val dias30: String = "0.00",
    val dias60: String = "0.00",
    val dias90: String = "0.00",
    val dias120: String = "0.00",
    val mas120: String = "0.00",
    val factPend: String = ""
) : Serializable
