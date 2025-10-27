package com.hans.gesticar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hans.gesticar.model.Cliente
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import com.hans.gesticar.ui.Routes
import com.hans.gesticar.viewmodel.MainViewModel

private data class PresupuestoItemForm(
    var tipo: ItemTipo = ItemTipo.MO,
    var descripcion: String = "",
    var cantidad: String = "1",
    var precioUnitario: String = ""
)

@Composable
fun CreateOtScreen(vm: MainViewModel, nav: NavController) {
    val uiState by vm.createOtUi.collectAsState()

    LaunchedEffect(Unit) {
        vm.prepararNuevaOt()
    }

    var rut by rememberSaveable { mutableStateOf("") }
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
    var seleccionMecanicos by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(uiState.exito) {
        if (uiState.exito) {
            rut = ""
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
            seleccionMecanicos = emptySet()
            items.clear()
            items += PresupuestoItemForm()
        }
    }

    LaunchedEffect(uiState.mecanicos) {
        val disponibles = uiState.mecanicos.map(Usuario::id).toSet()
        seleccionMecanicos = seleccionMecanicos.filter { it in disponibles }.toSet()
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
            rut = rut,
            onRutChange = { rut = it.uppercase() },
            nombre = nombre,
            onNombreChange = { nombre = it },
            correo = correo,
            onCorreoChange = { correo = it },
            direccion = direccion,
            onDireccionChange = { direccion = it },
            telefono = telefono,
            onTelefonoChange = { telefono = it }
        )

        VehiculoSection(
            patente = patente,
            onPatenteChange = { patente = it.uppercase() },
            marca = marca,
            onMarcaChange = { marca = it },
            modelo = modelo,
            onModeloChange = { modelo = it },
            anio = anio,
            onAnioChange = { anio = it.filter { ch -> ch.isDigit() } },
            color = color,
            onColorChange = { color = it },
            kilometraje = kilometraje,
            onKilometrajeChange = { kilometraje = it.filter { ch -> ch.isDigit() } },
            combustible = combustible,
            onCombustibleChange = { combustible = it },
            sintomas = sintomas,
            onSintomasChange = { sintomas = it }
        )

        MecanicosSection(
            mecanicos = uiState.mecanicos,
            seleccionados = seleccionMecanicos,
            onToggle = { id ->
                seleccionMecanicos = if (id in seleccionMecanicos) {
                    seleccionMecanicos - id
                } else {
                    seleccionMecanicos + id
                }
            }
        )

        PresupuestoSection(
            items = items,
            presupuestoAprobado = presupuestoAprobado,
            onToggleAprobado = { presupuestoAprobado = !presupuestoAprobado }
        )

        val anioInt = anio.toIntOrNull()
        val kmInt = kilometraje.toIntOrNull()
        val clienteValido = rut.isNotBlank() && nombre.isNotBlank()
        val vehiculoValido = patente.isNotBlank() && marca.isNotBlank() && modelo.isNotBlank() && anioInt != null
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
                    val cliente = Cliente(
                        rut = rut,
                        nombre = nombre,
                        correo = correo.takeIf { it.isNotBlank() },
                        direccion = direccion.takeIf { it.isNotBlank() },
                        telefono = telefono.takeIf { it.isNotBlank() }
                    )
                    val vehiculo = Vehiculo(
                        patente = patente,
                        clienteRut = rut,
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
                    val cliente = Cliente(
                        rut = rut,
                        nombre = nombre,
                        correo = correo.takeIf { it.isNotBlank() },
                        direccion = direccion.takeIf { it.isNotBlank() },
                        telefono = telefono.takeIf { it.isNotBlank() }
                    )
                    val vehiculo = Vehiculo(
                        patente = patente,
                        clienteRut = rut,
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
    nombre: String,
    onNombreChange: (String) -> Unit,
    correo: String,
    onCorreoChange: (String) -> Unit,
    direccion: String,
    onDireccionChange: (String) -> Unit,
    telefono: String,
    onTelefonoChange: (String) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Datos del cliente", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = rut,
                onValueChange = onRutChange,
                label = { Text("RUT") },
                modifier = Modifier.fillMaxWidth()
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
    sintomas: String,
    onSintomasChange: (String) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Datos del vehículo", style = MaterialTheme.typography.titleMedium)
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
                    modifier = Modifier.weight(1f)
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
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = combustible,
                    onValueChange = onCombustibleChange,
                    label = { Text("Combustible") },
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = sintomas,
                onValueChange = onSintomasChange,
                label = { Text("Síntomas entregados por el cliente") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MecanicosSection(
    mecanicos: List<Usuario>,
    seleccionados: Set<String>,
    onToggle: (String) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Mecánicos asignados", style = MaterialTheme.typography.titleMedium)
            if (mecanicos.isEmpty()) {
                Text("No hay mecánicos registrados", style = MaterialTheme.typography.bodyMedium)
            } else {
                mecanicos.forEach { mecanico ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = mecanico.id in seleccionados,
                            onCheckedChange = { onToggle(mecanico.id) }
                        )
                        Column {
                            Text(mecanico.nombre, style = MaterialTheme.typography.bodyLarge)
                            Text(mecanico.email, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
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
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = item.precioUnitario,
                onValueChange = { item.precioUnitario = it.filter { ch -> ch.isDigit() } },
                label = { Text("Precio unitario") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
