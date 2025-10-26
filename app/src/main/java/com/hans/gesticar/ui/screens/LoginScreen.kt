package com.hans.gesticar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hans.gesticar.viewmodel.UiState

@Composable
fun LoginScreen(uiState: UiState, onLogin: (String, String) -> Unit) {
    var email by remember { mutableStateOf("admin@gesticar.cl") }
    var pass by remember { mutableStateOf("admin") }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ingreso a GestiCar", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            Button(onClick = { onLogin(email, pass) }, modifier = Modifier.fillMaxWidth()) { Text("Ingresar") }
            uiState.mensaje?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}