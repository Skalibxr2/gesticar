// ui/screens/HomeMenuScreen.kt
package com.hans.gesticar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.OtState
import com.hans.gesticar.model.Rol
import com.hans.gesticar.ui.Routes
import com.hans.gesticar.ui.components.DropdownTextField
import com.hans.gesticar.viewmodel.MainViewModel

@Composable
fun HomeMenuScreen(vm: MainViewModel, nav: NavController) {
    val ui by vm.ui.collectAsState()
    val usuario = ui.usuarioActual

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Bienvenido${ui.displayName?.let { ", ${it}" } ?: ""}",
            style = MaterialTheme.typography.headlineSmall
        )
        if (usuario == null) {
            Text("Debes iniciar sesión para ver opciones.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = { vm.logout() }) {
                    Text("Cerrar sesión")
                }
            }

            when (usuario.rol) {
                Rol.ADMIN -> {
                    Text("Panel de administración", style = MaterialTheme.typography.titleMedium)
                    AdminActions(
                        ots = ui.ots,
                        onCrear = { nav.navigate(Routes.CREATE_OT) },
                        onBuscar = { nav.navigate(Routes.SEARCH_OT) },
                        onEstadoSeleccionado = { estado ->
                            vm.buscarPorEstado(estado)
                        },
                        onOtSeleccionada = { estado, otId ->
                            vm.buscarPorEstado(estado)
                            vm.seleccionarOt(otId)
                            nav.navigate(Routes.SEARCH_OT)
                        }
                    )
                }
                Rol.MECANICO -> {
                    Text("Panel de mecánico", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Solo puedes gestionar las OTs que te fueron asignadas.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    MechanicActions(onBuscar = { nav.navigate(Routes.SEARCH_OT) })
                }
            }
        }
    }
}

@Composable
private fun AdminActions(
    ots: List<Ot>,
    onCrear: () -> Unit,
    onBuscar: () -> Unit,
    onEstadoSeleccionado: (OtState) -> Unit,
    onOtSeleccionada: (OtState, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onCrear, modifier = Modifier.fillMaxWidth()) {
            Text("Crear Orden de Trabajo")
        }
        Button(onClick = onBuscar, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar Orden de Trabajo")
        }

        EstadoResumenGrid(
            ots = ots,
            onEstadoSeleccionado = onEstadoSeleccionado,
            onOtSeleccionada = onOtSeleccionada
        )
    }
}

@Composable
private fun MechanicActions(onBuscar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { /* TODO: ver OTs asignadas */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Ver mis OTs asignadas")
        }
        Button(onClick = onBuscar, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar mis OTs")
        }
    }
}

@Composable
private fun EstadoResumenGrid(
    ots: List<Ot>,
    onEstadoSeleccionado: (OtState) -> Unit,
    onOtSeleccionada: (OtState, String) -> Unit
) {
    val estadosConfig = listOf(
        OtState.BORRADOR to Color(0xFF90A4AE),
        OtState.DIAGNOSTICO to Color(0xFF81D4FA),
        OtState.PRESUPUESTO to Color(0xFFCE93D8),
        OtState.PEND_APROB to Color(0xFFFFF176),
        OtState.EN_EJECUCION to Color(0xFF80CBC4),
        OtState.FINALIZADA to Color(0xFFA5D6A7),
        OtState.CANCELADA to Color(0xFFEF9A9A)
    )

    var estadoExpandido by remember { mutableStateOf<OtState?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Órdenes por estado",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(estadosConfig) { (estado, color) ->
                val cantidad = ots.count { it.estado == estado }
                EstadoCard(
                    titulo = estado.toReadableName(),
                    cantidad = cantidad,
                    color = color,
                    seleccionado = estadoExpandido == estado,
                    onClick = {
                        val nuevoEstado = if (estadoExpandido == estado) null else estado
                        estadoExpandido = nuevoEstado
                        nuevoEstado?.let { onEstadoSeleccionado(it) }
                    }
                )
            }
        }

        estadoExpandido?.let { estado ->
            val otsEstado = ots.filter { it.estado == estado }
            if (otsEstado.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(otsEstado) { ot ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOtSeleccionada(estado, ot.id)
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("OT ${ot.numero}", style = MaterialTheme.typography.titleMedium)
                                Text("Patente: ${ot.vehiculoPatente}", style = MaterialTheme.typography.bodySmall)
                                Text("Estado: ${estado.toReadableName()}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                Text(
                    "No hay OTs en ${estado.toReadableName()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EstadoCard(
    titulo: String,
    cantidad: Int,
    color: Color,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    val tarjetaColor = if (seleccionado) color.copy(alpha = 0.9f) else color

    Box(
        modifier = Modifier
            .size(120.dp)
            .background(color = tarjetaColor, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    cantidad.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("OTs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun OtState.toReadableName(): String = when (this) {
    OtState.BORRADOR -> "Borrador"
    OtState.DIAGNOSTICO -> "Diagnóstico"
    OtState.PRESUPUESTO -> "Presupuesto"
    OtState.PEND_APROB -> "Pendiente de aprobación"
    OtState.EN_EJECUCION -> "En ejecución"
    OtState.FINALIZADA -> "Finalizada"
    OtState.CANCELADA -> "Cancelada"
}

@Composable
private fun CrearUsuarioDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Rol) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var rol by remember { mutableStateOf(Rol.MECANICO) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") })
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation()
                )
                DropdownTextField(
                    value = when (rol) {
                        Rol.ADMIN -> "Administrador"
                        Rol.MECANICO -> "Mecánico"
                    },
                    label = "Rol",
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) { closeMenu ->
                    DropdownMenuItem(
                        text = { Text("Administrador") },
                        onClick = {
                            rol = Rol.ADMIN
                            closeMenu()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mecánico") },
                        onClick = {
                            rol = Rol.MECANICO
                            closeMenu()
                        }
                    )
                }
            }
        },
        confirmButton = {
            val datosValidos = nombre.isNotBlank() && email.isNotBlank() && password.isNotBlank()
            TextButton(onClick = { onCreate(nombre, email, password, rol) }, enabled = datosValidos) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
