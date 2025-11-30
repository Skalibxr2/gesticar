package com.hans.gesticar.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.SintomaInput
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import com.hans.gesticar.ui.Routes
import com.hans.gesticar.ui.components.EditableTaskState
import com.hans.gesticar.ui.components.TasksSection
import com.hans.gesticar.ui.components.toTareaOt
import com.hans.gesticar.viewmodel.MainViewModel
import com.hans.gesticar.util.formatRutInput
import com.hans.gesticar.util.isRutValid
import com.hans.gesticar.util.normalizeRut
import com.hans.gesticar.util.sanitizeRutInput
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale

private class PresupuestoItemForm(
    val id: String = UUID.randomUUID().toString(),
    tipo: ItemTipo = ItemTipo.MO,
    titulo: String = "",
    descripcion: String = "",
    cantidad: String = "1",
    precioUnitario: String = "",
    expandido: Boolean = false
) {
    var tipo by mutableStateOf(tipo)
    var titulo by mutableStateOf(titulo)
    var descripcion by mutableStateOf(descripcion)
    var cantidad by mutableStateOf(cantidad)
    var precioUnitario by mutableStateOf(precioUnitario)
    var expandido by mutableStateOf(expandido)
}

private fun PresupuestoItemForm.copy(
    id: String = this.id,
    tipo: ItemTipo = this.tipo,
    titulo: String = this.titulo,
    descripcion: String = this.descripcion,
    cantidad: String = this.cantidad,
    precioUnitario: String = this.precioUnitario,
    expandido: Boolean = this.expandido
): PresupuestoItemForm = PresupuestoItemForm(
    id = id,
    tipo = tipo,
    titulo = titulo,
    descripcion = descripcion,
    cantidad = cantidad,
    precioUnitario = precioUnitario,
    expandido = expandido
)

private class SymptomForm(
    descripcion: String = "",
    fechaTexto: String = "",
    fotos: List<Uri> = emptyList()
) {
    var descripcion by mutableStateOf(descripcion)
    var fechaTexto by mutableStateOf(fechaTexto)
    val fotos = mutableStateListOf<Uri>().apply { addAll(fotos) }
}

private val symptomFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

private fun parseSymptomTimestamp(text: String): Long? = text.takeIf { it.isNotBlank() }?.let {
    runCatching { symptomFormatter.parse(it)?.time }.getOrNull()
}

