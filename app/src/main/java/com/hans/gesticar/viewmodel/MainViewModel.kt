package com.hans.gesticar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtDetalle
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.TareaOt
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
    val resultadosBusqueda: List<Ot> = emptyList(),
    val mensaje: String? = null,
    val detalleSeleccionado: OtDetalle? = null,
    val mecanicosDisponibles: List<Usuario> = emptyList(),
    val cargandoDetalle: Boolean = false
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

    private suspend fun actualizarDetalle(otId: String, mensaje: String? = null) {
        val detalle = repo.obtenerDetalleOt(otId)
        val mecanicos = repo.obtenerMecanicos()
        val ots = repo.obtenerOts()
        val resultadosActualizados = if (detalle != null) {
            _ui.value.resultadosBusqueda.map { if (it.id == detalle.ot.id) detalle.ot else it }
        } else {
            _ui.value.resultadosBusqueda
        }
        _ui.update {
            it.copy(
                detalleSeleccionado = detalle,
                mecanicosDisponibles = mecanicos,
                cargandoDetalle = false,
                mensaje = mensaje,
                resultadosBusqueda = resultadosActualizados,
                ots = ots
            )
        }
    }

    fun seleccionarOt(otId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(cargandoDetalle = true, mensaje = null) }
            actualizarDetalle(otId)
        }
    }

    fun limpiarSeleccion() {
        _ui.update { it.copy(detalleSeleccionado = null, cargandoDetalle = false) }
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
                resultadosBusqueda = emptyList(),
                detalleSeleccionado = null,
                mecanicosDisponibles = emptyList(),
                cargandoDetalle = false
            )
        }
    }

    // --- Búsquedas ---
    fun buscarPorNumero(numero: Int) {
        val usuario = _ui.value.usuarioActual
        if (usuario == null) {
            _ui.update {
                it.copy(
                    resultadosBusqueda = emptyList(),
                    mensaje = "Debes iniciar sesión para buscar órdenes.",
                    detalleSeleccionado = null,
                    cargandoDetalle = false
                )
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
            _ui.update {
                it.copy(
                    resultadosBusqueda = resultados,
                    mensaje = mensaje,
                    detalleSeleccionado = null,
                    cargandoDetalle = false
                )
            }
        }
    }

    fun buscarPorPatente(patente: String) {
        val usuario = _ui.value.usuarioActual
        if (usuario == null) {
            _ui.update {
                it.copy(
                    resultadosBusqueda = emptyList(),
                    mensaje = "Debes iniciar sesión para buscar órdenes.",
                    detalleSeleccionado = null,
                    cargandoDetalle = false
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val lista = repo.buscarOtPorPatente(patente).filtrarPara(usuario)
            val mensaje = if (lista.isEmpty()) "Sin resultados" else null
            _ui.update {
                it.copy(
                    resultadosBusqueda = lista,
                    mensaje = mensaje,
                    detalleSeleccionado = null,
                    cargandoDetalle = false
                )
            }
        }
    }

    fun buscarPorRut(rut: String) {
        val usuario = _ui.value.usuarioActual
        if (usuario == null) {
            _ui.update {
                it.copy(
                    resultadosBusqueda = emptyList(),
                    mensaje = "Debes iniciar sesión para buscar órdenes.",
                    detalleSeleccionado = null,
                    cargandoDetalle = false
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val lista = repo.buscarOtPorRut(rut).filtrarPara(usuario)
            val mensaje = if (lista.isEmpty()) "Sin resultados" else null
            _ui.update {
                it.copy(
                    resultadosBusqueda = lista,
                    mensaje = mensaje,
                    detalleSeleccionado = null,
                    cargandoDetalle = false
                )
            }
        }
    }

    fun buscarPorEstado(estado: OtState) {
        val usuario = _ui.value.usuarioActual
        if (usuario == null) {
            _ui.update {
                it.copy(
                    resultadosBusqueda = emptyList(),
                    mensaje = "Debes iniciar sesión para buscar órdenes.",
                    detalleSeleccionado = null,
                    cargandoDetalle = false
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val lista = repo.buscarOtPorEstado(estado).filtrarPara(usuario)
            val mensaje = if (lista.isEmpty()) "Sin resultados" else null
            _ui.update {
                it.copy(
                    resultadosBusqueda = lista,
                    mensaje = mensaje,
                    detalleSeleccionado = null,
                    cargandoDetalle = false
                )
            }
        }
    }

    // --- Gestión detalle OT ---
    fun guardarDatosOt(otId: String, notas: String?, mecanicosIds: List<String>, vehiculoPatente: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val detalleActual = repo.obtenerDetalleOt(otId)
            val vehiculoActual = detalleActual?.ot?.vehiculoPatente
            val patenteNormalizada = vehiculoPatente?.uppercase()?.takeIf { it.isNotBlank() }
            var mensaje: String? = null
            if (patenteNormalizada != null && patenteNormalizada != vehiculoActual) {
                val ok = repo.actualizarVehiculoOt(otId, patenteNormalizada)
                if (!ok) {
                    mensaje = "No se pudo actualizar el vehículo. Verifica la patente o el estado de la OT."
                }
            }
            repo.actualizarNotasOt(otId, notas)
            repo.actualizarMecanicosOt(otId, mecanicosIds)
            refreshOts()
            actualizarDetalle(otId, mensaje ?: "Datos generales guardados")
        }
    }

    fun guardarPresupuesto(otId: String, items: List<PresupuestoItem>, aprobado: Boolean, ivaPorc: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.guardarPresupuesto(otId, items, aprobado, ivaPorc)
            refreshOts()
            actualizarDetalle(otId, "Presupuesto actualizado")
        }
    }

    fun guardarTareas(otId: String, tareas: List<TareaOt>) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.guardarTareas(otId, tareas)
            actualizarDetalle(otId, "Tareas actualizadas")
        }
    }

    fun iniciarOt(otId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val detalle = repo.obtenerDetalleOt(otId)
            if (detalle == null) {
                _ui.update { it.copy(mensaje = "No se encontró la OT seleccionada") }
                return@launch
            }
            val datosCompletos = detalle.cliente != null && detalle.vehiculo != null &&
                detalle.ot.mecanicosAsignados.isNotEmpty() && detalle.presupuesto.items.isNotEmpty()
            if (!detalle.presupuesto.aprobado || !datosCompletos) {
                _ui.update {
                    it.copy(
                        mensaje = "Faltan datos críticos para iniciar la OT",
                        detalleSeleccionado = detalle
                    )
                }
                return@launch
            }
            val ok = repo.cambiarEstado(otId, OtState.EN_EJECUCION)
            if (!ok) {
                _ui.update { it.copy(mensaje = "No fue posible iniciar la OT") }
            } else {
                refreshOts()
                actualizarDetalle(otId, "OT iniciada")
            }
        }
    }

    fun finalizarOt(otId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repo.cambiarEstado(otId, OtState.FINALIZADA)
            if (!ok) {
                _ui.update { it.copy(mensaje = "No fue posible finalizar la OT") }
            } else {
                refreshOts()
                actualizarDetalle(otId, "OT finalizada")
            }
        }
    }

    // --- Acciones admin ---
    fun aprobarPresupuesto(otId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.aprobarPresupuesto(otId)
            val seleccionado = _ui.value.detalleSeleccionado?.ot?.id
            if (seleccionado == otId) {
                actualizarDetalle(otId, "Presupuesto aprobado")
            } else {
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
            val seleccionado = _ui.value.detalleSeleccionado?.ot?.id
            if (ok && seleccionado == otId) {
                actualizarDetalle(otId, "Estado actualizado")
            } else {
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
}

private fun List<Ot>.filtrarPara(usuario: Usuario?): List<Ot> {
    if (usuario?.rol == Rol.MECANICO) {
        return filter { usuario.id in it.mecanicosAsignados }
    }
    return this
}
