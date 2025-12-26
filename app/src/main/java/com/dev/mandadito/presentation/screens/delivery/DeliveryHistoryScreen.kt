package com.dev.mandadito.presentation.screens.delivery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryHistoryScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
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
                .padding(paddingValues)  // ✅ Agregar paddingValues
                .padding(16.dp)
        ) {
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Card(
                    modifier = Modifier.weight(1f).padding(4.dp)  // ✅ Quitar horizontalAlignment
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally  // ✅ Mover aquí
                    ) {
                        Text(
                            text = "$1000",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Ganado")
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).padding(4.dp)  // ✅ Quitar horizontalAlignment
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally  // ✅ Mover aquí
                    ) {
                        Text(
                            text = "6",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Entregas")
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).padding(4.dp)  // ✅ Quitar horizontalAlignment
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally  // ✅ Mover aquí
                    ) {
                        Text(
                            text = "4.8",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Rating")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filtros
            TabRow(selectedTabIndex = 0) {
                Tab(selected = true, onClick = { /* Hoy */ }, text = { Text("Hoy") })
                Tab(selected = false, onClick = { /* Semana */ }, text = { Text("Semana") })
                Tab(selected = false, onClick = { /* Mes */ }, text = { Text("Mes") })
                Tab(selected = false, onClick = { /* Todos */ }, text = { Text("Todos") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)  // ✅ Para que ocupe el espacio disponible
            ) {
                items(listOf(
                    Triple("PED1", "Colmado Rey", 156.0),
                    Triple("PED2", "Colmado El Men", 376.0),
                    Triple("PED3", "Colmado La Esquina", 196.0)
                )) { (id, colmado, total) ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = id,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(5) { index ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (index < 4) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = colmado,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "RD$ $total",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Botones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { /* Hechas */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Hechas")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { /* Activas */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Activas")
                }
            }
        }
    }
}