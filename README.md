# Mandadito App

Aplicación de delivery y marketplace desarrollada en Android con Kotlin y Jetpack Compose. Permite a usuarios pedir productos de colmados locales, con soporte para múltiples roles (cliente, vendedor, repartidor, administrador).

## Tabla de Contenidos

1. [Características Principales](#características-principales)
2. [Arquitectura](#arquitectura)
3. [Roles de Usuario](#roles-de-usuario)
4. [Tecnologías y Dependencias](#tecnologías-y-dependencias)
5. [Estructura del Proyecto](#estructura-del-proyecto)
6. [Configuración del Entorno](#configuración-del-entorno)
7. [Configuración de Base de Datos](#configuración-de-base-de-datos)
8. [APIs y Servicios](#apis-y-servicios)
9. [Ejecución del Proyecto](#ejecución-del-proyecto)
10. [Configuración de Producción](#configuración-de-producción)


---

## Características Principales

- **Autenticación**: Registro/login con email y contraseña через Supabase Auth
- **Multi-rol**: Sistema de 4 roles (cliente, vendedor, repartidor, administrador)
- **Carrito de Compras**: Gestión de productos por colmado
- **Pagos con Stripe**: Integración completa de pagos con Stripe Connect
- **Realtime Updates**: Actualizaciones en tiempo real con Supabase Realtime
- **Panel de Administración**: Gestión de usuarios, colmados y finanzas
- **Modo Oscuro**: Soporte completo para tema oscuro

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                      │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    Jetpack Compose                       ││
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐││
│  │  │   Screens   │ │  ViewModels │ │    Navigation       │││
│  │  └─────────────┘ └─────────────┘ └─────────────────────┘││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│                     DATA LAYER                               │
│  ┌─────────────────────────────────────────────────────────┐│
│  │   Repositories │ Models │ Network │ Local (Room)       ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│                    BACKEND (Supabase)                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────┐  ││
│  │ Postgrest│ │  Auth    │ │ Storage  │ │ Edge Functions │  ││
│  └──────────┘ └──────────┘ └──────────┘ └────────────────┘  ││
│  ┌─────────────────────────────────────────────────────────┐│
│  │              PostgreSQL + Row Level Security             ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### Patrones Utilizados

- **MVVM** - Model-View-ViewModel para gestión de estado
- **Repository Pattern** - Abstracción de acceso a datos
- **StateFlow** - Gestión reactiva del estado UI
- **Kotlin Coroutines** - Programación asíncrona
- **Clean Architecture** - Separación de responsabilidades

---

## Roles de Usuario

| Rol | Descripción | Permisos |
|-----|-------------|----------|
| **Cliente** | Usuario final que realiza pedidos | Ver colmados, agregar al carrito, pagar, gestionar direcciones |
| **Vendedor** | Propietario de colmado | Gestionar productos, ver pedidos, recibir pagos |
| **Repartidor** | Entrega de pedidos | Ver pedidos asignados, actualizar estado de entrega |
| **Administrador** | Gestión total del sistema | Gestionar usuarios, colmados, ver reportes financieros |

---

## Tecnologías y Dependencias

### Core
- **Kotlin** 2.0.21
- **Android SDK** 28-36 (minSdk 28, targetSdk 36)
- **Jetpack Compose** (BOM 2024.10.00)

### Backend as a Service
- **Supabase** 3.0.0
  - Auth (autenticación)
  - Postgrest (API REST automática)
  - Storage (almacenamiento de archivos)
  - Realtime (actualizaciones en tiempo real)

### Base de Datos
- **PostgreSQL** 17
- **Room** 2.6.1 (caché local)

### Pagos
- **Stripe Android SDK** 20.52.3
- **Stripe Connect** (pagos split para vendedores)

### Localización y Direcciones
- **Nominatim/OpenStreetMap** (geocodificación de direcciones)
- Las direcciones se geocodifican usando el servicio gratuito de OpenStreetMap

### Imagenes
- **Coil** 2.7.0 (carga de imágenes)
- **Accompanist Pager** 0.30.1 (slider de imágenes)

### Networking
- **Ktor** 3.0.0 (cliente HTTP)
- **Retrofit** 2.9.0 (llamadas HTTP)

### Navegación
- **Navigation Compose** 2.8.4

### Serialización
- **Kotlinx Serialization** 1.6.0
- **Kotlinx Coroutines** 1.8.1

---

## Estructura del Proyecto

```
Mandadito-App/
├── app/
│   ├── src/main/
│   │   ├── java/com/dev/mandadito/
│   │   │   ├── config/           # Configuración de app
│   │   │   ├── data/
│   │   │   │   ├── models/       # Modelos de datos
│   │   │   │   ├── network/      # Cliente Supabase
│   │   │   │   ├── repository/   # Repositorios
│   │   │   │   └── local/        # Room database
│   │   │   ├── presentation/
│   │   │   │   ├── screens/      # Pantallas Compose
│   │   │   │   ├── viewmodels/   # ViewModels
│   │   │   │   ├── navigation/   # NavHost
│   │   │   │   └── components/   # Componentes reutilizables
│   │   │   ├── ui/theme/         # Tema y colores
│   │   │   ├── utils/            # Utilidades
│   │   │   └── MainActivity.kt   # Entry point
│   │   └── res/
│   │       ├── values/           # Strings, styles
│   │       └── drawable/         # Recursos gráficos
│   └── src/test/                 # Unit tests
├── gradle/
│   └── libs.versions.toml        # Versiones de dependencias
├── supabase/
│   ├── config.toml               # Configuración Supabase local
│   ├── schema_completo.sql       # Schema de base de datos
│   └── functions/                # Edge Functions (Deno)
├── settings.gradle.kts
├── gradle.properties
└── local.properties              # Claves sensibles (NO commitear)
```

---

## Configuración del Entorno

### Requisitos Previos

1. **Android Studio** Hedgehog (2023.1.1) o superior
2. **JDK** 17 o superior
3. **Node.js** 18+ (para Supabase CLI)
4. **Supabase CLI** (opcional, para desarrollo local)

### Configuración de Variables de Entorno

Crear archivo `local.properties` en la raíz del proyecto:

```properties
# Supabase Configuration
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu-anon-key-aqui
SUPABASE_SERVICE_ROLE_KEY=tu-service-role-key-aqui

# Stripe Keys (usar en Edge Functions)
STRIPE_SECRET_KEY=sk_test_xxx
STRIPE_PUBLISHABLE_KEY=pk_test_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx
```

### Configuración de Supabase

#### 1. Crear Proyecto en Supabase

1. Ir a [supabase.com](https://supabase.com)
2. Crear nuevo proyecto
3. Anotar URL y ANON_KEY

#### 2. Configurar Base de Datos

Ejecutar el schema completo:

```bash
# Usando Supabase CLI
supabase db push

# O manualmente desde SQL Editor
# Copiar contenido de supabase/schema_completo.sql
```

#### 3. Habilitar Realtime

```sql
ALTER PUBLICATION supabase_realtime ADD TABLE orders;
ALTER PUBLICATION supabase_realtime ADD TABLE order_items;
ALTER PUBLICATION supabase_realtime ADD TABLE notifications;
ALTER PUBLICATION supabase_realtime ADD TABLE payments;
```

#### 4. Configurar Stripe Connect

1. Crear cuenta en [Stripe Dashboard](https://dashboard.stripe.com)
2. Habilitar Stripe Connect
3. Configurar Webhook: `https://tu-proyecto.supabase.co/functions/v1/stripe-webhook`

---

## Configuración de Base de Datos

### Tablas Principales

| Tabla | Descripción |
|-------|-------------|
| `auth.users` | Usuarios de autenticación |
| `profiles` | Perfiles extendidos de usuarios |
| `roles` | Roles del sistema (client, seller, delivery, admin) |
| `user_roles` | Relación usuario-rol |
| `colmados` | Tiendas/vendedores |
| `products` | Productos |
| `categories` | Categorías de productos |
| `addresses` | Direcciones de usuarios |
| `carts` | Carritos de compra |
| `cart_items` | Items del carrito |
| `orders` | Órdenes |
| `order_items` | Items de cada orden |
| `payments` | Pagos |
| `stripe_accounts` | Cuentas Stripe Connect |
| `notifications` | Notificaciones |

### Comisiones y Pagos

- **Plataforma**: 5% de comisión (`platform_fee`)
- **Delivery**: Tarifa fija configurable (default: RD$50)
- **Stripe Connect**: Transferencias automáticas a vendedores

### Políticas RLS (Row Level Security)

Todas las tablas tienen políticas RLS configuradas para:
- Usuarios solo ven sus propios datos
- Vendedores ven datos de sus colmados
- Repartidores ven pedidos asignados
- Admin ve todos los datos

---

## APIs y Servicios

### 1. Supabase Auth

```kotlin
// Login
supabase.auth.signInWith(Email)

signInWith(OAuth)

// Logout
supabase.auth.signOut()

// Obtener usuario actual
supabase.auth.currentUserOrNull()
```

### 2. Supabase Postgrest (API REST)

```kotlin
// SELECT
client.from("products").select() {
    filter { eq("colmado_id", id) }
}

// INSERT
client.from("products").insert(product)

// UPDATE
client.from("products").update {
    set("stock", newStock)
    filter { eq("id", productId) }
}

// DELETE
client.from("products").delete {
    filter { eq("id", productId) }
}
```

### 3. Edge Functions (Deno)

| Función | Endpoint | Descripción |
|---------|----------|-------------|
| `orders-create` | `/functions/v1/orders-create` | Crear nueva orden |
| `stripe-webhook` | `/functions/v1/stripe-webhook` | Webhook de Stripe |
| `finance-report` | `/functions/v1/finance-report` | Reportes financieros |
| `stripe-connect-create` | `/functions/v1/stripe-connect-create` | Onboarding Stripe Connect |
| `orders-get` | `/functions/v1/orders-get` | Obtener órdenes |

### 4. Stripe Payments

```kotlin
// Crear PaymentIntent desde Edge Function
val paymentSheet = rememberPaymentSheet { result ->
    // Manejar resultado
}

// Presentar hoja de pago
paymentSheet.presentWithPaymentIntent(clientSecret)
```

### 5. Geocodificación de Direcciones

Las direcciones se geocodifican usando Nominatim (OpenStreetMap):

```kotlin
// La geocodificación se realiza automáticamente al guardar una dirección
// Usa el servicio gratuito de OpenStreetMap para obtener coordenadas
val url = "https://nominatim.openstreetmap.org/search?format=json&q=$address&countrycodes=DO&limit=1"
```

### 6. Supabase Realtime

```kotlin
// Suscribirse a cambios en tabla
client.realtime.channel("payments")
    .on(PostgresChangeEvent.INSERT) { event ->
        // Actualizar UI
    }
    .subscribe()
```

---

## Ejecución del Proyecto

### 1. Clonar el Repositorio

```bash
git clone https://github.com/tu-usuario/Mandadito-App.git
cd Mandadito-App
```

### 2. Configurar Dependencias

```bash
# Sincronizar con Gradle
./gradlew build  # Linux/Mac
gradlew.bat build # Windows

# O usando Android Studio
# File > Sync Project with Gradle Files
```

### 3. Configurar Variables de Entorno

Crear/editar `local.properties` con tus claves:

```properties
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 4. Ejecutar en Emulador

1. Abrir Android Studio
2. Seleccionar dispositivo emulado (Android 12+)
3. Click en **Run** (Shift + F10)

### 5. Ejecutar en Dispositivo Físico

1. Habilitar **Developer Options** y **USB Debugging** en el dispositivo
2. Conectar dispositivo por USB
3. Seleccionar dispositivo en Android Studio
4. Click en **Run**

### 6. Verificar Logs

```bash
# Ver logs de la app
adb logcat | grep -i mandadito
```

---

## Configuración de Producción

### 1. Build Release

```bash
# Generar APK de release
./gradlew assembleRelease

# APK firmado (requiere keystore)
./gradlew assembleRelease -Pandroid.injected.signing.store.file=my-release-key.keystore
```

### 2. Configurar ProGuard

El proyecto incluye reglas en `proguard-rules.pro`:

```proguard
# Supabase
-keep class io.github.jan.supabase.** { *; }

# Stripe
-keep class com.stripe.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
```

### 3. Configurar Firebase (Opcional)

Si deseas push notifications:

1. Crear proyecto en Firebase Console
2. Descargar `google-services.json`
3. Colocar en `app/google-services.json`
4. Agregar dependencia:

```kotlin
implementation platform('com.google.firebase:firebase-bom:32.7.0')
implementation 'com.google.firebase:firebase-messaging-ktx'
```

### 4. Configurar SSL/TLS

Para producción, asegurar que:
- `cleartextTrafficPermitted = false` en `build.gradle.kts`
- Solo usar HTTPS en producción

---

## Testing

### Unit Tests

```bash
# Ejecutar todos los tests
./gradlew testDebugUnitTest

# Ejecutar tests de un ViewModel específico
./gradlew testDebugUnitTest --tests "*AuthViewModelTest*"

# Generar reporte de cobertura
./gradlew createDebugCoverageReport
```

### Instrumentation Tests

```bash
# Requiere dispositivo/emulador
./gradlew connectedDebugAndroidTest
```

---

## Solución de Problemas

### Error: "SUPABASE_URL not found"

1. Verificar que `local.properties` existe
2. Verificar que las claves están correctamente formateadas
3. Sincronizar proyecto con Gradle

### Error: "Authentication failed"

1. Verificar ANON_KEY en Supabase Dashboard
2. Verificar políticas RLS
3. Probar en Supabase SQL Editor directamente

### Error: "Stripe webhook failed"

1. Verificar STRIPE_WEBHOOK_SECRET
2. Verificar que el webhook está configurado en Stripe Dashboard
3. Revisar logs en Supabase Dashboard > Functions

---

## Contribución

1. Fork del repositorio
2. Crear branch: `git checkout -b feature/nueva-caracteristica`
3. Commit: `git commit -am 'Agregar nueva característica'`
4. Push: `git push origin feature/nueva-caracteristica`
5. Crear Pull Request

### Guías de Código

- Seguir convenciones de Kotlin (ver AGENTS.md)
- Escribir tests para nuevas funcionalidades
- Documentar APIs públicas con KDoc

---

