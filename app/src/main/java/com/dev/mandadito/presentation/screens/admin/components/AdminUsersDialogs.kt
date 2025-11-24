package com.dev.mandadito.presentation.screens.admin.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.data.models.UserProfile

@Composable
fun AdminUsersDialogs(
    showDialog: Boolean,
    showEditDialog: UserProfile?,
    showDeleteConfirm: UserProfile?,
    onDismissAddDialog: () -> Unit,
    // ✅ ACTUALIZADO: Agregado Uri? como último parámetro
    onUserAdded: (String, String, String, String?, Role, Uri?) -> Unit,
    onDismissEditDialog: () -> Unit,
    onUserUpdated: (String, String?) -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    // Diálogo de agregar usuario
    if (showDialog) {
        AddUserDialog(
            onDismiss = onDismissAddDialog,
            onUserAdded = onUserAdded // Ya incluye Uri?
        )
    }

    // Diálogo de editar usuario
    showEditDialog?.let { user ->
        EditUserDialog(
            user = user,
            onDismiss = onDismissEditDialog,
            onUserUpdated = onUserUpdated
        )
    }

    // Confirmación de eliminar
    showDeleteConfirm?.let { user ->
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirm,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Confirmar eliminación",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Estás seguro de que deseas eliminar permanentemente a ${user.nombre}? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = onDeleteConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteConfirm) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}