package com.unimarksa.gmv.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cart_items",
    foreignKeys = [ForeignKey(
        entity = CartEntity::class,
        parentColumns = ["id"],
        childColumns = ["cartId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cartId")]
)
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cartId: Long,
    val codArticulo: String,
    val nombre: String,
    val cantidad: Int,
    val precio: Double,
    val lineaSubtotal: Double,
    val bonificado: String? = null
)