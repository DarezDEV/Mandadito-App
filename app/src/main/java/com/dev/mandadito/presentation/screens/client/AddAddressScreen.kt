package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.mandadito.data.models.UiState
import com.dev.mandadito.presentation.viewmodels.client.AddressViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressScreen(
    viewModel: AddressViewModel,
    addressId: String? = null,
    onNavigateBack: (String?) -> Unit  // Ahora recibe el ID de la dirección creada
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditMode = addressId != null

    LaunchedEffect(addressId) {
        if (addressId != null) {
            viewModel.loadAddressForEditing(addressId)
        }
    }

    LaunchedEffect(saveState) {
        when (saveState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Dirección guardada exitosamente",
                    duration = SnackbarDuration.Short
                )
                val createdAddressId = (saveState as UiState.Success).data.id
                viewModel.resetSaveState()
                onNavigateBack(createdAddressId) // Pasar el ID de la dirección creada
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (saveState as UiState.Error).message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Dirección actualizada exitosamente",
                    duration = SnackbarDuration.Short
                )
                val updatedAddressId = (updateState as UiState.Success).data.id
                viewModel.resetUpdateState()
                onNavigateBack(updatedAddressId) // Pasar el ID de la dirección actualizada
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (updateState as UiState.Error).message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Editar Dirección" else "Nueva Dirección",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack(null) }) { // Pasar null al cancelar
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            // Sección: Información de Contacto
            item {
                SectionHeader(
                    icon = Icons.Default.Person,
                    title = "Información de Contacto"
                )
            }

            item {
                ModernTextField(
                    value = formState.firstName,
                    onValueChange = { viewModel.updateFirstName(it) },
                    label = "Nombre",
                    placeholder = "Ej: Juan",
                    isRequired = true,
                    leadingIcon = Icons.Default.Person
                )
            }

            item {
                ModernTextField(
                    value = formState.lastName,
                    onValueChange = { viewModel.updateLastName(it) },
                    label = "Apellido",
                    placeholder = "Ej: Pérez",
                    isRequired = true,
                    leadingIcon = Icons.Default.Person
                )
            }

            item {
                ModernTextField(
                    value = formState.phone,
                    onValueChange = { viewModel.updatePhone(it) },
                    label = "Teléfono",
                    placeholder = "8091234567",
                    isRequired = true,
                    leadingIcon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone
                )
            }

            // Switch de modo manual
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Ingreso manual",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Sin usar Google Maps",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = formState.isManualMode,
                            onCheckedChange = { viewModel.toggleManualMode(it) }
                        )
                    }
                }
            }

            // Búsqueda de dirección (si no está en modo manual)
            if (!formState.isManualMode) {
                item {
                    SectionHeader(
                        icon = Icons.Default.Search,
                        title = "Buscar Dirección"
                    )
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.searchPlaces(it)
                        },
                        label = { Text("Escribe tu dirección") },
                        placeholder = { Text("Ej: Av. Abraham Lincoln") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    viewModel.clearSearch()
                                }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Limpiar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }

                // Resultados de búsqueda
                if (searchResults.isNotEmpty()) {
                    items(searchResults) { prediction ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectPlace(prediction.placeId)
                                    searchQuery = prediction.fullText
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prediction.primaryText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = prediction.secondaryText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Dirección seleccionada
                if (formState.selectedPlaceDetails != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Dirección seleccionada",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formState.selectedPlaceDetails!!.formattedAddress,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Detalles de la dirección
            item {
                SectionHeader(
                    icon = Icons.Default.Home,
                    title = "Detalles de la Dirección"
                )
            }

            // Campo de calle (solo en modo manual)
            if (formState.isManualMode) {
                item {
                    ModernTextField(
                        value = formState.street,
                        onValueChange = { viewModel.updateStreet(it) },
                        label = "Calle y número",
                        placeholder = "Ej: Av. Principal 123",
                        isRequired = true,
                        leadingIcon = Icons.Default.Home
                    )
                }
            }

            item {
                ModernTextField(
                    value = formState.addressExtra,
                    onValueChange = { viewModel.updateAddressExtra(it) },
                    label = "Depto / Piso / Edificio",
                    placeholder = "Opcional",
                    isRequired = false,
                    leadingIcon = Icons.Default.Build
                )
            }

            // Ciudad (solo en modo manual)
            if (formState.isManualMode) {
                item {
                    ModernTextField(
                        value = formState.city,
                        onValueChange = { viewModel.updateCity(it) },
                        label = "Ciudad / Estado",
                        placeholder = "Ej: Santo Domingo",
                        isRequired = true,
                        leadingIcon = Icons.Default.LocationCity
                    )
                }
            }

            item {
                ModernTextField(
                    value = formState.postalCode,
                    onValueChange = { viewModel.updatePostalCode(it) },
                    label = "Código Postal",
                    placeholder = "Opcional",
                    isRequired = false,
                    leadingIcon = Icons.Default.Info,
                    keyboardType = KeyboardType.Number
                )
            }

            // Botón de guardar
            item {
                val isLoading = saveState is UiState.Loading || updateState is UiState.Loading
                Button(
                    onClick = {
                        if (isEditMode) {
                            viewModel.updateAddress()
                        } else {
                            viewModel.saveAddress()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && formState.isValid,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditMode) "Actualizar Dirección" else "Guardar Dirección",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isRequired: Boolean,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(if (isRequired) label else label)
        },
        placeholder = {
            Text(placeholder)
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )
}