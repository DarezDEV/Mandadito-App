# 📋 Guía Paso a Paso - Configuración Completa

## ✅ PASO 4: EJECUTAR SQL EN SUPABASE

### Método 1: Desde el Dashboard (MÁS FÁCIL)

1. **Abre tu navegador** y ve a: https://supabase.com/dashboard

2. **Selecciona tu proyecto** (Mandadito-App)

3. **Ve al SQL Editor**:
   - En el menú lateral izquierdo, busca el ícono `</>`
   - Haz clic en **"SQL Editor"**

4. **Crea una nueva query**:
   - Clic en el botón **"New Query"** (arriba a la derecha)
   - O usa el atajo: `Ctrl + Enter` (Windows) / `Cmd + Enter` (Mac)

5. **Abre el archivo SQL en tu editor**:
   ```
   D:\Espacio de Trabajo\Mandadito-App\supabase\orders_and_payments_schema.sql
   ```

6. **Copia TODO el contenido** del archivo:
   - `Ctrl + A` (seleccionar todo)
   - `Ctrl + C` (copiar)

7. **Pega en el SQL Editor de Supabase**:
   - Clic en el área de texto del SQL Editor
   - `Ctrl + V` (pegar)

8. **Ejecuta el script**:
   - Clic en el botón verde **"Run"** (esquina inferior derecha)
   - O usa `Ctrl + Enter`

9. **Espera a que termine**:
   - Verás un spinner girando
   - Cuando termine dirá: **"Success. No rows returned"** ✅
   - Si hay errores, aparecerán en rojo

10. **Verifica que las tablas se crearon**:
    - Ve a **"Table Editor"** en el menú lateral
    - Deberías ver las nuevas tablas:
      - stripe_accounts
      - orders
      - order_items
      - payments
      - order_status_history

---

## ✅ PASO 5: OBTENER TUS CREDENCIALES

### 5.1 Obtener Supabase Keys

1. **En el Dashboard de Supabase**, ve a:
   - **Settings** (⚙️ en la esquina inferior izquierda)
   - **API**

2. **Copia estas 2 claves**:

   **A) Project URL:**
   ```
   https://tuproyecto.supabase.co
   ```

   **B) service_role key (secret):**
   ```
   eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

   ⚠️ **IMPORTANTE**: Usa el `service_role` key, NO el `anon` key

### 5.2 Obtener Stripe Keys

1. **Ve a Stripe Dashboard**: https://dashboard.stripe.com

2. **Asegúrate de estar en modo TEST**:
   - Esquina superior derecha debe decir **"Test mode"**
   - Si no, actívalo con el switch

3. **Ve a Developers**:
   - En el menú superior, clic en **"Developers"**
   - Luego **"API keys"**

4. **Copia estas 2 claves**:

   **A) Publishable key:**
   ```
   pk_test_51xxxxxxxxxxxxx
   ```

   **B) Secret key:**
   ```
   sk_test_51xxxxxxxxxxxxx
   ```

   ⚠️ **NOTA**: Si no ves el Secret key completo, haz clic en "Reveal test key"

---

## ✅ PASO 6: CONFIGURAR EL BACKEND

### 6.1 Abrir la carpeta del backend

```bash
# Abrir terminal en VS Code
# Ctrl + Shift + ` (acento grave)

# Navegar a la carpeta backend
cd backend
```

### 6.2 Crear archivo .env

**Opción A: Desde la terminal**

```bash
# Windows (PowerShell)
Copy-Item .env.example .env

# Linux/Mac
cp .env.example .env
```

**Opción B: Manualmente**
1. En VS Code, clic derecho en `backend/.env.example`
2. "Copy"
3. Clic derecho en carpeta `backend`
4. "Paste"
5. Renombrar a `.env` (sin el .example)

### 6.3 Editar el archivo .env

Abre `backend/.env` y completa con tus datos:

```env
# =====================================================
# SUPABASE
# =====================================================
SUPABASE_URL=https://tuproyecto.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# =====================================================
# STRIPE
# =====================================================
STRIPE_SECRET_KEY=sk_test_51xxxxxxxxxxxxx
STRIPE_PUBLISHABLE_KEY=pk_test_51xxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxx  # Lo obtendremos en el siguiente paso

# Configuración
STRIPE_PLATFORM_FEE_PERCENT=10
STRIPE_CURRENCY=DOP

# =====================================================
# FLASK
# =====================================================
FLASK_ENV=development
FLASK_APP=app.py
SECRET_KEY=mi_super_secreto_cambiar_en_produccion_12345

PORT=5000
BASE_URL=http://localhost:5000
```

**⚠️ IMPORTANTE**: Por ahora deja `STRIPE_WEBHOOK_SECRET` vacío, lo completaremos después.

### 6.4 Crear entorno virtual e instalar dependencias

```bash
# Asegúrate de estar en la carpeta backend
cd backend

# Crear entorno virtual
python -m venv venv

# Activar entorno virtual
# Windows (PowerShell)
venv\Scripts\activate

# Windows (CMD)
venv\Scripts\activate.bat

# Linux/Mac
source venv/bin/activate

# Verás (venv) al inicio de la línea

