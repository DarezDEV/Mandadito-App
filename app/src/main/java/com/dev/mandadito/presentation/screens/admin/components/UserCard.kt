package com.dev.mandadito.presentation.screens.admin.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dev.mandadito.R
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCard(
    usuario: UserProfile,
    isDisabledSection: Boolean,
    onEdit: () -> Unit,
    onDisable: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    val isDisabled = !usuario.activo

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabled)
                MaterialTheme.colorScheme.surfaceContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDisabled) 0.dp else 2.dp
        )
    ) {
        Column {
            // === HEADER PRINCIPAL ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                    .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                // Avatar con Border Animado
                Box(
                    modifier = Modifier.size(64.dp)
                ) {
                    // Border gradient para usuarios activos
                    if (!isDisabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
        if (usuario.avatar_url != null) {
            AsyncImage(
                model = usuario.avatar_url,
                                contentDescription = "Avatar de ${usuario.nombre}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.profile_default),
                contentDescription = "Foto de perfil por defecto",
                                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
                        }
        }

                    // Badge de estado superpuesto
            Surface(
                modifier = Modifier
                            .size(20.dp)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                        color = if (isDisabled)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        border = BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        )
            ) {
                        Box(contentAlignment = Alignment.Center) {
                    Icon(
                                imageVector = if (isDisabled)
                                    Icons.Default.Cancel
                                else
                                    Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
    }
}

                // Información del Usuario
    Column(
                    modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = usuario.nombre,
            style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
            maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

        Row(
            verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = usuario.email,
                style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

                    // Chip de Estado y Rol
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chip de Estado
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDisabled)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDisabled)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                )
                                Text(
                                    text = if (isDisabled) "Inactivo" else "Activo",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDisabled)
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Badge de rol
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = getUserRoleColor(usuario.role).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = getUserRoleLabel(usuario.role),
                                style = MaterialTheme.typography.labelSmall,
                                color = getUserRoleColor(usuario.role),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Botón Expandir con animación
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = if (expanded) 2.dp else 0.dp
        ) {
            IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Contraer" else "Expandir",
                            modifier = Modifier.rotate(rotationAngle)
                        )
                    }
                }
            }

            // === SECCIÓN EXPANDIBLE ===
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                )
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Info Text
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
            ) {
                Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Puedes editar la información del usuario, cambiar su estado o eliminarlo del sistema.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                            )
                        }

                        // Acciones Principales
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Editar
                            OutlinedButton(
                                onClick = onEdit,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(
                                    1.5.dp,
                                    MaterialTheme.colorScheme.primary
                                )
            ) {
                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Editar", fontWeight = FontWeight.Medium)
                            }

                            // Activar/Desactivar
                            FilledTonalButton(
                                onClick = if (isDisabled) onActivate else onDisable,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isDisabled)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer,
                                    contentColor = if (isDisabled)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
            ) {
                Icon(
                                    imageVector = if (isDisabled)
                                        Icons.Outlined.PersonAddAlt
                                    else
                                        Icons.Outlined.PersonOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (isDisabled) "Activar" else "Pausar",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Eliminar (acción destructiva separada)
                        if (isDisabledSection) {
                            OutlinedButton(
                                onClick = onDelete,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                )
            ) {
                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Eliminar Usuario",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getUserRoleColor(role: Role?): Color {
    return when (role) {
        Role.CLIENT -> Color(0xFF2196F3)
        Role.SELLER -> Color(0xFF9C27B0)
        Role.DELIVERY -> Color(0xFFFF9800)
        Role.ADMIN -> Color(0xFFE91E63)
        null -> MaterialTheme.colorScheme.outline
    }
}

private fun getUserRoleLabel(role: Role?): String {
    return when (role) {
        Role.CLIENT -> "Cliente"
        Role.SELLER -> "Colmado"
        Role.DELIVERY -> "Delivery"
        Role.ADMIN -> "Admin"
        null -> "Sin rol"
    }
}