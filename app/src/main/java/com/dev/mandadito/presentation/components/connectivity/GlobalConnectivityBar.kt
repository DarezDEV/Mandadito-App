package com.dev.mandadito.presentation.components.connectivity

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Barra global de conectividad
 * Se muestra/oculta automáticamente según el estado de red
 */
@Composable
fun GlobalConnectivityBar(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    var wasDisconnected by remember { mutableStateOf(false) }

    // Mostrar cuando pierde conexión
    // Ocultar automáticamente después de 3 segundos cuando recupera conexión
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            visible = true
            wasDisconnected = true
        } else if (wasDisconnected) {
            // Mostrar "Conectado" por 3 segundos y luego ocultar
            visible = true
            delay(3000)
            visible = false
            wasDisconnected = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isConnected) {
                Color(0xFF4CAF50) // Verde
            } else {
                MaterialTheme.colorScheme.error // Rojo
            },
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = if (isConnected) {
                        "Conexión restaurada"
                    } else {
                        "Sin conexión a internet"
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Badge "Datos en caché" para mostrar en cards
 */
@Composable
fun CacheBadge(
    isFromCache: Boolean,
    cacheTimestamp: Long?,
    modifier: Modifier = Modifier
) {
    if (isFromCache && cacheTimestamp != null) {
        val timeAgo = remember(cacheTimestamp) {
            val diffMinutes = (System.currentTimeMillis() - cacheTimestamp) / 1000 / 60
            when {
                diffMinutes < 1 -> "ahora"
                diffMinutes < 60 -> "${diffMinutes}m"
                else -> "${diffMinutes / 60}h"
            }
        }

        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Caché · $timeAgo",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