# Instalar dependencias
pip install -r requirements.txt
```

**Espera a que instale** (puede tardar 1-2 minutos)

Deberías ver:

```
Successfully installed Flask-3.0.0 stripe-7.8.0 supabase-2.3.0 ...
```

### 6.5 Ejecutar el servidor

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

 * Serving Flask app 'app'
 * Debug mode: on
WARNING: This is a development server.
 * Running on all addresses (0.0.0.0)
 * Running on http://127.0.0.1:5000
 * Running on http://192.168.1.10:5000
```

✅ **¡El backend está funcionando!**

### 6.6 Probar que funciona

**Abre otra terminal** (deja la del servidor corriendo) y prueba:

```bash
# Windows (PowerShell)
Invoke-WebRequest -Uri http://localhost:5000/ -UseBasicParsing

# Linux/Mac/Git Bash
curl http://localhost:5000/
```

Deberías ver:

```json
{
  "name": "Mandadito API",
  "version": "1.0.0",
  "status": "running",
  ...
}
```

✅ **Backend funcionando correctamente!**

---

## ✅ PASO 7: CONFIGURAR WEBHOOKS DE STRIPE

### 7.1 Instalar Stripe CLI

**Windows (usando Scoop):**

```bash
# Si no tienes Scoop, instálalo primero:
# Abre PowerShell como administrador y ejecuta:
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# Instalar Stripe CLI
scoop install stripe
```

**Windows (sin Scoop) - Descargar manualmente:**

1. Ve a: https://github.com/stripe/stripe-cli/releases/latest
2. Descarga: `stripe_X.X.X_windows_x86_64.zip`
3. Descomprime
4. Mueve `stripe.exe` a `C:\Program Files\stripe\`
5. Agrega `C:\Program Files\stripe\` al PATH

**Mac:**

```bash
brew install stripe/stripe-cli/stripe
```

**Linux:**

```bash
wget https://github.com/stripe/stripe-cli/releases/download/v1.19.0/stripe_1.19.0_linux_x86_64.tar.gz
tar -xvf stripe_1.19.0_linux_x86_64.tar.gz
sudo mv stripe /usr/local/bin
```

### 7.2 Verificar instalación

```bash
stripe --version
```

Deberías ver: `stripe version X.X.X`

### 7.3 Login en Stripe CLI

```bash
stripe login
```

Se abrirá tu navegador para autorizar. Haz clic en **"Allow access"**.

Deberías ver:

```
✔ Done! The Stripe CLI is configured for [tu-email]
```

### 7.4 Escuchar webhooks localmente

**Abre OTRA terminal nueva** (necesitas 3 en total):
- Terminal 1: Backend Flask (corriendo)
- Terminal 2: Comandos generales
- Terminal 3: Stripe CLI (esta)

```bash
stripe listen --forward-to localhost:5000/webhooks/stripe
```

Deberías ver:

```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxxxxxxxxxx
> Waiting for events...
```

### 7.5 Copiar el Webhook Secret

**Copia el `whsec_xxxxxxxxxxxxx`** que aparece en la terminal.

### 7.6 Agregar el Webhook Secret al .env

1. Abre `backend/.env`
2. Busca la línea: `STRIPE_WEBHOOK_SECRET=`
3. Pega tu secret:
   ```env
   STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxx
   ```
4. Guarda el archivo

### 7.7 Reiniciar el servidor Flask

1. Ve a la terminal donde corre Flask
2. Presiona `Ctrl + C` para detenerlo
3. Vuelve a ejecutar:
   ```bash
   python app.py
   ```

✅ **¡Webhooks configurados!**

---

## ✅ PASO 8: CREAR CUENTA STRIPE PARA UN COLMADO

### 8.1 Obtener el ID de un colmado

**En Supabase Dashboard:**

1. Ve a **Table Editor**
2. Selecciona la tabla **"colmados"**
3. Busca un colmado de prueba
4. Copia su **ID** (es un UUID como: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`)

**O desde SQL Editor:**

```sql
SELECT id, name, email FROM colmados LIMIT 1;
```

Copia el `id` que aparece.

### 8.2 Crear la cuenta Stripe Connect

En una terminal, ejecuta este comando **reemplazando** `TU_COLMADO_ID` con el ID real:

**Windows (PowerShell):**

```powershell
$body = @{
    colmado_id = "bdcc7bbf-92a9-4169-ae63-3a633d2f6461"
    email = amaury26@gmail.com"
    business_name = "Colmado Los Amigos"
} | ConvertTo-Json

Invoke-WebRequest -Uri http://localhost:5000/stripe/connect/create `
    -Method POST `
    -Body $body `
    -ContentType "application/json" `
    -UseBasicParsing
```

**Linux/Mac/Git Bash:**

```bash
curl -X POST http://localhost:5000/stripe/connect/create \
  -H "Content-Type: application/json" \
  -d '{
    "colmado_id": "TU_COLMADO_ID_AQUI",
    "email": "seller@test.com",
    "business_name": "Mi Colmado de Prueba"
  }'
```

Deberías ver:

