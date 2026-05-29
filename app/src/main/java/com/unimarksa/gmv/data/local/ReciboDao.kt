package com.unimarksa.gmv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ReciboDao {

    @Insert
    suspend fun insertRecibo(recibo: ReciboEntity): Long

    @Insert
    suspend fun insertDetalles(detalles: List<ReciboDetalleEntity>)

    @Update
    suspend fun updateRecibo(recibo: ReciboEntity)

    @Query("UPDATE recibos SET estado = :estado, fechaEnvio = :fecha WHERE id = :id")
    suspend fun marcarEnviado(id: Long, estado: String = "enviado", fecha: Long = System.currentTimeMillis())

    @Query("SELECT * FROM recibos ORDER BY fecha DESC")
    suspend fun getAllRecibos(): List<ReciboEntity>

    @Query("SELECT * FROM recibos WHERE estado = :estado ORDER BY fecha DESC")
    suspend fun getRecibosByEstado(estado: String): List<ReciboEntity>

    @Query("SELECT * FROM recibo_detalles WHERE reciboId = :reciboId")
    suspend fun getDetallesByReciboId(reciboId: Long): List<ReciboDetalleEntity>
}