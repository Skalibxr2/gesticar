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
import com.hans.gesticar.viewmodel.MainViewModel

@Composable
fun HomeMenuScreen(vm: MainViewModel, nav: NavController) {
    val ui by vm.ui.collectAsState()
    var showCreateUser by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Bienvenido${ui.usuarioActual?.let { ", ${it.nombre}" } ?: ""}",
            style = MaterialTheme.typography.headlineSmall
        )
        Text("¿Qué deseas hacer hoy?", style = MaterialTheme.typography.titleMedium)

        // Opciones del administrador (agrega más cuando quieras)
        Button(
            onClick = { nav.navigate(Routes.SEARCH_OT) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Buscar Orden de Trabajo") }

        Button(
            onClick = { /* TODO: nav a Reportes */ },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Ver Reportes") }

        Button(
            onClick = { /* TODO: nav a Crear OT */ },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Crear nueva OT") }

        if (ui.usuarioActual?.rol == Rol.ADMIN) {
            Button(onClick = { showCreateUser = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Crear usuario")
            }
        }

        Button(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }

        ui.mensaje?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }
    }

    if (showCreateUser) {
        CrearUsuarioDialog(
            onDismiss = { showCreateUser = false },
            onCreate = { nombre, email, password, rol ->
                val creado = vm.crearUsuario(nombre, email, password, rol)
                if (creado) {
                    showCreateUser = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = when (rol) {
                            Rol.ADMIN -> "Administrador"
                            Rol.MECANICO -> "Mecánico"
                        },
                        onValueChange = {},
                        label = { Text("Rol") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Administrador") },
                            onClick = {
                                rol = Rol.ADMIN
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mecánico") },
                            onClick = {
                                rol = Rol.MECANICO
                                expanded = false
                            }
                        )
                    }
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
