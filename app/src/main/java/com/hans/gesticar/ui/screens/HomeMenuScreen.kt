// ui/screens/HomeMenuScreen.kt
package com.hans.gesticar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hans.gesticar.ui.Routes
import com.hans.gesticar.viewmodel.MainViewModel

@Composable
fun HomeMenuScreen(vm: MainViewModel, nav: NavController) {
    val ui by vm.ui.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Bienvenido${if (ui.displayName!=null) ", ${ui.displayName}" else ""}",
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
    }
}
