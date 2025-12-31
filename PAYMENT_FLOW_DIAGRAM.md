# 🔄 Diagrama de Flujo Completo - Sistema de Pagos

## FLUJO COMPLETO: Cliente → Pago → Colmado

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           ANDROID APP (CLIENTE)                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    1. Usuario presiona "Pagar"
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PaymentViewModel.createOrder()                                          │
│  - userId: "user_123"                                                    │
│  - cartId: "cart_456"                                                    │
│  - addressId: "addr_789"                                                 │
│  - deliveryFee: 50.0                                                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    2. POST /orders/create
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        FLASK BACKEND                                     │
│                                                                           │
│  OrderService.create_order_from_cart()                                   │
│  ├── 1. Obtener carrito de Supabase                                     │
│  │   SELECT * FROM view_cart_summary WHERE cart_id = ?                  │
│  │                                                                        │
│  ├── 2. Calcular totales                                                │
│  │   subtotal = 300.00                                                  │
│  │   delivery_fee = 50.00                                               │
│  │   total = 350.00                                                     │
│  │                                                                        │
│  ├── 3. Crear orden en Supabase                                         │
│  │   INSERT INTO orders (...)                                           │
│  │   ↳ Trigger genera order_number = "ORD-20250121-001"                │
│  │                                                                        │
│  ├── 4. Crear order_items                                               │
│  │   INSERT INTO order_items (...)                                      │
│  │                                                                        │
│  └── 5. Llamar a StripeService                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        STRIPE SERVICE                                    │
│                                                                           │
│  create_payment_intent()                                                 │
│  ├── 1. Obtener cuenta Stripe del colmado                               │
│  │   SELECT * FROM stripe_accounts WHERE colmado_id = ?                 │
│  │   ↳ stripe_account_id = "acct_xxxxx"                                 │
│  │                                                                        │
│  ├── 2. Verificar que charges_enabled = true                            │
│  │                                                                        │
│  ├── 3. Calcular montos                                                 │
│  │   amount_cents = 350.00 * 100 = 35,000                              │
│  │   platform_fee_cents = 35,000 * 10% = 3,500                         │
│  │   transfer_amount_cents = 35,000 - 3,500 = 31,500                   │
│  │                                                                        │
│  └── 4. Crear PaymentIntent en Stripe                                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           STRIPE API                                     │
│                                                                           │
│  stripe.PaymentIntent.create({                                           │
│    amount: 35000,                // 350.00 DOP en centavos              │
│    currency: 'dop',                                                      │
│    transfer_data: {                                                      │
│      destination: 'acct_xxxxx',  // Cuenta del colmado                  │
│      amount: 31500               // Sin comisión de plataforma          │
│    }                                                                     │
│  })                                                                      │
│                                                                           │
│  RETORNA:                                                                │
│  ├── payment_intent_id: "pi_xxxxx"                                      │
│  └── client_secret: "pi_xxxxx_secret_yyyyy"                             │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        FLASK BACKEND                                     │
│                                                                           │
│  5. Guardar payment en Supabase                                          │
│     INSERT INTO payments (...)                                           │
│                                                                           │
│  6. Actualizar orden → status = 'payment_processing'                    │
│     UPDATE orders SET status = 'payment_processing'                     │
│                                                                           │
│  7. Vaciar carrito                                                       │
│     DELETE FROM cart_items WHERE cart_id = ?                            │
│                                                                           │
│  8. Retornar response al Android                                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
           Response: {               │
             "success": true,         │
             "order_id": "uuid",      │
             "client_secret": "pi_...",
             "amount": 350.00         │
           }                          │
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        ANDROID APP                                       │
│                                                                           │
│  PaymentViewModel recibe client_secret                                   │
│  └── isReadyForPayment = true                                           │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
            LaunchedEffect detecta    │
            isReadyForPayment = true  │
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        STRIPE PAYMENT SHEET                              │
│                                                                           │
│  paymentSheet.presentWithPaymentIntent(                                  │
│    clientSecret = "pi_xxxxx_secret_yyyyy"                               │
│  )                                                                       │
│                                                                           │
│  ┌─────────────────────────────────────┐                                │
│  │  💳 Pagar 350.00 DOP                │                                │
│  │                                      │                                │
│  │  Número de tarjeta:                 │                                │
│  │  [4242 4242 4242 4242]              │                                │
│  │                                      │                                │
│  │  Fecha:        CVV:                 │                                │
│  │  [12/34]       [123]                │                                │
│  │                                      │                                │
│  │  [Pagar] │ [Cancelar]               │                                │
│  └─────────────────────────────────────┘                                │
│                                                                           │
│  Usuario completa los datos y presiona "Pagar"                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           STRIPE API                                     │
│                                                                           │
│  1. Procesa el pago                                                      │
│  2. Cobra 350.00 DOP de la tarjeta del cliente                          │
│  3. Retiene 35.00 DOP (10% comisión) en tu cuenta                       │
│  4. Transfiere 315.00 DOP a la cuenta del colmado                       │
│  5. Dispara evento: payment_intent.succeeded                            │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        STRIPE WEBHOOK                                    │
│                                                                           │
│  POST http://tu-backend.com/webhooks/stripe                             │
│  {                                                                        │
│    "type": "payment_intent.succeeded",                                  │
│    "data": {                                                             │
│      "object": {                                                         │
│        "id": "pi_xxxxx",                                                │
│        "amount": 35000,                                                 │
│        "status": "succeeded",                                           │
│        "charges": {                                                      │
│          "data": [{                                                      │
│            "id": "ch_xxxxx",                                           │
│            "payment_method_details": {                                  │
│              "card": {                                                   │
│                "brand": "visa",                                         │
│                "last4": "4242"                                          │
│              }                                                           │
│            }                                                             │
│          }]                                                              │
│        }                                                                 │
│      }                                                                   │
│    }                                                                     │
│  }                                                                        │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        FLASK BACKEND                                     │
│                                                                           │
│  webhooks/stripe_webhooks.py                                             │
│                                                                           │
│  1. Verificar firma del webhook                                          │
│     stripe.Webhook.construct_event(...)                                 │
│                                                                           │
│  2. Procesar evento: payment_intent.succeeded                           │
│     StripeService.handle_payment_succeeded()                            │
│                                                                           │
│     ├── Actualizar payment en Supabase                                  │
│     │   UPDATE payments SET                                             │
│     │     status = 'succeeded',                                         │
│     │     stripe_charge_id = 'ch_xxxxx',                               │
│     │     card_brand = 'visa',                                          │
│     │     card_last4 = '4242'                                           │
│     │                                                                     │
│     └── Actualizar orden en Supabase                                    │
│         UPDATE orders SET                                               │
│           status = 'paid',                                              │
│           paid_at = NOW()                                               │
│                                                                           │
│     ↳ Trigger registra cambio en order_status_history                  │
│                                                                           │
│  3. Retornar 200 OK a Stripe                                            │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        ANDROID APP                                       │
│                                                                           │
│  PaymentSheet retorna:                                                   │
│  PaymentSheetResult.Completed                                           │
│                                                                           │
│  handlePaymentResult()                                                   │
│  ├── paymentStatus = SUCCESS                                            │
│  ├── successMessage = "¡Pago exitoso!"                                  │
│  └── onPaymentSuccess(order_id)                                         │
│                                                                           │
│  Navega a: OrderSuccessScreen                                           │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    PANTALLA DE ÉXITO                                     │
│                                                                           │
│  ┌─────────────────────────────────────┐                                │
│  │                                      │                                │
│  │         ✅ ¡Pago Exitoso!           │                                │
│  │                                      │                                │
│  │  Orden: ORD-20250121-001            │                                │
│  │  Total: 350.00 DOP                  │                                │
│  │  Estado: Pagado                     │                                │
│  │                                      │                                │
│  │  Tu pedido está siendo preparado    │                                │
│  │                                      │                                │
│  │  [Ver detalles] [Volver al inicio] │                                │
│  │                                      │                                │
│  └─────────────────────────────────────┘                                │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                    RESULTADO FINAL                                       │
│                                                                           │
│  ✅ Cliente: Pagó 350.00 DOP                                            │
│  ✅ Plataforma (tú): Recibiste 35.00 DOP (10%)                          │
│  ✅ Colmado: Recibirá 315.00 DOP                                        │
│  ✅ Orden: Estado = 'paid'                                              │
│  ✅ Payment: Estado = 'succeeded'                                       │
│  ✅ Carrito: Vaciado                                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 DIAGRAMA DE ENTIDADES

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   CLIENTE    │────────▶│    CARRITO   │────────▶│   COLMADO    │
│  (Usuario)   │         │              │         │   (Seller)   │
└──────────────┘         └──────────────┘         └──────────────┘
       │                        │                         │
       │ crea                   │ tiene                   │ pertenece a
       │                        │                         │
       ▼                        ▼                         ▼
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│    ORDEN     │────────▶│  ORDER_ITEMS │         │STRIPE_ACCOUNT│
│              │         │              │         │              │
└──────────────┘         └──────────────┘         └──────────────┘
       │                                                   │
       │ genera                                            │
       │                                                   │
       ▼                                                   ▼
