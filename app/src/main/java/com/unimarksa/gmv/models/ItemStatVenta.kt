package com.unimarksa.gmv.models

data class ItemStatVenta(
    val ventaReal: Double = 0.0,
    val metaVenta: Double = 0.0,
    val ventaDif: Double = 0.0,
    val vntCanti: Int = 0,
    val vntCantiReal: Int = 0,
    val vntCantiDif: Int = 0
)
