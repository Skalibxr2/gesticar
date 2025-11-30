package com.hans.gesticar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtDetalle
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.SintomaInput
import com.hans.gesticar.model.TareaOt
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import com.hans.gesticar.repository.Repository
import com.hans.gesticar.util.normalizeRut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class SectionMessage(
    val text: String,
    val isError: Boolean = false
)

data class DetalleMensajes(
    val datos: SectionMessage? = null,
    val presupuesto: SectionMessage? = null,
    val tareas: SectionMessage? = null,
    val estado: SectionMessage? = null
)

data class UiState(
    val estaAutenticado: Boolean = false,
    val usuarioActual: Usuario? = null,
    val displayName: String? = null,
    val ots: List<Ot> = emptyList(),
    val resultadosBusqueda: List<SearchResult> = emptyList(),
    val mensaje: String? = null,
    val detalleSeleccionado: OtDetalle? = null,
    val mecanicosDisponibles: List<Usuario> = emptyList(),
    val vehiculosCliente: List<Vehiculo> = emptyList(),
    val detalleMensajes: DetalleMensajes = DetalleMensajes(),
    val cargandoDetalle: Boolean = false
)

data class SearchResult(
    val ot: Ot,
    val clienteNombre: String?,
    val patente: String,
    val estado: OtState
)

