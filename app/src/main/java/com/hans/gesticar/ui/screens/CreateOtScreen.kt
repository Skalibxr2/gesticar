package com.hans.gesticar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import com.hans.gesticar.ui.Routes
import com.hans.gesticar.ui.components.VehiclePhotosSection
import com.hans.gesticar.viewmodel.MainViewModel
import com.hans.gesticar.util.formatRutInput
import com.hans.gesticar.util.isRutValid
import com.hans.gesticar.util.normalizeRut
import com.hans.gesticar.util.sanitizeRutInput

private class PresupuestoItemForm(
    tipo: ItemTipo = ItemTipo.MO,
    descripcion: String = "",
    cantidad: String = "1",
    precioUnitario: String = ""
) {
    var tipo by mutableStateOf(tipo)
    var descripcion by mutableStateOf(descripcion)
    var cantidad by mutableStateOf(cantidad)
    var precioUnitario by mutableStateOf(precioUnitario)
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
    var telefono by rememberSaveable { mutableStateOf("") }

    var patente by rememberSaveable { mutableStateOf("") }
    var marca by rememberSaveable { mutableStateOf("") }
    var modelo by rememberSaveable { mutableStateOf("") }
    var anio by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("") }
    var kilometraje by rememberSaveable { mutableStateOf("") }
    var combustible by rememberSaveable { mutableStateOf("") }
    var sintomas by rememberSaveable { mutableStateOf("") }

    var presupuestoAprobado by rememberSaveable { mutableStateOf(false) }
    val items = remember { mutableStateListOf(PresupuestoItemForm()) }
    val seleccionMecanicos = remember { mutableStateListOf<String>() }
    var vehiculoSeleccionado by rememberSaveable { mutableStateOf<String?>(null) }
    var mostrarFormularioVehiculo by rememberSaveable { mutableStateOf(false) }
    var esEdicionVehiculo by rememberSaveable { mutableStateOf(false) }
    var flujoNuevoVehiculo by rememberSaveable { mutableStateOf(false) }
    var patenteBusqueda by rememberSaveable { mutableStateOf("") }
    var vehiculoInfoDialog by remember { mutableStateOf<Vehiculo?>(null) }
    var mostrarConfirmarCancelarRegistro by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.exito) {
        if (uiState.exito) {
            rutSanitized = ""
            nombre = ""
            correo = ""
            direccion = ""
            telefono = ""
            patente = ""
            marca = ""
            modelo = ""
            anio = ""
            color = ""
            kilometraje = ""
            combustible = ""
            sintomas = ""
            presupuestoAprobado = false
            seleccionMecanicos.clear()
            vehiculoSeleccionado = null
            items.clear()
            items += PresupuestoItemForm()
        }
    }

    LaunchedEffect(uiState.mecanicos) {
        val disponibles = uiState.mecanicos.map(Usuario::id).toSet()
        seleccionMecanicos.removeAll { it !in disponibles }
    }

    val rutValido = isRutValid(rutSanitized)
    val rutNormalizado = if (rutValido) normalizeRut(rutSanitized) else null

    LaunchedEffect(rutNormalizado) {
        rutNormalizado?.let { vm.buscarClientePorRut(it) }
    }

    LaunchedEffect(uiState.cliente?.rut, rutNormalizado) {
        val cliente = uiState.cliente
        if (cliente != null && rutNormalizado == normalizeRut(cliente.rut)) {
            nombre = cliente.nombre
            correo = cliente.correo.orEmpty()
            direccion = cliente.direccion.orEmpty()
            telefono = cliente.telefono.orEmpty()
        } else if (rutNormalizado == null || cliente == null) {
            nombre = ""
            correo = ""
            direccion = ""
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
            !uiState.mensajeVehiculo.contains("error", ignoreCase = true)
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
            telefono = telefono,
            onTelefonoChange = { telefono = it },
            onGuardarCliente = {
                if (rutNormalizado != null && nombre.isNotBlank()) {
                    vm.guardarCliente(
                        Cliente(
                            rut = rutNormalizado,
                            nombre = nombre,
                            correo = correo.takeIf { it.isNotBlank() },
                            direccion = direccion.takeIf { it.isNotBlank() },
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
            sintomas = sintomas,
            onSintomasChange = { sintomas = it },
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

        vehiculoInfoDialog?.let { vehiculo ->
            AlertDialog(
                onDismissRequest = { vehiculoInfoDialog = null },
                confirmButton = {
                    TextButton(onClick = { vehiculoInfoDialog = null }) {
                        Text("Cerrar")
                    }
                },
                title = { Text("Información del vehículo") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Patente: ${vehiculo.patente}")
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

        val anioInt = anio.toIntOrNull()
        val kmInt = kilometraje.toIntOrNull()
        val clienteValido = rutNormalizado != null && nombre.isNotBlank()
        val vehiculoValido = patente.isNotBlank() && marca.isNotBlank() && modelo.isNotBlank() && anioInt != null && anio.length == 4
        val presupuestoItemsValidos = items.mapNotNull { form ->
            val cantidadInt = form.cantidad.toIntOrNull()
            val precioInt = form.precioUnitario.toIntOrNull()
            if (form.descripcion.isBlank() || cantidadInt == null || precioInt == null) {
                null
            } else {
                PresupuestoItem(
                    tipo = form.tipo,
                    descripcion = form.descripcion,
                    cantidad = cantidadInt,
                    precioUnit = precioInt
                )
            }
        }
        val puedeGuardar = clienteValido && vehiculoValido && !uiState.guardando
        val puedeIniciar = puedeGuardar && presupuestoAprobado && presupuestoItemsValidos.isNotEmpty()

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
                    vm.crearOt(
                        cliente = cliente,
                        vehiculo = vehiculo,
                        mecanicosIds = seleccionMecanicos.toList(),
                        presupuestoItems = presupuestoItemsValidos,
                        presupuestoAprobado = presupuestoAprobado,
                        sintomas = sintomas.takeIf { it.isNotBlank() },
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
                    vm.crearOt(
                        cliente = cliente,
                        vehiculo = vehiculo,
                        mecanicosIds = seleccionMecanicos.toList(),
                        presupuestoItems = presupuestoItemsValidos,
                        presupuestoAprobado = true,
                        sintomas = sintomas.takeIf { it.isNotBlank() },
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
    telefono: String,
    onTelefonoChange: (String) -> Unit,
    onGuardarCliente: () -> Unit,
    guardandoCliente: Boolean,
    mensajeCliente: String?
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Datos del cliente", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = rut,
                onValueChange = onRutChange,
                label = { Text("RUT") },
                modifier = Modifier.fillMaxWidth(),
                isError = mostrarRutInvalido,
                supportingText = {
                    when {
                        guardandoCliente -> Text("Guardando cliente...")
                        mostrarRutInvalido -> Text("Ingresa un RUT válido con dígito verificador")
                        mensajeCliente != null -> {
                            val esError = mensajeCliente.contains("error", ignoreCase = true)
                            Text(
                                mensajeCliente,
                                color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = onNombreChange,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = correo,
                onValueChange = onCorreoChange,
                label = { Text("Correo") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = direccion,
                onValueChange = onDireccionChange,
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = telefono,
                onValueChange = onTelefonoChange,
                label = { Text("Teléfono") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onGuardarCliente,
                enabled = rutValido && nombre.isNotBlank() && !guardandoCliente,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Guardar cliente")
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
    sintomas: String,
    onSintomasChange: (String) -> Unit,
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = anio,
                        onValueChange = onAnioChange,
                        label = { Text("Año") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = color,
                        onValueChange = onColorChange,
                        label = { Text("Color") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
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
                    TextButton(onClick = onCancelarEdicion) {
                        Text(if (flujoNuevoVehiculo) "Cancelar" else "Cancelar cambios")
                    }
                    Button(
                        onClick = onGuardarVehiculo,
                        enabled = !guardandoVehiculo
                    ) {
                        Text(if (esEdicion) "Guardar cambios" else "Agregar vehículo")
                    }
                }
            }

            OutlinedTextField(
                value = sintomas,
                onValueChange = onSintomasChange,
                label = { Text("Síntomas entregados por el cliente") },
                modifier = Modifier.fillMaxWidth()
            )
            VehiclePhotosSection(
                receptionTitle = "Fotos al recibir el vehículo",
                completionTitle = "Fotos de avance o entrega"
            )
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Presupuesto", style = MaterialTheme.typography.titleMedium)
            items.forEachIndexed { index, item ->
                PresupuestoItemRow(
                    item = item,
                    onRemove = { if (items.size > 1) items.removeAt(index) }
                )
                Divider()
            }
            Button(onClick = { items += PresupuestoItemForm() }) {
                Text("Agregar ítem")
            }
            val subtotal = items.sumOf { form ->
                val cantidad = form.cantidad.toIntOrNull() ?: 0
                val precio = form.precioUnitario.toIntOrNull() ?: 0
                cantidad * precio
            }
            val iva = (subtotal * 19) / 100
            val total = subtotal + iva
            Text("Subtotal: ${subtotal}")
            Text("IVA (19%): ${iva}")
            Text("Total: ${total}")
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AssistChip(
                onClick = {
                    item.tipo = if (item.tipo == ItemTipo.MO) ItemTipo.REP else ItemTipo.MO
                },
                label = { Text(if (item.tipo == ItemTipo.MO) "Mano de obra" else "Repuestos") },
                colors = AssistChipDefaults.assistChipColors()
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
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
                label = { Text("Cantidad") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = item.precioUnitario,
                onValueChange = { item.precioUnitario = it.filter { ch -> ch.isDigit() } },
                label = { Text("Precio unitario") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}
