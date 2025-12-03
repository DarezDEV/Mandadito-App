package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dev.mandadito.presentation.viewmodels.ClientHomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientHomeScreen(
    navController: NavController,
    viewModel: ClientHomeViewModel = viewModel(),
    onStoreSelected: (String) -> Unit = {}
) {
    val colmados by viewModel.colmados.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Filtrar colmados localmente
    val filteredColmados = remember(searchQuery, colmados) {
        if (searchQuery.isBlank()) {
            colmados
        } else {
            colmados.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.address.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "¡Bienvenido a Mandadito!",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Encuentra los mejores colmados cercanos y haz tu pedido en minutos.",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                // Barra de búsqueda
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Buscar colmados cercanos...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, "Limpiar")
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Contenido dinámico
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = error ?: "Error desconocido",
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(onClick = { viewModel.retry() }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }

                    filteredColmados.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank())
                                    "No hay colmados disponibles"
                                else
                                    "No se encontraron colmados",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = filteredColmados,
                                key = { it.id }
                            ) { colmado ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onStoreSelected(colmado.id) }
                                        .shadow(6.dp, RoundedCornerShape(20.dp)),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(70.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primaryContainer,
                                                            MaterialTheme.colorScheme.tertiaryContainer
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Store,
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = colmado.name,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = colmado.address,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Ver productos"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}