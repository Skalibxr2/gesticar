package com.hans.gesticar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtDetalle
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.TareaOt
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import com.hans.gesticar.ui.components.DropdownTextField
import com.hans.gesticar.ui.components.VehiclePhotosSection
import com.hans.gesticar.util.formatRutInput
import com.hans.gesticar.util.normalizeRut
import com.hans.gesticar.viewmodel.MainViewModel
import com.hans.gesticar.viewmodel.DetalleMensajes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun SearchOtScreen(vm: MainViewModel) {
    val ui by vm.ui.collectAsState()
    val usuario = ui.usuarioActual

    var nroTexto by rememberSaveable { mutableStateOf("") }
    var patente by rememberSaveable { mutableStateOf("") }
    var rutTexto by rememberSaveable { mutableStateOf("") }
    var estadoExpanded by remember { mutableStateOf(false) }
    var estadoSeleccionado by remember { mutableStateOf<OtState?>(null) }
    var filtrosVisibles by rememberSaveable { mutableStateOf(true) }

    // Al seleccionar una OT mostramos solo el detalle para aprovechar la pantalla completa.
    LaunchedEffect(ui.detalleSeleccionado?.ot?.id) {
        filtrosVisibles = ui.detalleSeleccionado == null
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("GestiCar • Órdenes de Trabajo", style = MaterialTheme.typography.titleLarge)
        when (usuario?.rol) {
            Rol.ADMIN -> Text("Puedes administrar todas las órdenes registradas.")
            Rol.MECANICO -> Text("Solo verás y podrás editar tus propias órdenes asignadas.")
            else -> Text("Inicia sesión para realizar búsquedas.")
        }

        if (filtrosVisibles) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nroTexto,
                    onValueChange = { nroTexto = it.filter(Char::isDigit) },
                    label = { Text("N° OT") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    val numero = nroTexto.toIntOrNull()
                    if (numero != null) {
                        vm.buscarPorNumero(numero)
                    }
                }) { Text("Buscar N°") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = patente,
                    onValueChange = { patente = it.uppercase() },
                    label = { Text("Patente") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { if (patente.isNotBlank()) vm.buscarPorPatente(patente) }) {
                    Text("Buscar Patente")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rutTexto,
                    onValueChange = { rutTexto = formatRutInput(it) },
                    label = { Text("RUT Cliente") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    if (rutTexto.isNotBlank()) {
                        vm.buscarPorRut(normalizeRut(rutTexto))
                    }
                }) { Text("Buscar RUT") }
            }

            // Se despliega correctamente la lista de estados para filtrar por estado de la OT.
            DropdownTextField(
                value = estadoSeleccionado?.toReadableName().orEmpty(),
                label = "Estado OT",
                expanded = estadoExpanded,
                onExpandedChange = { estadoExpanded = it },
                onDismissRequest = { estadoExpanded = false },
                placeholder = { Text("Buscar por estado") },
                modifier = Modifier.fillMaxWidth()
            ) { closeMenu ->
                val estados = OtState.values()
                estados.forEach { estado ->
                    DropdownMenuItem(
                        text = { Text(estado.toReadableName()) },
                        onClick = {
                            estadoSeleccionado = estado
                            closeMenu()
                            vm.buscarPorEstado(estado)
                        }
                    )
                }
                if (estados.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Sin estados disponibles") },
                        onClick = {},
                        enabled = false
                    )
                }
            }

            TextButton(onClick = { filtrosVisibles = false }) {
                Text("Ocultar filtros")
            }
        } else {
            TextButton(onClick = { filtrosVisibles = true }) {
                Text("Mostrar filtros de búsqueda")
            }
        }

        ui.mensaje?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Divider()
        Text("Resultados", style = MaterialTheme.typography.titleMedium)
        if (ui.resultadosBusqueda.isEmpty()) {
            Text("Sin resultados para los filtros seleccionados")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.resultadosBusqueda) { ot ->
                    val seleccionado = ui.detalleSeleccionado?.ot?.id == ot.id
                    OtCard(ot = ot, seleccionado = seleccionado) {
                        vm.seleccionarOt(ot.id)
                    }
                }
            }
        }

        if (ui.cargandoDetalle) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        ui.detalleSeleccionado?.let { detalle ->
            OtDetailPanel(
                detalle = detalle,
                mecanicos = ui.mecanicosDisponibles,
                vehiculosCliente = ui.vehiculosCliente,
                mensajes = ui.detalleMensajes,
                onGuardarDatos = { notas, mecanicosIds, patenteNueva ->
                    vm.guardarDatosOt(detalle.ot.id, notas, mecanicosIds, patenteNueva)
                },
                onGuardarPresupuesto = { items, aprobado, iva ->
                    vm.guardarPresupuesto(detalle.ot.id, items, aprobado, iva)
                },
                onGuardarTareas = { tareas -> vm.guardarTareas(detalle.ot.id, tareas) },
                onIniciar = { vm.iniciarOt(detalle.ot.id) },
                onFinalizar = { vm.finalizarOt(detalle.ot.id) },
                onCerrar = {
                    filtrosVisibles = true
                    vm.limpiarSeleccion()
                }
            )
        }
    }
}

