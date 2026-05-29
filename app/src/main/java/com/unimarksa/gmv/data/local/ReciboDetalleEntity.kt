package com.unimarksa.gmv.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recibo_detalles",
    foreignKeys = [ForeignKey(
        entity = ReciboEntity::class,
        parentColumns = ["id"],
        childColumns = ["reciboId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("reciboId")]
)
data class ReciboDetalleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reciboId: Long,
    val numeroFactura: String,
    val total: Double = 0.0,
    val nc: Double = 0.0,
    val retencion: Double = 0.0,
    val descuento: Double = 0.0,
    val valorRecibido: Double = 0.0,
    val saldoPendiente: Double = 0.0,
    val codCliente: String = "",
    val tipoAccion: String = ""
)