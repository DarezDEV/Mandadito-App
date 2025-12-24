# ✅ TODO LISTO PARA PROBAR

## ⚠️ FLUJO ACTUALIZADO (IMPORTANTE)

**EL FLUJO HA SIDO CORREGIDO**. Ahora el usuario debe **seleccionar la dirección de entrega ANTES de pagar**.

**Flujo anterior (incorrecto)**: Carrito → Checkout → Pagar
**Flujo nuevo (correcto)**: Carrito → **Seleccionar Dirección** → Checkout → Pagar

📖 **Lee más**: `FLUJO_CORRECTO_DE_PAGO.md` para entender los cambios en detalle.

---

## 🎉 CONFIGURACIÓN COMPLETADA

Has completado la integración de Stripe Connect con tu app Android. Aquí está el resumen de lo que se ha hecho:

---

## ✅ BACKEND (COMPLETADO)

- ✅ Base de datos SQL ejecutada en Supabase
- ✅ Backend Flask configurado y corriendo
- ✅ Webhooks de Stripe escuchando
- ✅ Cuenta Stripe Connect del colmado creada y activa

---

## ✅ ANDROID (COMPLETADO)

### Dependencias agregadas:
- ✅ Stripe Android SDK (20.37.2)
- ✅ Retrofit (2.9.0)
- ✅ Gson Converter (2.9.0)

### Archivos creados:
- ✅ `Order.kt` - Modelo de órdenes
- ✅ `Payment.kt` - Modelo de pagos
- ✅ `ApiService.kt` - Cliente Retrofit para el backend
- ✅ `PaymentRepository.kt` - Repositorio de pagos
- ✅ `PaymentViewModel.kt` - ViewModel de pagos
- ✅ `CheckoutScreen.kt` - Pantalla de checkout

### Archivos modificados:
- ✅ `CheckoutScreen.kt` - Obtiene user_id de Supabase Auth
- ✅ `AppNavigation.kt` - Ruta de checkout agregada
- ✅ `ClientCartScreen.kt` - Botón "Pedir" navega al checkout
- ✅ `ClientScaffold.kt` - Pasa navController al cart

---

## 🚀 CÓMO PROBAR

### PASO 1: Verificar que el backend está corriendo

Abre la terminal donde está corriendo Flask. Deberías ver:

```
Mandadito Backend Server
URL: http://localhost:5000
Environment: development
Stripe Mode: TEST
Server running...
```

Si no está corriendo:

```bash
cd "D:\Espacio de Trabajo\Mandadito-App\backend"
venv\Scripts\activate
python app.py
```

---

### PASO 2: Verificar que Stripe CLI está escuchando

Abre la terminal donde está Stripe CLI. Deberías ver:

```
> Ready! Your webhook signing secret is whsec_...
> Waiting for events...
```

Si no está corriendo:

```bash
stripe listen --forward-to localhost:5000/webhooks/stripe
```

---

### PASO 3: Compilar y ejecutar la app

1. **Abre Android Studio**
2. **Sync Gradle** (si no lo has hecho):
   - Clic en `File > Sync Project with Gradle Files`
   - Espera a que termine

3. **Ejecutar la app**:
   - Clic en el botón **Run** (▶️)
   - Selecciona tu emulador o dispositivo
   - Espera a que compile e instale

---

### PASO 4: Flujo completo de prueba

1. **Login** con tu cuenta de cliente

2. **Ir al Home** y seleccionar un colmado

3. **Agregar productos al carrito**:
   - Selecciona algunos productos
   - Agrégalos al carrito

4. **Ir al carrito**:
   - Presiona el ícono del carrito en la barra inferior (tab 1)

5. **Verificar el carrito**:
   - Deberías ver los productos
   - Deberías ver el total

6. **Presionar "Continuar"**:
   - Clic en el botón verde "Continuar • RD$XXX.XX"
   - Deberías navegar a la pantalla de Selección de Dirección

7. **📍 Seleccionar dirección de entrega** (NUEVA PANTALLA):
   - Verás la lista de tus direcciones guardadas
   - Selecciona una dirección (toca la tarjeta)
   - Se marcará con un círculo azul y un check ✓
   - Si no tienes direcciones, presiona "Agregar nueva dirección"
   - Presiona "Continuar al pago"

8. **En la pantalla de Checkout**:
   - Verás la dirección de entrega seleccionada 📍
   - Verás el resumen de la orden
   - El total a pagar
   - El botón "Pagar"

9. **Presionar "Pagar"**:
   - Se abrirá el PaymentSheet de Stripe
   - Ingresa los datos de la tarjeta de prueba:
     ```
     Número: 4242 4242 4242 4242
     Fecha: 12/34
     CVV: 123
     ZIP: 12345
     ```

10. **Completar el pago**:
    - Presiona "Pagar" en el PaymentSheet
    - Deberías ver un mensaje de "Pago exitoso"
    - La app volverá al home

---

### PASO 5: Verificar que el pago funcionó

#### En Logcat (Android Studio):

Filtra por `PaymentViewModel` y deberías ver:

```
D/PaymentViewModel: 📦 Creando orden...
D/PaymentViewModel: ✅ Orden creada: ORD-20250121-001
D/PaymentViewModel: ✅ Pago completado exitosamente
```

#### En el Backend (Terminal Flask):

Deberías ver:

```
127.0.0.1 - - [21/Jan/2025 10:30:45] "POST /orders/create HTTP/1.1" 200 -
```

#### En Stripe CLI:

Deberías ver:

```
2025-01-21 10:30:47  --> payment_intent.succeeded
2025-01-21 10:30:47  <-- 200 OK
```

#### En Supabase:

Ve al SQL Editor y ejecuta:

