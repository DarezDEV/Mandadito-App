# 🧪 Guía Completa de Testing - Stripe Connect

## PASO 1: CONFIGURAR SUPABASE

### 1.1 Ejecutar el Schema SQL

```bash
# Opción 1: Desde Supabase Dashboard
1. Ve a https://supabase.com/dashboard
2. SQL Editor → New Query
3. Copia el contenido de: supabase/orders_and_payments_schema.sql
4. Ejecuta
```

### 1.2 Verificar Tablas Creadas

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name IN (
  'stripe_accounts',
  'orders',
  'order_items',
  'payments',
  'order_status_history'
);
```

---

## PASO 2: CONFIGURAR BACKEND FLASK

### 2.1 Crear Entorno Virtual

```bash
cd backend

# Windows
python -m venv venv
venv\Scripts\activate

# Linux/Mac
python3 -m venv venv
source venv/bin/activate
```

### 2.2 Instalar Dependencias

```bash
pip install -r requirements.txt
```

### 2.3 Configurar Variables de Entorno

```bash
# Copiar el archivo de ejemplo
cp .env.example .env

# Editar .env y completar:
# - SUPABASE_URL (de tu proyecto Supabase)
# - SUPABASE_SERVICE_ROLE_KEY (de Settings > API)
# - STRIPE_SECRET_KEY (de Stripe Dashboard)
# - STRIPE_PUBLISHABLE_KEY (de Stripe Dashboard)
```

### 2.4 Ejecutar el Servidor

```bash
python app.py
```

Deberías ver:

```
==================================================
🚀 Mandadito Backend Server
==================================================
📍 URL: http://localhost:5000
🔧 Environment: development
💳 Stripe Mode: TEST
==================================================

✅ Server running...
```

### 2.5 Probar Endpoints

```bash
# Health check
curl http://localhost:5000/

# Obtener config de Stripe
curl http://localhost:5000/stripe/config
```

---

## PASO 3: CONFIGURAR WEBHOOKS DE STRIPE

### 3.1 Instalar Stripe CLI

```bash
# Windows (con Scoop)
scoop install stripe

# Mac
brew install stripe/stripe-cli/stripe

# Linux
wget https://github.com/stripe/stripe-cli/releases/download/v1.19.0/stripe_1.19.0_linux_x86_64.tar.gz
tar -xvf stripe_1.19.0_linux_x86_64.tar.gz
sudo mv stripe /usr/local/bin
```

### 3.2 Login en Stripe CLI

```bash
stripe login
# Se abrirá el navegador para autenticar
```

### 3.3 Configurar Webhook Local

```bash
# En una nueva terminal (dejar corriendo)
stripe listen --forward-to localhost:5000/webhooks/stripe
```

Deberías ver algo como:

```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxx
```

**IMPORTANTE**: Copia el `whsec_xxxxx` y agrégalo a tu `.env`:

```bash
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxx
```

### 3.4 Reiniciar el Servidor Flask

```bash
# Ctrl+C para detener
python app.py
```

---

## PASO 4: CREAR CUENTA STRIPE PARA UN COLMADO (SELLER)

### 4.1 Obtener IDs de Supabase

```sql
-- Buscar un colmado de prueba
SELECT id, name, email FROM colmados LIMIT 1;
```

### 4.2 Crear Cuenta Stripe Connect

```bash
curl -X POST http://localhost:5000/stripe/connect/create \
  -H "Content-Type: application/json" \
  -d '{
    "colmado_id": "TU_COLMADO_ID_AQUI",
    "email": "seller@example.com",
    "business_name": "Mi Colmado de Prueba"
  }'
```

**Response:**

```json
{
  "success": true,
  "account_id": "acct_xxxxx",
  "onboarding_url": "https://connect.stripe.com/setup/...",
  "message": "Cuenta creada. Completar onboarding."
}
```

### 4.3 Completar Onboarding

1. Copia el `onboarding_url` del response
2. Ábrelo en tu navegador
3. Completa el formulario de Stripe con datos de prueba:
   - Email: seller@example.com
   - Nombre: Juan Pérez
   - Teléfono: 829-555-0100
   - **País**: República Dominicana
   - Número de cuenta: Usa los datos de prueba de Stripe
4. Envía el formulario

### 4.4 Verificar Estado de la Cuenta

```bash
curl -X POST http://localhost:5000/stripe/connect/status \
  -H "Content-Type: application/json" \
  -d '{
    "colmado_id": "TU_COLMADO_ID_AQUI"
  }'
