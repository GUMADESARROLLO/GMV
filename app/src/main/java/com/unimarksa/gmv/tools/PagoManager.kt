package com.unimarksa.gmv.tools

import com.unimarksa.gmv.models.ItemPagoFactura

object PagoManager {
    private val _pagos = mutableListOf<ItemPagoFactura>()

    val pagos: List<ItemPagoFactura> get() = _pagos.toList()

    fun add(pago: ItemPagoFactura) { _pagos.add(pago) }

    fun removeAt(index: Int) { if (index in _pagos.indices) _pagos.removeAt(index) }

    fun clear() { _pagos.clear() }

    val totalNC: Double get() = _pagos.sumOf { it.nc }
    val totalRetencion: Double get() = _pagos.sumOf { it.retencion }
    val totalDescuento: Double get() = _pagos.sumOf { it.descuento }
    val totalRecibido: Double get() = _pagos.sumOf { it.valorRecibido }

    val count: Int get() = _pagos.size
}
