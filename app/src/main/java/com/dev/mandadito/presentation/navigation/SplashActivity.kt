package com.dev.mandadito.presentation.navigation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.dev.mandadito.data.network.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dev.mandadito.MainActivity
import com.dev.mandadito.R
import com.dev.mandadito.config.AppConfig
import com.dev.mandadito.ui.theme.MandaditoTheme

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar pantalla completa
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        setContent {
            MandaditoTheme {
                SplashScreen()
            }
        }

        // Verificar sesión existente y navegar apropiadamente
        lifecycleScope.launch {
            delay(AppConfig.SPLASH_DURATION)
            try {
                val authRepository = AuthRepository(this@SplashActivity)
                
                if (authRepository.hasActiveSession()) {
                    // Usuario ya está logueado, navegar directamente a MainActivity
                    val session = authRepository.getCurrentSession()
                    android.util.Log.d("SplashActivity", "Sesión encontrada: ${session?.email} con rol: ${session?.role}")
                    
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // No hay sesión, navegar a MainActivity (que mostrará la pantalla de bienvenida)
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                // Manejar errores de navegación
                android.util.Log.e("SplashActivity", "Error en navegación: ${e.message}", e)
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = AppConfig.ANIMATION_DURATION.toInt())
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
        ) {
            // Logo con animación
            Image(
                painter = painterResource(id = R.drawable.logo_mandadito),
                contentDescription = "Logo de Mandadito",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(180.dp)
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Texto de la marca
            Text(
                text = "Mandadito",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Tu delivery de confianza",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Indicador de carga
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}
