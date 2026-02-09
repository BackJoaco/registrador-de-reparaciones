package com.Reparaciones

import android.content.Context
import android.util.Log

class Sincronizador(private val context: Context) {

    suspend fun sincronizarPendientes(): String {
        val database = AppDatabase.getDatabase(context)
        val dao = database.reparacionDao()
        val api = RetrofitClient.api

        // 1. Obtener TODAS las pendientes (incluye nuevas, modificadas y marcadas para borrar)
        val pendientes = dao.obtenerNoSincronizadas()

        if (pendientes.isEmpty()) {
            return "Todo está al día."
        }

        var subidas = 0
        var borradas = 0
        var errores = 0

        for (reparacion in pendientes) {
            try {
                // --- NUEVA LÓGICA: ¿Es un borrado pendiente? ---
                if (reparacion.esEliminado) {
                    val id = reparacion.id
                    if (id != null) {
                        // Llamamos a DELETE en la API
                        val responseDelete = api.eliminarReparacion(id)

                        // Si el servidor dice OK (200) O si dice que "No encontrado" (404)
                        // (el 404 es bueno aquí, significa que ya no está en la nube),
                        // entonces procedemos a borrarlo FÍSICAMENTE del celular para siempre.
                        if (responseDelete.isSuccessful || responseDelete.code() == 404) {
                            dao.eliminarFisicamente(reparacion)
                            borradas++
                        } else {
                            errores++
                            Log.e("Sync", "Fallo al borrar en nube ${reparacion.id}: ${responseDelete.code()}")
                        }
                    }
                } else {
                    // --- LÓGICA ANTERIOR: Crear o Actualizar ---

                    // INTENTO 1: Crear (POST)
                    val responseCreate = api.crearReparacion(reparacion)

                    if (responseCreate.isSuccessful) {
                        marcarComoSincronizada(dao, reparacion)
                        subidas++
                    } else {
                        // Si falla (probablemente porque ya existe ID), intentamos UPDATE (PUT)
                        val id = reparacion.id
                        if (id != null) {
                            val responseUpdate = api.actualizarReparacion(id, reparacion)
                            if (responseUpdate.isSuccessful) {
                                marcarComoSincronizada(dao, reparacion)
                                subidas++
                            } else {
                                errores++
                                Log.e("Sync", "Fallo al actualizar ${reparacion.id}: ${responseUpdate.code()}")
                            }
                        } else {
                            errores++
                        }
                    }
                }
            } catch (e: Exception) {
                // Error de red (sin internet o servidor apagado)
                errores++
                Log.e("Sync", "Error de conexión: ${e.message}")
            }
        }

        return "Sync: $subidas subidos, $borradas borrados, $errores errores."
    }

    private suspend fun marcarComoSincronizada(dao: ReparacionDao, reparacion: Reparacion) {
        // Creamos una copia del objeto con el flag en TRUE y lo guardamos
        val reparacionSync = reparacion.copy(esSincronizado = true)
        dao.guardar(reparacionSync)
    }
}