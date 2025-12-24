package com.dev.mandadito.presentation.screens.client

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dev.mandadito.data.models.Address
import com.dev.mandadito.data.network.SupabaseClient
import com.dev.mandadito.presentation.viewmodels.client.PaymentStatus
import com.dev.mandadito.presentation.viewmodels.client.PaymentViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

/**
 * Pantalla de Checkout con integración de Stripe
 *
 * @param cartId ID del carrito
 * @param addressId ID de la dirección de entrega
 * @param deliveryFee Costo de delivery
 * @param subtotal Subtotal del carrito
 * @param onPaymentSuccess Callback cuando el pago es exitoso
 * @param onBack Callback para volver atrás
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartId: String,
    addressId: String,
    deliveryFee: Double = 50.0,
    subtotal: Double,
    onPaymentSuccess: (String) -> Unit,  // Recibe order_id
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: PaymentViewModel = viewModel { PaymentViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()

    // Cargar dirección seleccionada
    var deliveryAddress by remember { mutableStateOf<Address?>(null) }

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
                // Error cargando dirección
                e.printStackTrace()
            }
        }
    }

    // Payment Sheet de Stripe
    val paymentSheet = rememberPaymentSheet { result ->
        viewModel.handlePaymentResult(result)
    }

    // Total a pagar
    val total = subtotal + deliveryFee

    // Efecto para mostrar PaymentSheet cuando esté listo
    LaunchedEffect(uiState.isReadyForPayment) {
        if (uiState.isReadyForPayment && uiState.clientSecret != null) {
            paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret = uiState.clientSecret!!,
                configuration = viewModel.getPaymentSheetConfiguration()
            )
        }
    }

    // Efecto para manejar pago exitoso
    LaunchedEffect(uiState.paymentStatus) {
        if (uiState.paymentStatus == PaymentStatus.SUCCESS) {
            uiState.orderResponse?.orderId?.let { orderId ->
                onPaymentSuccess(orderId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =====================================================
            // DIRECCIÓN DE ENTREGA
            // =====================================================
            deliveryAddress?.let { address ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dirección de entrega",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Divider()

                        Text(
                            text = address.formattedAddress,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        if (!address.city.isNullOrBlank()) {
                            Text(
                                text = address.city,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (!address.addressExtra.isNullOrBlank()) {
                            Text(
                                text = "Referencia: ${address.addressExtra}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // =====================================================
            // RESUMEN DE LA ORDEN
            // =====================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Resumen de la orden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Divider()

                    // Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal")
                        Text(
                            text = "$${String.format("%.2f", subtotal)}",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Delivery
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Costo de delivery")
                        Text(
                            text = "$${String.format("%.2f", deliveryFee)}",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Divider()

                    // Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$${String.format("%.2f", total)} DOP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // =====================================================
            // INFORMACIÓN DE SEGURIDAD
            // =====================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Column {
                        Text(
                            text = "Pago seguro con Stripe",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Tus datos están protegidos y encriptados",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // =====================================================
            // MENSAJES DE ERROR/ÉXITO
            // =====================================================
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            uiState.successMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // =====================================================
            // BOTÓN DE PAGAR
            // =====================================================
            Button(
                onClick = {
                    // Obtener user_id de Supabase Auth
                    val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                        ?: run {
                            // Si no hay usuario autenticado, mostrar error
                            return@Button
                        }

                    viewModel.createOrder(
                        userId = userId,
                        cartId = cartId,
                        addressId = addressId,
                        deliveryFee = deliveryFee,
                        customerNotes = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading && uiState.isStripeInitialized,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Procesando...")
                } else {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pagar $${String.format("%.2f", total)} DOP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mensaje de Stripe no inicializado
            if (!uiState.isStripeInitialized) {
                Text(
                    text = "Inicializando sistema de pagos...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