data class CreateOtUiState(
    val numeroOt: Int = 1000,
    val mecanicos: List<Usuario> = emptyList(),
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val exito: Boolean = false,
    val cliente: Cliente? = null,
    val vehiculosCliente: List<Vehiculo> = emptyList(),
    val vehiculoBuscado: Vehiculo? = null,
    val mensajeBusquedaVehiculo: String? = null,
    val buscandoVehiculo: Boolean = false,
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

    private suspend fun actualizarDetalle(
        otId: String,
        mensajesDetalle: DetalleMensajes = DetalleMensajes()
    ) {
        val detalle = repo.obtenerDetalleOt(otId)
        val mecanicos = repo.obtenerMecanicos()
        val ots = repo.obtenerOts()
        val resultadosActualizados = if (detalle != null) {
            _ui.value.resultadosBusqueda.map { resultado ->
                if (resultado.ot.id == detalle.ot.id) {
                    resultado.copy(
                        ot = detalle.ot,
                        clienteNombre = detalle.cliente?.nombre,
                        patente = detalle.vehiculo?.patente ?: detalle.ot.vehiculoPatente,
                        estado = detalle.ot.estado
                    )
                } else {
                    resultado
                }
            }
        } else {
            _ui.value.resultadosBusqueda
        }
        val rutCliente = detalle?.cliente?.rut ?: detalle?.vehiculo?.clienteRut
        val vehiculos = rutCliente?.let { repo.obtenerVehiculosPorRut(it) } ?: emptyList()
        _ui.update {
            it.copy(
                detalleSeleccionado = detalle,
                mecanicosDisponibles = mecanicos,
                cargandoDetalle = false,
                mensaje = null,
                resultadosBusqueda = resultadosActualizados,
                ots = ots,
                vehiculosCliente = vehiculos,
                detalleMensajes = mensajesDetalle
            )
        }
    }

    fun seleccionarOt(otId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update {
                it.copy(
                    cargandoDetalle = true,
                    mensaje = null,
                    detalleMensajes = DetalleMensajes(),
                    vehiculosCliente = emptyList()
                )
            }
            actualizarDetalle(otId)
        }
    }

    fun limpiarSeleccion() {
        _ui.update {
            it.copy(
                detalleSeleccionado = null,
                cargandoDetalle = false,
                vehiculosCliente = emptyList(),
                detalleMensajes = DetalleMensajes()
            )
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
                    vehiculoBuscado = null,
                    mensajeBusquedaVehiculo = null,
                    buscandoVehiculo = false,
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
                    vehiculoBuscado = null,
                    mensajeBusquedaVehiculo = null,
                    buscandoVehiculo = false,
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
                val yaExistia = repo.buscarClientePorRut(cliente.rut) != null
                repo.guardarCliente(cliente)
                val almacenado = repo.buscarClientePorRut(cliente.rut)
                val vehiculos = repo.obtenerVehiculosPorRut(cliente.rut)
                _createOtUi.update {
                    it.copy(
                        guardandoCliente = false,
                        cliente = almacenado,
                        vehiculosCliente = vehiculos,
                        mensajeCliente = if (yaExistia) "Cliente actualizado correctamente" else "Cliente creado correctamente"
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

    fun buscarVehiculoPorPatente(patente: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _createOtUi.update {
                it.copy(
                    buscandoVehiculo = true,
                    vehiculoBuscado = null,
                    mensajeBusquedaVehiculo = null,
                    mensajeVehiculo = null
                )
            }
            val encontrado = repo.buscarVehiculoPorPatente(patente)
            _createOtUi.update {
                it.copy(
                    buscandoVehiculo = false,
                    vehiculoBuscado = encontrado,
                    mensajeBusquedaVehiculo = if (encontrado != null) "Patente encontrada" else "Patente no existe en el sistema"
                )
            }
        }
    }

    fun desasociarVehiculoDeCliente(patente: String, clienteRut: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.desasociarVehiculo(patente)
                val vehiculos = repo.obtenerVehiculosPorRut(clienteRut)
                _createOtUi.update {
                    it.copy(
                        vehiculosCliente = vehiculos,
                        mensajeVehiculo = "Vehículo desvinculado del cliente"
                    )
                }
            } catch (e: Exception) {
                _createOtUi.update {
                    it.copy(mensajeVehiculo = e.message ?: "Error al desvincular el vehículo")
                }
            }
        }
    }

    fun reasignarVehiculoACliente(patente: String, clienteRut: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.actualizarClienteVehiculo(patente, clienteRut)
                val vehiculos = repo.obtenerVehiculosPorRut(clienteRut)
                _createOtUi.update {
                    it.copy(
                        vehiculosCliente = vehiculos,
                        mensajeVehiculo = "Vehículo asociado al cliente",
                        vehiculoBuscado = null,
                        mensajeBusquedaVehiculo = null
                    )
                }
            } catch (e: Exception) {
                _createOtUi.update {
                    it.copy(mensajeVehiculo = e.message ?: "Error al actualizar la asociación del vehículo")
                }
            }
        }
    }

    fun limpiarMensajesVehiculo() {
        _createOtUi.update { it.copy(mensajeVehiculo = null) }
    }

    fun reportarMensajeVehiculo(mensaje: String) {
        _createOtUi.update { it.copy(mensajeVehiculo = mensaje) }
    }

    fun limpiarBusquedaVehiculo() {
        _createOtUi.update {
            it.copy(
                vehiculoBuscado = null,
                mensajeBusquedaVehiculo = null,
                buscandoVehiculo = false
            )
        }
    }

    fun crearOt(
        cliente: Cliente,
        vehiculo: Vehiculo,
        mecanicosIds: List<String>,
        presupuestoItems: List<PresupuestoItem>,
        presupuestoAprobado: Boolean,
        sintomas: List<SintomaInput>,
        tareas: List<TareaOt>,
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
                    sintomas = sintomas,
                    tareas = tareas
                )

                if (iniciar) {
                    if (mecanicosIds.isEmpty()) {
                        _createOtUi.update {
                            it.copy(
                                guardando = false,
                                mensaje = "Asigna al menos un mecánico antes de iniciar la OT.",
                                exito = false
                            )
                        }
                        refreshOts()
                        return@launch
                    }
                    val pudoIniciar = repo.cambiarEstado(ot.id, OtState.EN_EJECUCION)
                    if (!pudoIniciar) {
                        _createOtUi.update {
                            it.copy(
                                guardando = false,
                                mensaje = "No se pudo iniciar la OT. Verifica que el presupuesto esté aprobado y que haya mecánicos asignados.",
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
                    mensaje = if (ok) null else "Credenciales inválidas",
                    detalleMensajes = DetalleMensajes(),
                    vehiculosCliente = emptyList(),
                    detalleSeleccionado = null,
                    mecanicosDisponibles = emptyList(),
                    cargandoDetalle = false
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
                cargandoDetalle = false,
                vehiculosCliente = emptyList(),
                detalleMensajes = DetalleMensajes(),
                mensaje = null
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
                    cargandoDetalle = false,
                    vehiculosCliente = emptyList(),
                    detalleMensajes = DetalleMensajes()
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
                    resultadosBusqueda = mapearResultados(resultados),
                    mensaje = mensaje,
                    detalleSeleccionado = null,
                    cargandoDetalle = false,
                    vehiculosCliente = emptyList(),
                    detalleMensajes = DetalleMensajes()
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
                    cargandoDetalle = false,
                    vehiculosCliente = emptyList(),
                    detalleMensajes = DetalleMensajes()
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val lista = repo.buscarOtPorPatente(patente).filtrarPara(usuario)
            val mensaje = if (lista.isEmpty()) "Sin resultados" else null
            _ui.update {
                it.copy(
                    resultadosBusqueda = mapearResultados(lista),
                    mensaje = mensaje,
                    detalleSeleccionado = null,
                    cargandoDetalle = false,
                    vehiculosCliente = emptyList(),
                    detalleMensajes = DetalleMensajes()
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
                    cargandoDetalle = false,
                    vehiculosCliente = emptyList(),
                    detalleMensajes = DetalleMensajes()
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val lista = repo.buscarOtPorRut(rut).filtrarPara(usuario)
            val mensaje = if (lista.isEmpty()) "Sin resultados" else null
            _ui.update {
                it.copy(
                    resultadosBusqueda = mapearResultados(lista),
                    mensaje = mensaje,
                    detalleSeleccionado = null,
                    cargandoDetalle = false,
                    vehiculosCliente = emptyList(),
                    detalleMensajes = DetalleMensajes()
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
                    cargandoDetalle = false,
                    vehiculosCliente = emptyList(),
                    detalleMensajes = DetalleMensajes()
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val lista = repo.buscarOtPorEstado(estado).filtrarPara(usuario)
            val mensaje = if (lista.isEmpty()) "Sin resultados" else null
            _ui.update {
                it.copy(
                    resultadosBusqueda = mapearResultados(lista),
                    mensaje = mensaje,
                    detalleSeleccionado = null,
                    cargandoDetalle = false,
                    vehiculosCliente = emptyList(),
                    detalleMensajes = DetalleMensajes()
                )
            }
        }
    }

    fun buscarPorFiltros(numero: Int?, patente: String?, rut: String?, estado: OtState?) {
        val usuario = _ui.value.usuarioActual
        if (usuario == null) {
            _ui.update {
                it.copy(
                    resultadosBusqueda = emptyList(),
                    mensaje = "Debes iniciar sesión para buscar órdenes.",
                    detalleSeleccionado = null,
                    cargandoDetalle = false,
                    vehiculosCliente = emptyList(),
                    detalleMensajes = DetalleMensajes()
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val rutNormalizado = rut?.let { normalizeRut(it) }
            val patenteNormalizada = patente?.uppercase()
            val idsPorRut = rutNormalizado?.let { rutInput ->
                repo.buscarOtPorRut(rutInput).map { it.id }.toSet()
            }
            val resultados = repo.obtenerOts()
                .filter { numero == null || it.numero == numero }
                .filter { patenteNormalizada == null || it.vehiculoPatente.equals(patenteNormalizada, ignoreCase = true) }
                .filter { idsPorRut == null || it.id in idsPorRut }
                .filter { estado == null || it.estado == estado }
                .filtrarPara(usuario)

            val mensaje = if (resultados.isEmpty()) "Sin resultados" else null
            _ui.update {
                it.copy(
                    resultadosBusqueda = mapearResultados(resultados),
                    mensaje = mensaje,
                    detalleSeleccionado = null,
                    cargandoDetalle = false,
                    vehiculosCliente = emptyList(),
                    detalleMensajes = DetalleMensajes()
                )
            }
        }
    }

    private suspend fun construirResultado(ot: Ot): SearchResult {
        val vehiculo = repo.buscarVehiculoPorPatente(ot.vehiculoPatente)
        val cliente = vehiculo?.let { v -> repo.buscarClientePorRut(v.clienteRut) }
        return SearchResult(
            ot = ot,
            clienteNombre = cliente?.nombre,
            patente = vehiculo?.patente ?: ot.vehiculoPatente,
            estado = ot.estado
        )
    }

    private suspend fun mapearResultados(ots: List<Ot>): List<SearchResult> = ots.map { construirResultado(it) }

    fun limpiarResultados() {
        _ui.update {
            it.copy(
                resultadosBusqueda = emptyList(),
                mensaje = null,
                detalleSeleccionado = null,
                cargandoDetalle = false,
                vehiculosCliente = emptyList(),
                detalleMensajes = DetalleMensajes()
            )
        }
    }

    // --- Gestión detalle OT ---
    fun guardarDatosOt(otId: String, notas: String?, mecanicosIds: List<String>, vehiculoPatente: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val detalleActual = repo.obtenerDetalleOt(otId)
            val vehiculoActual = detalleActual?.ot?.vehiculoPatente
            val patenteNormalizada = vehiculoPatente?.uppercase()?.takeIf { it.isNotBlank() }
            var error: String? = null
            if (patenteNormalizada != null && patenteNormalizada != vehiculoActual) {
                val ok = repo.actualizarVehiculoOt(otId, patenteNormalizada)
                if (!ok) {
                    error = "No se pudo actualizar el vehículo. Verifica la patente o el estado de la OT."
                }
            }
            repo.actualizarNotasOt(otId, notas)
            repo.actualizarMecanicosOt(otId, mecanicosIds)
            refreshOts()
            val mensajeSeccion = error?.let { SectionMessage(it, isError = true) }
                ?: SectionMessage("Datos generales guardados")
            actualizarDetalle(otId, DetalleMensajes(datos = mensajeSeccion))
        }
    }

    fun guardarPresupuesto(otId: String, items: List<PresupuestoItem>, aprobado: Boolean, ivaPorc: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.guardarPresupuesto(otId, items, aprobado, ivaPorc)
            refreshOts()
            actualizarDetalle(otId, DetalleMensajes(presupuesto = SectionMessage("Presupuesto actualizado")))
        }
    }

    fun guardarTareas(otId: String, tareas: List<TareaOt>) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.guardarTareas(otId, tareas)
            actualizarDetalle(otId, DetalleMensajes(tareas = SectionMessage("Tareas actualizadas")))
        }
    }

    fun iniciarOt(otId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val detalle = repo.obtenerDetalleOt(otId)
            if (detalle == null) {
                _ui.update {
                    it.copy(
                        mensaje = "No se encontró la OT seleccionada",
                        detalleMensajes = DetalleMensajes(),
                        vehiculosCliente = emptyList()
                    )
                }
                return@launch
            }
            val datosCompletos = detalle.cliente != null && detalle.vehiculo != null &&
                detalle.ot.mecanicosAsignados.isNotEmpty() && detalle.presupuesto.items.isNotEmpty()
            if (detalle.ot.mecanicosAsignados.isEmpty()) {
                _ui.update {
                    it.copy(
                        detalleSeleccionado = detalle,
                        detalleMensajes = DetalleMensajes(
                            estado = SectionMessage(
                                text = "Asigna al menos un mecánico antes de iniciar la OT",
                                isError = true
                            )
                        )
                    )
                }
                return@launch
            }
            if (!detalle.presupuesto.aprobado || !datosCompletos) {
                _ui.update {
                    it.copy(
                        detalleSeleccionado = detalle,
                        detalleMensajes = DetalleMensajes(
                            estado = SectionMessage(
                                text = "Faltan datos críticos para iniciar la OT",
                                isError = true
                            )
                        )
                    )
                }
                return@launch
            }
            val ok = repo.cambiarEstado(otId, OtState.EN_EJECUCION)
            if (!ok) {
                _ui.update {
                    it.copy(
                        detalleMensajes = DetalleMensajes(
                            estado = SectionMessage("No fue posible iniciar la OT", isError = true)
                        )
                    )
                }
            } else {
                refreshOts()
                actualizarDetalle(otId, DetalleMensajes(estado = SectionMessage("OT iniciada")))
            }
        }
    }

    fun finalizarOt(otId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repo.cambiarEstado(otId, OtState.FINALIZADA)
            if (!ok) {
                _ui.update {
                    it.copy(
                        detalleMensajes = DetalleMensajes(
                            estado = SectionMessage("No fue posible finalizar la OT", isError = true)
                        )
                    )
                }
            } else {
                refreshOts()
                actualizarDetalle(otId, DetalleMensajes(estado = SectionMessage("OT finalizada")))
            }
        }
    }

    // --- Acciones admin ---
    fun aprobarPresupuesto(otId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.aprobarPresupuesto(otId)
            val seleccionado = _ui.value.detalleSeleccionado?.ot?.id
            if (seleccionado == otId) {
                actualizarDetalle(otId, DetalleMensajes(presupuesto = SectionMessage("Presupuesto aprobado")))
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
                actualizarDetalle(otId, DetalleMensajes(estado = SectionMessage("Estado actualizado")))
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
