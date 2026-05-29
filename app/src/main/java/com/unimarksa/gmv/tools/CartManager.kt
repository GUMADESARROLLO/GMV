package com.unimarksa.gmv.tools

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.unimarksa.gmv.data.local.AppDatabase
import com.unimarksa.gmv.data.local.CartEntity
import com.unimarksa.gmv.data.local.CartItemEntity
import com.unimarksa.gmv.data.local.OrderEntity
import com.unimarksa.gmv.data.local.OrderItemEntity
import com.unimarksa.gmv.models.ItemCarrito
import com.unimarksa.gmv.models.ItemCliente
import com.unimarksa.gmv.models.ItemProducto
import com.unimarksa.gmv.tools.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CartManager {
    private val _items = MutableLiveData<MutableList<ItemCarrito>>(mutableListOf())
    val items: LiveData<MutableList<ItemCarrito>> = _items

    private val _cartCount = MutableLiveData(0)
    val cartCount: LiveData<Int> = _cartCount

    var ivaPorcentaje: Double = 0.0
        private set

    var currentCliente: ItemCliente? = null
        private set

    private var currentCartId: Long? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var db: AppDatabase? = null
    private var deviceHash: Int = 0

    val currentItems: List<ItemCarrito>
        get() = _items.value ?: emptyList()

    fun init(database: AppDatabase, androidId: String? = null) {
        db = database
        deviceHash = androidId?.hashCode() ?: 0
    }

    fun setIva(porcentaje: Double) {
        ivaPorcentaje = porcentaje
    }

    fun setCliente(cliente: ItemCliente?) {
        currentCliente = cliente
    }

    fun switchToClient(cliente: ItemCliente) {
        val database = db ?: return
        // Save current cart items to Room
        saveCurrentCart()
        // Set current cliente with real data
        currentCliente = cliente
        // Clear in-memory
        _items.value = mutableListOf()
        _cartCount.value = 0
        currentCartId = null
        // Load existing cart for new client
        scope.launch {
            val cart = database.cartDao().getActiveCart(cliente.codCliente)
            if (cart != null) {
                currentCartId = cart.id
                val items = database.cartDao().getItems(cart.id)
                val carritoItems = items.map { it.toItemCarrito() }
                _items.postValue(carritoItems.toMutableList())
                _cartCount.postValue(carritoItems.size)
            } else {
                val newId = database.cartDao().createCart(
                    CartEntity(clienteCodigo = cliente.codCliente, clienteNombre = cliente.nombre)
                )
                currentCartId = newId
            }
        }
    }

    fun add(producto: ItemProducto, cantidad: Double = 1.0, bonificado: String? = null) {
        val list = _items.value ?: mutableListOf()
        val item = ItemCarrito(producto = producto, cantidad = cantidad, selectedBonificado = bonificado)
        list.add(item)
        _items.value = list
        _cartCount.value = list.size
        persistItem(item)
    }

    fun updateQuantity(codArticulo: String, cantidad: Double, bonificado: String? = null) {
        val list = _items.value ?: return
        val item = list.find { it.producto.codArticulo == codArticulo && it.selectedBonificado == bonificado }
        if (item != null) {
            if (cantidad <= 0) {
                list.remove(item)
                removePersistedItem(codArticulo, bonificado)
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
        removePersistedItem(codArticulo, bonificado)
    }

    fun clear() {
        _items.value = mutableListOf()
        _cartCount.value = 0
        currentCliente = null
        currentCartId?.let { id ->
            scope.launch { db?.cartDao()?.clearItems(id) }
        }
    }

    fun generarCodigoPedido(): String {
        val prefix = SessionManager.getUsername().uppercase().ifEmpty { "F00" }
        val ts = java.lang.Long.toString(System.currentTimeMillis(), 36)
            .take(7).padStart(7, '0').uppercase()
        val h = deviceHash.let {
            val abs = if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it)
            "${"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"[abs % 36]}" +
            "${"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"[(abs / 36) % 36]}"
        }
        val rnd = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".random()
        return "$prefix-$ts$h$rnd"
    }

    fun submitOrder(codigo: String? = null, observaciones: String = "") {
        val database = db ?: return
        val cliente = currentCliente ?: return
        val cartItems = currentItems
        if (cartItems.isEmpty()) return
        val cartIdToDelete = currentCartId
        val orderCodigo = codigo ?: generarCodigoPedido()

        scope.launch {
            val order = OrderEntity(
                codigo = orderCodigo,
                clienteCodigo = cliente.codCliente,
                clienteNombre = cliente.nombre,
                total = total,
                observaciones = observaciones
            )
            val orderId = database.orderDao().insertOrder(order)
            val orderItems = cartItems.map { item ->
                OrderItemEntity(
                    orderId = orderId,
                    codArticulo = item.producto.codArticulo,
                    nombre = item.producto.nombre,
                    cantidad = item.cantidad.toInt(),
                    precio = item.producto.precio.replace(",", "").toDoubleOrNull() ?: 0.0,
                    lineaSubtotal = item.subtotal,
                    bonificado = item.selectedBonificado
                )
            }
            database.orderDao().insertItems(orderItems)
            // Clear cart
            cartIdToDelete?.let { id ->
                database.cartDao().deleteCartById(id)
            }
        }

        // Clear UI immediately
        _items.postValue(mutableListOf())
        _cartCount.postValue(0)
        currentCartId = null
    }

    val subtotal: Double
        get() = currentItems.sumOf { it.subtotal }

    val iva: Double
        get() = subtotal * ivaPorcentaje

    val total: Double
        get() = subtotal + iva

    val itemCount: Int
        get() = _cartCount.value ?: 0

    private fun saveCurrentCart() {
        val database = db ?: return
        val cartItems = currentItems
        val cartId = currentCartId ?: return
        scope.launch {
            database.cartDao().clearItems(cartId)
            for (item in cartItems) {
                database.cartDao().addItem(item.toCartItemEntity(cartId))
            }
        }
    }

    private fun persistItem(item: ItemCarrito) {
        val database = db ?: return
        var cartId = currentCartId
        scope.launch {
            if (cartId == null) {
                val cliente = currentCliente ?: return@launch
                cartId = database.cartDao().createCart(
                    CartEntity(clienteCodigo = cliente.codCliente, clienteNombre = cliente.nombre)
                )
                currentCartId = cartId
            }
            database.cartDao().addItem(item.toCartItemEntity(cartId!!))
        }
    }

    private fun removePersistedItem(codArticulo: String, bonificado: String?) {
        val database = db ?: return
        val cartId = currentCartId ?: return
        scope.launch {
            database.cartDao().getItems(cartId).forEach { item ->
                if (item.codArticulo == codArticulo) {
                    database.cartDao().removeItem(item.id)
                }
            }
        }
    }

    private fun ItemCarrito.toCartItemEntity(cartId: Long) = CartItemEntity(
        cartId = cartId,
        codArticulo = producto.codArticulo,
        nombre = producto.nombre,
        cantidad = cantidad.toInt(),
        precio = producto.precio.replace(",", "").toDoubleOrNull() ?: 0.0,
        lineaSubtotal = subtotal ?: 0.0,
        bonificado = selectedBonificado
    )

    private fun CartItemEntity.toItemCarrito(): ItemCarrito {
        val producto = ItemProducto(
            nombre = nombre,
            sku = codArticulo,
            codArticulo = codArticulo,
            cantidad = cantidad.toString(),
            precio = String.format("%.2f", precio),
            und = null,
            oferta = null,
            bonificado = null,
            imagenUrl = null
        )
        return ItemCarrito(producto = producto, cantidad = cantidad.toDouble(), selectedBonificado = bonificado)
    }
}
