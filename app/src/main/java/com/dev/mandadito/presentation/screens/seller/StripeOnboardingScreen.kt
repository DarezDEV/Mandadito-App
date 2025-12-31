package com.dev.mandadito.presentation.screens.seller

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dev.mandadito.presentation.viewmodels.seller.StripeOnboardingViewModel
import com.dev.mandadito.utils.SharedPreferenHelper

/**
 * Pantalla de configuración de Stripe Connect
 * Esta pantalla se muestra SIEMPRE hasta que el seller complete el onboarding de Stripe
 * Incluye opción para saltar (solo para desarrollo/testing)
 */
@Composable
fun StripeOnboardingScreen(
    viewModel: StripeOnboardingViewModel,
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPrefsHelper = remember { SharedPreferenHelper(context) }

    // Estado para capturar información del negocio
    var showCreateAccountDialog by remember { mutableStateOf(false) }

    // Estado para el diálogo de confirmación de saltar
    var showSkipDialog by remember { mutableStateOf(false) }

    // Log para debug
    android.util.Log.d("StripeOnboardingScreen", "🎨 Renderizando pantalla - checkingStatus=${uiState.checkingStatus}, stripeReady=${uiState.stripeReady}, needsOnboarding=${uiState.needsOnboarding}, url=${uiState.onboardingUrl}")

    // Verificar el estado cuando la app vuelve del browser (onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Usuario regresó de completar el onboarding - verificar estado
                android.util.Log.d("StripeOnboardingScreen", "📱 ON_RESUME - Verificando estado de Stripe...")
                viewModel.checkStripeStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Si completó el onboarding, notificar
    LaunchedEffect(uiState.stripeReady) {
        if (uiState.stripeReady) {
            onOnboardingComplete()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                // Estado: Verificando el estado de Stripe
                uiState.checkingStatus -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Verificando configuración de pagos...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Estado: Stripe ya está configurado
                uiState.stripeReady -> {
                    SuccessContent()
                }

                // Estado: Necesita completar onboarding
                else -> {
                    OnboardingRequiredContent(
                        uiState = uiState,
                        onStartOnboarding = {
                            uiState.onboardingUrl?.let { url ->
                                // Abrir el link de Stripe en el browser
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        },
                        onCreateAccount = {
                            // Obtener email del usuario
                            val email = sharedPrefsHelper.getUserEmail() ?: "vendedor@mandadito.com"
                            val businessName = "Mi Colmado" // Nombre genérico por ahora

                            // Crear cuenta Stripe
                            viewModel.createStripeAccount(businessName, email)
                        },
                        onRefreshLink = {
                            viewModel.refreshOnboardingLink()
                        },
                        onSkip = {
                            showSkipDialog = true
                        }
                    )
                }
            }
        }
    }

    // Diálogo de confirmación para saltar
    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "⚠️ Advertencia",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Si saltas la configuración de Stripe:",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("• Tu colmado NO aparecerá a los clientes")
                    Text("• NO podrás recibir pedidos ni pagos")
                    Text("• NO podrás vender hasta configurar Stripe")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Esta opción es solo para desarrollo/testing.",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSkipDialog = false
                        onOnboardingComplete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Saltar de todos modos")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Contenido cuando el onboarding ya está completo
 */
@Composable
private fun SuccessContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Ícono de éxito
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(100.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "¡Configuración Completa!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Tu colmado ya está listo para recibir pagos.\nAhora los clientes pueden verte y comprarte.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

/**
 * Contenido cuando se necesita completar el onboarding
 */
@Composable
private fun OnboardingRequiredContent(
    uiState: com.dev.mandadito.presentation.viewmodels.seller.StripeOnboardingUiState,
    onStartOnboarding: () -> Unit,
    onCreateAccount: () -> Unit,
    onRefreshLink: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Ícono de advertencia
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(100.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }

        // Título
        Text(
            text = "Configuración de Pagos Requerida",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Mensaje principal
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "⚠️ Tu colmado NO aparecerá a los clientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = "Para recibir pagos y aparecer en la aplicación, necesitas configurar tu cuenta de Stripe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "El proceso toma aproximadamente 5 minutos y requiere:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RequirementItem("Tu información personal")
                    RequirementItem("Información de tu negocio")
                    RequirementItem("Cuenta bancaria para recibir pagos")
                }
            }
        }

        // Mostrar error si existe
        if (uiState.error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "⚠️ ${uiState.error}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón principal - Crear cuenta o continuar onboarding
        if (uiState.onboardingUrl == null) {
            // No hay URL de onboarding - mostrar botón para crear cuenta
            Button(
                onClick = onCreateAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.creatingAccount,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.creatingAccount) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Creando cuenta...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Crear Cuenta de Pagos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            // Ya hay URL - mostrar botón para abrir onboarding
            Button(
                onClick = onStartOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.creatingAccount,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Configurar Cuenta de Pagos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Botón secundario - Refrescar link si expiró
            if (!uiState.creatingAccount) {
                TextButton(
                    onClick = onRefreshLink,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("¿El enlace no funciona? Refrescar")
                }
            }
        }

        // Nota informativa
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "💡",
                    fontSize = 20.sp
                )
                Text(
                    text = "Stripe es el sistema de pagos más seguro del mundo. Tu información está protegida con encriptación bancaria.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    lineHeight = 18.sp
                )
            }
        }

        // Botón para saltar (solo para desarrollo)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = "Saltar configuración (solo testing)",
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Item de la lista de requerimientos
 */
@Composable
private fun RequirementItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
