package com.dev.mandadito.presentation.screens.client

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dev.mandadito.data.models.Address
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.presentation.viewmodels.client.PaymentStatus
import com.dev.mandadito.presentation.viewmodels.client.PaymentViewModel
import com.stripe.android.paymentsheet.rememberPaymentSheet
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartId: String,
    addressId: String,
    deliveryFee: Double = 50.0,
    subtotal: Double,
    onPaymentSuccess: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: PaymentViewModel = viewModel { PaymentViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    val userId = remember { SupabaseClient.client.auth.currentUserOrNull()?.id ?: "" }

    var deliveryAddress by remember { mutableStateOf<Address?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(addressId) {
        withContext(Dispatchers.IO) {
            try {
                deliveryAddress = SupabaseClient.client.from("addresses")
                    .select {
                        filter {
                            eq("id", addressId)
                        }
                    }
                    .decodeSingle<Address>()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val paymentSheet = rememberPaymentSheet { result ->
        viewModel.handlePaymentResult(result, userId, cartId)
    }

    val total = subtotal + deliveryFee

    LaunchedEffect(uiState.isReadyForPayment) {
        if (uiState.isReadyForPayment && uiState.clientSecret != null) {
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = uiState.clientSecret!!,
                configuration = viewModel.getPaymentSheetConfiguration()
            )
        }
    }

    // SnackbarHostState para mostrar notificaciones
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.paymentStatus) {
        if (uiState.paymentStatus == PaymentStatus.SUCCESS) {
            showSuccessDialog = true
        }
    }

    Scaffold(
        snackbarHost = {
            com.dev.mandadito.presentation.components.CustomSnackbarHost(
                hostState = snackbarHostState
            )
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Checkout",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Delivery Address Section
                deliveryAddress?.let { address ->
                    DeliveryAddressCard(address = address)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Order Summary Section
                OrderSummaryCard(
                    subtotal = subtotal,
                    deliveryFee = deliveryFee,
                    total = total
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Security Badge
                SecurityBadgeSection()

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Payment Button
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shadowElevation = 12.dp,
                color = Color.White
            ) {
                PaymentActionButton(
                    total = total,
                    isLoading = uiState.isLoading,
                    isEnabled = !uiState.isLoading && uiState.isStripeInitialized,
                    onClick = {
                        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                        userId?.let {
                            viewModel.createOrder(
                                userId = it,
                                cartId = cartId,
                                addressId = addressId,
                                deliveryFee = deliveryFee,
                                customerNotes = null
                            )
                        }
                    }
                )
            }
        }
    }

    // Manejar notificaciones de error y éxito
    com.dev.mandadito.presentation.components.SnackbarHandler(
        snackbarHostState = snackbarHostState,
        errorMessage = uiState.errorMessage,
        successMessage = null, // No mostramos success porque usamos el dialog
        onErrorDismiss = { viewModel.clearError() },
        onSuccessDismiss = { }
    )

    if (showSuccessDialog) {
        SuccessPaymentDialog(
            orderNumber = uiState.orderResponse?.orderNumber ?: "",
            onDismiss = {
                showSuccessDialog = false
                viewModel.resetPaymentState()
                uiState.orderResponse?.orderId?.let { orderId ->
                    onPaymentSuccess(orderId)
                }
            }
        )
    }
}

@Composable
private fun DeliveryAddressCard(address: Address) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD8E2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color(0xFF1C49C0)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Dirección de entrega",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1B1F)
                )
            }

            HorizontalDivider(
                color = Color(0xFFE1E2EC),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = address.formattedAddress,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1B1F),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (!address.city.isNullOrBlank()) {
                Text(
                    text = address.city,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF44464F)
                )
            }

            if (!address.addressExtra.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF5558A3)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = address.addressExtra,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF44464F)
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(
    subtotal: Double,
    deliveryFee: Double,
    total: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD8E2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color(0xFF1C49C0)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Resumen del pedido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1B1F)
                )
            }

            HorizontalDivider(
                color = Color(0xFFE1E2EC),
                thickness = 1.dp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Subtotal
            SummaryItemRow(
                label = "Subtotal",
                value = subtotal,
                isHighlighted = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Delivery
            SummaryItemRow(
                label = "Costo de delivery",
                value = deliveryFee,
                isHighlighted = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                color = Color(0xFFE1E2EC),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total a pagar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1B1F)
                )
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$${String.format("%.2f", total)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C49C0)
                    )
                    Text(
                        text = "DOP",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF5558A3)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryItemRow(
    label: String,
    value: Double,
    isHighlighted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF44464F)
        )
        Text(
            text = "$${String.format("%.2f", value)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isHighlighted) Color(0xFF1C49C0) else Color(0xFF1A1B1F)
        )
    }
}

@Composable
private fun SecurityBadgeSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE1E0FF)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF5558A3)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Pago seguro con Stripe",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF11135C)
                )
                Text(
                    text = "Tus datos están protegidos y encriptados",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5558A3)
                )
            }
        }
    }
}

@Composable
private fun StatusMessageCard(
    message: String,
    isError: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                Color(0xFFFFDAD6)
            } else {
                Color(0xFFD8E2FF)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Outlined.Error else Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (isError) Color(0xFFBA1A1A) else Color(0xFF1C49C0)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) Color(0xFF410002) else Color(0xFF001A41)
            )
        }
    }
}

@Composable
private fun PaymentActionButton(
    total: Double,
    isLoading: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
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
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "button_content"
            ) { loading ->
                if (loading) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Procesando pago...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Pagar $${String.format("%.2f", total)} DOP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessPaymentDialog(
    orderNumber: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(50.dp)
                )
            }
        },
        title = {
            Text(
                text = "¡Pago exitoso!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF1A1B1F),
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tu pedido ha sido confirmado y está en proceso",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF44464F)
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD8E2FF)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Número de pedido",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF5558A3)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "#$orderNumber",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C49C0)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF5558A3)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recibirás notificaciones del estado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF44464F)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C49C0)
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Text(
                    text = "Ver mi pedido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}