# 📱 Configuración de Android - Paso a Paso

## ✅ PASO 1: AGREGAR DEPENDENCIAS DE STRIPE

### 1.1 Abrir build.gradle.kts

Abre el archivo:
```
app/build.gradle.kts
```

### 1.2 Buscar la sección dependencies

Busca donde dice `dependencies {`

### 1.3 Agregar estas líneas

Agrega ANTES del último `}`:

```kotlin
dependencies {
    // ... tus dependencias existentes (Supabase, Compose, etc.)

    // ===== AGREGAR ESTAS LÍNEAS =====

    // Stripe Android SDK
    implementation("com.stripe:stripe-android:20.37.2")

    // Retrofit (para llamadas HTTP al backend)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

### 1.4 Sincronizar Gradle

1. Haz clic en **"Sync Now"** que aparece arriba
2. Espera a que descargue las dependencias (1-2 minutos)
3. Verás "BUILD SUCCESSFUL" en la consola

✅ **Dependencias instaladas!**

---

## ✅ PASO 2: CONFIGURAR LA URL DEL BACKEND

Ya creamos el archivo `ApiService.kt`, pero necesitas actualizar la URL.

### 2.1 Abrir ApiService.kt

```
app/src/main/java/com/dev/mandadito/data/network/ApiService.kt
```

### 2.2 Buscar la línea de BASE_URL

Busca esta sección (está cerca de la línea 60):

```kotlin
companion object {
    private const val BASE_URL = "http://10.0.2.2:5000/"  // <-- ESTA LÍNEA
```

### 2.3 Elegir la URL correcta

**¿Vas a probar en un emulador o dispositivo físico?**

**OPCIÓN A: Emulador Android (AVD)**
```kotlin
private const val BASE_URL = "http://10.0.2.2:5000/"
```
✅ **Usa esta si corres la app en el emulador de Android Studio**

**OPCIÓN B: Dispositivo físico (celular real)**

Primero, obtén la IP de tu computadora:

```bash
# Windows (PowerShell)
ipconfig

# Busca "IPv4 Address" en "Wireless LAN adapter Wi-Fi"
# Ejemplo: 192.168.1.10

# Mac/Linux
ifconfig

# Busca "inet" en "en0" o "wlan0"
```

Luego usa esa IP:

```kotlin
private const val BASE_URL = "http://192.168.1.10:5000/"  // <-- Reemplaza con tu IP
```

⚠️ **IMPORTANTE**: Tu celular y tu PC deben estar en la MISMA red WiFi.

### 2.4 Guardar el archivo

`Ctrl + S`

---

## ✅ PASO 3: AGREGAR RUTA DE CHECKOUT EN NAVEGACIÓN

### 3.1 Abrir AppNavigation.kt

```
app/src/main/java/com/dev/mandadito/presentation/navigation/AppNavigation.kt
```

### 3.2 Buscar donde están las rutas de cliente

Busca la sección donde defines las rutas, probablemente después de `ClientScaffold`.

### 3.3 Agregar la ruta de checkout

Agrega esta ruta ANTES del último `}` del `NavHost`:

```kotlin
// Ruta de Checkout (FUERA del ClientScaffold)
composable(
    route = "checkout/{cartId}/{addressId}/{subtotal}",
    arguments = listOf(
        navArgument("cartId") { type = NavType.StringType },
        navArgument("addressId") { type = NavType.StringType },
        navArgument("subtotal") { type = NavType.FloatType }
    )
) { backStackEntry ->
    val context = LocalContext.current

    CheckoutScreen(
        cartId = backStackEntry.arguments?.getString("cartId") ?: "",
        addressId = backStackEntry.arguments?.getString("addressId") ?: "",
        subtotal = backStackEntry.arguments?.getFloat("subtotal")?.toDouble() ?: 0.0,
        onPaymentSuccess = { orderId ->
            // Navegar a pantalla de éxito
            navController.navigate("order_success/$orderId") {
                popUpTo("client_home") { inclusive = false }
            }
        },
        onBack = {
            navController.popBackStack()
        }
    )
}
```

---

## ✅ PASO 4: AGREGAR BOTÓN "PAGAR" EN EL CARRITO

### 4.1 Abrir ClientCartScreen.kt

```
app/src/main/java/com/dev/mandadito/presentation/screens/client/ClientCartScreen.kt
```

### 4.2 Buscar el footer del carrito

Busca donde está el botón de totales o el final de la pantalla.

### 4.3 Agregar botón de Checkout

Agrega este botón en el footer (después del total):

```kotlin
// Al final de la columna del resumen
Button(
    onClick = {
        // Obtener el primer carrito (o el activo)
        val cart = (uiState.cartsState as? UiState.Success)?.data?.firstOrNull()

        if (cart != null) {
            // TODO: Obtener addressId del usuario
            // Por ahora usa una dirección de prueba
            val addressId = "tu_address_id_aqui"  // <-- Reemplazar con dirección real

            navController.navigate(
                "checkout/${cart.id}/$addressId/${cart.summary.subtotal}"
            )
        }
    },
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
    enabled = totalItems > 0,
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
    )
) {
    Icon(
        imageVector = Icons.Default.CreditCard,
        contentDescription = null
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
        text = "Proceder al pago",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}
```

### 4.4 Obtener una dirección real

**Opción A: Desde Supabase**

En SQL Editor:

```sql
-- Obtener una dirección de prueba
SELECT id FROM addresses WHERE user_id = 'TU_USER_ID' LIMIT 1;
```

Copia el `id` y reemplaza en el código.

**Opción B: Usar la dirección predeterminada del usuario**

Mejor aún, modifica el código para obtenerla dinámicamente:

```kotlin
// En lugar de hardcodear el addressId, obtenerlo del ViewModel
val defaultAddress = addressViewModel.getDefaultAddress()  // Necesitas este método

if (defaultAddress != null) {
    navController.navigate(
        "checkout/${cart.id}/${defaultAddress.id}/${cart.summary.subtotal}"
    )
} else {
    // Mostrar mensaje: "Por favor selecciona una dirección de entrega"
}
```

---

## ✅ PASO 5: ACTUALIZAR CheckoutScreen.kt

### 5.1 Obtener el User ID

El `CheckoutScreen.kt` que creamos tiene esta línea:

```kotlin
val userId = "tu_user_id_aqui"  // TODO: Obtener del auth
```

Necesitas reemplazarla con el user ID real.

### 5.2 Opción A: Obtener del AuthViewModel (si lo tienes)

```kotlin
// En CheckoutScreen
@Composable
fun CheckoutScreen(...) {
    val authViewModel: AuthViewModel = viewModel()
    val userId = authViewModel.getCurrentUserId()

    // ... resto del código
}
```

### 5.3 Opción B: Pasar como parámetro

Modifica la firma de CheckoutScreen:

```kotlin
@Composable
fun CheckoutScreen(
    userId: String,  // <-- Agregar este parámetro
    cartId: String,
    addressId: String,
    subtotal: Double,
    // ... resto
)
```

Y pásalo desde la navegación:

```kotlin
composable("checkout/{cartId}/{addressId}/{subtotal}") { ... ->
    val userId = "obtener_del_auth"  // Tu lógica de auth

    CheckoutScreen(
        userId = userId,  // <-- Pasar aquí
        cartId = ...,
        // ...
    )
}
```

### 5.4 Opción C: Hardcodear temporalmente para testing

```kotlin
// SOLO PARA TESTING
val userId = "tu_user_id_real_de_supabase"
```

Para obtener tu user ID:

```sql
-- En Supabase SQL Editor
SELECT id FROM auth.users LIMIT 1;
```

---

## ✅ PASO 6: PROBAR LA APP

### 6.1 Asegúrate que el backend está corriendo

En la terminal del backend deberías ver:

```
Server running...
```

Si no, ejecuta:

```bash
cd backend
venv\Scripts\activate  # Windows
python app.py
```

### 6.2 Asegúrate que Stripe CLI está escuchando

En otra terminal:

```bash
stripe listen --forward-to localhost:5000/webhooks/stripe
```

### 6.3 Compilar y ejecutar la app

1. En Android Studio, haz clic en el botón **Run** (▶️)
2. Selecciona tu emulador o dispositivo
3. Espera a que compile e instale

### 6.4 Flujo de prueba

1. **Login** con tu usuario
2. **Agregar productos** al carrito
3. Ir al **carrito**
4. Presionar **"Proceder al pago"**
5. Deberías ver la pantalla de Checkout
6. Presionar el botón **"Pagar"**
7. Se abrirá el **PaymentSheet de Stripe**

### 6.5 Usar tarjeta de prueba

En el PaymentSheet, ingresa:

```
Número de tarjeta: 4242 4242 4242 4242
Fecha de expiración: 12/34
CVV: 123
Código postal: 12345
```

### 6.6 Completar el pago

1. Presiona **"Pagar"**
2. Deberías ver "✅ ¡Pago exitoso!"
3. La app navegará a la pantalla de éxito

---

## ✅ PASO 7: VERIFICAR QUE TODO FUNCIONÓ

### 7.1 Verificar en Logcat (Android Studio)

En Android Studio:

1. Ve a **Logcat** (View → Tool Windows → Logcat)
2. Filtra por `PaymentViewModel`
3. Deberías ver:

```
D/PaymentViewModel: 📦 Creando orden...
D/PaymentViewModel: ✅ Orden creada: ORD-20250121-001
D/PaymentViewModel: ✅ Pago completado exitosamente
```

### 7.2 Verificar en el Backend

En la terminal del Flask deberías ver:

```
127.0.0.1 - - [21/Jan/2025 10:30:45] "POST /orders/create HTTP/1.1" 200 -
127.0.0.1 - - [21/Jan/2025 10:30:46] "GET /stripe/config HTTP/1.1" 200 -
```

### 7.3 Verificar en Stripe CLI

En la terminal de Stripe CLI:

```
2025-01-21 10:30:47  --> payment_intent.succeeded
2025-01-21 10:30:47  <-- 200 OK
```

### 7.4 Verificar en Supabase

En SQL Editor:

```sql
-- Ver la orden creada
SELECT * FROM orders ORDER BY created_at DESC LIMIT 1;

-- Verificar que el estado es 'paid'
SELECT order_number, status, total FROM orders
ORDER BY created_at DESC LIMIT 1;

-- Ver el pago
SELECT status, amount, card_brand, card_last4 FROM payments
ORDER BY created_at DESC LIMIT 1;
```

Deberías ver:

```
order_number: ORD-20250121-001
status: paid
total: 350.50

payment status: succeeded
card_brand: visa
card_last4: 4242
```

### 7.5 Verificar en Stripe Dashboard

1. Ve a https://dashboard.stripe.com/test/payments
2. Deberías ver el pago reciente
3. Haz clic en él
4. Verás:
   - Monto: 350.50 DOP
   - Estado: Succeeded
   - Transfer: 315.45 DOP al colmado
   - Fee: 35.05 DOP (tu comisión)

---

## ✅ EXTRAS: PANTALLA DE ÉXITO

### Crear OrderSuccessScreen.kt

```kotlin
package com.dev.mandadito.presentation.screens.client

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OrderSuccessScreen(
    orderId: String,
    onNavigateHome: () -> Unit,
    onViewOrder: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡Pago Exitoso!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tu pedido está siendo preparado",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onViewOrder(orderId) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ver detalles del pedido")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onNavigateHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver al inicio")
        }
    }
}
```

### Agregar ruta en AppNavigation.kt

```kotlin
composable(
    route = "order_success/{orderId}",
    arguments = listOf(
        navArgument("orderId") { type = NavType.StringType }
    )
) { backStackEntry ->
    val orderId = backStackEntry.arguments?.getString("orderId") ?: ""

    OrderSuccessScreen(
        orderId = orderId,
        onNavigateHome = {
            navController.navigate("client_home") {
                popUpTo("client_home") { inclusive = true }
            }
        },
        onViewOrder = { id ->
            navController.navigate("order_details/$id")
        }
    )
}
```

---

## 🐛 TROUBLESHOOTING

### Problema: "Connection refused" en Android

**Causa**: La URL del backend está mal configurada.

**Solución**:

1. Si usas **emulador**: Usa `http://10.0.2.2:5000/`
2. Si usas **dispositivo físico**:
   - Obtén la IP de tu PC: `ipconfig` (Windows) o `ifconfig` (Mac/Linux)
   - Usa `http://TU_IP:5000/`
   - Asegúrate de estar en la misma red WiFi