@Composable
fun CreateOtScreen(vm: MainViewModel, nav: NavController) {
    val uiState by vm.createOtUi.collectAsState()

    LaunchedEffect(Unit) {
        vm.prepararNuevaOt()
    }

    var rutSanitized by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var comuna by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }

    var patente by rememberSaveable { mutableStateOf("") }
    var marca by rememberSaveable { mutableStateOf("") }
    var modelo by rememberSaveable { mutableStateOf("") }
    var anio by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("") }
    var kilometraje by rememberSaveable { mutableStateOf("") }
    var combustible by rememberSaveable { mutableStateOf("") }

    var detallesClienteExpandido by rememberSaveable { mutableStateOf(false) }
    var modoEdicionCliente by rememberSaveable { mutableStateOf(false) }
    var creandoCliente by rememberSaveable { mutableStateOf(false) }
    var mostrarFormularioVehiculo by rememberSaveable { mutableStateOf(false) }
    var esEdicionVehiculo by rememberSaveable { mutableStateOf(false) }
    var flujoNuevoVehiculo by rememberSaveable { mutableStateOf(false) }
    var patenteBusqueda by rememberSaveable { mutableStateOf("") }
    var mostrarConfirmarCancelarRegistro by rememberSaveable { mutableStateOf(false) }
    var vehiculoInfoDialog by remember { mutableStateOf<Vehiculo?>(null) }

    var presupuestoAprobado by rememberSaveable { mutableStateOf(false) }
    val items = remember { mutableStateListOf<PresupuestoItemForm>() }
    val seleccionMecanicos = remember { mutableStateListOf<String>() }
    var vehiculoSeleccionado by rememberSaveable { mutableStateOf<String?>(null) }
    val sintomas = remember { mutableStateListOf(SymptomForm()) }
    val tareas = remember { mutableStateListOf(EditableTaskState()) }

    LaunchedEffect(uiState.exito) {
        if (uiState.exito) {
            rutSanitized = ""
            nombre = ""
            correo = ""
            direccion = ""
            comuna = ""
            telefono = ""
            patente = ""
            marca = ""
            modelo = ""
            anio = ""
            color = ""
            kilometraje = ""
            combustible = ""
            presupuestoAprobado = false
            detallesClienteExpandido = false
            modoEdicionCliente = false
            creandoCliente = false
            mostrarFormularioVehiculo = false
            esEdicionVehiculo = false
            flujoNuevoVehiculo = false
            patenteBusqueda = ""
            vehiculoInfoDialog = null
            seleccionMecanicos.clear()
            vehiculoSeleccionado = null
            items.clear()
            sintomas.clear()
            sintomas += SymptomForm()
            tareas.clear()
            tareas += EditableTaskState()
        }
    }

    LaunchedEffect(uiState.mecanicos) {
        val disponibles = uiState.mecanicos.map(Usuario::id).toSet()
        seleccionMecanicos.removeAll { it !in disponibles }
    }

    val rutValido = isRutValid(rutSanitized)
    val rutNormalizado = if (rutValido) normalizeRut(rutSanitized) else null
    val clienteEncontrado = rutNormalizado != null && uiState.cliente?.rut == rutNormalizado

    LaunchedEffect(rutNormalizado) {
        modoEdicionCliente = false
        detallesClienteExpandido = false
        if (rutNormalizado == null) {
            creandoCliente = false
        } else {
            vm.buscarClientePorRut(rutNormalizado)
        }
    }

    LaunchedEffect(clienteEncontrado) {
        if (clienteEncontrado) {
            modoEdicionCliente = false
            creandoCliente = false
        }
    }

    LaunchedEffect(uiState.guardandoCliente, uiState.mensajeCliente, clienteEncontrado) {
        val guardadoExitoso = uiState.mensajeCliente != null &&
            uiState.mensajeCliente?.contains("error", ignoreCase = true) != true

        if (!uiState.guardandoCliente && clienteEncontrado && guardadoExitoso && modoEdicionCliente) {
            modoEdicionCliente = false
        }
    }

    LaunchedEffect(uiState.cliente?.rut, rutNormalizado, creandoCliente) {
        val cliente = uiState.cliente
        if (cliente != null && rutNormalizado == normalizeRut(cliente.rut)) {
            nombre = cliente.nombre
            correo = cliente.correo.orEmpty()
            direccion = cliente.direccion.orEmpty()
            comuna = cliente.comuna.orEmpty()
            telefono = cliente.telefono.orEmpty()
        } else if ((rutNormalizado == null || cliente == null) && !creandoCliente) {
            nombre = ""
            correo = ""
            direccion = ""
            comuna = ""
            telefono = ""
        }
    }

    LaunchedEffect(uiState.vehiculosCliente, rutNormalizado) {
        if (rutNormalizado == uiState.cliente?.rut) {
            val actuales = uiState.vehiculosCliente.map(Vehiculo::patente).toSet()
            if (vehiculoSeleccionado != null && vehiculoSeleccionado !in actuales) {
                vehiculoSeleccionado = null
            }
            if (vehiculoSeleccionado == null && uiState.vehiculosCliente.isNotEmpty()) {
                vehiculoSeleccionado = uiState.vehiculosCliente.first().patente
            }
        } else {
            vehiculoSeleccionado = null
            patente = ""
            marca = ""
            modelo = ""
            anio = ""
            color = ""
            kilometraje = ""
            combustible = ""
        }
    }

    LaunchedEffect(vehiculoSeleccionado, uiState.vehiculosCliente) {
        val vehiculo = uiState.vehiculosCliente.firstOrNull { it.patente == vehiculoSeleccionado }
        if (vehiculo != null) {
            patente = vehiculo.patente
            marca = vehiculo.marca
            modelo = vehiculo.modelo
            anio = vehiculo.anio.toString()
            color = vehiculo.color.orEmpty()
            kilometraje = vehiculo.kilometraje?.toString().orEmpty()
            combustible = vehiculo.combustible.orEmpty()
        }
    }

    LaunchedEffect(uiState.guardandoVehiculo, uiState.mensajeVehiculo) {
        if (!uiState.guardandoVehiculo && uiState.mensajeVehiculo != null &&
            !uiState.mensajeVehiculo!!.contains("error", ignoreCase = true)
        ) {
            mostrarFormularioVehiculo = false
            esEdicionVehiculo = false
            flujoNuevoVehiculo = false
            patenteBusqueda = ""
            vm.limpiarBusquedaVehiculo()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Nueva Orden de Trabajo",
            style = MaterialTheme.typography.headlineSmall
        )
        OutlinedTextField(
            value = uiState.numeroOt.toString(),
            onValueChange = {},
            label = { Text("Código OT") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.mensaje != null) {
            Text(
                text = uiState.mensaje!!,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (uiState.guardando) {
            Text("Guardando...", style = MaterialTheme.typography.bodySmall)
        }

        val camposClienteEditables = (clienteEncontrado && modoEdicionCliente) || (!clienteEncontrado && creandoCliente)

        ClienteSection(
            rut = formatRutInput(rutSanitized),
            onRutChange = { rutSanitized = sanitizeRutInput(it) },
            rutValido = rutValido,
            mostrarRutInvalido = rutSanitized.isNotBlank() && !rutValido,
            nombre = nombre,
            onNombreChange = { nombre = it },
            correo = correo,
            onCorreoChange = { correo = it },
            direccion = direccion,
            onDireccionChange = { direccion = it },
            comuna = comuna,
            onComunaChange = { comuna = it },
            telefono = telefono,
            onTelefonoChange = { telefono = it },
            detallesExpandido = detallesClienteExpandido,
            onToggleDetalles = { detallesClienteExpandido = !detallesClienteExpandido },
            camposHabilitados = camposClienteEditables,
            clienteEncontrado = clienteEncontrado,
            creandoCliente = creandoCliente,
            onModificarCliente = {
                modoEdicionCliente = true
                detallesClienteExpandido = true
            },
            onIniciarCreacion = {
                creandoCliente = true
                modoEdicionCliente = true
                detallesClienteExpandido = true
            },
            onGuardarCliente = {
                if (rutNormalizado != null && nombre.isNotBlank()) {
                    vm.guardarCliente(
                        Cliente(
                            rut = rutNormalizado,
                            nombre = nombre,
                            correo = correo.takeIf { it.isNotBlank() },
                            direccion = direccion.takeIf { it.isNotBlank() },
                            comuna = comuna.takeIf { it.isNotBlank() },
                            telefono = telefono.takeIf { it.isNotBlank() }
                        )
                    )
                }
            },
            guardandoCliente = uiState.guardandoCliente,
            mensajeCliente = uiState.mensajeCliente
        )

        VehiculoSection(
            rutCliente = rutNormalizado,
            patente = patente,
            onPatenteChange = { patente = it.uppercase() },
            marca = marca,
            onMarcaChange = { marca = it },
            modelo = modelo,
            onModeloChange = { modelo = it },
            anio = anio,
            onAnioChange = { anio = it.filter { ch -> ch.isDigit() }.take(4) },
            color = color,
            onColorChange = { color = it },
            kilometraje = kilometraje,
            onKilometrajeChange = { kilometraje = it.filter { ch -> ch.isDigit() } },
            combustible = combustible,
            onCombustibleChange = { combustible = it },
            vehiculos = if (rutNormalizado == uiState.cliente?.rut) uiState.vehiculosCliente else emptyList(),
            vehiculoSeleccionado = vehiculoSeleccionado,
            onSeleccionarVehiculo = { vehiculo ->
                vehiculoSeleccionado = vehiculo?.patente
                mostrarFormularioVehiculo = false
                esEdicionVehiculo = false
                flujoNuevoVehiculo = false
            },
            onGuardarVehiculo = {
                val errores = mutableListOf<String>()
                val anioInt = anio.toIntOrNull()
                val kmInt = kilometraje.toIntOrNull()
                if (rutNormalizado == null) errores += "cliente"
                if (patente.isBlank()) errores += "patente"
                if (marca.isBlank()) errores += "marca"
                if (modelo.isBlank()) errores += "modelo"
                if (anio.length != 4 || anioInt == null) errores += "año"
                if (flujoNuevoVehiculo && kilometraje.isBlank()) errores += "kilometraje"
                if (kilometraje.isNotBlank() && kmInt == null) errores += "kilometraje numérico"
                if (errores.isNotEmpty()) {
                    vm.reportarMensajeVehiculo("Completa correctamente: ${errores.joinToString(", ")}")
                    return@VehiculoSection
                }
                val vehiculo = Vehiculo(
                    patente = patente.uppercase(),
                    clienteRut = rutNormalizado ?: "",
                    marca = marca,
                    modelo = modelo,
                    anio = anioInt ?: 0,
                    color = color.takeIf { it.isNotBlank() },
                    kilometraje = if (kilometraje.isBlank()) null else kmInt,
                    combustible = combustible.takeIf { it.isNotBlank() }
                )
                vm.guardarVehiculo(vehiculo)
                vehiculoSeleccionado = vehiculo.patente.uppercase()
            },
            guardandoVehiculo = uiState.guardandoVehiculo,
            mensajeVehiculo = uiState.mensajeVehiculo,
            mostrarFormulario = mostrarFormularioVehiculo,
            esEdicion = esEdicionVehiculo,
            flujoNuevoVehiculo = flujoNuevoVehiculo,
            patenteBusqueda = patenteBusqueda,
            onPatenteBusquedaChange = { patenteBusqueda = it.uppercase() },
            onBuscarPatente = {
                vm.buscarVehiculoPorPatente(patenteBusqueda.uppercase())
                patente = patenteBusqueda.uppercase()
            },
            vehiculoBuscado = uiState.vehiculoBuscado,
            mensajeBusquedaVehiculo = uiState.mensajeBusquedaVehiculo,
            buscandoVehiculo = uiState.buscandoVehiculo,
            onMostrarFormularioNuevo = {
                flujoNuevoVehiculo = true
                mostrarFormularioVehiculo = false
                esEdicionVehiculo = false
                patenteBusqueda = ""
                vm.limpiarBusquedaVehiculo()
                vm.limpiarMensajesVehiculo()
            },
            onEditarVehiculo = { vehiculo ->
                flujoNuevoVehiculo = false
                esEdicionVehiculo = true
                mostrarFormularioVehiculo = true
                vehiculoSeleccionado = vehiculo.patente
                patente = vehiculo.patente
                marca = vehiculo.marca
                modelo = vehiculo.modelo
                anio = vehiculo.anio.toString()
                color = vehiculo.color.orEmpty()
                kilometraje = vehiculo.kilometraje?.toString().orEmpty()
                combustible = vehiculo.combustible.orEmpty()
            },
            onMostrarInfo = { vehiculoInfoDialog = it },
            onDesvincular = { vehiculo ->
                if (rutNormalizado == null) {
                    vm.reportarMensajeVehiculo("Primero selecciona o registra un cliente")
                } else {
                    vm.desasociarVehiculoDeCliente(vehiculo.patente, rutNormalizado)
                }
            },
            onRegistrarNuevoVehiculo = {
                mostrarFormularioVehiculo = true
                esEdicionVehiculo = false
                patente = patenteBusqueda.uppercase()
                marca = ""
                modelo = ""
                anio = ""
                color = ""
                kilometraje = ""
                combustible = ""
            },
            onCancelarEdicion = {
                if (flujoNuevoVehiculo) {
                    mostrarConfirmarCancelarRegistro = true
                } else {
                    mostrarFormularioVehiculo = false
                }
            },
            mostrarConfirmarCancelacion = mostrarConfirmarCancelarRegistro,
            onConfirmarCancelarNuevo = {
                mostrarConfirmarCancelarRegistro = false
                mostrarFormularioVehiculo = false
                flujoNuevoVehiculo = false
                patenteBusqueda = ""
                vm.limpiarBusquedaVehiculo()
                patente = ""
                marca = ""
                modelo = ""
                anio = ""
                color = ""
                kilometraje = ""
                combustible = ""
            },
            onDismissCancelarNuevo = { mostrarConfirmarCancelarRegistro = false },
            onAsociarVehiculo = {
                rutNormalizado?.let {
                    vehiculoSeleccionado = patenteBusqueda.uppercase()
                    vm.reasignarVehiculoACliente(patenteBusqueda.uppercase(), it)
                }
            },
            onReasignarVehiculo = {
                rutNormalizado?.let {
                    vehiculoSeleccionado = patenteBusqueda.uppercase()
                    vm.reasignarVehiculoACliente(patenteBusqueda.uppercase(), it)
                }
            }
        )

        SymptomsSection(
            sintomas = sintomas,
            onAgregarSintoma = { sintomas += SymptomForm() },
            onEliminarSintoma = { index -> if (sintomas.size > 1) sintomas.removeAt(index) }
        )

        MecanicosSection(
            mecanicos = uiState.mecanicos,
            seleccionados = uiState.mecanicos.filter { it.id in seleccionMecanicos },
            onSelect = { mecanico ->
                if (mecanico.id !in seleccionMecanicos) {
                    seleccionMecanicos += mecanico.id
                }
            },
            onRemove = { mecanico -> seleccionMecanicos.remove(mecanico.id) }
        )

        PresupuestoSection(
            items = items,
            presupuestoAprobado = presupuestoAprobado,
            onToggleAprobado = { presupuestoAprobado = !presupuestoAprobado }
        )

        TasksSection(
            tasks = tareas,
            soloLectura = false,
            title = "Tareas preventivas o correctivas",
            modifier = Modifier.fillMaxWidth()
        )

        val anioInt = anio.toIntOrNull()
        val kmInt = kilometraje.toIntOrNull()
        val clienteValido = rutNormalizado != null && nombre.isNotBlank()
        val vehiculoValido =
            patente.isNotBlank() && marca.isNotBlank() && modelo.isNotBlank() && anioInt != null && anio.length == 4
        val presupuestoItemsValidos = items.mapNotNull { form ->
            val cantidadInt = form.cantidad.toIntOrNull()
            val precioInt = form.precioUnitario.toIntOrNull()
            if (form.titulo.isBlank() || cantidadInt == null || precioInt == null) {
                null
            } else {
                PresupuestoItem(
                    tipo = form.tipo,
                    descripcion = buildItemDescripcion(form.titulo, form.descripcion),
                    cantidad = cantidadInt,
                    precioUnit = precioInt
                )
            }
        }
        val sintomasValidos = sintomas.mapNotNull { form ->
            if (form.descripcion.isBlank()) return@mapNotNull null
            val timestamp = parseSymptomTimestamp(form.fechaTexto)
            SintomaInput(
                descripcion = form.descripcion,
                registradoEn = timestamp,
                fotos = form.fotos.map(Uri::toString)
            )
        }
        val puedeGuardar = clienteValido && vehiculoValido && !uiState.guardando
        val puedeIniciar = puedeGuardar && presupuestoAprobado &&
            presupuestoItemsValidos.isNotEmpty() && seleccionMecanicos.isNotEmpty()

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (!puedeGuardar) return@Button
                    val rutCliente = rutNormalizado ?: return@Button
                    val cliente = Cliente(
                        rut = rutCliente,
                        nombre = nombre,
                        correo = correo.takeIf { it.isNotBlank() },
                        direccion = direccion.takeIf { it.isNotBlank() },
                        comuna = comuna.takeIf { it.isNotBlank() },
                        telefono = telefono.takeIf { it.isNotBlank() }
                    )
                    val vehiculo = Vehiculo(
                        patente = patente.uppercase(),
                        clienteRut = rutCliente,
                        marca = marca,
                        modelo = modelo,
                        anio = anioInt ?: 0,
                        color = color.takeIf { it.isNotBlank() },
                        kilometraje = kmInt,
                        combustible = combustible.takeIf { it.isNotBlank() }
                    )
                    val tareasValidas = tareas
                        .filter { it.descripcion.isNotBlank() }
                        .map { it.toTareaOt() }
                    vm.crearOt(
                        cliente = cliente,
                        vehiculo = vehiculo,
                        mecanicosIds = seleccionMecanicos.toList(),
                        presupuestoItems = presupuestoItemsValidos,
                        presupuestoAprobado = presupuestoAprobado,
                        sintomas = sintomasValidos,
                        tareas = tareasValidas,
                        iniciar = false
                    )
                },
                enabled = puedeGuardar,
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar como borrador")
            }
            Button(
                onClick = {
                    if (!puedeIniciar) return@Button
                    val rutCliente = rutNormalizado ?: return@Button
                    val cliente = Cliente(
                        rut = rutCliente,
                        nombre = nombre,
                        correo = correo.takeIf { it.isNotBlank() },
                        direccion = direccion.takeIf { it.isNotBlank() },
                        comuna = comuna.takeIf { it.isNotBlank() },
                        telefono = telefono.takeIf { it.isNotBlank() }
                    )
                    val vehiculo = Vehiculo(
                        patente = patente.uppercase(),
                        clienteRut = rutCliente,
                        marca = marca,
                        modelo = modelo,
                        anio = anioInt ?: 0,
                        color = color.takeIf { it.isNotBlank() },
                        kilometraje = kmInt,
                        combustible = combustible.takeIf { it.isNotBlank() }
                    )
                    val tareasValidas = tareas
                        .filter { it.descripcion.isNotBlank() }
                        .map { it.toTareaOt() }
                    vm.crearOt(
                        cliente = cliente,
                        vehiculo = vehiculo,
                        mecanicosIds = seleccionMecanicos.toList(),
                        presupuestoItems = presupuestoItemsValidos,
                        presupuestoAprobado = true,
                        sintomas = sintomasValidos,
                        tareas = tareasValidas,
                        iniciar = true
                    )
                },
                enabled = puedeIniciar,
                modifier = Modifier.weight(1f)
            ) {
                Text("Iniciar OT")
            }
        }

        TextButton(onClick = {
            nav.navigate(Routes.HOME) {
                popUpTo(Routes.HOME) { inclusive = true }
            }
        }) {
            Text("Volver al menú principal")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    vehiculoInfoDialog?.let { vehiculo ->
        AlertDialog(
            onDismissRequest = { vehiculoInfoDialog = null },
            confirmButton = {
                TextButton(onClick = { vehiculoInfoDialog = null }) { Text("Cerrar") }
            },
            title = { Text("Vehículo ${vehiculo.patente}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Marca: ${vehiculo.marca}")
                    Text("Modelo: ${vehiculo.modelo}")
                    Text("Año: ${vehiculo.anio}")
                    vehiculo.color?.let { Text("Color: $it") }
                    vehiculo.kilometraje?.let { Text("Kilometraje: ${it} km") }
                    vehiculo.combustible?.let { Text("Combustible: $it") }
                }
            }
        )
    }
}

