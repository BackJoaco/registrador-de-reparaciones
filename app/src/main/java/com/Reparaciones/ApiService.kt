package com.Reparaciones

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("reparaciones")
    suspend fun obtenerReparaciones(): Response<List<Reparacion>>

    @POST("reparaciones")
    suspend fun crearReparacion(@Body reparacion: Reparacion): Response<Reparacion>

    @PUT("reparaciones/{id}")
    suspend fun actualizarReparacion(@Path("id") id: String, @Body reparacion: Reparacion): Response<Reparacion>

    @DELETE("reparaciones/{id}")
    suspend fun eliminarReparacion(@Path("id") id: String): Response<Void>
}