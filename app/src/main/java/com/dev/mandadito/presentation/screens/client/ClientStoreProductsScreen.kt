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
import com.dev.mandadito.presentation.components.skeleton.SkeletonProductCard
import com.dev.mandadito.presentation.viewmodels.client.ClientStoreProductsUiState
import com.dev.mandadito.presentation.components.connectivity.GlobalConnectivityBar
import com.dev.mandadito.presentation.components.connectivity.CacheBadge
import com.dev.mandadito.presentation.viewmodels.common.UiState


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

    // Inicializar ViewModel con el colmadoId
    LaunchedEffect(colmadoId) {
        viewModel.initialize(colmadoId)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Solo mostrar skeleton después de 500ms para evitar "flash" en cargas rápidas
    var showSkeleton by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.productsState) {
        if (uiState.productsState is UiState.Loading) {
            kotlinx.coroutines.delay(500)
            showSkeleton = true
        } else {
            showSkeleton = false
        }
    }

    // Mostrar mensajes de éxito
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            Column {
                // NUEVO: Barra de conectividad global
                GlobalConnectivityBar(isConnected = uiState.isConnected)

                TopAppBar(
                    title = { Text("Productos", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
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
                val categoriesState = uiState.categoriesState
                val categories = if (categoriesState is UiState.Success) categoriesState.data else emptyList()
                if (categories.isNotEmpty()) {
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

                        items(categories) { categoria ->
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

                // Estados de carga y error con el nuevo UiState
                when (val state = uiState.productsState) {
                    is UiState.Loading -> {
                        if (showSkeleton) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(6) {
                                    SkeletonProductCard()
                                }
                            }
                        }
                    }

                    is UiState.Error -> {
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
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 16.sp
                                )
                                if (state.canRetry) {
                                    Button(onClick = { viewModel.loadProducts() }) {
                                        Text("Reintentar")
                                    }
                                }
                            }
                        }
                    }

                    is UiState.Success -> {
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
                                            .clickable { onProductSelected(producto.id) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                    ) {
                                        Column {
                                            // Imagen
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(160.dp)
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(producto.primaryImage ?: producto.imageUrl)
                                                        .crossfade(300)
                                                        .memoryCacheKey(producto.primaryImage ?: producto.imageUrl)
                                                        .diskCacheKey(producto.primaryImage ?: producto.imageUrl)
                                                        .build(),
                                                    contentDescription = producto.name,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentScale = ContentScale.Crop
                                                )

                                                // NUEVO: Badge de caché
                                                if (state.isFromCache) {
                                                    CacheBadge(
                                                        isFromCache = state.isFromCache,
                                                        cacheTimestamp = state.cacheTimestamp,
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(8.dp)
                                                    )
                                                }

                                                // Badge de stock bajo
                                                if (producto.stock < 5) {
                                                    Surface(
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .padding(8.dp),
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = MaterialTheme.colorScheme.errorContainer
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Warning,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(12.dp),
                                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                                            )
                                                            Text(
                                                                text = "¡${producto.stock} left!",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onErrorContainer
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Contenido
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Nombre
                                                Text(
                                                    text = producto.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    lineHeight = 18.sp
                                                )

                                                // Descripción
                                                producto.description?.let { desc ->
                                                    Text(
                                                        text = desc,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        lineHeight = 14.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                // Precio y botón
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Bottom
                                                ) {
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = "RD$${String.format("%.2f", producto.price)}",
                                                            fontSize = 20.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Inventory,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(12.dp),
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = "${producto.stock} disponibles",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    FilledTonalIconButton(
                                                        onClick = {
                                                            viewModel.addToCart(producto.id)
                                                        },
                                                        modifier = Modifier.size(40.dp),
                                                        enabled = producto.stock > 0,
                                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.primary
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ShoppingCart,
                                                            contentDescription = "Agregar al carrito",
                                                            modifier = Modifier.size(22.dp)
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

                    else -> {
                        // Estados Idle u otros
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}