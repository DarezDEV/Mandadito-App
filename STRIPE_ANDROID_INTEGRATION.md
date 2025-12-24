# 📱 Integración de Stripe en Android (Kotlin)

## PASO 1: Agregar Dependencias

Abre `app/build.gradle.kts` y agrega:

```kotlin
dependencies {
    // ... tus dependencias existentes

    // Stripe Android SDK
    implementation("com.stripe:stripe-android:20.37.2")

    // Coroutines (si no las tienes)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit (para llamadas HTTP al backend)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
```

## PASO 2: Configurar AndroidManifest.xml

Agrega permisos de internet (si no los tienes):

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## PASO 3: Estructura de Archivos Android

```
app/src/main/java/com/dev/mandadito/
├── data/
│   ├── models/
│   │   ├── Order.kt              (ya existe, actualizar)
│   │   └── Payment.kt            (nuevo)
│   ├── network/
│   │   ├── ApiService.kt         (nuevo - Retrofit)
│   │   └── StripeApiClient.kt    (nuevo)
│   └── repository/
│       ├── OrderRepository.kt    (nuevo)
│       └── PaymentRepository.kt  (nuevo)
├── presentation/
│   ├── viewmodels/
│   │   └── PaymentViewModel.kt   (nuevo)
│   └── screens/
│       └── CheckoutScreen.kt     (nuevo)
└── config/
    └── StripeConfig.kt           (nuevo)
```

---

# IMPLEMENTACIÓN COMPLETA

Ahora voy a crear todos los archivos necesarios...

