package com.dev.mandadito.presentation.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dev.mandadito.R
import com.dev.mandadito.data.models.Role
import com.dev.mandadito.presentation.viewmodels.auth.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Estados de validación en tiempo real
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    // Función para validar email en tiempo real
    fun validateEmail() {
        if (email.isNotBlank()) {
            val result = authViewModel.isValidEmail(email)
            emailError = if (result is AuthViewModel.ValidationResult.Error) result.message else null
        } else {
            emailError = null
        }
    }
    
    // Función para validar password en tiempo real
    fun validatePassword() {
        if (password.isNotBlank()) {
            if (password.length < 8) {
            } else {
                passwordError = null
            }
        } else {
            passwordError = null
        }
    }

    // Navegar según el rol cuando el login sea exitoso
    // Solo navegar si el usuario acabó de hacer login (no por sesión previa)
    var hasJustLoggedIn by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.isLoggedIn, uiState.userRole) {
        if (uiState.isLoggedIn && uiState.userRole != null && hasJustLoggedIn) {
            val destination = when (uiState.userRole) {
                Role.CLIENT -> "client_home"
                Role.SELLER -> "seller_home"
                Role.DELIVERY -> "delivery_home"
                Role.ADMIN -> "admin_home"
                null -> return@LaunchedEffect
            }

            navController.navigate(destination) {
                popUpTo("welcome") { inclusive = true }
            }
            authViewModel.clearSuccess()
            hasJustLoggedIn = false // Resetear flag después de navegar
        }
    }
    
    // Detectar cuando el usuario hace click en el botón de login
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) {
            hasJustLoggedIn = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo y título
            Surface(
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_mandadito),
                    contentDescription = "Logo de la app",
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Bienvenido de nuevo",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Inicia sesión para continuar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Campos de login
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        validateEmail()
                    },
                    label = { Text("Correo electrónico") },
                    placeholder = { Text("ejemplo@correo.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = if (emailError != null) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (emailError != null) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    isError = emailError != null,
                    supportingText = {
                        if (emailError != null) {
                            Text(
                                text = emailError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (emailError != null) MaterialTheme.colorScheme.error 
                                            else MaterialTheme.colorScheme.primary,
                        focusedLabelColor = if (emailError != null) MaterialTheme.colorScheme.error 
                                           else MaterialTheme.colorScheme.primary,
                        focusedLeadingIconColor = if (emailError != null) MaterialTheme.colorScheme.error 
                                                  else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (emailError != null) MaterialTheme.colorScheme.error 
                                              else MaterialTheme.colorScheme.outline
                    )
                )

                // Contraseña
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        validatePassword()
                    },
                    label = { Text("Contraseña") },
                    placeholder = { Text("Ingresa tu contraseña") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (passwordError != null) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (passwordError != null) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = if (passwordError != null) MaterialTheme.colorScheme.error 
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = {
                        if (passwordError != null) {
                            Text(
                                text = passwordError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (passwordError != null) MaterialTheme.colorScheme.error 
                                            else MaterialTheme.colorScheme.primary,
                        focusedLabelColor = if (passwordError != null) MaterialTheme.colorScheme.error 
                                           else MaterialTheme.colorScheme.primary,
                        focusedLeadingIconColor = if (passwordError != null) MaterialTheme.colorScheme.error 
                                                  else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (passwordError != null) MaterialTheme.colorScheme.error 
                                              else MaterialTheme.colorScheme.outline
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Botón de inicio de sesión con loading
            val isFormValid = email.isNotBlank() && password.isNotBlank() && 
                            emailError == null && passwordError == null
            
            Button(
                onClick = {
                    authViewModel.login(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !uiState.isLoading && isFormValid
            ) {
                if (uiState.isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Iniciando sesión...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mostrar errores
            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enlace de registro
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                Text(
                    text = "¿No tienes una cuenta? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Regístrate",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        navController.navigate("register")
                    }
                )
            }
        }

        // Botón de volver
        IconButton(
            onClick = {
                navController.navigate("welcome")
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 50.dp)
                .size(35.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}