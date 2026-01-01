package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dev.mandadito.data.models.Colmado
import com.dev.mandadito.data.models.ColmadoWithOwner
import com.dev.mandadito.presentation.viewmodels.client.ClientHomeViewModel
import com.dev.mandadito.presentation.components.skeleton.SkeletonStoreCard
import com.dev.mandadito.presentation.viewmodels.common.UiState
import com.dev.mandadito.presentation.components.connectivity.GlobalConnectivityBar
import com.dev.mandadito.presentation.components.connectivity.CacheBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientHomeScreen(
    navController: NavController,
    onStoreSelected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { ClientHomeViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlobalConnectivityBar(isConnected = uiState.isConnected)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Banner Hero
                HeroBanner()

                Spacer(modifier = Modifier.height(20.dp))

                // Barra de búsqueda
                SearchBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Contenido principal
                when (val state = uiState.colmadosState) {
                    is UiState.Idle -> LoadingState()
                    is UiState.Loading -> LoadingState()
                    is UiState.Success -> SuccessState(
                        filteredColmados = viewModel.filteredColmados,
                        searchQuery = uiState.searchQuery,
                        isFromCache = state.isFromCache,
                        cacheTimestamp = state.cacheTimestamp,
                        onStoreSelected = onStoreSelected
                    )
                    is UiState.Retrying -> LoadingState()
                    is UiState.Error -> ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadColmados() }
                    )
                    is UiState.Offline -> OfflineState(
                        cachedData = state.cachedData,
                        searchQuery = uiState.searchQuery,
                        message = state.message,
                        onStoreSelected = onStoreSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C49C0)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "¡Bienvenido!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Encuentra colmados cercanos y haz tu pedido",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Buscar colmados...",
                    color = Color(0xFF75777F)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Color(0xFF1C49C0)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Limpiar",
                            tint = Color(0xFF75777F)
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color(0xFF1C49C0)
            ),
            singleLine = true
        )
    }
}

@Composable
private fun LoadingState() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) {
            SkeletonStoreCard()
        }
    }
}

@Composable
private fun SuccessState(
    filteredColmados: List<ColmadoWithOwner>,
    searchQuery: String,
    isFromCache: Boolean,
    cacheTimestamp: Long?,
    onStoreSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isFromCache && cacheTimestamp != null) {
            item {
                CacheBadge(
                    isFromCache = isFromCache,
                    cacheTimestamp = cacheTimestamp
                )
            }
        }

        if (filteredColmados.isEmpty()) {
            item {
                EmptySearchResults(searchQuery)
            }
        } else {
            items(
                items = filteredColmados,
                key = { it.id }
            ) { colmadoWithOwner ->
                StoreCard(
                    colmadoWithOwner = colmadoWithOwner,
                    onClick = { onStoreSelected(colmadoWithOwner.id) }
                )
            }
        }
    }
}


@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFDAD6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xFFBA1A1A)
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF44464F),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C49C0)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reintentar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OfflineState(
    cachedData: List<ColmadoWithOwner>?,
    searchQuery: String,
    message: String,
    onStoreSelected: (String) -> Unit
) {
    if (cachedData != null && cachedData.isNotEmpty()) {
        val filteredColmados = cachedData.filter { colmadoWithOwner ->
            val query = searchQuery.lowercase()
            if (query.isBlank()) {
                true
            } else {
                colmadoWithOwner.name.lowercase().contains(query) ||
                        colmadoWithOwner.address.lowercase().contains(query) ||
                        colmadoWithOwner.description?.lowercase()?.contains(query) == true
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CacheBadge(
                    isFromCache = true,
                    cacheTimestamp = System.currentTimeMillis()
                )
            }

            if (filteredColmados.isEmpty()) {
                item {
                    EmptySearchResults(searchQuery)
                }
            } else {
                items(
                    items = filteredColmados,
                    key = { it.id }
                ) { colmadoWithOwner ->
                    StoreCard(
                        colmadoWithOwner = colmadoWithOwner,
                        onClick = { onStoreSelected(colmadoWithOwner.id) }
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFDAD6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color(0xFFBA1A1A)
                    )
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF44464F),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EmptySearchResults(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF75777F)
            )
            Text(
                text = if (searchQuery.isNotEmpty())
                    "No se encontraron colmados"
                else
                    "No hay colmados disponibles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1B1F),
                textAlign = TextAlign.Center
            )
            if (searchQuery.isNotEmpty()) {
                Text(
                    text = "Intenta con otra búsqueda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF75777F),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StoreCard(
    colmadoWithOwner: ColmadoWithOwner,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono del colmado
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFD8E2FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF1C49C0)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info del colmado
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = colmadoWithOwner.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1B1F)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF5558A3)
                    )
                    Text(
                        text = colmadoWithOwner.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF44464F),
                        maxLines = 1
                    )
                }

                if (colmadoWithOwner.description != null) {
                    Text(
                        text = colmadoWithOwner.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF75777F),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Badges y rating
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFD8E2FF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF1C49C0)
                        )
                        Text(
                            text = colmadoWithOwner.phone.takeLast(4),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C49C0)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "4.5",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1B1F)
                    )
                }
            }
        }
    }
}