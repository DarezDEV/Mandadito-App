package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDeliveryAddressScreen(
    navController: NavController,
    onAddressSelected: (String) -> Unit = {}
) {
    var selectedAddress by remember { mutableStateOf<String?>(null) }

    val addresses = remember {
        listOf(
            "Dirección actual detectada",
            "Usar ubicación actual",
            "Dirección guardada 1"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecciona dónde entregar") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Selecciona dónde entregar",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Direcciones guardadas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(addresses) { address ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAddress = address
                                onAddressSelected(address)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedAddress == address) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = address,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (selectedAddress == address) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Seleccionado",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            if (selectedAddress == null) {
                Text(
                    text = "Aún no hay direcciones guardadas",
                    modifier = Modifier.fillMaxWidth(),  // ✅ Usa Modifier
                    textAlign = TextAlign.Center,  // ✅ Usa textAlign para centrar el texto
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Navegar a agregar dirección */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAddress != null
            ) {
                Text("Confirmar dirección")
            }

            OutlinedButton(
                onClick = { /* Agregar nueva dirección */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agregar una dirección")
                }
            }
        }
    }
}