┌──────────────┐                                  ┌──────────────┐
│   PAYMENT    │─────────────────────────────────▶│PAYMENT_INTENT│
│  (Supabase)  │         sincronizado             │   (Stripe)   │
└──────────────┘                                  └──────────────┘
       │                                                   │
       │                                                   │
       │                                                   │
       ▼                                                   ▼
┌──────────────┐                                  ┌──────────────┐
│ORDER_STATUS_ │                                  │  TRANSFER    │
│   HISTORY    │                                  │   (Stripe)   │
└──────────────┘                                  └──────────────┘
```

---

## 🔐 FLUJO DE SEGURIDAD

```
1. ANDROID APP
   └── Nunca maneja datos de tarjeta
   └── Solo envía client_secret a Stripe
   └── Stripe SDK encripta todo

2. FLASK BACKEND
   └── Service role key de Supabase (privada)
   └── Stripe secret key (privada)
   └── Verifica firma de webhooks

3. STRIPE
   └── PCI compliant
   └── Encriptación end-to-end
   └── Webhooks firmados con secret

4. SUPABASE
   └── Row Level Security (RLS)
   └── Solo usuarios autorizados ven sus datos
```

---

## ⏱️ TIMELINE TÍPICO

```
0ms     ───▶ Usuario presiona "Pagar"
100ms   ───▶ Android llama POST /orders/create
200ms   ───▶ Backend consulta Supabase
300ms   ───▶ Backend llama a Stripe API
500ms   ───▶ Stripe crea PaymentIntent
600ms   ───▶ Backend retorna client_secret
700ms   ───▶ PaymentSheet se abre
...     ───▶ Usuario ingresa datos de tarjeta
10s     ───▶ Usuario presiona "Pagar"
11s     ───▶ Stripe procesa pago
12s     ───▶ Stripe envía webhook
12.1s   ───▶ Backend actualiza orden → 'paid'
12.5s   ───▶ Android recibe PaymentSheetResult.Completed
13s     ───▶ Android navega a pantalla de éxito
```

**Total: ~13 segundos** (la mayoría es el usuario ingresando datos)

---

## 💰 FLUJO DE DINERO

```
CLIENTE              STRIPE              PLATAFORMA          COLMADO
  │                    │                    │                  │
  │  350.00 DOP        │                    │                  │
  │──────────────────▶│                    │                  │
  │                    │                    │                  │
  │                    │  35.00 DOP (10%)   │                  │
  │                    │───────────────────▶│                  │
  │                    │                    │                  │
  │                    │  315.00 DOP (90%)                     │
  │                    │──────────────────────────────────────▶│
  │                    │                    │                  │

Tiempo de transferencia:
  - A la plataforma: Instantáneo
  - Al colmado: Según config (puede ser instantáneo o en 2-7 días)
```

---

## 🎯 PUNTOS CLAVE

1. **El cliente nunca ve su dinero dividido** - Para él es un solo pago de 350 DOP

2. **Stripe maneja la división automáticamente** - Usando `transfer_data.destination`

3. **El colmado recibe 315 DOP directamente** - No pasa por tu cuenta

4. **Tu comisión (35 DOP) se queda en tu cuenta Stripe** - Automáticamente

5. **Todo es atómico** - Si el pago falla, nadie recibe nada

6. **Webhooks aseguran consistencia** - Supabase siempre refleja el estado real de Stripe

---

## 📝 NOTAS IMPORTANTES

- **Modo TEST**: Usa tarjetas de prueba de Stripe
- **Moneda**: DOP (Pesos Dominicanos)
- **Comisión de plataforma**: 10% (configurable en `.env`)
- **Comisión de Stripe**: ~2.9% + 0.30 USD (viene de tu comisión)
- **Sin costos iniciales**: Solo pagas cuando hay ventas

