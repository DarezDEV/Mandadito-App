package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dev.mandadito.R
import com.dev.mandadito.presentation.viewmodels.client.ClientStoreProductsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientStoreProductsScreen(
    colmadoId: String,
    navController: NavController,
    onProductSelected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { ClientStoreProductsViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Productos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Barra de búsqueda
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar productos...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Limpiar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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

                // Categorías
                if (uiState.categories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botón "Todas"
                        item {
                            FilterChip(
                                selected = uiState.selectedCategoryId == null,
                                onClick = { viewModel.setSelectedCategory(null) },
                                label = { Text("Todas") },
                                leadingIcon = {
                                    if (uiState.selectedCategoryId == null) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }

                        items(uiState.categories) { categoria ->
                            FilterChip(
                                selected = uiState.selectedCategoryId == categoria.id,
                                onClick = {
                                    viewModel.setSelectedCategory(
                                        if (uiState.selectedCategoryId == categoria.id) null else categoria.id
                                    )
                                },
                                label = { Text(categoria.name) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (uiState.selectedCategoryId == categoria.id)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.primaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Category,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (uiState.selectedCategoryId == categoria.id)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Estados de carga y error
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = uiState.error ?: "Error desconocido",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 16.sp
                                )
                                Button(onClick = { viewModel.loadProducts() }) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                    else -> {
                        val filteredProducts = viewModel.filteredProducts

                        // Productos en grid
                        if (filteredProducts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "No hay productos disponibles",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                gridItems(filteredProducts) { producto ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(8.dp, RoundedCornerShape(20.dp))
                                            .clickable { onProductSelected(producto.id) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Box {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(producto.primaryImage ?: producto.imageUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = producto.name,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(160.dp)
                                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                                                contentScale = ContentScale.Crop,
                                                placeholder = painterResource(R.drawable.ic_launcher_foreground),
                                                error = painterResource(R.drawable.ic_launcher_foreground)
                                            )

                                            // Badge de stock bajo
                                            if (producto.stock < 5) {
                                                Surface(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.errorContainer
                                                ) {
                                                    Text(
                                                        text = "Pocas unidades",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = producto.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            producto.description?.let { desc ->
                                                Text(
                                                    text = desc,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "$${producto.price}",
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "Stock: ${producto.stock}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { onProductSelected(producto.id) },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Agregar",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
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
        }
    }
}