package com.unimarksa.gmv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReciboCartDao {

    @Query("SELECT * FROM recibo_cart")
    suspend fun getAll(): List<ReciboCartEntity>

    @Insert
    suspend fun insert(item: ReciboCartEntity): Long

    @Query("DELETE FROM recibo_cart WHERE numeroFactura = :numeroFactura")
    suspend fun deleteByFactura(numeroFactura: String)

    @Query("DELETE FROM recibo_cart")
    suspend fun deleteAll()
}