```

**Response (debe decir `charges_enabled: true`):**

```json
{
  "success": true,
  "account_id": "acct_xxxxx",
  "onboarding_completed": true,
  "charges_enabled": true,
  "payouts_enabled": true
}
```

✅ **Tu colmado ya puede recibir pagos!**

---

## PASO 5: CONFIGURAR ANDROID

### 5.1 Actualizar build.gradle.kts

```kotlin
dependencies {
    // Stripe
    implementation("com.stripe:stripe-android:20.37.2")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
```

### 5.2 Sincronizar Gradle

```bash
# En Android Studio
File > Sync Project with Gradle Files
```

### 5.3 Actualizar URL del Backend

Edita `ApiService.kt`:

```kotlin
companion object {
    // Emulador Android (localhost del PC)
    private const val BASE_URL = "http://10.0.2.2:5000/"

    // Dispositivo físico (IP de tu PC en la red local)
    // private const val BASE_URL = "http://192.168.1.10:5000/"

    // Producción
    // private const val BASE_URL = "https://tu-dominio.com/"
}
```

**¿Cómo obtener tu IP local?**

```bash
# Windows
ipconfig

# Mac/Linux
ifconfig
```

Busca la dirección IPv4 (ej: 192.168.1.10)

---

## PASO 6: PROBAR EL FLUJO COMPLETO

### 6.1 Crear un Carrito en Android

1. Abre la app
2. Selecciona productos de un colmado
3. Agrégalos al carrito
4. Ve al carrito

### 6.2 Ir a Checkout

```kotlin
// En tu composable del carrito
Button(onClick = {
    navController.navigate("checkout/$cartId/$addressId/$subtotal")
}) {
    Text("Proceder al pago")
}
```

### 6.3 Agregar Ruta de Checkout

En `AppNavigation.kt`:

```kotlin
composable(
    route = "checkout/{cartId}/{addressId}/{subtotal}",
    arguments = listOf(
        navArgument("cartId") { type = NavType.StringType },
        navArgument("addressId") { type = NavType.StringType },
        navArgument("subtotal") { type = NavType.FloatType }
    )
) { backStackEntry ->
    CheckoutScreen(
        cartId = backStackEntry.arguments?.getString("cartId") ?: "",
        addressId = backStackEntry.arguments?.getString("addressId") ?: "",
        subtotal = backStackEntry.arguments?.getFloat("subtotal")?.toDouble() ?: 0.0,
        onPaymentSuccess = { orderId ->
            navController.navigate("order_success/$orderId") {
                popUpTo("client_home") { inclusive = false }
            }
        },
        onBack = { navController.popBackStack() }
    )
}
```

### 6.4 Presionar "Pagar"

1. Se abrirá el PaymentSheet de Stripe
2. Usa una tarjeta de prueba:

**Tarjetas de Prueba de Stripe:**

```
✅ Tarjeta exitosa:
   Número: 4242 4242 4242 4242
   Fecha: 12/34
   CVV: 123
   ZIP: 12345

❌ Tarjeta que falla:
   Número: 4000 0000 0000 0002

🔒 Requiere 3D Secure:
   Número: 4000 0027 6000 3184
```

### 6.5 Verificar en los Logs

**Terminal de Flask:**

```
📨 Webhook recibido: payment_intent.succeeded
✅ Pago exitoso: pi_xxxxx
✅ Orden ORD-20250121-001 marcada como pagada
```

**Terminal de Stripe CLI:**

```
2025-01-21 10:30:45   --> payment_intent.succeeded
2025-01-21 10:30:45   <-- 200 OK
```

**Logcat de Android:**

```
D/PaymentViewModel: ✅ Pago completado exitosamente
```

---

## PASO 7: VERIFICAR EN LAS BASES DE DATOS

### 7.1 Verificar en Supabase

```sql
-- Ver la orden creada
SELECT * FROM orders ORDER BY created_at DESC LIMIT 1;

-- Ver el pago
SELECT * FROM payments ORDER BY created_at DESC LIMIT 1;

-- Ver items de la orden
SELECT * FROM order_items WHERE order_id = 'tu_order_id';

-- Ver historial de cambios
SELECT * FROM order_status_history ORDER BY created_at DESC LIMIT 5;
```

### 7.2 Verificar en Stripe Dashboard

1. Ve a https://dashboard.stripe.com/test/payments
2. Deberías ver el pago reciente
3. Verifica que el **monto** sea correcto
4. Verifica que la **transferencia** al seller aparezca

---

## PASO 8: PROBAR CASOS DE ERROR

### 8.1 Tarjeta Declinada

```
Número: 4000 0000 0000 0002
```

Deberías ver:

```
❌ Pago falló: Your card was declined
```

### 8.2 Seller sin Onboarding Completo

1. Crea una cuenta Stripe sin completar el onboarding
2. Intenta crear una orden
3. Deberías ver: "El colmado no ha completado su onboarding de Stripe"

### 8.3 Carrito Vacío

1. Intenta crear una orden con un carrito vacío
2. Deberías ver: "El carrito está vacío"

---

## PASO 9: MONITOREO Y LOGS

### 9.1 Logs de Flask

```bash
python app.py
```

### 9.2 Logs de Webhooks

```bash
stripe listen --forward-to localhost:5000/webhooks/stripe
```

### 9.3 Logs de Android (Logcat)

En Android Studio:

```
View > Tool Windows > Logcat
```

Filtra por:

```
tag:PaymentViewModel OR tag:PaymentRepository OR tag:StripeService
```

---

## 📊 VERIFICACIÓN FINAL

Checklist de que todo funciona:

- [ ] Base de datos de Supabase tiene las tablas nuevas
- [ ] Backend Flask corre en http://localhost:5000
- [ ] Stripe CLI escucha webhooks
- [ ] Cuenta Stripe Connect del colmado tiene `charges_enabled: true`
- [ ] Android puede crear órdenes
- [ ] PaymentSheet se abre correctamente
- [ ] Pagos exitosos se registran en Supabase
- [ ] Webhooks actualizan el estado de la orden
- [ ] El dinero aparece en la cuenta del seller en Stripe Dashboard

---

## 🎯 PRÓXIMOS PASOS

1. **Producción**:
   - Cambiar a claves de Stripe en modo LIVE
   - Configurar webhooks en producción (no usar CLI)
   - Agregar manejo de errores más robusto

2. **Features adicionales**:
   - Historial de órdenes en Android
   - Notificaciones push cuando cambia el estado
   - Reembolsos desde la app
   - Dashboard para sellers

3. **Seguridad**:
   - Autenticación con JWT
   - Rate limiting en el backend
   - Validación de montos en el servidor

---

## ❓ TROUBLESHOOTING

### Problema: "Connection refused" en Android

**Solución**:

```kotlin
// Si usas emulador, usa:
private const val BASE_URL = "http://10.0.2.2:5000/"

// Si usas dispositivo físico, usa la IP de tu PC:
private const val BASE_URL = "http://192.168.1.10:5000/"
```

### Problema: Webhooks no llegan

**Solución**:

```bash
# 1. Verificar que Stripe CLI está corriendo
stripe listen --forward-to localhost:5000/webhooks/stripe

# 2. Verificar que el webhook secret está en .env
echo $STRIPE_WEBHOOK_SECRET

# 3. Reiniciar Flask
python app.py
```

### Problema: "Stripe not initialized"

**Solución**:

1. Verifica que el backend está corriendo
2. Verifica que la URL en `ApiService.kt` es correcta
3. Revisa Logcat para ver el error exacto

### Problema: Orden se crea pero el pago falla

**Solución**:

```sql
-- Verificar estado de la cuenta Stripe del colmado
SELECT * FROM stripe_accounts WHERE colmado_id = 'tu_colmado_id';

-- Debe tener charges_enabled = true
```

Si es `false`, completar el onboarding del seller.

---

## 📚 RECURSOS

- [Stripe Testing Cards](https://stripe.com/docs/testing)
- [Stripe Connect Docs](https://stripe.com/docs/connect)
- [Stripe Android SDK](https://stripe.com/docs/mobile/android)
- [Supabase Docs](https://supabase.com/docs)

---

¡Listo! 🎉 Ahora tienes un sistema de pagos completo con Stripe Connect.
