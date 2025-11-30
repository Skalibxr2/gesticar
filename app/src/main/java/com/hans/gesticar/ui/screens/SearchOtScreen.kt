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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hans.gesticar.model.ItemTipo
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtDetalle
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.PresupuestoItem
import com.hans.gesticar.model.Rol
import com.hans.gesticar.model.TareaEstado
import com.hans.gesticar.model.TareaOt
import com.hans.gesticar.model.Usuario
import com.hans.gesticar.model.Vehiculo
import com.hans.gesticar.ui.components.EditableTaskState
import com.hans.gesticar.ui.components.DropdownTextField
import com.hans.gesticar.ui.components.TasksSection
import com.hans.gesticar.ui.components.toEditableTaskState
import com.hans.gesticar.ui.components.toTareaOt
import com.hans.gesticar.util.formatRutInput
import com.hans.gesticar.util.normalizeRut
import com.hans.gesticar.viewmodel.MainViewModel
import com.hans.gesticar.viewmodel.DetalleMensajes
import com.hans.gesticar.viewmodel.SearchResult
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
    var filtrosExpandido by rememberSaveable { mutableStateOf(true) }
    var resultadosExpandido by rememberSaveable { mutableStateOf(false) }
    var filtroError by rememberSaveable { mutableStateOf<String?>(null) }
    val filtrosFocusRequester = remember { FocusRequester() }
    var solicitarFocoFiltros by remember { mutableStateOf(false) }

    // Al seleccionar una OT mostramos solo el detalle para aprovechar la pantalla completa.
    LaunchedEffect(ui.detalleSeleccionado?.ot?.id) {
        filtrosExpandido = ui.detalleSeleccionado == null
    }

    LaunchedEffect(solicitarFocoFiltros) {
        if (solicitarFocoFiltros) {
            filtrosFocusRequester.requestFocus()
            solicitarFocoFiltros = false
        }
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

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Filtros de búsqueda", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Cada campo añade una capa al resultado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AssistChip(
                        onClick = { filtrosExpandido = !filtrosExpandido },
                        label = { Text(if (filtrosExpandido) "Contraer" else "Expandir") }
                    )
                }

                if (filtrosExpandido) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = nroTexto,
                                onValueChange = { nroTexto = it.filter(Char::isDigit) },
                                label = { Text("N° OT") },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(filtrosFocusRequester)
                            )
                            OutlinedTextField(
                                value = patente,
                                onValueChange = { patente = it.uppercase() },
                                label = { Text("Patente") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rutTexto,
                                onValueChange = { rutTexto = formatRutInput(it) },
                                label = { Text("RUT Cliente") },
                                modifier = Modifier.weight(1f)
                            )
                            DropdownTextField(
                                value = estadoSeleccionado?.toReadableName().orEmpty(),
                                label = "Estado OT",
                                expanded = estadoExpanded,
                                onExpandedChange = { estadoExpanded = it },
                                onDismissRequest = { estadoExpanded = false },
                                placeholder = { Text("Buscar por estado") },
                                modifier = Modifier.weight(1f)
                            ) { closeMenu ->
                                val estados = OtState.values()
                                estados.forEach { estado ->
                                    DropdownMenuItem(
                                        text = { Text(estado.toReadableName()) },
                                        onClick = {
                                            estadoSeleccionado = estado
                                            closeMenu()
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
                        }

                        filtroError?.let { mensaje ->
                            Text(mensaje, color = MaterialTheme.colorScheme.error)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                val numero = nroTexto.toIntOrNull()
                                val patenteFiltro = patente.takeIf { it.isNotBlank() }?.uppercase()
                                val rutFiltro = rutTexto.takeIf { it.isNotBlank() }?.let { normalizeRut(it) }
                                val estadoFiltro = estadoSeleccionado
                                val hayFiltros = listOfNotNull(numero, patenteFiltro, rutFiltro, estadoFiltro).isNotEmpty()

                                if (!hayFiltros) {
                                    filtroError = "Ingresa al menos un parámetro de búsqueda"
                                    return@Button
                                }

                                filtroError = null
                                resultadosExpandido = true
                                vm.buscarPorFiltros(numero, patenteFiltro, rutFiltro, estadoFiltro)
                            }) {
                                Text("Buscar")
                            }
                            TextButton(onClick = {
                                nroTexto = ""
                                patente = ""
                                rutTexto = ""
                                estadoSeleccionado = null
                                filtroError = null
                            }) {
                                Text("Limpiar")
                            }
                        }
                    }
                }
            }
        }

        ui.mensaje?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Divider()
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Resultados", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Se mostrarán las órdenes encontradas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AssistChip(
                        onClick = { resultadosExpandido = !resultadosExpandido },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (resultadosExpandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (resultadosExpandido) "Contraer" else "Expandir")
                            }
                        }
                    )
                }

                if (resultadosExpandido) {
                    if (ui.resultadosBusqueda.isEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No se encontraron resultados para tu búsqueda")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    vm.limpiarResultados()
                                    resultadosExpandido = false
                                    filtrosExpandido = true
                                    solicitarFocoFiltros = true
                                }) {
                                    Text("Realizar nueva búsqueda")
                                }
                                TextButton(onClick = {
                                    vm.limpiarResultados()
                                    resultadosExpandido = false
                                    filtrosExpandido = true
                                    solicitarFocoFiltros = true
                                }) {
                                    Text("Limpiar resultados")
                                }
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ui.resultadosBusqueda) { resultado ->
                                val seleccionado = ui.detalleSeleccionado?.ot?.id == resultado.ot.id
                                OtCard(resultado = resultado, seleccionado = seleccionado) {
                                    vm.seleccionarOt(resultado.ot.id)
                                }
                            }
                        }
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
                onCancelar = { vm.cancelarOt(detalle.ot.id) },
                onEliminarBorrador = { vm.eliminarBorrador(detalle.ot.id) },
                onCerrar = {
                    filtrosExpandido = true
                    vm.limpiarSeleccion()
                }
            )
        }
    }
}