@Composable
private fun ClienteSection(
    rut: String,
    onRutChange: (String) -> Unit,
    rutValido: Boolean,
    mostrarRutInvalido: Boolean,
    nombre: String,
    onNombreChange: (String) -> Unit,
    correo: String,
    onCorreoChange: (String) -> Unit,
    direccion: String,
    onDireccionChange: (String) -> Unit,
    comuna: String,
    onComunaChange: (String) -> Unit,
    telefono: String,
    onTelefonoChange: (String) -> Unit,
    detallesExpandido: Boolean,
    onToggleDetalles: () -> Unit,
    camposHabilitados: Boolean,
    clienteEncontrado: Boolean,
    creandoCliente: Boolean,
    onModificarCliente: () -> Unit,
    onIniciarCreacion: () -> Unit,
    onGuardarCliente: () -> Unit,
    guardandoCliente: Boolean,
    mensajeCliente: String?,
) {
    val mensajeEsError = mensajeCliente?.contains("error", ignoreCase = true) == true
    val textoAccion = if (clienteEncontrado) "Guardar cambios" else "Crear cliente"
    val puedeGuardar = camposHabilitados && rutValido && nombre.isNotBlank() && !guardandoCliente

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Datos del cliente", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = rut,
                onValueChange = onRutChange,
                label = { Text("RUT") },
                modifier = Modifier.fillMaxWidth(),
                isError = mostrarRutInvalido,
                enabled = !guardandoCliente,
                readOnly = clienteEncontrado && camposHabilitados,
                supportingText = {
                    when {
                        guardandoCliente -> Text("Guardando cliente...")
                        mostrarRutInvalido -> Text("Ingresa un RUT válido con dígito verificador")
                        mensajeCliente != null -> Text(
                            mensajeCliente,
                            color = if (mensajeEsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = onNombreChange,
                label = { Text("Nombre del cliente") },
                modifier = Modifier.fillMaxWidth(),
                enabled = camposHabilitados,
                readOnly = !camposHabilitados
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleDetalles) {
                    Text(if (detallesExpandido) "Ocultar detalles" else "Ver más detalles")
                }

                when {
                    clienteEncontrado && !camposHabilitados -> {
                        Button(onClick = onModificarCliente, enabled = !guardandoCliente) {
                            Text("Modificar cliente")
                        }
                    }

                    !clienteEncontrado && !creandoCliente -> {
                        Button(onClick = onIniciarCreacion, enabled = rutValido && !guardandoCliente) {
                            Text("Crear cliente")
                        }
                    }

                    camposHabilitados -> {
                        Button(onClick = onGuardarCliente, enabled = puedeGuardar) {
                            Text(textoAccion)
                        }
                    }
                }
            }

            if (detallesExpandido) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = correo,
                        onValueChange = onCorreoChange,
                        label = { Text("Correo") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = camposHabilitados,
                        readOnly = !camposHabilitados
                    )
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = onTelefonoChange,
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = camposHabilitados,
                        readOnly = !camposHabilitados
                    )
                    OutlinedTextField(
                        value = direccion,
                        onValueChange = onDireccionChange,
                        label = { Text("Dirección") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = camposHabilitados,
                        readOnly = !camposHabilitados
                    )
                    OutlinedTextField(
                        value = comuna,
                        onValueChange = onComunaChange,
                        label = { Text("Comuna") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = camposHabilitados,
                        readOnly = !camposHabilitados
                    )
                }
            }
        }
    }
}

