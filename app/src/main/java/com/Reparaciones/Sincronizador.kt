package com.Reparaciones

import android.content.Context
import android.util.Log

class Sincronizador(private val context: Context) {

    suspend fun sincronizar(): String {
        val database = AppDatabase.getDatabase(context)
        val dao = database.reparacionDao()
        val api = RetrofitClient.api

        var subidas = 0
        var borradas = 0
        var bajadas = 0
        var errores = 0

        // =================================================================
        // PASO 1: SUBIR CAMBIOS LOCALES (PUSH)
        // =================================================================
        val pendientes = dao.obtenerNoSincronizadas()

        for (reparacion in pendientes) {
            try {
                if (reparacion.esEliminado) {
                    // Borrado Lógico -> Borrado Físico en Nube
                    val id = reparacion.id
                    if (id != null) {
                        val responseDelete = api.eliminarReparacion(id)
                        if (responseDelete.isSuccessful || responseDelete.code() == 404) {
                            dao.eliminarFisicamente(reparacion)
                            borradas++
                        } else {
                            errores++
                        }
                    }
                } else {
                    // Creación o Actualización
                    val responseCreate = api.crearReparacion(reparacion)
                    if (responseCreate.isSuccessful) {
                        marcarComoSincronizada(dao, reparacion)
                        subidas++
                    } else {
                        // Si falla, intentamos actualizar (PUT)
                        val id = reparacion.id
                        if (id != null) {
                            val responseUpdate = api.actualizarReparacion(id, reparacion)
                            if (responseUpdate.isSuccessful) {
                                marcarComoSincronizada(dao, reparacion)
                                subidas++
                            } else {
                                errores++
                            }
                        } else {
                            errores++
                        }
                    }
                }
            } catch (e: Exception) {
                errores++
                Log.e("Sync", "Error subiendo: ${e.message}")
            }
        }

        // =================================================================
        // PASO 2: DESCARGAR DE LA NUBE (PULL)
        // =================================================================
        try {
            val responseGet = api.obtenerReparaciones()

            if (responseGet.isSuccessful) {
                val listaNube = responseGet.body() ?: emptyList()

                // Recorremos lo que viene de la nube y lo guardamos localmente
                for (repNube in listaNube) {
                    // IMPORTANTE: Lo que viene de la nube YA está sincronizado.
                    // Debemos marcarlo como true para que la app no intente subirlo de nuevo.
                    repNube.esSincronizado = true
                    repNube.esEliminado = false // Aseguramos que no venga marcado como eliminado

                    // Room usa REPLACE, así que si ya existe, lo actualiza. Si no, lo crea.
                    dao.guardar(repNube)
                    bajadas++
                }
            } else {
                errores++
                Log.e("Sync", "Error descargando: ${responseGet.code()}")
            }
        } catch (e: Exception) {
            errores++
            Log.e("Sync", "Error conexión descarga: ${e.message}")
        }

        return "Sync: $subidas subidos, $borradas borrados, $bajadas descargados. ($errores errores)"
    }

    private suspend fun marcarComoSincronizada(dao: ReparacionDao, reparacion: Reparacion) {
        val reparacionSync = reparacion.copy(esSincronizado = true)
        dao.guardar(reparacionSync)
    }
}