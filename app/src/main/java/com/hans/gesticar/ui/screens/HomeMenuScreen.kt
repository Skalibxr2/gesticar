// ui/screens/HomeMenuScreen.kt
package com.hans.gesticar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
            when (usuario.rol) {
                Rol.ADMIN -> {
                    Text("Panel de administración", style = MaterialTheme.typography.titleMedium)
                    AdminActions(
                        onCrear = { nav.navigate(Routes.CREATE_OT) },
                        onBuscar = { nav.navigate(Routes.SEARCH_OT) }
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
private fun AdminActions(onCrear: () -> Unit, onBuscar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onCrear, modifier = Modifier.fillMaxWidth()) {
            Text("Crear Orden de Trabajo")
        }
        Button(onClick = onBuscar, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar Orden de Trabajo")
        }
        Button(onClick = { /* TODO: implementar edición */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Editar Orden de Trabajo")
        }
        Button(onClick = { /* TODO: implementar cierre */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar Orden de Trabajo")
        }
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
        Button(onClick = { /* TODO: edición parcial */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Editar OT asignada")
        }
    }
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