```json
{
  "success": true,
  "account_id": "acct_xxxxxxxxxxxxx",
  "onboarding_url": "https://connect.stripe.com/setup/e/acct_xxxxx/xxxxxxx",
  "message": "Cuenta creada. Completar onboarding."
}
```

### 8.3 Completar el Onboarding

1. **Copia el `onboarding_url`** del response
2. **Pégalo en tu navegador** y presiona Enter
3. Verás un formulario de Stripe

**Completa con estos datos de PRUEBA:**

```
Email: seller@test.com
Tipo de negocio: Individual
Nombre: Juan Pérez
Apellido: González
Fecha de nacimiento: 01/01/1990
Teléfono: +1 829-555-0100

Dirección:
  País: República Dominicana
  Calle: Calle Principal 123
  Ciudad: Santo Domingo
  Código postal: 10101

Información bancaria:
  Número de cuenta: 000123456789
  Código de banco: 110000000
```

⚠️ **NOTA**: En modo TEST, Stripe acepta cualquier número de cuenta.

4. Haz clic en **"Submit"**

5. Deberías ver: **"✅ ¡Onboarding Completado!"**

### 8.4 Verificar que la cuenta está activa

```bash
# Reemplaza TU_COLMADO_ID con el ID real

# PowerShell
$body = @{
    colmado_id = "bdcc7bbf-92a9-4169-ae63-3a633d2f6461"
} | ConvertTo-Json

Invoke-WebRequest -Uri http://localhost:5000/stripe/connect/status `
    -Method POST `
    -Body $body `
    -ContentType "application/json" `
    -UseBasicParsing

# Linux/Mac/Bash
curl -X POST http://localhost:5000/stripe/connect/status \
  -H "Content-Type: application/json" \
  -d '{
    "colmado_id": "TU_COLMADO_ID_AQUI"
  }'
```

Deberías ver:

```json
{
  "success": true,
  "account_id": "acct_xxxxx",
  "onboarding_completed": true,
  "charges_enabled": true,
  "payouts_enabled": true,
  "requirements": []
}
```

✅ **Si `charges_enabled: true`, el colmado YA PUEDE RECIBIR PAGOS!**

---

## ✅ PASO 9: PROBAR MANUALMENTE (SIN ANDROID)

Antes de tocar Android, probemos que todo funciona con comandos:

### 9.1 Crear una orden de prueba

Primero necesitas:
- `user_id`: Tu ID de usuario en Supabase
- `cart_id`: Un carrito existente con productos
- `address_id`: Una dirección de entrega

**Obtener IDs desde Supabase:**

```sql
-- Obtener tu user_id
SELECT id FROM auth.users LIMIT 1;

-- Obtener un cart_id
SELECT id FROM carts WHERE user_id = 'TU_USER_ID' LIMIT 1;

-- Obtener una address_id
SELECT id FROM addresses WHERE user_id = 'TU_USER_ID' LIMIT 1;
```

**Crear la orden:**

```bash
# PowerShell
$body = @{
    user_id = "c429a1be-c6f7-4de3-b9db-f0b063c78fe6"
    cart_id = "1139d1c3-7126-49ab-8639-5f3fd7da47dd"
    address_id = "cb2a9d29-1183-40d7-aaa0-f07f87a6f219"
    delivery_fee = 50.0
    customer_notes = "Sin cebolla por favor"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri http://localhost:5000/orders/create `
    -Method POST `
    -Body $body `
    -ContentType "application/json" `
    -UseBasicParsing

$response.Content | ConvertFrom-Json | ConvertTo-Json

# Bash
curl -X POST http://localhost:5000/orders/create \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "TU_USER_ID",
    "cart_id": "TU_CART_ID",
    "address_id": "TU_ADDRESS_ID",
    "delivery_fee": 50.0,
    "customer_notes": "Sin cebolla por favor"
  }'
```

Deberías ver:

```json
{
  "success": true,
  "order_id": "uuid-de-la-orden",
  "order_number": "ORD-20250121-001",
  "client_secret": "pi_xxxxx_secret_yyyyy",
  "amount": 350.50,
  "message": "Orden creada. Proceder con pago."
}
```

✅ **¡Orden creada!**

### 9.2 Verificar en Supabase

```sql
-- Ver la orden
SELECT * FROM orders ORDER BY created_at DESC LIMIT 1;

-- Ver el pago pendiente
SELECT * FROM payments ORDER BY created_at DESC LIMIT 1;

-- Ver los items
SELECT * FROM order_items
WHERE order_id = 'TU_ORDER_ID';
```

✅ **Si ves los datos, TODO FUNCIONA!**

---

## 🎯 RESUMEN DE LO QUE YA TIENES

Después de estos pasos:

✅ Base de datos configurada en Supabase
✅ Backend Flask corriendo en `http://localhost:5000`
✅ Webhooks de Stripe escuchando
✅ Cuenta Stripe Connect del colmado activa
✅ Sistema probado manualmente

---

## 📱 SIGUIENTE PASO: CONFIGURAR ANDROID

Ahora que el backend funciona, podemos configurar Android.

¿Quieres que te ayude con la configuración de Android ahora?

Te voy a crear un archivo con los pasos específicos para Android...
