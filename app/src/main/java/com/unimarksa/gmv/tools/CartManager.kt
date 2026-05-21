package com.unimarksa.gmv.tools

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.unimarksa.gmv.models.ItemCarrito
import com.unimarksa.gmv.models.ItemProducto

import com.unimarksa.gmv.models.ItemCliente

object CartManager {
    private val _items = MutableLiveData<MutableList<ItemCarrito>>(mutableListOf())
    val items: LiveData<MutableList<ItemCarrito>> = _items

    private val _cartCount = MutableLiveData(0)
    val cartCount: LiveData<Int> = _cartCount

    var ivaPorcentaje: Double = 0.15
        private set

    var currentCliente: ItemCliente? = null
        private set

    val currentItems: List<ItemCarrito>
        get() = _items.value ?: emptyList()

    fun setIva(porcentaje: Double) {
        ivaPorcentaje = porcentaje
    }

    fun setCliente(cliente: ItemCliente?) {
        currentCliente = cliente
    }

    fun add(producto: ItemProducto, cantidad: Double = 1.0, bonificado: String? = null) {
        val list = _items.value ?: mutableListOf()
        val existing = list.find {
            it.producto.codArticulo == producto.codArticulo && it.selectedBonificado == bonificado
        }
        if (existing != null) {
            existing.cantidad += cantidad
        } else {
            list.add(ItemCarrito(producto = producto, cantidad = cantidad, selectedBonificado = bonificado))
        }
        _items.value = list
        _cartCount.value = list.size
    }

    fun updateQuantity(codArticulo: String, cantidad: Double, bonificado: String? = null) {
        val list = _items.value ?: return
        val item = list.find { it.producto.codArticulo == codArticulo && it.selectedBonificado == bonificado }
        if (item != null) {
            if (cantidad <= 0) {
                list.remove(item)
            } else {
                item.cantidad = cantidad
            }
        }
        _items.value = list
        _cartCount.value = list.size
    }

    fun remove(codArticulo: String, bonificado: String? = null) {
        val list = _items.value ?: return
        list.removeAll { it.producto.codArticulo == codArticulo && it.selectedBonificado == bonificado }
        _items.value = list
        _cartCount.value = list.size
    }

    fun clear() {
        _items.value = mutableListOf()
        _cartCount.value = 0
        currentCliente = null
    }

    val subtotal: Double
        get() = currentItems.sumOf { it.subtotal }

    val iva: Double
        get() = subtotal * ivaPorcentaje

    val total: Double
        get() = subtotal + iva

    val itemCount: Int
        get() = _cartCount.value ?: 0
}