@Composable
private fun SymptomsSection(
    sintomas: List<SymptomForm>,
    onAgregarSintoma: () -> Unit,
    onEliminarSintoma: (Int) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Síntomas del vehículo", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Agrega cada síntoma por separado con su fecha opcional y adjunta las fotos correspondientes.",
                style = MaterialTheme.typography.bodySmall
            )
            sintomas.forEachIndexed { index, sintoma ->
                SymptomCard(
                    indice = index + 1,
                    sintoma = sintoma,
                    onRemove = if (sintomas.size > 1) {
                        { onEliminarSintoma(index) }
                    } else {
                        null
                    }
                )
                if (index < sintomas.lastIndex) {
                    Divider()
                }
            }
            Button(onClick = onAgregarSintoma, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Agregar síntoma")
            }
        }
    }
}

@Composable
private fun SymptomCard(indice: Int, sintoma: SymptomForm, onRemove: (() -> Unit)?) {
    val pickGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { uri ->
            if (uri !in sintoma.fotos) {
                sintoma.fotos.add(uri)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Síntoma #$indice", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.weight(1f))
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar síntoma")
                }
            }
        }
        OutlinedTextField(
            value = sintoma.descripcion,
            onValueChange = { sintoma.descripcion = it },
            label = { Text("Descripción del síntoma") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = sintoma.fechaTexto,
            onValueChange = { sintoma.fechaTexto = it },
            label = { Text("Fecha u hora (opcional)") },
            supportingText = { Text("Formato sugerido: 2024-05-01 09:30") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                pickGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Icon(imageVector = Icons.Default.Collections, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Galería")
            }
        }
        if (sintoma.fotos.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sintoma.fotos) { uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Foto de síntoma",
                            modifier = Modifier.size(96.dp),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(onClick = {
                            sintoma.fotos.remove(uri)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar foto")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehiculoSection(
    rutCliente: String?,
    patente: String,
    onPatenteChange: (String) -> Unit,
    marca: String,
    onMarcaChange: (String) -> Unit,
    modelo: String,
    onModeloChange: (String) -> Unit,
    anio: String,
    onAnioChange: (String) -> Unit,
    color: String,
    onColorChange: (String) -> Unit,
    kilometraje: String,
    onKilometrajeChange: (String) -> Unit,
    combustible: String,
    onCombustibleChange: (String) -> Unit,
    vehiculos: List<Vehiculo>,
    vehiculoSeleccionado: String?,
    onSeleccionarVehiculo: (Vehiculo?) -> Unit,
    onGuardarVehiculo: () -> Unit,
    guardandoVehiculo: Boolean,
    mensajeVehiculo: String?,
    mostrarFormulario: Boolean,
    esEdicion: Boolean,
    flujoNuevoVehiculo: Boolean,
    patenteBusqueda: String,
    onPatenteBusquedaChange: (String) -> Unit,
    onBuscarPatente: () -> Unit,
    vehiculoBuscado: Vehiculo?,
    mensajeBusquedaVehiculo: String?,
    buscandoVehiculo: Boolean,
    onMostrarFormularioNuevo: () -> Unit,
    onEditarVehiculo: (Vehiculo) -> Unit,
    onMostrarInfo: (Vehiculo) -> Unit,
    onDesvincular: (Vehiculo) -> Unit,
    onRegistrarNuevoVehiculo: () -> Unit,
    onCancelarEdicion: () -> Unit,
    mostrarConfirmarCancelacion: Boolean,
    onConfirmarCancelarNuevo: () -> Unit,
    onDismissCancelarNuevo: () -> Unit,
    onAsociarVehiculo: () -> Unit,
    onReasignarVehiculo: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Datos del vehículo", style = MaterialTheme.typography.titleMedium)
            if (vehiculos.isEmpty()) {
                Text("Registra un nuevo vehículo para este cliente", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    "Vehículos registrados para este cliente",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vehiculos.forEach { vehiculo ->
                        val esSeleccionado = vehiculoSeleccionado == vehiculo.patente
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = esSeleccionado,
                                    onClick = { onSeleccionarVehiculo(vehiculo) },
                                    role = Role.RadioButton
                                )
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = esSeleccionado,
                                onClick = { onSeleccionarVehiculo(vehiculo) }
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text("${vehiculo.patente} • ${vehiculo.marca} ${vehiculo.modelo}")
                                Text(
                                    listOfNotNull(
                                        vehiculo.anio.toString(),
                                        vehiculo.color,
                                        vehiculo.kilometraje?.let { "${it} km" }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { onMostrarInfo(vehiculo) }) {
                                    Icon(Icons.Default.Info, contentDescription = "Información del vehículo")
                                }
                                IconButton(onClick = { onEditarVehiculo(vehiculo) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar vehículo")
                                }
                                IconButton(onClick = { onDesvincular(vehiculo) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Desvincular vehículo")
                                }
                            }
                        }
                    }
                }
                vehiculoSeleccionado?.let {
                    Text("Vehículo seleccionado: $it", style = MaterialTheme.typography.bodySmall)
                }
            }

            AssistChip(
                onClick = onMostrarFormularioNuevo,
                label = { Text("Registrar nuevo vehículo") },
                enabled = rutCliente != null,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Registrar nuevo"
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )

            if (flujoNuevoVehiculo) {
                Divider()
                Text(
                    "Flujo basado en la patente",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = patenteBusqueda,
                    onValueChange = onPatenteBusquedaChange,
                    label = { Text("Patente") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onBuscarPatente,
                        enabled = patenteBusqueda.isNotBlank() && !buscandoVehiculo,
                    ) {
                        Text(if (buscandoVehiculo) "Buscando..." else "Buscar patente")
                    }
                    TextButton(onClick = onCancelarEdicion) {
                        Text("Cancelar")
                    }
                }
                mensajeBusquedaVehiculo?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                vehiculoBuscado?.let { vehiculo ->
                    if (vehiculo.clienteRut.isBlank()) {
                        Text("La patente ya existe pero no tiene cliente asignado", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onAsociarVehiculo) { Text("Asociar al cliente actual") }
                            TextButton(onClick = onCancelarEdicion) { Text("Cancelar") }
                        }
                    } else if (rutCliente != null && normalizeRut(vehiculo.clienteRut) != normalizeRut(rutCliente)) {
                        Text(
                            "La patente ya está vinculada a otro cliente. Actualizarla la moverá al cliente actual.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onReasignarVehiculo) { Text("Actualizar cliente") }
                            TextButton(onClick = onCancelarEdicion) { Text("Cancelar") }
                        }
                    } else {
                        Text("La patente ya está asociada a este cliente", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (vehiculoBuscado == null && mensajeBusquedaVehiculo?.contains("no existe", ignoreCase = true) == true) {
                    Button(onClick = onRegistrarNuevoVehiculo) {
                        Text("Registrar nuevo vehículo")
                    }
                }
            }

            if (mostrarFormulario) {
                Divider()
                val patenteSoloLectura = esEdicion || flujoNuevoVehiculo
                Text(
                    if (esEdicion) "Modificar vehículo" else "Nuevo vehículo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = patente,
                    onValueChange = onPatenteChange,
                    label = { Text("Patente") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !patenteSoloLectura
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = marca,
                        onValueChange = onMarcaChange,
                        label = { Text("Marca") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = modelo,
                        onValueChange = onModeloChange,
                        label = { Text("Modelo") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = anio,
                        onValueChange = onAnioChange,
                        label = { Text("Año") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = color,
                        onValueChange = onColorChange,
                        label = { Text("Color") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = kilometraje,
                        onValueChange = onKilometrajeChange,
                        label = { Text("Kilometraje") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = combustible,
                        onValueChange = onCombustibleChange,
                        label = { Text("Combustible") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onGuardarVehiculo) {
                        Text(if (esEdicion) "Guardar cambios" else "Guardar vehículo")
                    }
                    TextButton(onClick = onCancelarEdicion) { Text("Cancelar") }
                }
                if (guardandoVehiculo) {
                    Text("Guardando vehículo...", style = MaterialTheme.typography.bodySmall)
                } else if (mensajeVehiculo != null) {
                    val esError = mensajeVehiculo.contains("error", ignoreCase = true)
                    Text(
                        mensajeVehiculo,
                        color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
    if (mostrarConfirmarCancelacion) {
        AlertDialog(
            onDismissRequest = onDismissCancelarNuevo,
            confirmButton = {
                TextButton(onClick = onConfirmarCancelarNuevo) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCancelarNuevo) {
                    Text("No")
                }
            },
            title = { Text("¿Desea cancelar el registro de un nuevo vehículo?") },
            text = { Text("Se descartará toda la información ingresada.") }
        )
    }
}


@Composable
private fun MecanicosSection(
    mecanicos: List<Usuario>,
    seleccionados: List<Usuario>,
    onSelect: (Usuario) -> Unit,
    onRemove: (Usuario) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Mecánicos asignados", style = MaterialTheme.typography.titleMedium)
            if (mecanicos.isEmpty()) {
                Text("No hay mecánicos registrados", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            val seleccionadosIds = seleccionados.map(Usuario::id).toSet()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                mecanicos.forEach { mecanico ->
                    val estaSeleccionado = mecanico.id in seleccionadosIds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = estaSeleccionado,
                                role = Role.Checkbox,
                                onValueChange = { marcado ->
                                    if (marcado && !estaSeleccionado) {
                                        onSelect(mecanico)
                                    } else if (!marcado && estaSeleccionado) {
                                        onRemove(mecanico)
                                    }
                                }
                            )
                    ) {
                        Checkbox(
                            checked = estaSeleccionado,
                            onCheckedChange = null
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(mecanico.nombre, style = MaterialTheme.typography.bodyLarge)
                            Text(mecanico.email, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (seleccionados.isEmpty()) {
                Text("Selecciona al menos un mecánico", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PresupuestoSection(
    items: MutableList<PresupuestoItemForm>,
    presupuestoAprobado: Boolean,
    onToggleAprobado: () -> Unit
) {
    var nuevoItem by remember { mutableStateOf(PresupuestoItemForm()) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Presupuesto", style = MaterialTheme.typography.titleMedium)

            Text("Ítems del presupuesto", style = MaterialTheme.typography.titleSmall)
            if (items.isEmpty()) {
                Text("Aún no hay ítems agregados")
            }
            items.forEach { item ->
                PresupuestoItemRow(
                    item = item,
                    onRemove = { items.remove(item) }
                )
            }

            Divider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Agregar ítem", style = MaterialTheme.typography.titleSmall)
                Text("Tipo de ítem del presupuesto", style = MaterialTheme.typography.bodySmall)
                AssistChip(
                    onClick = {
                        nuevoItem = nuevoItem.copy(tipo = if (nuevoItem.tipo == ItemTipo.MO) ItemTipo.REP else ItemTipo.MO)
                    },
                    label = { Text(if (nuevoItem.tipo == ItemTipo.MO) "Mano de obra" else "Repuestos") },
                    colors = AssistChipDefaults.assistChipColors()
                )
                OutlinedTextField(
                    value = nuevoItem.titulo,
                    onValueChange = { nuevoItem = nuevoItem.copy(titulo = it) },
                    label = { Text("Título del ítem") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nuevoItem.descripcion,
                    onValueChange = { nuevoItem = nuevoItem.copy(descripcion = it) },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = nuevoItem.cantidad,
                        onValueChange = { nuevoItem = nuevoItem.copy(cantidad = it.filter(Char::isDigit)) },
                        label = { Text("Cantidad (días)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = nuevoItem.precioUnitario,
                        onValueChange = { nuevoItem = nuevoItem.copy(precioUnitario = it.filter(Char::isDigit)) },
                        label = { Text("Precio/día") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Button(
                    onClick = {
                        val cantidad = nuevoItem.cantidad.toIntOrNull()
                        val precio = nuevoItem.precioUnitario.toIntOrNull()
                        if (nuevoItem.titulo.isNotBlank() && cantidad != null && precio != null) {
                            items += nuevoItem.copy(expandido = true)
                            nuevoItem = PresupuestoItemForm()
                        }
                    }
                ) {
                    Text("Agregar ítem")
                }
            }

            val subtotal = items.sumOf { form ->
                val cantidad = form.cantidad.toIntOrNull() ?: 0
                val precio = form.precioUnitario.toIntOrNull() ?: 0
                cantidad * precio
            }
            val iva = (subtotal * 19) / 100
            val total = subtotal + iva
            Text("Subtotal: ${formatCurrency(subtotal)}")
            Text("IVA (19%): ${formatCurrency(iva)}")
            Text("Total: ${formatCurrency(total)}")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(
                    checked = presupuestoAprobado,
                    onCheckedChange = { onToggleAprobado() },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
                Text(if (presupuestoAprobado) "Presupuesto aprobado" else "Presupuesto pendiente")
            }
        }
    }
}

@Composable
private fun PresupuestoItemRow(
    item: PresupuestoItemForm,
    onRemove: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.expandido = !item.expandido }
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (item.tipo == ItemTipo.MO) "MO" else "Repuesto", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(item.titulo.ifBlank { "Sin título" }, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
            if (item.expandido) {
                Text("Tipo de ítem del presupuesto", style = MaterialTheme.typography.bodySmall)
                AssistChip(
                    onClick = {
                        item.tipo = if (item.tipo == ItemTipo.MO) ItemTipo.REP else ItemTipo.MO
                    },
                    label = { Text(if (item.tipo == ItemTipo.MO) "Mano de obra" else "Repuestos") },
                    colors = AssistChipDefaults.assistChipColors()
                )
                OutlinedTextField(
                    value = item.titulo,
                    onValueChange = { item.titulo = it },
                    label = { Text("Título del ítem") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = item.descripcion,
                    onValueChange = { item.descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = item.cantidad,
                        onValueChange = { item.cantidad = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Cantidad (días)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = item.precioUnitario,
                        onValueChange = { item.precioUnitario = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Precio/día") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                val cantidad = item.cantidad.toIntOrNull() ?: 0
                val precio = item.precioUnitario.toIntOrNull() ?: 0
                Text("Subtotal: ${formatCurrency(cantidad * precio)}")
            }
        }
    }
}

private fun buildItemDescripcion(titulo: String, descripcion: String): String {
    val partes = listOf(titulo.trim(), descripcion.trim()).filter { it.isNotBlank() }
    return partes.joinToString(" • ")
}

private fun formatCurrency(monto: Int): String {
    val positivo = monto >= 0
    val valor = kotlin.math.abs(monto)
    val conMiles = valor.toString().reversed().chunked(3).joinToString(".").reversed()
    val prefijo = if (positivo) "$ " else "-$ "
    return prefijo + conMiles
}
