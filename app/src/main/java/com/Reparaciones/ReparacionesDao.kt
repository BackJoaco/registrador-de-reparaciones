package com.Reparaciones

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ReparacionDao {
    // Obtener todas
    @Query("SELECT * FROM reparaciones WHERE esEliminado = 0 ORDER BY fecha DESC")
    suspend fun obtenerTodas(): List<Reparacion>

    // Buscar por nombre (reemplaza tu filtro manual)
    @Query("SELECT * FROM reparaciones WHERE nombre LIKE '%' || :query || '%' OR apellido LIKE '%' || :query || '%'")
    suspend fun buscar(query: String): List<Reparacion>

    @Query("UPDATE reparaciones SET esEliminado = 1, esSincronizado = 0 WHERE id = :id")
    suspend fun marcarComoEliminado(id: String)

    // Guardar o Actualizar (Si el ID existe, lo reemplaza)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(reparacion: Reparacion)

    // Borrar
    @Delete
    suspend fun eliminarFisicamente(reparacion: Reparacion)

    @Query("SELECT * FROM reparaciones WHERE esSincronizado = 0")
    suspend fun obtenerNoSincronizadas(): List<Reparacion>

    // Necesario para buscar por ID en el sincronizador
    @Query("SELECT * FROM reparaciones WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): Reparacion?
}