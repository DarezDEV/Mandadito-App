package com.dev.mandadito.presentation.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.mandadito.data.models.ProductWithCategories
import com.dev.mandadito.presentation.screens.seller.components.AddProductDialog
import com.dev.mandadito.presentation.screens.seller.components.EditProductDialog
import com.dev.mandadito.presentation.screens.seller.components.ProductCard
import com.dev.mandadito.presentation.screens.seller.components.SkeletonProductCard
import com.dev.mandadito.presentation.viewmodels.seller.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProductsScreen(
    viewModel: ProductViewModel? = null
) {
    val context = LocalContext.current
    val productViewModel = viewModel ?: remember { ProductViewModel(context) }
    val uiState by productViewModel.uiState.collectAsStateWithLifecycle()
    val filteredProducts = productViewModel.filteredProducts
    val showSkeleton = uiState.isLoading && filteredProducts.isEmpty()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<ProductWithCategories?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<ProductWithCategories?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            productViewModel.clearSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            productViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Productos",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${filteredProducts.size} productos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar producto",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Barra de búsqueda y filtros
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Búsqueda
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { productViewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Buscar productos...",
                                style = MaterialTheme.typography.bodyLarge
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
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { productViewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    // Filtros
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FilterChip(
                            selected = uiState.showActiveOnly,
                            onClick = { productViewModel.setShowActiveOnly(!uiState.showActiveOnly) },
                            label = {
                                Text(
                                    "Solo activos",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            leadingIcon = if (uiState.showActiveOnly) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        // Filtro de categoría
                        if (uiState.categories.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                FilterChip(
                                    selected = uiState.selectedCategoryFilter != null,
                                    onClick = { expanded = true },
                                    label = {
                                        Text(
                                            uiState.selectedCategoryFilter?.let { categoryId ->
                                                uiState.categories.find { it.id == categoryId }?.let {
                                                    it.icon?.let { icon -> "$icon ${it.name}" } ?: it.name
                                                } ?: "Categoría"
                                            } ?: "Todas",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Todas las categorías") },
                                        onClick = {
                                            productViewModel.setCategoryFilter(null)
                                            expanded = false
                                        }
                                    )
                                    uiState.categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    category.icon?.let { "$it ${category.name}" } ?: category.name
                                                )
                                            },
                                            onClick = {
                                                productViewModel.setCategoryFilter(category.id)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Contador de resultados
                        if (uiState.searchQuery.isNotEmpty() || uiState.showActiveOnly || uiState.selectedCategoryFilter != null) {
                            Text(
                                text = "${filteredProducts.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            // Contenido
            when {
                showSkeleton -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(5) {
                            SkeletonProductCard()
                        }
                    }
                }

                filteredProducts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.size(120.dp)
                            ) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty())
                                    "No se encontraron productos"
                                else
                                    "No hay productos",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty())
                                    "Intenta con otros términos de búsqueda"
                                else
                                    "Agrega tu primer producto para comenzar a vender",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.searchQuery.isEmpty()) {
                                Button(
                                    onClick = { showAddDialog = true },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Crear Producto")
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredProducts,
                            key = { it.id }
                        ) { product ->
                            ProductCard(
                                product = product,
                                onEdit = { showEditDialog = product },
                                onDelete = { showDeleteConfirm = product },
                                onToggleActive = {
                                    productViewModel.updateProduct(
                                        productId = product.id,
                                        name = product.name,
                                        description = product.description,
                                        price = product.price,
                                        stock = product.stock,
                                        newImageUris = emptyList(),
                                        existingImageUrls = product.allImageUrls,
                                        categoryIds = product.categories.map { it.id },
                                        isActive = !product.isActive
                                    )
                                }
                            )
                        }

                        // Espaciado para el FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Diálogos
    if (showAddDialog) {
        AddProductDialog(
            categories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onProductAdded = { name, description, price, stock, imageUris, categoryIds ->
                productViewModel.createProduct(name, description, price, stock, imageUris, categoryIds)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { product ->
        EditProductDialog(
            product = product,
            categories = uiState.categories,
            onDismiss = { showEditDialog = null },
            onProductUpdated = { name, description, price, stock, newImageUris, existingImageUrls, categoryIds ->
                productViewModel.updateProduct(
                    productId = product.id,
                    name = name,
                    description = description,
                    price = price,
                    stock = stock,
                    newImageUris = newImageUris,
                    existingImageUrls = existingImageUrls,
                    categoryIds = categoryIds
                )
                showEditDialog = null
            }
        )
    }

    showDeleteConfirm?.let { product ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            icon = {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Eliminar producto") },
            text = {
                Text("¿Estás seguro de que deseas eliminar el producto \"${product.name}\"? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        productViewModel.deleteProduct(product.id)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}