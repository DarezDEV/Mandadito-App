package com.dev.mandadito.presentation.screens.seller.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dev.mandadito.data.models.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCard(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (category.isActive)
                MaterialTheme.colorScheme.surfaceContainer
            else
                MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (category.isActive) 2.dp else 0.dp
        )
    ) {
        Column {
            // Header Principal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ícono con color de fondo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            color = category.color?.let {
                                try {
                                    Color(android.graphics.Color.parseColor(it)).copy(alpha = 0.2f)
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            } ?: MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (category.icon != null) {
                        Text(
                            text = category.icon,
                            fontSize = 32.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Category,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Información
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Badge de estado
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (category.isActive)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = if (category.isActive) "Activa" else "Inactiva",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (category.isActive)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Botón expandir
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Contraer" else "Expandir",
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                }
            }

            // Descripción y acciones (expandible)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Descripción
                    if (!category.description.isNullOrBlank()) {
                        Text(
                            text = category.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    } else {
                        Text(
                            text = "Sin descripción",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    // Botones de acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Activar/Desactivar
                        OutlinedButton(
                            onClick = onToggleActive,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (category.isActive)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (category.isActive) "Desactivar" else "Activar")
                        }

                        // Editar
                        FilledTonalButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Editar")
                        }

                        // Eliminar
                        IconButton(
                            onClick = onDelete,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar"
                            )
                        }
                    }
                }
            }
        }
    }
}