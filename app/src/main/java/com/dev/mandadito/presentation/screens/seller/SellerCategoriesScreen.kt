package com.dev.mandadito.presentation.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.mandadito.data.models.Category
import com.dev.mandadito.presentation.components.connectivity.CacheBadge
import com.dev.mandadito.presentation.components.connectivity.GlobalConnectivityBar
import com.dev.mandadito.presentation.screens.seller.components.AddCategoryDialog
import com.dev.mandadito.presentation.screens.seller.components.CategoryCard
import com.dev.mandadito.presentation.screens.seller.components.EditCategoryDialog
import com.dev.mandadito.presentation.viewmodels.common.UiState
import com.dev.mandadito.presentation.viewmodels.seller.CategoryViewModel
import com.dev.mandadito.presentation.screens.seller.components.SkeletonCategoryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerCategoriesScreen(
    viewModel: CategoryViewModel? = null
) {
    val context = LocalContext.current
    val categoryViewModel = viewModel ?: remember { CategoryViewModel(context) }
    val uiState by categoryViewModel.uiState.collectAsStateWithLifecycle()
    val filteredCategories = categoryViewModel.filteredCategories

    // Solo mostrar skeleton después de 500ms para evitar "flash" en cargas rápidas
    var showSkeleton by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.categoriesState, filteredCategories.isEmpty()) {
        if (uiState.categoriesState is UiState.Loading && filteredCategories.isEmpty()) {
            kotlinx.coroutines.delay(500)
            showSkeleton = true
        } else {
            showSkeleton = false
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Category?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Category?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            categoryViewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            Column {
                // Barra de conectividad global
                GlobalConnectivityBar(isConnected = uiState.isConnected)

                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Categorías",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${filteredCategories.size} categorías",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
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
                    contentDescription = "Agregar Categoría",
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
                        onValueChange = { categoryViewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Buscar categorías...",
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
                                IconButton(onClick = { categoryViewModel.setSearchQuery("") }) {
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
                            onClick = { categoryViewModel.setShowActiveOnly(!uiState.showActiveOnly) },
                            label = {
                                Text(
                                    "Solo activas",
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

                        // Contador de resultados
                        if (uiState.searchQuery.isNotEmpty() || uiState.showActiveOnly) {
                            Text(
                                text = "${filteredCategories.size} resultados",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            // Contenido
            when (val state = uiState.categoriesState) {
                is UiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize())
                }

                is UiState.Offline -> {
                    // Mostrar datos en caché con indicador de offline
                    if (state.cachedData != null && state.cachedData.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = state.cachedData,
                                key = { it.id }
                            ) { category ->
                                CategoryCard(
                                    category = category,
                                    onEdit = { showEditDialog = category },
                                    onDelete = { showDeleteConfirm = category },
                                    onToggleActive = {
                                        categoryViewModel.updateCategory(
                                            categoryId = category.id,
                                            name = category.name,
                                            description = category.description,
                                            icon = category.icon,
                                            color = category.color,
                                            isActive = !category.isActive
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(state.message)
                        }
                    }
                }

                is UiState.Loading -> {
                    if (showSkeleton) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(5) {
                                SkeletonCategoryCard()
                            }
                        }
                    } else {
                        // Mientras espera mostrar skeleton (primeros 500ms), no mostrar nada
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }

                is UiState.Success -> {
                    if (filteredCategories.isEmpty()) {
                        // Estado vacío
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
                                        Icons.Default.Category,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = if (uiState.searchQuery.isNotEmpty())
                                        "No se encontraron categorías"
                                    else
                                        "No hay categorías",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.searchQuery.isNotEmpty())
                                        "Intenta con otros términos de búsqueda"
                                    else
                                        "Crea tu primera categoría para organizar tus productos",
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
                                        Text("Crear Categoría")
                                    }
                                }
                            }
                        }
                    } else {
                        // Mostrar categorías
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = filteredCategories,
                                key = { it.id }
                            ) { category ->
                                Box {
                                    CategoryCard(
                                        category = category,
                                        onEdit = { showEditDialog = category },
                                        onDelete = { showDeleteConfirm = category },
                                        onToggleActive = {
                                            categoryViewModel.updateCategory(
                                                categoryId = category.id,
                                                name = category.name,
                                                description = category.description,
                                                icon = category.icon,
                                                color = category.color,
                                                isActive = !category.isActive
                                            )
                                        }
                                    )

                                    // Badge de caché
                                    if (state.isFromCache) {
                                        CacheBadge(
                                            isFromCache = true,
                                            cacheTimestamp = state.cacheTimestamp,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(16.dp)
                                        )
                                    }
                                }
                            }

                            // Espaciado para el FAB
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }

                is UiState.Retrying -> {
                    // Tratar como loading - sin mensaje de reintento
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(5) {
                            SkeletonCategoryCard()
                        }
                    }
                }

                is UiState.Error -> {
                    // Estado de error
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
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                modifier = Modifier.size(120.dp)
                            ) {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = "Error al cargar categorías",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.canRetry) {
                                Button(
                                    onClick = { categoryViewModel.loadCategories() },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogos
    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onCategoryAdded = { name, description, icon, color ->
                categoryViewModel.createCategory(name, description, icon, color)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { category ->
        EditCategoryDialog(
            category = category,
            onDismiss = { showEditDialog = null },
            onCategoryUpdated = { name, description, icon, color ->
                categoryViewModel.updateCategory(
                    categoryId = category.id,
                    name = name,
                    description = description,
                    icon = icon,
                    color = color
                )
                showEditDialog = null
            }
        )
    }

    showDeleteConfirm?.let { category ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            icon = {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Eliminar categoría") },
            text = {
                Text("¿Estás seguro de que deseas eliminar la categoría \"${category.name}\"? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoryViewModel.deleteCategory(category.id)
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