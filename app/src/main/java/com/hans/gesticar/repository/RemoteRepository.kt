package com.hans.gesticar.repository

import com.hans.gesticar.BuildConfig
import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtDetalle
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import com.hans.gesticar.network.ExternalApiService
import com.hans.gesticar.network.GesticarApiService
import com.hans.gesticar.network.dto.LoginRequest
import com.hans.gesticar.network.dto.OtDetalleDto
import com.hans.gesticar.util.normalizeRut
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

internal class AuthTokenStore {
    val token = AtomicReference<String?>(null)
}

internal class RemoteRepository(
    private val api: GesticarApiService,
    private val externalApi: ExternalApiService,
    private val tokenStore: AuthTokenStore = AuthTokenStore()
) {

    private var cachedOts: List<Ot> = emptyList()
    private val detalleCache = ConcurrentHashMap<String, OtDetalle>()

    suspend fun login(email: String, password: String): Result<Usuario> = runCatching {
        val response = api.login(LoginRequest(email, password))
        tokenStore.token.set(response.token)
        response.usuario?.toDomain(Rol.ADMIN) ?: Usuario(
            nombre = email.substringBefore("@"),
            email = email,
            password = password,
            rol = Rol.ADMIN
        )
    }

    suspend fun obtenerOts(forceRefresh: Boolean = false): Result<List<Ot>> = runCatching {
        if (cachedOts.isNotEmpty() && !forceRefresh) return@runCatching cachedOts
        val remotos = api.obtenerOts().mapNotNull { it.toDomain() }
        cachedOts = remotos
        remotos
    }

    suspend fun obtenerDetalleOt(otId: String, forceRefresh: Boolean = false): Result<OtDetalle?> = runCatching {
        if (!forceRefresh) {
            detalleCache[otId]?.let { return@runCatching it }
        }
        val dto = api.obtenerDetalle(otId)
        val detalle = dto.toDomain()
        if (detalle != null) {
            detalleCache[otId] = detalle
        }
        detalle
    }

    suspend fun obtenerCliente(rut: String): Result<Cliente?> = runCatching {
        val normalized = normalizeRut(rut)
        api.obtenerCliente(normalized).toDomain()
    }

    suspend fun obtenerVehiculosPorRut(rut: String): Result<List<Vehiculo>> = runCatching {
        val normalized = normalizeRut(rut)
        api.obtenerVehiculosPorRut(normalized).mapNotNull { it.toDomain() }
    }

    suspend fun obtenerVehiculoPorPatente(patente: String): Result<Vehiculo?> = runCatching {
        api.obtenerVehiculoPorPatente(patente.uppercase()).toDomain()
    }

    suspend fun obtenerDetalleLiviano(otId: String): Result<OtDetalleDto> = runCatching {
        api.obtenerDetalle(otId)
    }

    suspend fun obtenerVehiculoExterno(patente: String): Result<Vehiculo?> = runCatching {
        externalApi.obtenerVehiculo(patente.uppercase()).toVehiculo(patente)
    }

    fun limpiarSesion() {
        cachedOts = emptyList()
        detalleCache.clear()
        tokenStore.token.set(null)
    }

    companion object {
        fun create(): RemoteRepository {
            val tokenStore = AuthTokenStore()
            val authInterceptor = Interceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                tokenStore.token.get()?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logger)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            val api = retrofit.create(GesticarApiService::class.java)

            val externalRetrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.EXTERNAL_API_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            val externalApi = externalRetrofit.create(ExternalApiService::class.java)

            return RemoteRepository(api, externalApi, tokenStore)
        }
    }
}