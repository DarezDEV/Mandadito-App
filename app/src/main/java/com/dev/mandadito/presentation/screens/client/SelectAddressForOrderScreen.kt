package com.dev.mandadito.presentation.screens.client

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dev.mandadito.data.models.Address
import com.dev.mandadito.data.models.UiState
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.presentation.viewmodels.client.AddressViewModel
import com.dev.mandadito.presentation.viewmodels.client.AddressViewModelFactory
import io.github.jan.supabase.auth.auth
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAddressForOrderScreen(
    cartId: String,
    subtotal: Double,
    onAddressSelected: (String) -> Unit,
    onAddNewAddress: () -> Unit,
    onBack: () -> Unit,
    newAddressId: String? = null
) {
    val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
    val context = LocalContext.current

    val viewModel: AddressViewModel = viewModel(
        factory = AddressViewModelFactory(context)
    )

    val addressesState by viewModel.addressesState.collectAsState()
    var selectedAddressId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(newAddressId) {
        if (newAddressId != null) {
            selectedAddressId = newAddressId
            viewModel.loadAddresses()
        }
    }

    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.loadAddresses()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Dirección de entrega",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1C49C0),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = addressesState) {
                is UiState.Idle, is UiState.Loading -> {
                    LoadingStateContent()
                }

                is UiState.Error -> {
                    ErrorStateContent(
                        message = state.message,
                        onRetry = { viewModel.loadAddresses() }
                    )
                }

                is UiState.Success -> {
                    val addresses = state.data

                    if (addresses.isEmpty()) {
                        EmptyStateContent(onAddNewAddress = onAddNewAddress)
                    } else {
                        AddressListScreen(
                            addresses = addresses,
                            selectedAddressId = selectedAddressId,
                            onAddressSelected = { selectedAddressId = it },
                            onAddNewAddress = onAddNewAddress
                        )
                    }
                }
            }

            // Bottom Action Button
            AnimatedVisibility(
                visible = addressesState is UiState.Success &&
                        (addressesState as? UiState.Success)?.data?.isNotEmpty() == true,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ContinueButton(
                    isEnabled = selectedAddressId != null,
                    onClick = {
                        selectedAddressId?.let { onAddressSelected(it) }
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingStateContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(50.dp),
                strokeWidth = 4.dp,
                color = Color(0xFF1C49C0)
            )
            Text(
                text = "Cargando direcciones...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF44464F)
            )
        }
    }
}

@Composable
private fun ErrorStateContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Error al cargar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF1A1B1F)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF44464F),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

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
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reintentar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyStateContent(
    onAddNewAddress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFFD8E2FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = Color(0xFF1C49C0)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No hay direcciones",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF1A1B1F)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Agrega una dirección de entrega para continuar con tu pedido",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF44464F),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddNewAddress,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1C49C0)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Agregar dirección",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AddressListScreen(
    addresses: List<Address>,
    selectedAddressId: String?,
    onAddressSelected: (String) -> Unit,
    onAddNewAddress: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Selecciona dónde deseas recibir tu pedido",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF44464F),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            AddNewAddressCard(onClick = onAddNewAddress)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(addresses) { address ->
            AddressSelectionCard(
                address = address,
                isSelected = selectedAddressId == address.id,
                onClick = {
                    address.id?.let { onAddressSelected(it) }
                }
            )
        }
    }
}

@Composable
private fun AddNewAddressCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            width = 2.dp,
            color = Color(0xFF1C49C0)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD8E2FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF1C49C0),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "Agregar nueva dirección",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C49C0)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressSelectionCard(
    address: Address,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                Color(0xFFD8E2FF)
            } else {
                Color.White
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF1C49C0))
        } else {
            BorderStroke(1.dp, Color(0xFFE1E2EC))
        },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Radio Button
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF1C49C0),
                    unselectedColor = Color(0xFF75777F)
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Address Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF1C49C0).copy(alpha = 0.2f)
                                else Color(0xFFE1E2EC)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected) Color(0xFF1C49C0) else Color(0xFF75777F)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = address.formattedAddress,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF001A41) else Color(0xFF1A1B1F)
                    )
                }

                if (!address.city.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationCity,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF5558A3)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = address.city,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF44464F)
                        )
                    }
                }

                if (!address.addressExtra.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF5558A3)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = address.addressExtra,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF44464F)
                        )
                    }
                }
            }

            // Check Icon
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Seleccionado",
                        tint = Color(0xFF1C49C0),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueButton(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 12.dp,
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C49C0),
                    disabledContainerColor = Color(0xFFE1E2EC)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp,
                    disabledElevation = 0.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Continuar al pago",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}