# 🗺️ Guía Completa: Configuración de Google Maps & Places API

> **Guía paso a paso para configurar Google Maps SDK y Places API en Mandadito App**

---

## 📋 Tabla de Contenidos

1. [Pre-requisitos](#pre-requisitos)
2. [Crear Proyecto en Google Cloud](#paso-1-crear-proyecto-en-google-cloud)
3. [Configurar Facturación](#paso-2-configurar-facturación)
4. [Habilitar APIs Necesarias](#paso-3-habilitar-apis-necesarias)
5. [Crear API Key](#paso-4-crear-api-key)
6. [Configurar Restricciones de Seguridad](#paso-5-configurar-restricciones-de-seguridad)
7. [Integrar en el Proyecto Android](#paso-6-integrar-en-el-proyecto-android)
8. [Verificar Configuración](#paso-7-verificar-configuración)
9. [Troubleshooting](#troubleshooting)
10. [Costos y Límites](#costos-y-límites)
11. [Mejores Prácticas](#mejores-prácticas)

---

## 📌 Pre-requisitos

Antes de comenzar, asegúrate de tener:

- ✅ Una cuenta de Google (Gmail)
- ✅ Tarjeta de crédito/débito válida (requerida aunque el tier gratuito es generoso)
- ✅ Acceso al código fuente de Mandadito App
- ✅ Android Studio instalado
- ✅ Conocimientos básicos de línea de comandos

---

## 🚀 Paso 1: Crear Proyecto en Google Cloud

### 1.1 Acceder a Google Cloud Console

1. Abre tu navegador y ve a: **https://console.cloud.google.com/**
2. Inicia sesión con tu cuenta de Google
3. Si es tu primera vez, acepta los Términos de Servicio

### 1.2 Crear Nuevo Proyecto

1. En la esquina superior izquierda, haz click en el selector de proyectos:
   ```
   [Nombre del Proyecto ▼]
   ```

2. En el modal que aparece, click en **"NUEVO PROYECTO"**

3. Completa el formulario:
   ```
   Nombre del proyecto: Mandadito-App
   Organización: Sin organización (o tu organización si tienes)
   Ubicación: Sin organización
   ```

4. Click en **"CREAR"**

5. Espera 10-30 segundos mientras se crea el proyecto

6. Verás una notificación: ✅ "El proyecto Mandadito-App se creó correctamente"

7. Click en **"SELECCIONAR PROYECTO"** en la notificación

### 1.3 Verificar Proyecto Seleccionado

En la parte superior, verifica que dice:
```
Mandadito-App
```

---

## 💳 Paso 2: Configurar Facturación

> ⚠️ **Importante**: Aunque hay un tier gratuito generoso, Google requiere una tarjeta para activar las APIs.

### 2.1 Crear Cuenta de Facturación

1. En el menú lateral (☰), busca **"Facturación"** o ve a:
   ```
   https://console.cloud.google.com/billing
   ```

2. Click en **"VINCULAR UNA CUENTA DE FACTURACIÓN"**

3. Si es tu primera vez:
   - Click en **"CREAR CUENTA DE FACTURACIÓN"**
   - Selecciona tu país: **República Dominicana**
   - Acepta los términos de servicio

4. Completa los datos:
   ```
   Tipo de cuenta: Individual / Empresa
   Nombre: [Tu nombre o empresa]
   Dirección: [Tu dirección en RD]
   ```

5. **Agregar método de pago**:
   - Número de tarjeta
   - Fecha de vencimiento
   - CVV
   - Nombre en la tarjeta

6. Click en **"INICIAR MI PRUEBA GRATUITA"**

7. **Importante**: Recibirás $300 USD en créditos gratuitos por 90 días (usuarios nuevos)

### 2.2 Vincular Facturación al Proyecto

1. Asegúrate de estar en el proyecto **Mandadito-App**

2. En Facturación, selecciona:
   ```
   Cuenta de facturación: [La cuenta que acabas de crear]
   ```

3. Click en **"ESTABLECER CUENTA"**

---

## 🔌 Paso 3: Habilitar APIs Necesarias

### 3.1 Acceder a Biblioteca de APIs

1. En el menú lateral (☰), ve a:
   ```
   APIs y servicios > Biblioteca
   ```
   O directamente: https://console.cloud.google.com/apis/library

### 3.2 Habilitar Maps SDK for Android

1. En el buscador de la biblioteca, escribe:
   ```
   Maps SDK for Android
   ```

2. Click en **"Maps SDK for Android"**

3. Click en el botón azul **"HABILITAR"**

4. Espera 10-20 segundos

5. Verás: ✅ "API habilitada"

### 3.3 Habilitar Places API

1. Click en el botón **"← Volver a la biblioteca"** (arriba a la izquierda)

2. En el buscador, escribe:
   ```
   Places API
   ```

3. Click en **"Places API"** (asegúrate que sea la correcta, no "Places API (New)")

4. Click en **"HABILITAR"**

5. Espera 10-20 segundos

6. Verás: ✅ "API habilitada"

### 3.4 Verificar APIs Habilitadas

1. Ve a: **APIs y servicios > APIs y servicios habilitados**

2. Deberías ver:
   ```
   ✅ Maps SDK for Android
   ✅ Places API
   ```

---

## 🔑 Paso 4: Crear API Key

### 4.1 Crear Credenciales

1. En el menú lateral, ve a:
   ```
   APIs y servicios > Credenciales
   ```
   O: https://console.cloud.google.com/apis/credentials

2. Click en **"+ CREAR CREDENCIALES"** (arriba)

3. Selecciona: **"Clave de API"**

4. Se creará automáticamente y verás un modal:
   ```
   ✅ Se creó la clave de API
   AIzaSyC... [tu clave]
   ```

5. **MUY IMPORTANTE**:
   - **COPIA ESTA CLAVE AHORA** (la necesitarás después)
   - Guárdala temporalmente en un lugar seguro (Notepad, etc.)
   ```
   AIzaSyC_ejemplo_clave_1234567890abcdef
   ```

6. **NO CIERRES** el modal todavía, antes click en **"EDITAR CLAVE DE API"**

---

## 🔒 Paso 5: Configurar Restricciones de Seguridad

> ⚠️ **CRÍTICO**: Sin restricciones, cualquiera puede usar tu API Key y generar costos.

### 5.1 Nombre de la API Key

1. En "Nombre de la clave de API", cambia a algo descriptivo:
   ```
   Mandadito-App-Android-Key
   ```

### 5.2 Restricción de Aplicación

1. En **"Restricción de aplicación"**, selecciona:
   ```
   ⦿ Aplicaciones de Android
   ```

2. Click en **"+ AGREGAR UN ELEMENTO"**

3. Completa:
   - **Nombre del paquete**:
     ```
     com.dev.mandadito
     ```
   - **Huella digital del certificado SHA-1**:

#### 5.2.1 Obtener SHA-1 (Debug)

**En Windows:**
```cmd
cd D:\Espacio de Trabajo\Mandadito-App
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**En Mac/Linux:**
```bash
cd ~/ruta/a/Mandadito-App
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

4. Copia el **SHA-1** que aparece, ejemplo:
   ```
   SHA1: A1:B2:C3:D4:E5:F6:G7:H8:I9:J0:K1:L2:M3:N4:O5:P6:Q7:R8:S9:T0
   ```

5. Pega el SHA-1 en el campo "Huella digital del certificado SHA-1"

#### 5.2.2 Agregar SHA-1 de Release (Producción)

6. Click en **"+ AGREGAR UN ELEMENTO"** nuevamente

7. Agrega otro registro:
   - **Nombre del paquete**: `com.dev.mandadito` (mismo)
   - **SHA-1 de Release**: (obtén este del keystore de producción)

**Para obtener SHA-1 de Release:**
```cmd
keytool -list -v -keystore ruta\a\tu\release.keystore -alias tu_alias
```

### 5.3 Restricción de API

1. En **"Restricción de API"**, selecciona:
   ```
   ⦿ Restringir clave
   ```

2. En el dropdown, selecciona:
   ```
   ☑ Maps SDK for Android
   ☑ Places API
   ```

3. **NO** selecciones otras APIs para mantener la seguridad

### 5.4 Guardar Configuración

1. Scroll hacia abajo y click en **"GUARDAR"**

2. Espera 5-10 segundos

3. Verás: ✅ "Se guardaron los cambios en las credenciales"

> ⚠️ **Nota**: Los cambios pueden tardar hasta 5 minutos en propagarse

---

## 📱 Paso 6: Integrar en el Proyecto Android

### 6.1 Crear archivo local.properties

1. Abre el proyecto en Android Studio

2. En la raíz del proyecto, busca el archivo `local.properties`
   - Si NO existe, créalo: Click derecho en raíz → New → File → `local.properties`

3. Abre `local.properties` y agrega:

```properties
# Google Maps API Key
MAPS_API_KEY=AIzaSyC_TU_CLAVE_AQUI

# Supabase Configuration (si ya las tienes)
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu_supabase_anon_key
```

4. **Reemplaza** `AIzaSyC_TU_CLAVE_AQUI` con tu API Key real

5. **Guarda el archivo** (Ctrl+S / Cmd+S)

### 6.2 Verificar AndroidManifest.xml

El archivo ya debería tener esta configuración (verifica):

**Ubicación**: `app/src/main/AndroidManifest.xml`

```xml
<manifest>
    <application>
        <!-- ... otras configuraciones ... -->

        <!-- Google Maps API Key -->
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="${MAPS_API_KEY}" />

    </application>

    <!-- Permisos de ubicación -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.INTERNET" />
</manifest>
```

### 6.3 Verificar build.gradle.kts

El archivo ya debería tener las dependencias (verifica):

**Ubicación**: `app/build.gradle.kts`

```kotlin
android {
    // ...
    defaultConfig {
        // ...

        // Cargar API Key desde local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use {
                localProperties.load(it)
            }
        }

        manifestPlaceholders["MAPS_API_KEY"] =
            localProperties.getProperty("MAPS_API_KEY", "")
    }
}

dependencies {
    // Google Maps & Places
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.libraries.places:places:3.3.0")

    // ... otras dependencias
}
```

### 6.4 Sync del Proyecto

1. Click en **"Sync Now"** en la barra amarilla que aparece arriba

   O:

2. Menú: **File → Sync Project with Gradle Files**

3. Espera a que termine el sync (1-3 minutos)

4. Verifica que no haya errores en la pestaña **"Build"**

---

## ✅ Paso 7: Verificar Configuración

### 7.1 Compilar la App

1. Click en el botón **Run** ▶️ (o Shift+F10)

2. Selecciona un dispositivo (emulador o físico)

3. Espera a que compile e instale

### 7.2 Probar Búsqueda de Direcciones

1. Abre la app en el dispositivo

2. Navega a: **Perfil → Direcciones → Agregar Dirección**

3. En el campo de búsqueda, escribe:
   ```
   Malecón Santo Domingo
   ```

4. **Si funciona correctamente**:
   - Verás sugerencias de Google Places aparecer
   - Las sugerencias tendrán nombres reales de lugares en RD
   - Al seleccionar, se cargará la dirección completa con coordenadas

5. **Si NO funciona**:
   - Ve a la sección [Troubleshooting](#troubleshooting)

### 7.3 Revisar Logs

En Android Studio, abre **Logcat** y busca:

**✅ Funcionando correctamente:**
```
AddressRepository: ✅ 5 predicciones encontradas
AddressRepository: ✅ Lugar seleccionado: Malecón de Santo Domingo
```

**❌ Error de API Key:**
```
Google Maps SDK error: API key not authorized
```

---

## 🔧 Troubleshooting

### Problema 1: "API key not authorized"

**Síntomas:**
- No aparecen sugerencias de lugares
- Logcat muestra: `API key not authorized`

**Soluciones:**

1. **Verificar SHA-1**:
   ```cmd
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```
   - Copia el SHA-1
   - Ve a Google Cloud Console → Credenciales
   - Edita la API Key
   - Verifica que el SHA-1 coincida EXACTAMENTE

2. **Verificar Package Name**:
   - En `app/build.gradle.kts`, busca:
     ```kotlin
     applicationId = "com.dev.mandadito"
     ```
   - En Google Cloud Console, verifica que sea el mismo

3. **Esperar propagación**:
   - Los cambios tardan hasta 5 minutos
   - Espera y vuelve a intentar

4. **Verificar APIs habilitadas**:
   - Google Cloud Console → APIs habilitadas
   - Debe aparecer: **Maps SDK for Android** y **Places API**

### Problema 2: Aparece "Modo Desarrollo"

**Síntomas:**
- Banner naranja que dice "Modo Desarrollo"
- Aparecen datos de prueba en lugar de búsqueda real

**Solución:**

1. Verifica que `local.properties` tenga:
   ```properties
   MAPS_API_KEY=AIzaSy... (debe empezar con AIza)
   ```

2. NO debe estar vacío ni tener placeholder

3. Re-sync el proyecto:
   - File → Sync Project with Gradle Files

### Problema 3: "Billing not enabled"

**Síntomas:**
- Error: `This API project is not authorized to use this API`

**Solución:**

1. Ve a: https://console.cloud.google.com/billing

2. Verifica que el proyecto **Mandadito-App** tenga una cuenta de facturación vinculada

3. Si no la tiene, vincúlala siguiendo el [Paso 2](#paso-2-configurar-facturación)

### Problema 4: La app crashea al buscar

**Síntomas:**
- App se cierra al escribir en el campo de búsqueda
- Logcat muestra: `NullPointerException`

**Solución:**

1. Verifica permisos en `AndroidManifest.xml`:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

2. Verifica que Places API esté inicializado en `AddressNavGraph.kt`

3. Limpia y reconstruye:
   ```
   Build → Clean Project
   Build → Rebuild Project
   ```

### Problema 5: Solo muestra datos mock

**Síntomas:**
- Siempre aparecen los mismos 7 lugares
- No hay búsqueda en tiempo real

**Solución:**

1. Abre `AddressFeatureFlags.kt`

2. Verifica que detecte correctamente:
   ```kotlin
   val hasGoogleMapsApiKey: Boolean
       get() = AppConfig.mapsApiKey.isNotBlank() &&
               AppConfig.mapsApiKey.startsWith("AIza")
   ```

3. Agrega logs para debug:
   ```kotlin
   Log.d("AddressFlags", "API Key: ${AppConfig.mapsApiKey}")
   ```

---

## 💰 Costos y Límites

### Tier Gratuito (Generoso)

Google ofrece **$200 USD de crédito mensual** que se aplica automáticamente:

| API | Crédito Mensual | Equivale a |
|-----|----------------|------------|
| **Places API - Autocomplete** | $200 USD | ~28,500 búsquedas/mes |
| **Places API - Place Details** | $200 USD | ~40,000 detalles/mes |
| **Maps SDK for Android** | Gratis | Sin cargo |

### Cálculo de Uso Estimado

Para **1,000 usuarios activos** al mes:
- ~3 búsquedas por usuario = 3,000 búsquedas
- **Costo estimado**: $0 USD (dentro del tier gratuito)

Para **10,000 usuarios**:
- ~30,000 búsquedas
- **Costo estimado**: ~$6 USD/mes

### Monitorear Uso

1. Ve a: https://console.cloud.google.com/apis/dashboard

2. Selecciona el proyecto **Mandadito-App**

3. Verás gráficos de uso en tiempo real

### Establecer Alertas de Presupuesto

1. Ve a: **Facturación → Presupuestos y alertas**

2. Click en **"CREAR PRESUPUESTO"**

3. Configura:
   ```
   Nombre: Alerta Mandadito App
   Proyecto: Mandadito-App
   Monto: $50 USD/mes
   Alertas: 50%, 90%, 100%
   ```

4. Recibirás emails si te acercas al límite

---

## 🎯 Mejores Prácticas

### 1. Seguridad de API Key

✅ **HACER:**
- Mantener `local.properties` en `.gitignore`
- Usar restricciones de aplicación (SHA-1)
- Restringir solo a las APIs necesarias
- Rotar API Keys cada 6 meses

❌ **NO HACER:**
- Hacer commit de `local.properties` a Git
- Compartir tu API Key públicamente
- Usar la misma key en múltiples apps
- Dejar la key sin restricciones

### 2. Optimización de Costos

✅ **Reducir Costos:**
- Usar **Session Tokens** (ya implementado en `AddressRepository.kt`)
  ```kotlin
  private var sessionToken = AutocompleteSessionToken.newInstance()
  ```
- Implementar debounce (ya implementado: 300ms)
- Cachear resultados comunes
- Usar autocomplete por "as you type" en lugar de "per session"

### 3. Experiencia de Usuario

✅ **Mejorar UX:**
- Mostrar loading state durante búsqueda
- Manejar errores de red gracefully
- Ofrecer modo manual como fallback
- Filtrar por país (República Dominicana)

### 4. Testing

Antes de producción:

1. **Test con SHA-1 de Release**:
   ```cmd
   keytool -list -v -keystore release.keystore -alias release_alias
   ```

2. **Test en múltiples dispositivos**:
   - Emulador Android
   - Dispositivo físico
   - Diferentes versiones de Android

3. **Test de edge cases**:
   - Sin conexión a internet
   - API Key inválida
   - Búsqueda con caracteres especiales

### 5. Monitoreo en Producción

Configurar monitoreo:

1. **Firebase Crashlytics**: Para detectar crashes
2. **Google Analytics**: Para medir uso de direcciones
3. **Cloud Logging**: Para logs de API errors

---

## 📚 Referencias Oficiales

- **Google Maps Platform**: https://developers.google.com/maps
- **Places API Docs**: https://developers.google.com/maps/documentation/places/android-sdk
- **Pricing**: https://mapsplatform.google.com/pricing/
- **Support**: https://support.google.com/googleapi

---

## ✅ Checklist Final

Antes de considerar la configuración completa, verifica:

- [ ] Proyecto creado en Google Cloud Console
- [ ] Facturación habilitada y tarjeta agregada
- [ ] Maps SDK for Android habilitado
- [ ] Places API habilitado
- [ ] API Key creada
- [ ] Restricciones de aplicación configuradas (SHA-1)
- [ ] Restricciones de API configuradas
- [ ] `local.properties` creado con API Key
- [ ] `local.properties` en `.gitignore`
- [ ] Proyecto Android sincronizado
- [ ] App compilada sin errores
- [ ] Búsqueda de direcciones probada y funcionando
- [ ] Alertas de presupuesto configuradas
- [ ] SHA-1 de Release agregado (para producción)

---

## 🆘 ¿Necesitas Ayuda?

Si algo no funciona:

1. **Revisa Troubleshooting** arriba
2. **Verifica Logs** en Android Studio → Logcat
3. **Consulta Estado de Google**: https://status.cloud.google.com/
4. **Stack Overflow**: Busca tu error específico
5. **Abre un issue** en el repositorio del proyecto

---

**¡Configuración Completa!** 🎉

Tu app ahora puede usar Google Places para autocompletado de direcciones en República Dominicana.
