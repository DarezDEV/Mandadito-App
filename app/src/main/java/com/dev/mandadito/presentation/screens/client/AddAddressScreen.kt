package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onNavigateBack: (String?) -> Unit
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

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
                onNavigateBack(createdAddressId)
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
                onNavigateBack(updatedAddressId)
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
                    IconButton(onClick = { onNavigateBack(null) }) {
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

            item {
                SectionHeader(
                    icon = Icons.Default.LocationOn,
                    title = "Dirección"
                )
            }

            item {
                ModernTextField(
                    value = formState.street,
                    onValueChange = { viewModel.updateStreet(it) },
                    label = "Calle y número",
                    placeholder = "Ej: Av. Abraham Lincoln 123",
                    isRequired = true,
                    leadingIcon = Icons.Default.Home
                )
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

            item {
                ModernTextField(
                    value = formState.city,
                    onValueChange = { viewModel.updateCity(it) },
                    label = "Ciudad",
                    placeholder = "Ej: Santo Domingo",
                    isRequired = true,
                    leadingIcon = Icons.Default.LocationCity
                )
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
            Text(if (isRequired) "$label *" else label)
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
