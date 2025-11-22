package com.hans.gesticar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hans.gesticar.model.TareaEstado

@Composable
fun TasksSection(
    tasks: SnapshotStateList<EditableTaskState>,
    soloLectura: Boolean,
    modifier: Modifier = Modifier,
    title: String = "Tareas",
    onAddTask: (() -> Unit)? = null,
    onRemoveTask: ((EditableTaskState) -> Unit)? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (tasks.isEmpty()) {
            Text("Aún no hay tareas registradas", style = MaterialTheme.typography.bodySmall)
        }
        tasks.forEach { tarea ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tarea", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                if (!soloLectura) {
                                    onRemoveTask?.invoke(tarea) ?: tasks.remove(tarea)
                                }
                            },
                            enabled = !soloLectura && tasks.size > 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar tarea"
                            )
                        }
                    }
                    OutlinedTextField(
                        value = tarea.descripcion,
                        onValueChange = { tarea.descripcion = it },
                        label = { Text("Descripción de la tarea") },
                        enabled = !soloLectura,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Requerido") }
                    )
                    OutlinedTextField(
                        value = tarea.detalle,
                        onValueChange = { tarea.detalle = it },
                        label = { Text("Notas o detalle") },
                        enabled = !soloLectura,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = tarea.fechaCreacion,
                            onValueChange = { tarea.fechaCreacion = it },
                            label = { Text("Fecha de creación") },
                            placeholder = { Text("dd/MM/aaaa") },
                            enabled = !soloLectura,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = { Text("Usa formato dd/MM/aaaa") }
                        )
                        OutlinedTextField(
                            value = tarea.fechaInicio,
                            onValueChange = { tarea.fechaInicio = it },
                            label = { Text("Fecha de inicio") },
                            placeholder = { Text("dd/MM/aaaa") },
                            enabled = !soloLectura,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = tarea.fechaTermino,
                            onValueChange = { tarea.fechaTermino = it },
                            label = { Text("Fecha de término") },
                            placeholder = { Text("dd/MM/aaaa") },
                            enabled = !soloLectura,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        DropdownTextField(
                            value = tarea.estado.toReadableName(),
                            label = "Estado",
                            expanded = tarea.estadoMenuExpanded,
                            onExpandedChange = { tarea.estadoMenuExpanded = it },
                            onDismissRequest = { tarea.estadoMenuExpanded = false },
                            modifier = Modifier.weight(1f),
                            enabled = !soloLectura
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
        if (!soloLectura) {
            TextButton(
                onClick = { onAddTask?.invoke() ?: tasks.add(EditableTaskState()) }
            ) {
                Text("Agregar tarea")
            }
        }
    }
}
