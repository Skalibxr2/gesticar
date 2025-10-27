package com.hans.gesticar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import com.hans.gesticar.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class UiState(
    val estaAutenticado: Boolean = false,
    val usuarioActual: Usuario? = null,
    val displayName: String? = null,
    val ots: List<Ot> = emptyList(),
    val seleccion: Ot? = null,
    val resultadosBusqueda: List<Ot> = emptyList(),
    val mensaje: String? = null
)

data class CreateOtUiState(
    val numeroOt: Int = 1000,
    val mecanicos: List<Usuario> = emptyList(),
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val exito: Boolean = false,
    val cliente: Cliente? = null,
    val vehiculosCliente: List<Vehiculo> = emptyList(),
    val guardandoCliente: Boolean = false,
    val mensajeCliente: String? = null,
    val guardandoVehiculo: Boolean = false,
    val mensajeVehiculo: String? = null
)

class MainViewModel(
    private val repo: Repository
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private val _createOtUi = MutableStateFlow(CreateOtUiState())
    val createOtUi: StateFlow<CreateOtUiState> = _createOtUi

    init {
        refreshOts()
    }

    private fun refreshOts() {
        viewModelScope.launch(Dispatchers.IO) {
            val ots = repo.obtenerOts()
            _ui.update { it.copy(ots = ots) }
        }
    }

    fun prepararNuevaOt() {
        viewModelScope.launch(Dispatchers.IO) {
            val numero = repo.obtenerSiguienteNumeroOt()
            val mecanicos = repo.obtenerMecanicos()
            _createOtUi.update {
                it.copy(
                    numeroOt = numero,
                    mecanicos = mecanicos,
                    guardando = false,
                    mensaje = null,
                    exito = false,
                    cliente = null,
                    vehiculosCliente = emptyList(),
                    guardandoCliente = false,
                    mensajeCliente = null,
                    guardandoVehiculo = false,
                    mensajeVehiculo = null
                )
            }
        }
    }

    fun buscarClientePorRut(rut: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cliente = repo.buscarClientePorRut(rut)
            val vehiculos = if (cliente != null) repo.obtenerVehiculosPorRut(rut) else emptyList()
            _createOtUi.update {
                it.copy(
                    cliente = cliente,
                    vehiculosCliente = vehiculos,
                    mensajeCliente = if (cliente != null) "Cliente encontrado" else "Cliente no registrado",
                    guardandoCliente = false,
                    guardandoVehiculo = false,
                    mensajeVehiculo = null
                )
            }
        }
    }

    fun guardarCliente(cliente: Cliente) {
        viewModelScope.launch(Dispatchers.IO) {
            _createOtUi.update { it.copy(guardandoCliente = true, mensajeCliente = null) }
            try {
                repo.guardarCliente(cliente)
                val almacenado = repo.buscarClientePorRut(cliente.rut)
                val vehiculos = repo.obtenerVehiculosPorRut(cliente.rut)
                _createOtUi.update {
                    it.copy(
                        guardandoCliente = false,
                        cliente = almacenado,
                        vehiculosCliente = vehiculos,
                        mensajeCliente = "Cliente guardado correctamente"
                    )
                }
            } catch (e: Exception) {
                _createOtUi.update {
                    it.copy(
                        guardandoCliente = false,
                        mensajeCliente = e.message ?: "Error al guardar el cliente"
                    )
                }
            }
        }
    }

    fun guardarVehiculo(vehiculo: Vehiculo) {
        viewModelScope.launch(Dispatchers.IO) {
            _createOtUi.update { it.copy(guardandoVehiculo = true, mensajeVehiculo = null) }
            try {
                repo.guardarVehiculo(vehiculo)
                val vehiculos = repo.obtenerVehiculosPorRut(vehiculo.clienteRut)
                _createOtUi.update {
                    it.copy(
                        guardandoVehiculo = false,
                        vehiculosCliente = vehiculos,
                        mensajeVehiculo = "Vehículo guardado"
                    )
                }
            } catch (e: Exception) {
                _createOtUi.update {
                    it.copy(
                        guardandoVehiculo = false,
                        mensajeVehiculo = e.message ?: "Error al guardar el vehículo"
                    )
                }
            }
        }
    }

    fun crearOt(
        cliente: Cliente,
        vehiculo: Vehiculo,
        mecanicosIds: List<String>,
        presupuestoItems: List<PresupuestoItem>,
        presupuestoAprobado: Boolean,
        sintomas: String?,
        iniciar: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _createOtUi.update { it.copy(guardando = true, mensaje = null, exito = false) }
            try {
                val ot = repo.crearOt(
                    cliente = cliente,
                    vehiculo = vehiculo,
                    mecanicosIds = mecanicosIds,
                    presupuestoItems = presupuestoItems,
                    presupuestoAprobado = presupuestoAprobado,
                    sintomas = sintomas
                )

                if (iniciar) {
                    val pudoIniciar = repo.cambiarEstado(ot.id, OtState.EN_EJECUCION)
                    if (!pudoIniciar) {
                        _createOtUi.update {
                            it.copy(
                                guardando = false,
                                mensaje = "No se pudo iniciar la OT. Verifica que el presupuesto esté aprobado.",
                                exito = false
                            )
                        }
                        refreshOts()
                        return@launch
                    }
                }

                val numeroSiguiente = repo.obtenerSiguienteNumeroOt()
                val mecanicosActualizados = repo.obtenerMecanicos()
                refreshOts()
                _createOtUi.update {
                    it.copy(
                        numeroOt = numeroSiguiente,
                        mecanicos = mecanicosActualizados,
                        guardando = false,
                        mensaje = if (iniciar) "OT creada e iniciada" else "OT guardada como borrador",
                        exito = true,
                        cliente = null,
                        vehiculosCliente = emptyList(),
                        mensajeCliente = null,
                        mensajeVehiculo = null,
                        guardandoCliente = false,
                        guardandoVehiculo = false
                    )
                }
            } catch (e: Exception) {
                _createOtUi.update {
                    it.copy(
                        guardando = false,
                        mensaje = e.message ?: "Error al crear la OT",
                        exito = false
                    )
                }
            }
        }
    }

    // --- Login ---
    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repo.findUserByEmail(email)
            val ok = user != null && (user.password.isBlank() || user.password == password)

            _ui.update {
                it.copy(
                    estaAutenticado = ok,
                    usuarioActual = if (ok) user else null,
                    displayName = user?.nombre,
                    resultadosBusqueda = emptyList(),
                    mensaje = if (ok) null else "Credenciales inválidas"
                )
            }
        }
    }

    fun logout() {
        _ui.update {
            it.copy(
                estaAutenticado = false,
                usuarioActual = null,
                resultadosBusqueda = emptyList()
            )
        }
    }

    // --- Búsquedas ---
    fun buscarPorNumero(numero: Int) {
        val usuario = _ui.value.usuarioActual
        if (usuario == null) {
            _ui.update {
                it.copy(resultadosBusqueda = emptyList(), mensaje = "Debes iniciar sesión para buscar órdenes.")
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val ot = repo.buscarOtPorNumero(numero)
            val resultados = listOfNotNull(ot).filtrarPara(usuario)
            val mensaje = when {
                ot == null -> "Sin resultados"
                resultados.isEmpty() -> "Esta OT no está asignada a ti"
                else -> null
            }
            _ui.update { it.copy(resultadosBusqueda = resultados, mensaje = mensaje) }
        }
    }

    fun buscarPorPatente(patente: String) {
        val usuario = _ui.value.usuarioActual
        if (usuario == null) {
            _ui.update {
                it.copy(resultadosBusqueda = emptyList(), mensaje = "Debes iniciar sesión para buscar órdenes.")
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val lista = repo.buscarOtPorPatente(patente).filtrarPara(usuario)
            val mensaje = if (lista.isEmpty()) "Sin resultados" else null
            _ui.update { it.copy(resultadosBusqueda = lista, mensaje = mensaje) }
        }
    }

    // --- Acciones admin ---
    fun aprobarPresupuesto(otId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.aprobarPresupuesto(otId)
            val currentResults = _ui.value.resultadosBusqueda
            val updatedOt = currentResults.firstOrNull { it.id == otId }?.numero?.let { repo.buscarOtPorNumero(it) }
            val refreshedResults = if (updatedOt != null) {
                currentResults.map { if (it.id == otId) updatedOt else it }
            } else {
                currentResults
            }
            val ots = repo.obtenerOts()
            _ui.update {
                it.copy(
                    mensaje = "Presupuesto aprobado",
                    resultadosBusqueda = refreshedResults,
                    ots = ots
                )
            }
        }
    }

    fun cambiarEstado(otId: String, nuevo: OtState) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repo.cambiarEstado(otId, nuevo)
            val currentResults = _ui.value.resultadosBusqueda
            val refreshedOt = if (ok) {
                currentResults.firstOrNull { it.id == otId }?.numero?.let { repo.buscarOtPorNumero(it) }
            } else {
                null
            }
            val resultados = if (refreshedOt != null) {
                currentResults.map { if (it.id == otId) refreshedOt else it }
            } else {
                currentResults
            }
            val ots = if (ok) repo.obtenerOts() else _ui.value.ots
            _ui.update {
                it.copy(
                    mensaje = if (ok) "Estado actualizado" else "Transición inválida",
                    resultadosBusqueda = resultados,
                    ots = ots
                )
            }
        }
    }
}

private fun List<Ot>.filtrarPara(usuario: Usuario?): List<Ot> {
    if (usuario?.rol == Rol.MECANICO) {
        return filter { usuario.id in it.mecanicosAsignados }
    }
    return this
}
