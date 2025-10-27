package com.hans.gesticar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hans.gesticar.model.Ot
import com.hans.gesticar.model.Rol
import com.hans.gesticar.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SearchOtScreen(vm: MainViewModel) {
    val ui by vm.ui.collectAsState()
    val usuario = ui.usuarioActual

    var nroTexto by remember { mutableStateOf("") }
    var patente by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("GestiCar • Órdenes de Trabajo", style = MaterialTheme.typography.titleLarge)
        when (usuario?.rol) {
            Rol.ADMIN -> Text("Puedes administrar todas las órdenes registradas.")
            Rol.MECANICO -> Text("Solo verás y podrás editar tus propias órdenes asignadas.")
            else -> Text("Inicia sesión para realizar búsquedas.")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = nroTexto,
                onValueChange = { nroTexto = it.filter { ch -> ch.isDigit() } },
                label = { Text("N° OT") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                val n = nroTexto.toIntOrNull()
                if (n != null) vm.buscarPorNumero(n)
            }) { Text("Buscar N°") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = patente,
                onValueChange = { patente = it.uppercase() },
                label = { Text("Patente") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { if (patente.isNotBlank()) vm.buscarPorPatente(patente) }) { Text("Buscar Patente") }
        }

        ui.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Divider()
        Text("Resultados", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.resultadosBusqueda) { ot ->
                OtCard(ot)
            }
        }
    }
}

@Composable
private fun OtCard(ot: Ot) {
    ElevatedCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val fechaFormateada = remember(ot.fechaCreacion) {
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                formatter.format(Date(ot.fechaCreacion))
            }
            Text("OT #${'$'}{ot.numero}", style = MaterialTheme.typography.titleMedium)
            Text("Creada el: ${'$'}fechaFormateada", style = MaterialTheme.typography.bodySmall)
            Text("Estado: ${'$'}{ot.estado}")
        }
    }
}

