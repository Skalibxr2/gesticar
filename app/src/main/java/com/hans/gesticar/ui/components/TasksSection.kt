package com.hans.gesticar.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hans.gesticar.model.TareaEstado
import java.util.Calendar

@Composable
private fun DatePickerField(
    label: String,
    value: String,
    minDate: Long?,
    onDateSelected: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "dd/MM/aaaa"
) {
    val context = LocalContext.current
    val pickerEnabled = enabled

    fun showPicker() {
        val initialDate = parseTaskDate(value) ?: minDate ?: System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = initialDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                onDateSelected(selected)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            minDate?.let { datePicker.minDate = it }
        }.show()
    }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        readOnly = true,
        enabled = true,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = pickerEnabled) { showPicker() },
        trailingIcon = {
            IconButton(onClick = { showPicker() }, enabled = pickerEnabled) {
                Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
            }
        },
        colors = TextFieldDefaults.outlinedTextFieldColors()
    )
}

@Composable
fun TasksSection(
    tasks: SnapshotStateList<EditableTaskState>,
    soloLectura: Boolean,
    permiteCambiarEstado: Boolean,
    modifier: Modifier = Modifier,
    title: String = "Tareas",
    expandido: Boolean,
    mostrarFormulario: Boolean,
    onToggleExpandido: () -> Unit,
    onToggleFormulario: () -> Unit,
    onAddTask: ((EditableTaskState) -> Unit)? = null,
    onRemoveTask: ((EditableTaskState) -> Unit)? = null,
    permiteEliminar: Boolean = true
) {
    var nuevaDescripcion by rememberSaveable { mutableStateOf("") }
    var nuevoDetalle by rememberSaveable { mutableStateOf("") }
    var nuevaFechaCreacion by rememberSaveable { mutableStateOf(defaultCreationDate()) }
    var nuevaFechaInicio by rememberSaveable { mutableStateOf("") }
    var nuevaFechaTermino by rememberSaveable { mutableStateOf("") }
    var nuevoEstado by rememberSaveable { mutableStateOf(TareaEstado.CREADA) }
    var nuevoEstadoMenu by remember { mutableStateOf(false) }
    var mostrarErroresNuevaTarea by remember { mutableStateOf(false) }

    LaunchedEffect(mostrarFormulario) {
        if (!mostrarFormulario) {
            nuevaDescripcion = ""
            nuevoDetalle = ""
            nuevaFechaCreacion = defaultCreationDate()
            nuevaFechaInicio = ""
            nuevaFechaTermino = ""
            nuevoEstado = TareaEstado.CREADA
            nuevoEstadoMenu = false
            mostrarErroresNuevaTarea = false
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpandido() }
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        if (!expandido) {
                            Text(
                                "Toca para ver y editar las tareas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expandido) "Contraer tareas" else "Expandir tareas"
                    )
                }

                if (expandido) {
                    if (!mostrarFormulario) {
                        OutlinedButton(onClick = onToggleFormulario, enabled = !soloLectura) {
                            Text("Agregar nueva tarea")
                        }
                    }

                    if (mostrarFormulario) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Agregar tarea", style = MaterialTheme.typography.titleSmall)
                            val descripcionVacia = nuevaDescripcion.isBlank()
                            OutlinedTextField(
                                value = nuevaDescripcion,
                                onValueChange = { nuevaDescripcion = it },
                                label = { Text("Descripción de la tarea") },
                                isError = mostrarErroresNuevaTarea && descripcionVacia,
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = {
                                    if (mostrarErroresNuevaTarea && descripcionVacia) {
                                        Text("Campo obligatorio", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = nuevoDetalle,
                                onValueChange = { nuevoDetalle = it },
                                label = { Text("Notas o detalle") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = nuevaFechaCreacion,
                                    onValueChange = {},
                                    label = { Text("Fecha de creación") },
                                    placeholder = { Text("dd/MM/aaaa") },
                                    modifier = Modifier.weight(1f),
                                    readOnly = true,
                                    colors = TextFieldDefaults.outlinedTextFieldColors()
                                )
                                DatePickerField(
                                    label = "Fecha de inicio",
                                    value = nuevaFechaInicio,
                                    minDate = parseTaskDate(nuevaFechaCreacion),
                                    onDateSelected = { nuevaFechaInicio = it },
                                    enabled = !soloLectura,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                DatePickerField(
                                    label = "Fecha de término",
                                    value = nuevaFechaTermino,
                                    minDate = parseTaskDate(nuevaFechaInicio) ?: parseTaskDate(nuevaFechaCreacion),
                                    onDateSelected = { nuevaFechaTermino = it },
                                    enabled = !soloLectura,
                                    modifier = Modifier.weight(1f)
                                )
                                DropdownTextField(
                                    value = nuevoEstado.toReadableName(),
                                    label = "Estado",
                                    expanded = nuevoEstadoMenu,
                                    onExpandedChange = { if (permiteCambiarEstado) nuevoEstadoMenu = it },
                                    onDismissRequest = { nuevoEstadoMenu = false },
                                    modifier = Modifier.weight(1f),
                                    enabled = permiteCambiarEstado
                                ) { closeMenu ->
                                    TareaEstado.values().forEach { estado ->
                                        DropdownMenuItem(
                                            text = { Text(estado.toReadableName()) },
                                            onClick = {
                                                nuevoEstado = estado
                                                nuevoEstadoMenu = false
                                                closeMenu()
                                            }
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = onToggleFormulario) {
                                    Text("Cancelar")
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    mostrarErroresNuevaTarea = true
                                    if (descripcionVacia) return@TextButton

                                    val nuevaEntrada = EditableTaskState(
                                        descripcion = nuevaDescripcion,
                                        detalle = nuevoDetalle,
                                        fechaCreacion = nuevaFechaCreacion,
                                        fechaInicio = nuevaFechaInicio,
                                        fechaTermino = nuevaFechaTermino,
                                        estado = nuevoEstado,
                                        expandido = false
                                    )
                                    onAddTask?.invoke(nuevaEntrada) ?: tasks.add(nuevaEntrada)
                                    tasks.forEach { it.expandido = false }
                                    onToggleFormulario()
                                }) {
                                    Text("Agregar tarea")
                                }
                            }
                        }
                    }

                    if (tasks.isEmpty()) {
                        Text("Aún no hay tareas registradas", style = MaterialTheme.typography.bodySmall)
                    }

                    tasks.forEach { tarea ->
                        TaskItemCard(
                            tarea = tarea,
                            soloLectura = soloLectura,
                            permiteCambiarEstado = permiteCambiarEstado,
                            onExpand = {
                                val debeExpandir = !tarea.expandido
                                tasks.forEach { it.expandido = it.id == tarea.id && debeExpandir }
                            },
                            onRemove = {
                                if (!soloLectura && permiteEliminar) {
                                    onRemoveTask?.invoke(tarea) ?: tasks.remove(tarea)
                                }
                            },
                            permiteEliminar = permiteEliminar
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskItemCard(
    tarea: EditableTaskState,
    soloLectura: Boolean,
    permiteCambiarEstado: Boolean,
    onExpand: () -> Unit,
    onRemove: () -> Unit,
    permiteEliminar: Boolean,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onExpand() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tarea", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Icon(
                            imageVector = if (tarea.expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (tarea.expandido) "Contraer tarea" else "Expandir tarea"
                        )
                    }
                    Text(
                        tarea.descripcion.ifBlank { "Sin descripción" },
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        tarea.estado.toReadableName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove, enabled = !soloLectura && permiteEliminar) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar tarea")
                }
            }

            if (tarea.expandido) {
                val fechaCreacionMillis = parseTaskDate(tarea.fechaCreacion) ?: System.currentTimeMillis()
                OutlinedTextField(
                    value = tarea.descripcion,
                    onValueChange = { tarea.descripcion = it },
                    label = { Text("Descripción de la tarea") },
                    readOnly = soloLectura,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Requerido") }
                )
                OutlinedTextField(
                    value = tarea.detalle,
                    onValueChange = { tarea.detalle = it },
                    label = { Text("Notas o detalle") },
                    readOnly = soloLectura,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tarea.fechaCreacion,
                    onValueChange = {},
                    label = { Text("Fecha de creación") },
                    placeholder = { Text("dd/MM/aaaa") },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors()
                )
                DatePickerField(
                    label = "Fecha de inicio",
                    value = tarea.fechaInicio,
                    minDate = fechaCreacionMillis,
                    onDateSelected = { tarea.fechaInicio = it },
                    enabled = !soloLectura,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val fechaInicioMillis = parseTaskDate(tarea.fechaInicio) ?: fechaCreacionMillis
                DatePickerField(
                    label = "Fecha de término",
                    value = tarea.fechaTermino,
                    minDate = fechaInicioMillis,
                    onDateSelected = { tarea.fechaTermino = it },
                    enabled = !soloLectura,
                    modifier = Modifier.weight(1f)
                )
                DropdownTextField(
                    value = tarea.estado.toReadableName(),
                    label = "Estado",
                    expanded = tarea.estadoMenuExpanded,
                    onExpandedChange = { if (permiteCambiarEstado && !soloLectura) tarea.estadoMenuExpanded = it },
                    onDismissRequest = { tarea.estadoMenuExpanded = false },
                    modifier = Modifier.weight(1f),
                    enabled = permiteCambiarEstado && !soloLectura
                ) { closeMenu ->
                    TareaEstado.values().forEach { estado ->
                        DropdownMenuItem(
                            text = { Text(estado.toReadableName()) },
                            onClick = {
                                    tarea.estado = estado
                                    tarea.estadoMenuExpanded = false
                                    closeMenu()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
