package com.unimarksa.gmv.tools

import com.unimarksa.gmv.data.local.AppDatabase
import com.unimarksa.gmv.data.local.ReciboCartEntity
import com.unimarksa.gmv.models.ItemPagoFactura
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PagoManager {
    private val _pagos = mutableListOf<ItemPagoFactura>()
    private var db: AppDatabase? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    val pagos: List<ItemPagoFactura> get() = _pagos.toList()

    fun init(database: AppDatabase) {
        db = database
    }

    fun loadFromDb(onLoaded: () -> Unit = {}) {
        scope.launch {
            val items = db?.reciboCartDao()?.getAll() ?: emptyList()
            _pagos.clear()
            _pagos.addAll(items.map { it.toItemPagoFactura() })
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onLoaded()
            }
        }
    }

    fun add(pago: ItemPagoFactura) {
        _pagos.add(pago)
        scope.launch {
            db?.reciboCartDao()?.insert(pago.toReciboCartEntity())
        }
    }

    /** Reinserta un pago en su posición original (para "Deshacer"). */
    fun insertAt(index: Int, pago: ItemPagoFactura) {
        _pagos.add(index.coerceIn(0, _pagos.size), pago)
        scope.launch {
            db?.reciboCartDao()?.insert(pago.toReciboCartEntity())
        }
    }

    fun removeAt(index: Int) {
        if (index in _pagos.indices) {
            val removed = _pagos.removeAt(index)
            scope.launch {
                db?.reciboCartDao()?.deleteByFactura(removed.numero)
            }
        }
    }

    fun clear() {
        _pagos.clear()
        scope.launch {
            db?.reciboCartDao()?.deleteAll()
        }
    }

    val totalNC: Double get() = _pagos.sumOf { it.nc }
    val totalRetencion: Double get() = _pagos.sumOf { it.retencion }
    val totalDescuento: Double get() = _pagos.sumOf { it.descuento }
    val totalRecibido: Double get() = _pagos.sumOf { it.valorRecibido }

    val count: Int get() = _pagos.size

    private fun ItemPagoFactura.toReciboCartEntity() = ReciboCartEntity(
        numeroFactura = numero,
        nc = nc,
        retencion = retencion,
        descuento = descuento,
        valorRecibido = valorRecibido,
        saldo = saldo,
        action = action,
        estado = estado
    )

    private fun ReciboCartEntity.toItemPagoFactura() = ItemPagoFactura(
        numero = numeroFactura,
        nc = nc,
        retencion = retencion,
        descuento = descuento,
        valorRecibido = valorRecibido,
        saldo = saldo,
        action = action,
        estado = estado
    )
}
