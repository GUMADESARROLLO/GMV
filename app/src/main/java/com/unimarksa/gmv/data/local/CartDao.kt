package com.unimarksa.gmv.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CartDao {

    @Query("SELECT * FROM carts WHERE clienteCodigo = :codigo")
    suspend fun getActiveCart(codigo: String): CartEntity?

    @Query("SELECT * FROM carts WHERE id = :id")
    suspend fun getCartById(id: Long): CartEntity?

    @Insert
    suspend fun createCart(cart: CartEntity): Long

    @Update
    suspend fun updateCart(cart: CartEntity)

    @Delete
    suspend fun deleteCart(cart: CartEntity)

    @Query("DELETE FROM carts WHERE id = :id")
    suspend fun deleteCartById(id: Long)

    @Query("SELECT * FROM cart_items WHERE cartId = :cartId")
    suspend fun getItems(cartId: Long): List<CartItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addItem(item: CartItemEntity): Long

    @Query("UPDATE cart_items SET cantidad = :cantidad, lineaSubtotal = :subtotal WHERE id = :id")
    suspend fun updateItemQty(id: Long, cantidad: Int, subtotal: Double)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun removeItem(id: Long)

    @Query("DELETE FROM cart_items WHERE cartId = :cartId")
    suspend fun clearItems(cartId: Long)
}