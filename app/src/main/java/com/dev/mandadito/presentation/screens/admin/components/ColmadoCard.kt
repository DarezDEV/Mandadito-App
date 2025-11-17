package com.dev.mandadito.presentation.screens.admin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.ColmadoWithOwner

@Composable
fun ColmadoCard(
    colmado: ColmadoWithOwner,
    isInactiveSection: Boolean,
    onDeactivate: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    val isInactive = !colmado.isActive

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isInactive) 0.7f else 1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInactive)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInactive) 0.dp else 2.dp),
        border = if (isInactive) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono del colmado
            ColmadoIcon(isInactive = isInactive)

            // Información del colmado
            ColmadoInfo(
                colmado = colmado,
                isInactive = isInactive,
                modifier = Modifier.weight(1f)
            )

            // Acciones
            ColmadoActions(
                isInactiveSection = isInactiveSection,
                onDeactivate = onDeactivate,
                onActivate = onActivate,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun ColmadoIcon(isInactive: Boolean) {
    Box {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (isInactive)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = "Colmado",
                    tint = if (isInactive)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Indicador de estado
        if (isInactive) {
            Surface(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF9E9E9E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Inactivo",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = Color(0xFF4CAF50),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
            ) {}
        }
    }
}

@Composable
private fun ColmadoInfo(
    colmado: ColmadoWithOwner,
    isInactive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Nombre del colmado
        Text(
            text = colmado.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isInactive)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Dirección
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isInactive)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Text(
                text = colmado.address,
                style = MaterialTheme.typography.bodySmall,
                color = if (isInactive)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Teléfono
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Phone,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isInactive)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Text(
                text = colmado.phone,
                style = MaterialTheme.typography.bodySmall,
                color = if (isInactive)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Información del dueño y usuarios
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Badge del dueño
            if (colmado.ownerName != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = colmado.ownerName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Contador de usuarios
            if ((colmado.totalUsers ?: 0) > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "${colmado.totalUsers}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColmadoActions(
    isInactiveSection: Boolean,
    onDeactivate: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    if (isInactiveSection) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón activar
            IconButton(
                onClick = onActivate,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Activar",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Botón eliminar
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        // Botón desactivar
        IconButton(
            onClick = onDeactivate,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = "Desactivar",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}