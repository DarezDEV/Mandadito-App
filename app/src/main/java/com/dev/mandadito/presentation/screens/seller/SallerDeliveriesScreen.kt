package com.dev.mandadito.presentation.screens.seller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.mandadito.presentation.screens.seller.components.CreateDeliveryDialog
import com.dev.mandadito.presentation.screens.seller.components.DeliveryCard
import com.dev.mandadito.presentation.screens.seller.components.EditDeliveryDialog
import com.dev.mandadito.presentation.viewmodels.seller.DeliveriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDeliveriesScreen(
    viewModel: DeliveriesViewModel? = null
) {
    val context = LocalContext.current
    val deliveriesViewModel = viewModel ?: remember { DeliveriesViewModel(context) }
    val uiState by deliveriesViewModel.uiState.collectAsStateWithLifecycle()
    val filteredDeliveries = deliveriesViewModel.filteredDeliveries

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<com.dev.mandadito.data.models.DeliveryUser?>(null) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showToggleDialog by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            deliveriesViewModel.clearSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            deliveriesViewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mis Deliveries",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredDeliveries.size} en tu equipo",
                            style = MaterialTheme.typography.bodyMedium,
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
            ExtendedFloatingActionButton(
                onClick = {
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Agregar Delivery") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barra de búsqueda y filtros
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Búsqueda
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { deliveriesViewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por nombre o email") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Filtro de estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !uiState.showInactiveOnly,
                        onClick = { deliveriesViewModel.setShowInactiveOnly(false) },
                        label = { Text("Activos") },
                        leadingIcon = if (!uiState.showInactiveOnly) {
                            { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )

                    FilterChip(
                        selected = uiState.showInactiveOnly,
                        onClick = { deliveriesViewModel.setShowInactiveOnly(true) },
                        label = { Text("Inactivos") },
                        leadingIcon = if (uiState.showInactiveOnly) {
                            { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido principal
            when {
                uiState.isLoading && uiState.deliveries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Cargando deliveries...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.error != null && uiState.deliveries.isEmpty() && !uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = uiState.error ?: "Error desconocido",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                filteredDeliveries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (uiState.searchQuery.isBlank())
                                    "No hay deliveries en tu equipo"
                                else
                                    "No se encontraron deliveries",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (uiState.searchQuery.isBlank())
                                    "Agrega deliveries a tu equipo usando el botón +"
                                else
                                    "Intenta con otro término de búsqueda",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        items(filteredDeliveries, key = { it.id }) { delivery ->
                            DeliveryCard(
                                delivery = delivery,
                                onEdit = {
                                    showEditDialog = delivery
                                },
                                onToggleActive = {
                                    showToggleDialog = Pair(delivery.id, delivery.activo)
                                },
                                onRemove = {
                                    showDeleteDialog = delivery.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo: Crear delivery
    if (showAddDialog) {
        CreateDeliveryDialog(
            onDismiss = { showAddDialog = false },
            onDeliveryCreated = { email, password, nombre, avatarUri ->
                deliveriesViewModel.createDeliveryForColmado(email, password, nombre, avatarUri)
                showAddDialog = false
            }
        )
    }

    // Diálogo: Editar delivery
    showEditDialog?.let { delivery ->
        EditDeliveryDialog(
            delivery = delivery,
            onDismiss = { showEditDialog = null },
            onDeliveryUpdated = { nombre, email, avatarUri ->
                deliveriesViewModel.updateDelivery(delivery.id, nombre, email, avatarUri)
                showEditDialog = null
            }
        )
    }

    // Diálogo: Confirmar eliminación
    showDeleteDialog?.let { userId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "¿Eliminar delivery?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Este delivery será desvinculado de tu colmado. Podrás agregarlo nuevamente más tarde si lo necesitas.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deliveriesViewModel.removeDeliveryFromColmado(userId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo: Confirmar activar/desactivar
    showToggleDialog?.let { (userId, isActive) ->
        AlertDialog(
            onDismissRequest = { showToggleDialog = null },
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = if (isActive) "¿Deshabilitar delivery?" else "¿Habilitar delivery?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isActive)
                        "Este delivery no podrá realizar entregas hasta que lo habilites nuevamente."
                    else
                        "Este delivery podrá volver a realizar entregas."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isActive) {
                            deliveriesViewModel.disableDelivery(userId)
                        } else {
                            deliveriesViewModel.enableDelivery(userId)
                        }
                        showToggleDialog = null
                    },
                    colors = if (isActive) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(if (isActive) "Deshabilitar" else "Habilitar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showToggleDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}