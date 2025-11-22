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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
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
import com.hans.gesticar.ui.components.VehiclePhotosSection
import com.hans.gesticar.viewmodel.MainViewModel
import com.hans.gesticar.util.formatRutInput
import com.hans.gesticar.util.isRutValid
import com.hans.gesticar.util.normalizeRut
import com.hans.gesticar.util.sanitizeRutInput
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

private class SymptomForm(
    descripcion: String = "",
    fechaTexto: String = "",
    fotos: MutableList<Uri> = mutableStateListOf()
) {
    var descripcion by mutableStateOf(descripcion)
    var fechaTexto by mutableStateOf(fechaTexto)
    val fotos = fotos
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

    var presupuestoAprobado by rememberSaveable { mutableStateOf(false) }
    val items = remember { mutableStateListOf(PresupuestoItemForm()) }
    val seleccionMecanicos = remember { mutableStateListOf<String>() }
    var vehiculoSeleccionado by rememberSaveable { mutableStateOf<String?>(null) }
    val sintomas = remember { mutableStateListOf(SymptomForm()) }

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
            presupuestoAprobado = false
            seleccionMecanicos.clear()
            vehiculoSeleccionado = null
            items.clear()
            items += PresupuestoItemForm()
            sintomas.clear()
            sintomas += SymptomForm()
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
            onSeleccionarVehiculo = { vehiculo -> vehiculoSeleccionado = vehiculo?.patente },
            onCrearNuevoVehiculo = {
                vehiculoSeleccionado = null
                patente = ""
                marca = ""
                modelo = ""
                anio = ""
                color = ""
                kilometraje = ""
                combustible = ""
            },
            onGuardarVehiculo = {
                val anioInt = anio.toIntOrNull()
                if (rutNormalizado != null && patente.isNotBlank() && marca.isNotBlank() && modelo.isNotBlank() && anioInt != null) {
                    val kmInt = kilometraje.toIntOrNull()
                    vm.guardarVehiculo(
                        Vehiculo(
                            patente = patente.uppercase(),
                            clienteRut = rutNormalizado,
                            marca = marca,
                            modelo = modelo,
                            anio = anioInt,
                            color = color.takeIf { it.isNotBlank() },
                            kilometraje = kmInt,
                            combustible = combustible.takeIf { it.isNotBlank() }
                        )
                    )
                    vehiculoSeleccionado = patente.uppercase()
                }
            },
            guardandoVehiculo = uiState.guardandoVehiculo,
            mensajeVehiculo = uiState.mensajeVehiculo
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
                        sintomas = sintomasValidos,
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
                        sintomas = sintomasValidos,
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
    onCrearNuevoVehiculo: () -> Unit,
    onGuardarVehiculo: () -> Unit,
    guardandoVehiculo: Boolean,
    mensajeVehiculo: String?
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
                                    role = Role.RadioButton,
                                    onClick = { onSeleccionarVehiculo(vehiculo) }
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = esSeleccionado,
                                onClick = { onSeleccionarVehiculo(vehiculo) }
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text("${vehiculo.patente} • ${vehiculo.marca} ${vehiculo.modelo}")
                                Text(
                                    text = listOfNotNull(
                                        vehiculo.anio.toString(),
                                        vehiculo.color,
                                        vehiculo.kilometraje?.let { "${it} km" }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = onCrearNuevoVehiculo,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Registrar nuevo vehículo")
                }
            }
            OutlinedTextField(
                value = patente,
                onValueChange = onPatenteChange,
                label = { Text("Patente") },
                modifier = Modifier.fillMaxWidth()
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
            Button(
                onClick = onGuardarVehiculo,
                enabled = patente.isNotBlank() && marca.isNotBlank() && modelo.isNotBlank() && anio.length == 4 && !guardandoVehiculo,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Guardar vehículo")
            }
        }
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

private fun parseSymptomTimestamp(input: String): Long? {
    if (input.isBlank()) return null
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return runCatching {
        LocalDateTime.parse(input.trim(), formatter)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}
