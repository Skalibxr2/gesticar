package com.hans.gesticar.network

import com.hans.gesticar.network.dto.ClienteDto
import com.hans.gesticar.network.dto.ExternalVehicleDto
import com.hans.gesticar.network.dto.LoginRequest
import com.hans.gesticar.network.dto.LoginResponse
import com.hans.gesticar.network.dto.OtDetalleDto
import com.hans.gesticar.network.dto.OtDto
import com.hans.gesticar.network.dto.PresupuestoDto
import com.hans.gesticar.network.dto.TareaDto
import com.hans.gesticar.network.dto.VehiculoDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GesticarApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("ots")
    suspend fun obtenerOts(): List<OtDto>

    @GET("ots/{id}")
    suspend fun obtenerDetalle(@Path("id") otId: String): OtDetalleDto

    @GET("clientes/{rut}")
    suspend fun obtenerCliente(@Path("rut") rut: String): ClienteDto

    @GET("clientes/{rut}/vehiculos")
    suspend fun obtenerVehiculosPorRut(@Path("rut") rut: String): List<VehiculoDto>

    @GET("vehiculos/{patente}")
    suspend fun obtenerVehiculoPorPatente(@Path("patente") patente: String): VehiculoDto

    @GET("ots/{id}/tareas")
    suspend fun obtenerTareas(@Path("id") otId: String): List<TareaDto>

    @GET("ots/{id}/presupuesto")
    suspend fun obtenerPresupuesto(@Path("id") otId: String): PresupuestoDto
}

interface ExternalApiService {
    @GET("vehicles/{patente}")
    suspend fun obtenerVehiculo(@Path("patente") patente: String): ExternalVehicleDto
}