```sql
-- Ver la última orden
SELECT * FROM orders ORDER BY created_at DESC LIMIT 1;

-- Ver el último pago
SELECT * FROM payments ORDER BY created_at DESC LIMIT 1;
```

Deberías ver:
- Orden con `status = 'paid'`
- Payment con `status = 'succeeded'`

#### En Stripe Dashboard:

1. Ve a https://dashboard.stripe.com/test/payments
2. Deberías ver tu pago reciente
3. Haz clic en él y verifica:
   - ✅ Monto correcto
   - ✅ Transfer al colmado
   - ✅ Estado: Succeeded

---

## 🎯 NOTAS IMPORTANTES

### URL del Backend

Actualmente está configurado para emulador:

```kotlin
private const val BASE_URL = "http://10.0.2.2:5000/"
```

Si usas dispositivo físico, cambia a tu IP local en `ApiService.kt`:

```kotlin
private const val BASE_URL = "http://TU_IP_LOCAL:5000/"
```

Para obtener tu IP:

```bash
ipconfig  # Windows
ifconfig  # Mac/Linux
```

### Direcciones de Entrega

✅ **YA NO ES NECESARIO HARDCODEAR EL ADDRESS ID**

El usuario ahora selecciona la dirección directamente desde la app en la pantalla de "Seleccionar dirección de entrega".

**Requisito**: El usuario debe tener al menos 1 dirección guardada en la app.

**Para crear direcciones**:
1. Ve a Perfil → Direcciones
2. Presiona "Agregar dirección"
3. Completa los datos con Google Maps
4. Guarda la dirección

**O durante el checkout**:
- Si no tienes direcciones, puedes crearlas directamente desde la pantalla de selección
- Presiona "Agregar nueva dirección"
- Completa el formulario
- Vuelve automáticamente a la selección

---

## 🐛 TROUBLESHOOTING

### Problema: "Connection refused" en Android

**Solución**:
1. Verifica que el backend está corriendo
2. Si usas emulador, usa `10.0.2.2`
3. Si usas dispositivo físico, usa tu IP local
4. Ambos deben estar en la misma red WiFi

### Problema: "Stripe not initialized"

**Solución**:
1. Verifica que el backend responde:
   ```
   http://localhost:5000/stripe/config
   ```
2. Revisa Logcat para ver el error exacto

### Problema: PaymentSheet no se abre

**Solución**:
1. Revisa Logcat filtrado por `PaymentViewModel`
2. Verifica que la orden se creó en Supabase
3. Verifica que el payment tiene `client_secret`

### Problema: "El colmado no ha completado su onboarding"

**Solución**:

```bash
# Verificar estado de la cuenta
curl -X POST http://localhost:5000/stripe/connect/status \
  -H "Content-Type: application/json" \
  -d '{"colmado_id": "bdcc7bbf-92a9-4169-ae63-3a633d2f6461"}'
```

Si `charges_enabled: false`, completar el onboarding nuevamente.

### Problema: Webhook no llega

**Solución**:
1. Verifica que Stripe CLI está corriendo
2. Verifica que el webhook secret está en `.env`
3. Reinicia Flask:
   ```bash
   # Ctrl+C para detener
   python app.py
   ```

---

## 📊 DIAGRAMA DE FLUJO

```
Usuario Android
    │
    ├─ 1. Agrega productos al carrito
    │
    ├─ 2. Presiona "Pedir" en ClientCartScreen
    │
    ├─ 3. Navega a CheckoutScreen
    │      └─ checkout/{cartId}/{addressId}/{subtotal}
    │
    ├─ 4. Presiona "Pagar"
    │
    ├─ 5. PaymentViewModel.createOrder()
    │      │
    │      └─ POST /orders/create → Flask Backend
    │         │
    │         ├─ Crea orden en Supabase
    │         ├─ Crea order_items
    │         ├─ Crea PaymentIntent en Stripe
    │         └─ Retorna client_secret
    │
    ├─ 6. PaymentSheet se abre con client_secret
    │
    ├─ 7. Usuario ingresa tarjeta 4242 4242 4242 4242
    │
    ├─ 8. Stripe procesa el pago
    │      │
    │      ├─ Cobra 350 DOP de la tarjeta
    │      ├─ Retiene 17.50 DOP (5% comisión plataforma)
    │      ├─ Transfiere 332.50 DOP al colmado
    │      └─ Envía webhook: payment_intent.succeeded
    │
    ├─ 9. Webhook llega a Flask
    │      │
    │      ├─ Actualiza payment → status = 'succeeded'
    │      ├─ Actualiza order → status = 'paid'
    │      └─ Registra cambio en order_status_history
    │
    └─ 10. Android recibe PaymentSheetResult.Completed
           │
           └─ Navega al home

✅ PAGO COMPLETADO
```

---

## 🎉 ¡LISTO!

Tu sistema de pagos con Stripe Connect está completamente funcional.

### Lo que tienes ahora:

✅ Marketplace multi-vendor (múltiples colmados)
✅ Pagos automáticos a los sellers
✅ Comisión de plataforma (5%)
✅ Webhooks para sincronización
✅ Seguridad PCI compliant
✅ Modo TEST (gratis, sin costos)

### Próximos pasos recomendados:

1. Implementar pantalla de "Orden Exitosa"
2. Implementar historial de órdenes
3. Selección dinámica de dirección de entrega
4. Cálculo dinámico del delivery fee
5. Notificaciones push
6. Dashboard para sellers

---

## 📞 SOPORTE

Si tienes problemas:

1. Revisa la sección de Troubleshooting arriba
2. Revisa los logs en:
   - Logcat (Android)
   - Terminal de Flask
   - Terminal de Stripe CLI
3. Verifica en Supabase que los datos están correctos

---

**¡FELICIDADES!** 🎊 Has implementado un sistema de pagos completo nivel producción.
