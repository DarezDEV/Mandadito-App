# 🚀 Guía de Despliegue - Stripe Connect Edge Functions

Esta guía te ayudará a desplegar las Edge Functions de Stripe Connect en Supabase.

## 📋 Prerequisitos

1. **Cuenta de Supabase** con proyecto creado
2. **Supabase CLI** instalado
3. **Cuenta de Stripe** (modo test para desarrollo)

## 🔧 Paso 1: Instalar Supabase CLI

```bash
# Windows (con Scoop)
scoop bucket add supabase https://github.com/supabase/scoop-bucket.git
scoop install supabase

# O con NPM
npm install -g supabase

# Verificar instalación
supabase --version
```

## 🔐 Paso 2: Login en Supabase

```bash
# Hacer login
supabase login

# Te abrirá el navegador para autenticarte
```

## 🔗 Paso 3: Conectar con tu Proyecto

```bash
# Ve a la carpeta del proyecto
cd "D:\Espacio de Trabajo\Mandadito-App"

# Conectar con tu proyecto de Supabase
supabase link --project-ref TU_PROJECT_REF

# TU_PROJECT_REF lo encuentras en:
# Supabase Dashboard > Settings > General > Reference ID
```

## 🔑 Paso 4: Configurar Secrets (Variables de Entorno)

```bash
# Configurar STRIPE_SECRET_KEY
supabase secrets set STRIPE_SECRET_KEY=sk_test_tu_clave_secreta_de_stripe

# Verificar que se guardó
supabase secrets list
```

**⚠️ IMPORTANTE:**
- Usa tu **Test Secret Key** de Stripe (empieza con `sk_test_`)
- Encuéntrala en: https://dashboard.stripe.com/test/apikeys
- **NUNCA** subas esta clave a Git

## 📤 Paso 5: Desplegar las Edge Functions

```bash
# Desplegar TODAS las funciones a la vez
supabase functions deploy stripe-connect-create
supabase functions deploy stripe-connect-status
supabase functions deploy stripe-connect-refresh
supabase functions deploy stripe-onboarding-complete
supabase functions deploy stripe-onboarding-refresh

# O una por una si prefieres
```

**Salida esperada:**
```
Deploying Function stripe-connect-create...
Function URL: https://[tu-proyecto].supabase.co/functions/v1/stripe-connect-create
✅ Deployed successfully
```

## ✅ Paso 6: Verificar Despliegue

```bash
# Ver todas las funciones desplegadas
supabase functions list
```

Deberías ver:
```
┌─────────────────────────────────┬──────────┬──────────────┐
│ NAME                            │ STATUS   │ UPDATED AT   │
├─────────────────────────────────┼──────────┼──────────────┤
│ stripe-connect-create           │ deployed │ just now     │
│ stripe-connect-status           │ deployed │ just now     │
│ stripe-connect-refresh          │ deployed │ just now     │
│ stripe-onboarding-complete      │ deployed │ just now     │
│ stripe-onboarding-refresh       │ deployed │ just now     │
└─────────────────────────────────┴──────────┴──────────────┘
```

## 🧪 Paso 7: Probar las Funciones

### Probar desde línea de comandos:

```bash
# Probar stripe-connect-status
curl -X POST https://[tu-proyecto].supabase.co/functions/v1/stripe-connect-status \
  -H "Content-Type: application/json" \
  -d '{"colmado_id": "test-123"}'

# Respuesta esperada:
# {"success":true,"has_account":false,"onboarding_completed":false,...}
```

### Probar desde el navegador:

Abre en tu navegador:
```
https://[tu-proyecto].supabase.co/functions/v1/stripe-onboarding-complete
```

Deberías ver la página HTML de éxito.

## 📱 Paso 8: Actualizar Android App

La app Android ya está configurada para usar las Edge Functions automáticamente.

**Verifica que `local.properties` tenga:**
```properties
SUPABASE_URL=https://[tu-proyecto].supabase.co
SUPABASE_ANON_KEY=tu_anon_key
MAPS_API_KEY=tu_maps_key
```

## 🔍 Paso 9: Ver Logs en Tiempo Real

```bash
# Ver logs de todas las funciones
supabase functions logs

# Ver logs de una función específica
supabase functions logs stripe-connect-create

# Ver logs en tiempo real (live tail)
supabase functions logs --tail
```

## 🐛 Troubleshooting

### Error: "STRIPE_SECRET_KEY not configured"
```bash
# Verificar secrets
supabase secrets list

# Reconfigurar
supabase secrets set STRIPE_SECRET_KEY=sk_test_tu_clave
```

### Error: "Project not linked"
```bash
# Relink el proyecto
supabase link --project-ref TU_PROJECT_REF
```

### Error: "Function failed to deploy"
```bash
# Ver logs detallados
supabase functions deploy stripe-connect-create --debug
```

## 📊 Monitoreo

### Dashboard de Supabase
1. Ve a tu proyecto en Supabase
2. Click en "Edge Functions" en el menú lateral
3. Verás todas tus funciones con:
   - Número de invocaciones
   - Errores
   - Latencia promedio

### Ver invocaciones
```bash
# Ver estadísticas
supabase functions list --show-metrics
```

## 🔄 Actualizar Funciones

Cuando hagas cambios en el código:

```bash
# Redesplegar una función
supabase functions deploy stripe-connect-status

# Redesplegar todas
supabase functions deploy stripe-connect-create
supabase functions deploy stripe-connect-status
supabase functions deploy stripe-connect-refresh
```

## 🌐 URLs Finales

Después del despliegue, tus URLs serán:

```
https://[tu-proyecto].supabase.co/functions/v1/stripe-connect-create
https://[tu-proyecto].supabase.co/functions/v1/stripe-connect-status
https://[tu-proyecto].supabase.co/functions/v1/stripe-connect-refresh
https://[tu-proyecto].supabase.co/functions/v1/stripe-onboarding-complete
https://[tu-proyecto].supabase.co/functions/v1/stripe-onboarding-refresh
```

## ✨ Ventajas vs Flask Backend

✅ **Funciona en cualquier dispositivo** (emulador Y real)
✅ **Sin servidor que mantener** (serverless)
✅ **Auto-escalable** (maneja cualquier carga)
✅ **Siempre disponible** (no se cae)
✅ **HTTPS incluido** (seguro por defecto)
✅ **Logs centralizados** (fácil debugging)
✅ **Despliegue instantáneo** (segundos)

## 🎉 Listo!

Ahora tu app funcionará en:
- ✅ Emulador Android
- ✅ Dispositivos físicos
- ✅ Cualquier red WiFi/Móvil
- ✅ Producción

**No necesitas el servidor Flask corriendo** 🎊