@Composable
private fun OtCard(resultado: SearchResult, seleccionado: Boolean, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val fechaFormateada = remember(resultado.ot.fechaCreacion) {
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                formatter.format(Date(resultado.ot.fechaCreacion))
            }
            Text("OT ${resultado.ot.numero}", style = MaterialTheme.typography.titleMedium)
            resultado.clienteNombre?.let { nombre ->
                Text("Cliente: $nombre", style = MaterialTheme.typography.bodySmall)
            }
            Text("Patente: ${resultado.patente}", style = MaterialTheme.typography.bodySmall)
            Text("Estado: ${resultado.estado.toReadableName()}")
            Text("Creada el: $fechaFormateada", style = MaterialTheme.typography.bodySmall)
            if (seleccionado) {
                Text("Seleccionada", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private class PresupuestoItemFormState(
    val id: String = UUID.randomUUID().toString(),
    tipo: ItemTipo = ItemTipo.MO,
    titulo: String = "",
    descripcion: String = "",
    cantidad: String = "1",
    precio: String = "",
    expandido: Boolean = false
) {
    var tipo by mutableStateOf(tipo)
    var titulo by mutableStateOf(titulo)
    var descripcion by mutableStateOf(descripcion)
    var cantidad by mutableStateOf(cantidad)
    var precio by mutableStateOf(precio)
    var expandido by mutableStateOf(expandido)
}

private fun PresupuestoItemFormState.copy(
    id: String = this.id,
    tipo: ItemTipo = this.tipo,
    titulo: String = this.titulo,
    descripcion: String = this.descripcion,
    cantidad: String = this.cantidad,
    precio: String = this.precio,
    expandido: Boolean = this.expandido,
): PresupuestoItemFormState = PresupuestoItemFormState(
    id = id,
    tipo = tipo,
    titulo = titulo,
    descripcion = descripcion,
    cantidad = cantidad,
    precio = precio,
    expandido = expandido,
)

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
    onCancelar: () -> Unit,
    onEliminarBorrador: () -> Unit,
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
    var tareasExpandido by remember { mutableStateOf(false) }
    var mostrarFormularioTareas by remember { mutableStateOf(false) }
    var presupuestoError by remember { mutableStateOf<String?>(null) }
    var nuevoItem by remember { mutableStateOf(PresupuestoItemFormState()) }
    var mostrarErroresNuevoItem by remember { mutableStateOf(false) }

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
                titulo = item.descripcion,
                descripcion = "",
                cantidad = item.cantidad.toString(),
                precio = item.precioUnit.toString(),
                expandido = true
            )
        }
        if (items.isEmpty()) {
            items += PresupuestoItemFormState(expandido = true)
        }
        nuevoItem = PresupuestoItemFormState()
        mostrarErroresNuevoItem = false
        tareas.clear()
        detalle.tareas.forEach { tarea ->
            tareas += tarea.toEditableTaskState()
        }
        tareasExpandido = false
        mostrarFormularioTareas = false
    }

    val enEjecucion = detalle.ot.estado == OtState.EN_EJECUCION
    val vehiculoEditable = when (detalle.ot.estado) {
        OtState.EN_EJECUCION, OtState.FINALIZADA, OtState.CANCELADA -> false
        else -> true
    }
    val permiteCambiarEstadoTareas = enEjecucion
    // Una OT finalizada queda solo para consulta; bloqueamos todas las acciones.
    val soloLectura = detalle.ot.estado == OtState.FINALIZADA || detalle.ot.estado == OtState.CANCELADA
    val permiteEliminarTareas = !soloLectura && detalle.ot.estado != OtState.EN_EJECUCION
    val tieneItemsValidos = items.any { item ->
        val cantidad = item.cantidad.toIntOrNull() ?: 0
        val precio = item.precio.toIntOrNull() ?: 0
        item.descripcion.isNotBlank() && cantidad > 0 && precio > 0
    }
    val puedeIniciar = detalle.ot.estado !in listOf(OtState.EN_EJECUCION, OtState.FINALIZADA, OtState.CANCELADA) &&
        presupuestoAprobado && tieneItemsValidos &&
        detalle.cliente != null && patente.isNotBlank() && mecanicosSeleccionados.isNotEmpty()
    val puedeFinalizar = detalle.ot.estado == OtState.EN_EJECUCION
    var mostrarErroresDatos by remember { mutableStateOf(false) }
    var mensajeValidacionEstado by remember { mutableStateOf<String?>(null) }
    val patenteVacia = patente.isBlank()
    val sinMecanicos = mecanicosSeleccionados.isEmpty()
    val notasVacias = notas.isBlank()
    var mostrarConfirmacionEliminar by remember { mutableStateOf(false) }
    var mostrarConfirmacionCancelar by remember { mutableStateOf(false) }
    val guardarDatosValidados = {
        mostrarErroresDatos = true
        mensajeValidacionEstado = null
        if (!patenteVacia && !sinMecanicos && !notasVacias) {
            onGuardarDatos(notas, mecanicosSeleccionados.toList(), patente)
        }
    }

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
                Text("OT ${detalle.ot.numero}", style = MaterialTheme.typography.titleMedium)
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
                if (mostrarErroresDatos && patenteVacia) {
                    Text(
                        "Selecciona un vehículo para continuar",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas / Síntomas") },
                    enabled = !soloLectura,
                    isError = mostrarErroresDatos && notasVacias,
                    supportingText = {
                        if (mostrarErroresDatos && notasVacias) {
                            Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    ),
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
                            if (mostrarErroresDatos && sinMecanicos) {
                                Text(
                                    "Debes asignar mecánicos antes de guardar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    if (mecanicos.isEmpty()) {
                        Text("No hay mecánicos disponibles", color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    onClick = guardarDatosValidados,
                    enabled = !soloLectura
                ) {
                    Text("Guardar OT")
                }
                mensajes.datos?.let { mensaje ->
                    Text(
                        mensaje.text,
                        color = if (mensaje.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TasksSection(
                    tasks = tareas,
                    soloLectura = soloLectura,
                    permiteCambiarEstado = permiteCambiarEstadoTareas,
                    modifier = Modifier.fillMaxWidth(),
                    title = "Tareas preventivas o correctivas",
                    expandido = tareasExpandido,
                    mostrarFormulario = mostrarFormularioTareas,
                    onToggleExpandido = {
                        tareasExpandido = !tareasExpandido
                        if (!tareasExpandido) {
                            mostrarFormularioTareas = false
                        }
                    },
                    onToggleFormulario = { mostrarFormularioTareas = !mostrarFormularioTareas },
                    onAddTask = { nuevaTarea -> tareas += nuevaTarea },
                    onRemoveTask = { tarea -> tareas.remove(tarea) },
                    permiteEliminar = permiteEliminarTareas
                )
                Button(
                    onClick = {
                        val tareasValidas = tareas
                            .filter { it.descripcion.isNotBlank() }
                            .map { it.toTareaOt() }
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Presupuesto", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Los precios/día incluyen IVA",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!soloLectura) {
                    Text("Agregar ítem", style = MaterialTheme.typography.titleSmall)
                    Text("Tipo de ítem del presupuesto", style = MaterialTheme.typography.bodySmall)
                    AssistChip(
                        onClick = {
                            nuevoItem = nuevoItem.copy(tipo = if (nuevoItem.tipo == ItemTipo.MO) ItemTipo.REP else ItemTipo.MO)
                        },
                        label = { Text(if (nuevoItem.tipo == ItemTipo.MO) "Mano de obra" else "Repuestos") },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                    val tituloInvalido = nuevoItem.titulo.isBlank()
                    OutlinedTextField(
                        value = nuevoItem.titulo,
                        onValueChange = { nuevoItem = nuevoItem.copy(titulo = it) },
                        label = { Text("Título del ítem") },
                        enabled = !soloLectura,
                        isError = mostrarErroresNuevoItem && tituloInvalido,
                        supportingText = {
                            if (mostrarErroresNuevoItem && tituloInvalido) {
                                Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nuevoItem.descripcion,
                        onValueChange = { nuevoItem = nuevoItem.copy(descripcion = it) },
                        label = { Text("Descripción") },
                        enabled = !soloLectura,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val cantidad = nuevoItem.cantidad.toIntOrNull()
                    val precio = nuevoItem.precio.toIntOrNull()
                    val cantidadInvalida = cantidad == null
                    val precioInvalido = precio == null
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = nuevoItem.cantidad,
                            onValueChange = { nuevoItem = nuevoItem.copy(cantidad = it.filter(Char::isDigit)) },
                            label = { Text("Cantidad (días)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !soloLectura,
                            isError = mostrarErroresNuevoItem && cantidadInvalida,
                            supportingText = {
                                if (mostrarErroresNuevoItem && cantidadInvalida) {
                                    Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                        OutlinedTextField(
                            value = nuevoItem.precio,
                            onValueChange = { nuevoItem = nuevoItem.copy(precio = it.filter(Char::isDigit)) },
                            label = { Text("Precio/día (con IVA)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !soloLectura,
                            isError = mostrarErroresNuevoItem && precioInvalido,
                            supportingText = {
                                if (mostrarErroresNuevoItem && precioInvalido) {
                                    Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                    Button(
                        onClick = {
                            val tituloValido = nuevoItem.titulo.isNotBlank()
                            val cantidadValida = cantidad != null
                            val precioValido = precio != null
                            if (tituloValido && cantidadValida && precioValido) {
                                mostrarErroresNuevoItem = false
                                items += nuevoItem.copy(expandido = false)
                                nuevoItem = PresupuestoItemFormState()
                            } else {
                                mostrarErroresNuevoItem = true
                            }
                        },
                        enabled = !soloLectura
                    ) {
                        Text("Agregar ítem")
                    }
                    Divider()
                }
                Text("Ítems del presupuesto", style = MaterialTheme.typography.titleSmall)
                if (items.isEmpty()) {
                    Text("Aún no hay ítems agregados")
                }
                items.forEach { item ->
                    PresupuestoItemEditor(
                        item = item,
                        soloLectura = soloLectura,
                        onRemove = { if (!soloLectura) items.remove(item) }
                    )
                }
                Divider()
                val ivaPorcentaje = ivaTexto.toIntOrNull() ?: 0
                val divisorIva = (100 + ivaPorcentaje).takeIf { it > 0 } ?: 100
                var subtotalMoSinIva = 0
                var subtotalRepSinIva = 0
                var totalConIva = 0
                items.forEach { item ->
                    val cantidadItem = item.cantidad.toIntOrNull() ?: 0
                    val precioItem = item.precio.toIntOrNull() ?: 0
                    val totalItem = cantidadItem * precioItem
                    totalConIva += totalItem
                    val itemSinIva = (totalItem * 100) / divisorIva
                    if (item.tipo == ItemTipo.REP) subtotalRepSinIva += itemSinIva else subtotalMoSinIva += itemSinIva
                }
                val subtotalSinIva = subtotalMoSinIva + subtotalRepSinIva
                val iva = totalConIva - subtotalSinIva
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
                Text("Subtotal MO (sin IVA): ${formatCurrency(subtotalMoSinIva)}")
                Text("Subtotal Repuestos (sin IVA): ${formatCurrency(subtotalRepSinIva)}")
                Text("Subtotal (sin IVA): ${formatCurrency(subtotalSinIva)}")
                Text("IVA (${ivaPorcentaje}%): ${formatCurrency(iva)}")
                Text("Total (con IVA): ${formatCurrency(totalConIva)}", style = MaterialTheme.typography.bodyLarge)
                presupuestoError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        val ivaInt = ivaTexto.toIntOrNull()
                        val itemsValidos = items.mapNotNull { item ->
                            val cantidad = item.cantidad.toIntOrNull()
                            val precio = item.precio.toIntOrNull()
                            if (cantidad == null || precio == null || item.titulo.isBlank()) {
                                null
                            } else {
                                PresupuestoItem(
                                    id = item.id,
                                    tipo = item.tipo,
                                    descripcion = buildItemDescripcion(item.titulo, item.descripcion),
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

            Divider()

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (detalle.ot.estado == OtState.BORRADOR) {
                    OutlinedButton(onClick = { mostrarConfirmacionEliminar = true }, enabled = !soloLectura) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Eliminar borrador")
                    }
                    Button(onClick = guardarDatosValidados, enabled = !soloLectura) {
                        Text("Guardar borrador")
                    }
                }
                Button(onClick = onIniciar, enabled = puedeIniciar && !soloLectura) {
                    Text("Iniciar OT")
                }
                if (enEjecucion) {
                    OutlinedButton(onClick = { mostrarConfirmacionCancelar = true }, enabled = !soloLectura) {
                        Text("Cancelar OT")
                    }
                }
                Button(
                    onClick = {
                        mostrarErroresDatos = true
                        val tareasInvalidas = tareas.any { it.estado !in listOf(TareaEstado.TERMINADA, TareaEstado.CANCELADA) }
                        if (patenteVacia || sinMecanicos || notasVacias) {
                            mensajeValidacionEstado = "Faltan datos obligatorios para finalizar"
                            return@Button
                        }
                        if (tareasInvalidas) {
                            mensajeValidacionEstado = "Asegúrate de que todas las tareas estén terminadas o canceladas"
                            return@Button
                        }
                        mensajeValidacionEstado = null
                        onFinalizar()
                    },
                    enabled = puedeFinalizar && !soloLectura
                ) {
                    Text("Finalizar OT")
                }
            }
            mensajeValidacionEstado?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            mensajes.estado?.let { mensaje ->
                Text(
                    mensaje.text,
                    color = if (mensaje.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (mostrarConfirmacionEliminar) {
                AlertDialog(
                    onDismissRequest = { mostrarConfirmacionEliminar = false },
                    title = { Text("Eliminar borrador") },
                    text = { Text("Esto eliminará el borrador por completo y no se podrá recuperar.") },
                    confirmButton = {
                        TextButton(onClick = {
                            mostrarConfirmacionEliminar = false
                            onEliminarBorrador()
                        }) {
                            Text("Eliminar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarConfirmacionEliminar = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            if (mostrarConfirmacionCancelar) {
                AlertDialog(
                    onDismissRequest = { mostrarConfirmacionCancelar = false },
                    title = { Text("Cancelar OT") },
                    text = { Text("Esto dará por cancelada la OT y no se podrá modificar.") },
                    confirmButton = {
                        TextButton(onClick = {
                            mostrarConfirmacionCancelar = false
                            onCancelar()
                        }) {
                            Text("Cancelar OT")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarConfirmacionCancelar = false }) {
                            Text("Volver")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PresupuestoItemEditor(item: PresupuestoItemFormState, soloLectura: Boolean, onRemove: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !soloLectura) { item.expandido = !item.expandido }
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (item.tipo == ItemTipo.MO) "MO" else "Repuesto", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(item.titulo.ifBlank { "Sin título" }, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemove, enabled = !soloLectura) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar ítem")
                }
            }
            if (item.expandido) {
                Text("Tipo de ítem del presupuesto", style = MaterialTheme.typography.bodySmall)
                AssistChip(
                    onClick = {
                        if (!soloLectura) {
                            item.tipo = if (item.tipo == ItemTipo.MO) ItemTipo.REP else ItemTipo.MO
                        }
                    },
                    label = { Text(if (item.tipo == ItemTipo.MO) "Mano de obra" else "Repuestos") },
                    enabled = !soloLectura,
                    colors = AssistChipDefaults.assistChipColors()
                )
                OutlinedTextField(
                    value = item.titulo,
                    onValueChange = { item.titulo = it },
                    label = { Text("Título del ítem") },
                    enabled = !soloLectura,
                    modifier = Modifier.fillMaxWidth()
                )
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
                        label = { Text("Cantidad (días)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !soloLectura,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = item.precio,
                        onValueChange = { item.precio = it.filter(Char::isDigit) },
                        label = { Text("Precio/día (con IVA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !soloLectura,
                        modifier = Modifier.weight(1f)
                    )
                }
                val cantidad = item.cantidad.toIntOrNull() ?: 0
                val precio = item.precio.toIntOrNull() ?: 0
                Text("Subtotal (con IVA): ${formatCurrency(cantidad * precio)}")
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

// Formatea el estado interno a un nombre amigable para los usuarios.
private fun OtState.toReadableName(): String = when (this) {
    OtState.BORRADOR -> "Borrador"
    OtState.DIAGNOSTICO -> "Diagnóstico"
    OtState.PRESUPUESTO -> "Presupuesto"
    OtState.PEND_APROB -> "Pendiente de aprobación"
    OtState.EN_EJECUCION -> "En ejecución"
    OtState.FINALIZADA -> "Finalizada"
    OtState.CANCELADA -> "Cancelada"
}