### Problema: "Stripe not initialized"

**Causa**: El backend no está corriendo o la URL está mal.

**Solución**:

1. Verifica que Flask esté corriendo:
   ```bash
   python app.py
   ```

2. Prueba el endpoint desde tu navegador:
   ```
   http://localhost:5000/stripe/config
   ```

3. Revisa Logcat para ver el error exacto

### Problema: "El colmado no ha completado su onboarding"

**Causa**: La cuenta Stripe del colmado no tiene `charges_enabled: true`.

**Solución**:

```bash
curl -X POST http://localhost:5000/stripe/connect/status \
  -H "Content-Type: application/json" \
  -d '{"colmado_id": "TU_COLMADO_ID"}'
```

Si `charges_enabled: false`, completa el onboarding nuevamente.

### Problema: PaymentSheet no se abre

**Causa**: El `client_secret` no llega correctamente.

**Solución**:

1. Revisa Logcat:
   ```
   tag:PaymentViewModel
   ```

2. Verifica que la orden se creó:
   ```sql
   SELECT * FROM orders ORDER BY created_at DESC LIMIT 1;
   ```

3. Verifica que el payment tiene client_secret:
   ```sql
   SELECT client_secret FROM payments ORDER BY created_at DESC LIMIT 1;
   ```

---

## 🎉 ¡LISTO!

Ahora tienes:

✅ Android configurado con Stripe
✅ Pantalla de Checkout funcional
✅ PaymentSheet integrado
✅ Flujo completo de pago funcionando

¿Qué sigue?

- Agregar pantalla de historial de órdenes
- Notificaciones push cuando cambia el estado
- Mejorar el manejo de errores
- Agregar loading states