@Composable
private fun OtCard(ot: Ot, seleccionado: Boolean, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val fechaFormateada = remember(ot.fechaCreacion) {
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                formatter.format(Date(ot.fechaCreacion))
            }
            Text("OT #${ot.numero}", style = MaterialTheme.typography.titleMedium)
            Text("Creada el: $fechaFormateada", style = MaterialTheme.typography.bodySmall)
            Text("Estado: ${ot.estado.toReadableName()}")
            if (seleccionado) {
                Text("Seleccionada", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private class PresupuestoItemFormState(
    val id: String = UUID.randomUUID().toString(),
    tipo: ItemTipo = ItemTipo.MO,
    descripcion: String = "",
    cantidad: String = "1",
    precio: String = ""
) {
    var tipo by mutableStateOf(tipo)
    var descripcion by mutableStateOf(descripcion)
    var cantidad by mutableStateOf(cantidad)
    var precio by mutableStateOf(precio)
    var tipoMenuExpanded by mutableStateOf(false)
}

private class EditableTaskState(
    val id: String = UUID.randomUUID().toString(),
    titulo: String = "",
    descripcion: String = "",
    completada: Boolean = false
) {
    var titulo by mutableStateOf(titulo)
    var descripcion by mutableStateOf(descripcion)
    var completada by mutableStateOf(completada)
}

@Composable
private fun OtDetailPanel(
    detalle: OtDetalle,
    mecanicos: List<Usuario>,
    vehiculosCliente: List<Vehiculo>,
    mensajes: DetalleMensajes,
    onGuardarDatos: (String?, List<String>, String?) -> Unit,
    onGuardarPresupuesto: (List<PresupuestoItem>, Boolean, Int) -> Unit,
    onGuardarTareas: (List<TareaOt>) -> Unit,
    onIniciar: () -> Unit,
    onFinalizar: () -> Unit,
    onCerrar: () -> Unit
) {
    val scrollState = rememberScrollState()
    var notas by remember { mutableStateOf(detalle.ot.notas.orEmpty()) }
    var patente by remember { mutableStateOf(detalle.vehiculo?.patente ?: detalle.ot.vehiculoPatente) }
    val mecanicosSeleccionados = remember { mutableStateListOf<String>() }
    var selectorMecanicosExpandido by remember { mutableStateOf(false) }
    var selectorVehiculoExpandido by remember { mutableStateOf(false) }
    var presupuestoAprobado by remember { mutableStateOf(detalle.presupuesto.aprobado) }
    var ivaTexto by remember { mutableStateOf(detalle.presupuesto.ivaPorc.toString()) }
    val items = remember { mutableStateListOf<PresupuestoItemFormState>() }
    val tareas = remember { mutableStateListOf<EditableTaskState>() }
    var presupuestoError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(detalle.ot.id, vehiculosCliente) {
        notas = detalle.ot.notas.orEmpty()
        patente = detalle.vehiculo?.patente ?: detalle.ot.vehiculoPatente
        mecanicosSeleccionados.clear()
        mecanicosSeleccionados.addAll(detalle.ot.mecanicosAsignados)
        selectorMecanicosExpandido = false
        selectorVehiculoExpandido = false
        presupuestoAprobado = detalle.presupuesto.aprobado
        ivaTexto = detalle.presupuesto.ivaPorc.toString()
        items.clear()
        detalle.presupuesto.items.forEach { item ->
            items += PresupuestoItemFormState(
                id = item.id,
                tipo = item.tipo,
                descripcion = item.descripcion,
                cantidad = item.cantidad.toString(),
                precio = item.precioUnit.toString()
            )
        }
        if (items.isEmpty()) {
            items += PresupuestoItemFormState()
        }
        tareas.clear()
        detalle.tareas.forEach { tarea ->
            tareas += EditableTaskState(
                id = tarea.id,
                titulo = tarea.titulo,
                descripcion = tarea.descripcion.orEmpty(),
                completada = tarea.completada
            )
        }
        if (tareas.isEmpty()) {
            tareas += EditableTaskState()
        }
    }

    val vehiculoEditable = when (detalle.ot.estado) {
        OtState.EN_EJECUCION, OtState.FINALIZADA -> false
        else -> true
    }
    // Una OT finalizada queda solo para consulta; bloqueamos todas las acciones.
    val soloLectura = detalle.ot.estado == OtState.FINALIZADA
    val tieneItemsValidos = items.any { item ->
        val cantidad = item.cantidad.toIntOrNull() ?: 0
        val precio = item.precio.toIntOrNull() ?: 0
        item.descripcion.isNotBlank() && cantidad > 0 && precio > 0
    }
    val puedeIniciar = detalle.ot.estado !in listOf(OtState.EN_EJECUCION, OtState.FINALIZADA) &&
        presupuestoAprobado && tieneItemsValidos &&
        detalle.cliente != null && patente.isNotBlank() && mecanicosSeleccionados.isNotEmpty()
    val puedeFinalizar = detalle.ot.estado == OtState.EN_EJECUCION

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Detalle OT #${detalle.ot.numero}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCerrar) { Text("Cerrar") }
            }
            Text("Estado actual: ${detalle.ot.estado.toReadableName()}")
            detalle.cliente?.let { cliente ->
                Text("Cliente: ${cliente.nombre} (${cliente.rut})", style = MaterialTheme.typography.bodyMedium)
            } ?: Text("Cliente: no registrado", color = MaterialTheme.colorScheme.error)
            val vehiculoActual = vehiculosCliente.firstOrNull { it.patente == patente } ?: detalle.vehiculo
            vehiculoActual?.let { vehiculo ->
                Text("Vehículo actual: ${vehiculo.marca} ${vehiculo.modelo} (${vehiculo.patente})")
            } ?: Text("Vehículo no registrado", color = MaterialTheme.colorScheme.error)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Datos generales", style = MaterialTheme.typography.titleSmall)
                DropdownTextField(
                    value = vehiculoActual?.let { "${it.patente} • ${it.marca} ${it.modelo}" }
                        ?: patente,
                    label = "Vehículo asociado",
                    expanded = selectorVehiculoExpandido,
                    onExpandedChange = { selectorVehiculoExpandido = it },
                    onDismissRequest = { selectorVehiculoExpandido = false },
                    enabled = vehiculoEditable && !soloLectura && vehiculosCliente.isNotEmpty(),
                    placeholder = { Text("Selecciona un vehículo registrado") },
                    modifier = Modifier.fillMaxWidth()
                ) { closeMenu ->
                    if (vehiculosCliente.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Sin vehículos disponibles") },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        vehiculosCliente.forEach { vehiculo ->
                            DropdownMenuItem(
                                text = { Text("${vehiculo.patente} • ${vehiculo.marca} ${vehiculo.modelo}") },
                                onClick = {
                                    patente = vehiculo.patente
                                    closeMenu()
                                }
                            )
                        }
                    }
                }
                if (!vehiculoEditable) {
                    Text(
                        "No es posible cambiar el vehículo una vez iniciada la OT",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (vehiculosCliente.isEmpty()) {
                    Text(
                        "El cliente no tiene otros vehículos registrados",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas / Síntomas") },
                    enabled = !soloLectura,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mecánicos asignados", style = MaterialTheme.typography.labelLarge)
                    // Usamos el mismo patrón que en la creación de OT: menú para agregar y lista editable.
                    DropdownTextField(
                        value = "",
                        label = "Agregar mecánico",
                        expanded = selectorMecanicosExpandido,
                        onExpandedChange = { selectorMecanicosExpandido = it },
                        onDismissRequest = { selectorMecanicosExpandido = false },
                        placeholder = { Text("Selecciona un mecánico disponible") },
                        enabled = !soloLectura && mecanicos.any { it.id !in mecanicosSeleccionados },
                        modifier = Modifier.fillMaxWidth()
                    ) { closeMenu ->
                        val disponibles = mecanicos.filter { it.id !in mecanicosSeleccionados }
                        if (disponibles.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Sin mecánicos disponibles") },
                                onClick = {},
                                enabled = false
                            )
                        } else {
                            disponibles.forEach { mecanico ->
                                DropdownMenuItem(
                                    text = { Text(mecanico.nombre) },
                                    onClick = {
                                        mecanicosSeleccionados += mecanico.id
                                        closeMenu()
                                    }
                                )
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        mecanicos.filter { it.id in mecanicosSeleccionados }.forEach { mecanico ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(mecanico.nombre)
                                    Text(mecanico.email, style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(
                                    onClick = { mecanicosSeleccionados.remove(mecanico.id) },
                                    enabled = !soloLectura
                                ) {
                                    Text("Quitar")
                                }
                            }
                        }
                        if (mecanicosSeleccionados.isEmpty()) {
                            Text("Selecciona al menos un mecánico", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (mecanicos.isEmpty()) {
                        Text("No hay mecánicos disponibles", color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    onClick = {
                        onGuardarDatos(notas, mecanicosSeleccionados.toList(), patente)
                    },
                    enabled = !soloLectura
                ) {
                    Text("Guardar datos generales")
                }
                mensajes.datos?.let { mensaje ->
                    Text(
                        mensaje.text,
                        color = if (mensaje.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                VehiclePhotosSection(
                    receptionTitle = "Evidencia al recibir el vehículo",
                    completionTitle = "Evidencia de reparación o entrega"
                )
            }

            Divider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Presupuesto", style = MaterialTheme.typography.titleSmall)
                items.forEach { item ->
                    PresupuestoItemEditor(
                        item = item,
                        soloLectura = soloLectura,
                        onRemove = { if (!soloLectura && items.size > 1) items.remove(item) }
                    )
                }
                if (!soloLectura) {
                    TextButton(onClick = { items += PresupuestoItemFormState() }) {
                        Text("Agregar ítem")
                    }
                }
                var subtotal = 0
                var subtotalRep = 0
                var subtotalMo = 0
                items.forEach { item ->
                    val cantidad = item.cantidad.toIntOrNull() ?: 0
                    val precio = item.precio.toIntOrNull() ?: 0
                    val total = cantidad * precio
                    subtotal += total
                    if (item.tipo == ItemTipo.REP) subtotalRep += total else subtotalMo += total
                }
                val iva = ((subtotal * (ivaTexto.toIntOrNull() ?: 0)) / 100)
                val total = subtotal + iva
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = presupuestoAprobado,
                        onCheckedChange = { presupuestoAprobado = it },
                        enabled = !soloLectura,
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (presupuestoAprobado) "Presupuesto aprobado" else "Presupuesto pendiente")
                }
                OutlinedTextField(
                    value = ivaTexto,
                    onValueChange = { valor ->
                        ivaTexto = valor.filter { it.isDigit() }
                    },
                    label = { Text("IVA %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !soloLectura,
                    modifier = Modifier.width(120.dp)
                )
                Text("Subtotal MO: $subtotalMo")
                Text("Subtotal Repuestos: $subtotalRep")
                Text("Subtotal: $subtotal")
                Text("IVA: $iva")
                Text("Total: $total", style = MaterialTheme.typography.bodyLarge)
                presupuestoError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        val ivaInt = ivaTexto.toIntOrNull()
                        val itemsValidos = items.mapNotNull { item ->
                            val cantidad = item.cantidad.toIntOrNull()
                            val precio = item.precio.toIntOrNull()
                            if (cantidad == null || precio == null || item.descripcion.isBlank()) {
                                null
                            } else {
                                PresupuestoItem(
                                    id = item.id,
                                    tipo = item.tipo,
                                    descripcion = item.descripcion.trim(),
                                    cantidad = cantidad,
                                    precioUnit = precio
                                )
                            }
                        }
                        if (ivaInt == null || itemsValidos.size != items.size) {
                            presupuestoError = "Revisa los datos numéricos del presupuesto"
                        } else {
                            presupuestoError = null
                            onGuardarPresupuesto(itemsValidos, presupuestoAprobado, ivaInt)
                        }
                    },
                    enabled = !soloLectura
                ) {
                    Text("Guardar presupuesto")
                }
                mensajes.presupuesto?.let { mensaje ->
                    Text(
                        mensaje.text,
                        color = if (mensaje.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Divider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tareas", style = MaterialTheme.typography.titleSmall)
                tareas.forEach { tarea ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = tarea.completada,
                                    onCheckedChange = { tarea.completada = it },
                                    enabled = !soloLectura
                                )
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = tarea.titulo,
                                    onValueChange = { tarea.titulo = it },
                                    label = { Text("Nombre de tarea") },
                                    enabled = !soloLectura,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { if (!soloLectura && tareas.size > 1) tareas.remove(tarea) },
                                    enabled = !soloLectura
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar tarea")
                                }
                            }
                            OutlinedTextField(
                                value = tarea.descripcion,
                                onValueChange = { tarea.descripcion = it },
                                label = { Text("Descripción realizada") },
                                enabled = !soloLectura,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                if (!soloLectura) {
                    TextButton(onClick = { tareas += EditableTaskState() }) { Text("Agregar tarea") }
                }
                Button(
                    onClick = {
                        val tareasValidas = tareas.filter { it.titulo.isNotBlank() }.map {
                            TareaOt(
                                id = it.id,
                                titulo = it.titulo.trim(),
                                descripcion = it.descripcion.takeIf(String::isNotBlank),
                                completada = it.completada
                            )
                        }
                        onGuardarTareas(tareasValidas)
                    },
                    enabled = !soloLectura
                ) {
                    Text("Guardar tareas")
                }
                mensajes.tareas?.let { mensaje ->
                    Text(
                        mensaje.text,
                        color = if (mensaje.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Divider()

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onIniciar, enabled = puedeIniciar && !soloLectura) {
                    Text("Iniciar OT")
                }
                Button(onClick = onFinalizar, enabled = puedeFinalizar && !soloLectura) {
                    Text("Finalizar OT")
                }
            }
            mensajes.estado?.let { mensaje ->
                Text(
                    mensaje.text,
                    color = if (mensaje.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PresupuestoItemEditor(item: PresupuestoItemFormState, soloLectura: Boolean, onRemove: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ítem", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemove, enabled = !soloLectura) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar ítem")
                }
            }
            DropdownTextField(
                value = when (item.tipo) {
                    ItemTipo.MO -> "Mano de obra"
                    ItemTipo.REP -> "Repuesto"
                },
                label = "Tipo",
                expanded = item.tipoMenuExpanded,
                onExpandedChange = { item.tipoMenuExpanded = it },
                onDismissRequest = { item.tipoMenuExpanded = false },
                enabled = !soloLectura,
                modifier = Modifier.fillMaxWidth()
            ) { closeMenu ->
                DropdownMenuItem(text = { Text("Mano de obra") }, onClick = {
                    if (!soloLectura) {
                        item.tipo = ItemTipo.MO
                        closeMenu()
                    }
                })
                DropdownMenuItem(text = { Text("Repuesto") }, onClick = {
                    if (!soloLectura) {
                        item.tipo = ItemTipo.REP
                        closeMenu()
                    }
                })
            }
            OutlinedTextField(
                value = item.descripcion,
                onValueChange = { item.descripcion = it },
                label = { Text("Descripción") },
                enabled = !soloLectura,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.cantidad,
                    onValueChange = { item.cantidad = it.filter(Char::isDigit) },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !soloLectura,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = item.precio,
                    onValueChange = { item.precio = it.filter(Char::isDigit) },
                    label = { Text("Precio unitario") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !soloLectura,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Formatea el estado interno a un nombre amigable para los usuarios.
private fun OtState.toReadableName(): String = when (this) {
    OtState.BORRADOR -> "Borrador"
    OtState.DIAGNOSTICO -> "Diagnóstico"
    OtState.PRESUPUESTO -> "Presupuesto"
    OtState.PEND_APROB -> "Pendiente de aprobación"
    OtState.EN_EJECUCION -> "En ejecución"
    OtState.FINALIZADA -> "Finalizada"
}

