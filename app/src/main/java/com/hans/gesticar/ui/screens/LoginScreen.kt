package com.hans.gesticar.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hans.gesticar.R

@Composable
fun LoginScreen(mensaje: String?, onLogin: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Logotipo de GestiCar",
                modifier = Modifier
                    .size(96.dp)
            )
            Text("Ingreso a GestiCar", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true
            )
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            if (mensaje != null) {
                Text(mensaje, color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = { onLogin(email, pass) }, modifier = Modifier.fillMaxWidth()) { Text("Ingresar") }
            Text(
                "Administrador: admin@gesticar.cl / admin\nMecánico: mecanico@gesticar.cl / mecanico",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
