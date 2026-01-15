package com.dev.mandadito.presentation.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.mandadito.data.models.*
import com.dev.mandadito.presentation.components.InternetErrorAlert
import com.dev.mandadito.presentation.viewmodels.admin.AdminFinanceViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFinanceScreen(viewModel: AdminFinanceViewModel? = null) {
    val context = LocalContext.current
    val financeViewModel = viewModel ?: remember { AdminFinanceViewModel(context) }
    val uiState by financeViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "DO")) }

    var showInternetAlert by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            if (uiState.error!!.contains("conexion", ignoreCase = true)) {
                showInternetAlert = true
            } else {
                snackbarHostState.showSnackbar(uiState.error!!)
                financeViewModel.clearError()
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            InternetErrorAlert(
                isVisible = showInternetAlert,
                onRetry = {
                    showInternetAlert = false
                    financeViewModel.loadFinanceData()
                },
                onDismiss = {
                    showInternetAlert = false
                    financeViewModel.clearError()
                },
                message = "No se pudieron cargar los datos financieros."
            )

            FinanceHeader()

            if (uiState.hasRealtimeUpdate) {
                RealtimeUpdateBanner(
                    onUpdate = { financeViewModel.refreshData() },
                    onDismiss = { financeViewModel.clearRealtimeUpdate() }
                )
            }

            FinancePeriodFilter(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = { financeViewModel.setPeriod(it) }
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        FinanceSummaryCard(
                            summary = uiState.summary,
                            currencyFormat = currencyFormat,
                            successRate = financeViewModel.successRate
                        )
                    }

                    item {
                        RevenueChartCard(
                            dailyRevenue = uiState.dailyRevenue,
                            maxRevenue = financeViewModel.maxRevenue,
                            currencyFormat = currencyFormat
                        )
                    }

                    item {
                        Text(
                            text = "Ingresos por Vendedor",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (uiState.revenueByColmado.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No hay ordenes completadas",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }

                    items(uiState.revenueByColmado.take(5)) { colmado ->
                        ColmadoRevenueCard(colmado = colmado, currencyFormat = currencyFormat)
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun FinanceHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        Text(
            text = "Gestion de Finanzas",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = "Comisiones y pagos de Stripe Connect",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun RealtimeUpdateBanner(onUpdate: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Nuevos datos disponibles",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Ignorar", fontSize = 12.sp)
                }
                FilledTonalButton(onClick = onUpdate) {
                    Text("Actualizar", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FinancePeriodFilter(selectedPeriod: DatePeriod, onPeriodSelected: (DatePeriod) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = DatePeriod.entries.indexOf(selectedPeriod),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp
    ) {
        DatePeriod.entries.forEach { period ->
            Tab(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                text = {
                    Text(
                        text = when (period) {
                            DatePeriod.TODAY -> "Hoy"
                            DatePeriod.WEEK -> "7 Dias"
                            DatePeriod.MONTH -> "30 Dias"
                            DatePeriod.YEAR -> "1 Ano"
                            DatePeriod.CUSTOM -> "Personalizado"
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun FinanceSummaryCard(summary: FinancialSummary?, currencyFormat: NumberFormat, successRate: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Resumen Financiero", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FinanceMetricItem(icon = Icons.Default.AccountBalance, value = currencyFormat.format(summary?.totalRevenue ?: 0.0), label = "Ingresos Totales", color = MaterialTheme.colorScheme.primary)
                FinanceMetricItem(icon = Icons.Default.Percent, value = currencyFormat.format(summary?.totalPlatformFees ?: 0.0), label = "Comisiones (5%)", color = MaterialTheme.colorScheme.tertiary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FinanceMetricItem(icon = Icons.Default.TrendingUp, value = currencyFormat.format(summary?.totalTransfers ?: 0.0), label = "Transferido a Vendedores", color = MaterialTheme.colorScheme.secondary)
                FinanceMetricItem(icon = Icons.Default.ShoppingCart, value = "${summary?.totalOrders ?: 0}", label = "Pedidos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatusIndicator(icon = Icons.Default.CheckCircle, count = summary?.successfulPayments ?: 0, label = "Exitosos", color = MaterialTheme.colorScheme.primary)
                StatusIndicator(icon = Icons.Default.Error, count = summary?.failedPayments ?: 0, label = "Fallidos", color = MaterialTheme.colorScheme.error)
                StatusIndicator(icon = Icons.Default.Schedule, count = summary?.pendingPayments ?: 0, label = "Pendientes", color = MaterialTheme.colorScheme.tertiary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { successRate },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Text(
                text = "Tasa de exito: ${(successRate * 100).toInt()}%",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun StatusIndicator(icon: ImageVector, count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$count", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FinanceMetricItem(icon: ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RevenueChartCard(dailyRevenue: List<DailyRevenue>, maxRevenue: Double, currencyFormat: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Tendencia de Ingresos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))

            if (dailyRevenue.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No hay datos para el periodo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    val chartData = if (dailyRevenue.size > 14) dailyRevenue.takeLast(14) else dailyRevenue
                    chartData.forEach { day ->
                        RevenueBar(revenue = day.revenue, maxRevenue = maxRevenue, date = day.date, currencyFormat = currencyFormat)
                    }
                }
            }
        }
    }
}

@Composable
fun RevenueBar(revenue: Double, maxRevenue: Double, date: String, currencyFormat: NumberFormat) {
    val heightFraction = (revenue / maxRevenue).toFloat().coerceAtLeast(0.02f)
    val dateFormatted = date.takeLast(2)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = currencyFormat.format(revenue), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(2.dp))
        Box(modifier = Modifier.width(18.dp).fillMaxHeight(heightFraction).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(MaterialTheme.colorScheme.primary))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = dateFormatted, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ColmadoRevenueCard(colmado: RevenueByColmado, currencyFormat: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = colmado.colmadoName, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "${colmado.successfulPayments} ventas exitosas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = currencyFormat.format(colmado.totalRevenue), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Comision: ${currencyFormat.format(colmado.platformFees)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun AmountRow(label: String, amount: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
