# Documentación de APIs - Mandadito App

## Tabla de Contenidos

1. [Visión General](#visión-general)
2. [Autenticación](#autenticación)
3. [Perfiles de Usuario](#perfiles-de-usuario)
4. [colmados](#colmados)
5. [Productos](#productos)
6. [Categorías](#categorías)
7. [Carrito de Compras](#carrito-de-compras)
8. [Pedidos](#pedidos)
9. [Direcciones](#direcciones)
10. [Pagos (Stripe)](#pagos-stripe)
11. [Notificaciones](#notificaciones)
12. [Google Places](#google-places)
13. [Edge Functions](#edge-functions)
14. [Almacenamiento](#almacenamiento)

---

## Visión General

La aplicación Mandadito utiliza **Supabase** como backend principal, proporcionando:
- **Autenticación**: OAuth y autenticación con email/contraseña
- **Base de Datos**: PostgreSQL con Row Level Security (RLS)
- **Storage**: Almacenamiento de archivos (imágenes de productos)
- **Realtime**: Actualizaciones en tiempo real
- **Edge Functions**: Funciones serverless personalizadas

### Configuración Base

```kotlin
// URL Base de Supabase
SUPABASE_URL = "https://[project-id].supabase.co"

// Clave anónima ( pública )
SUPABASE_ANON_KEY = "[anon-key]"
```

### Endpoints Base

```
Auth: https://[project-id].supabase.co/auth/v1
REST: https://[project-id].supabase.co/rest/v1
Storage: https://[project-id].supabase.co/storage/v1
Functions: https://[project-id].supabase.co/functions/v1
```

---

## Autenticación

### Fuente
`app/src/main/java/com/dev/mandadito/data/repository/AuthRepository.kt`

### Endpoints

#### Registro de Usuario
```http
POST /auth/v1/signup
Content-Type: application/json

{
    "email": "usuario@email.com",
    "password": "contraseña123",
    "data": {
        "nombre": "Nombre del Usuario",
        "role": "client"
    }
}
```

**Respuesta Éxito:**
```json
{
    "id": "uuid-usuario",
    "email": "usuario@email.com"
}
```

#### Inicio de Sesión
```http
POST /auth/v1/token?grant_type=password
Content-Type: application/json

{
    "email": "usuario@email.com",
    "password": "contraseña123"
}
```

**Respuesta Éxito:**
```json
{
    "access_token": "jwt-token",
    "refresh_token": "refresh-token",
    "token_type": "bearer",
    "user": {
        "id": "uuid-usuario",
        "email": "usuario@email.com"
    }
}
```

#### Cerrar Sesión
```http
POST /auth/v1/logout
Authorization: Bearer [access-token]
```

#### Crear Usuario como Admin (Edge Function)
```http
POST /functions/v1/create-user
Authorization: Bearer [admin-token]
Content-Type: application/json

{
    "email": "nuevo@email.com",
    "password": "contraseña123",
    "nombre": "Nombre",
    "telefono": "+18091234567",
    "role": "seller",
    "avatar_base64": "data:image/jpeg;base64,..."
}
```

### Funciones RPC

#### Obtener Rol del Usuario
```sql
-- Consulta directa a tablas user_roles y roles
SELECT r.name 
FROM user_roles ur
JOIN roles r ON ur.role_id = r.id
WHERE ur.user_id = 'uuid-usuario';
```

---

## Perfiles de Usuario

### Fuente
`app/src/main/java/com/dev/mandadito/data/repository/ProfileRepository.kt`

### Tabla: `profiles`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID del usuario (PK, FK a auth.users) |
| nombre | VARCHAR(100) | Nombre completo |
| email | VARCHAR(255) | Email del usuario |
| telefono | VARCHAR(20) | Teléfono |
| avatar_url | TEXT | URL de la imagen de perfil |
| activo | BOOLEAN | Estado de la cuenta |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Última actualización |

### Endpoints

#### Obtener Perfil Actual
```http
GET /rest/v1/profiles?id=eq.[user-id]
Authorization: Bearer [token]
```

#### Actualizar Perfil (Edge Function)
```http
POST /functions/v1/update-profile
Authorization: Bearer [token]
Content-Type: application/json

{
    "nombre": "Nuevo Nombre",
    "email": "nuevo@email.com",
    "avatar_base64": "data:image/jpeg;base64,..."
}
```

**Respuesta:**
```json
{
    "success": true,
    "user": {
        "id": "uuid",
        "nombre": "Nuevo Nombre",
        "email": "nuevo@email.com",
        "avatar_url": "https://..."
    },
    "error": null
}
```

#### Cambiar Contraseña
```http
PUT /auth/v1/user
Authorization: Bearer [token]
Content-Type: application/json

{
    "password": "nueva-contraseña"
}
```

---

## Colmados

### Fuente
`app/src/main/java/com/dev/mandadito/data/repository/ColmadosRepository.kt`

### Tabla: `colmados`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID del colmado (PK) |
| seller_id | UUID | ID del vendedor (FK a profiles) |
| name | VARCHAR(100) | Nombre del colmado |
| address | TEXT | Dirección |
| phone | VARCHAR(20) | Teléfono |
| latitude | DOUBLE | Latitud |
| longitude | DOUBLE | Longitud |
| logo_url | TEXT | URL del logo |
| description | TEXT | Descripción |
| delivery_fee | DECIMAL | Costo de delivery |
| delivery_time | INT | Tiempo estimado de entrega (min) |
| is_active | BOOLEAN | Estado activo |
| stripe_ready | BOOLEAN | Stripe configurado |
| stripe_account_id | VARCHAR | ID de cuenta Stripe |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Última actualización |

### Vista: `colmados_with_owner`

Combina información del colmado con los datos del propietario.

### Endpoints

#### Listar Colmados Activos
```http
GET /rest/v1/colmados_with_owner?is_active=eq.true&stripe_ready=eq.true
Authorization: Bearer [token]
```

#### Obtener Colmado por ID
```http
GET /rest/v1/colmados_with_owner?id=eq.[colmado-id]
Authorization: Bearer [token]
```

#### Buscar Colmados
```http
GET /rest/v1/colmados_with_owner
Authorization: Bearer [token]
```
*Los filtros se aplican localmente en el cliente.*

#### Actualizar Colmado
```http
PATCH /rest/v1/colmados?id=eq.[colmado-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "name": "Nuevo Nombre",
    "address": "Nueva Dirección",
    "delivery_fee": 50.00
}
```

#### Desactivar Colmado
```http
PATCH /rest/v1/colmados?id=eq.[colmado-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "is_active": false
}
```

#### Eliminar Colmado
```http
DELETE /rest/v1/colmados?id=eq.[colmado-id]
Authorization: Bearer [token]
```

---

## Productos

### Fuente
`app/src/main/java/com/dev/mandadito/data/repository/ProductRepository.kt`

### Tabla: `products`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID del producto (PK) |
| colmado_id | UUID | ID del colmado (FK) |
| name | VARCHAR(100) | Nombre del producto |
| description | TEXT | Descripción |
| price | DECIMAL | Precio |
| stock | INT | Cantidad en stock |
| min_stock | INT | Stock mínimo para alertas |
| is_active | BOOLEAN | Producto activo |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Última actualización |

### Tabla: `product_images`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID de la imagen (PK) |
| product_id | UUID | ID del producto (FK) |
| image_url | TEXT | URL de la imagen |
| display_order | INT | Orden de visualización |
| is_primary | BOOLEAN | Es imagen principal |

### Tabla: `product_categories`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| product_id | UUID | ID del producto (FK) |
| category_id | UUID | ID de la categoría (FK) |

### Endpoints

#### Listar Todos los Productos
```http
GET /rest/v1/products
Authorization: Bearer [token]
```

#### Obtener Producto por ID
```http
GET /rest/v1/products?id=eq.[product-id]
Authorization: Bearer [token]
```

#### Obtener Imágenes de Producto
```http
GET /rest/v1/product_images?product_id=eq.[product-id]
Authorization: Bearer [token]
Order: display_order
```

#### Crear Producto
```http
POST /rest/v1/products
Authorization: Bearer [token]
Content-Type: application/json

{
    "colmado_id": "uuid-colmado",
    "name": "Producto Nuevo",
    "description": "Descripción",
    "price": 100.00,
    "stock": 50,
    "min_stock": 10,
    "is_active": true
}
```

#### Actualizar Producto
```http
PATCH /rest/v1/products?id=eq.[product-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "name": "Producto Actualizado",
    "price": 120.00,
    "stock": 45
}
```

#### Eliminar Producto
```http
DELETE /rest/v1/products?id=eq.[product-id]
Authorization: Bearer [token]
```

#### Asignar Categoría a Producto
```http
POST /rest/v1/product_categories
Authorization: Bearer [token]
Content-Type: application/json

{
    "product_id": "uuid-producto",
    "category_id": "uuid-categoria"
}
```

---

## Categorías

### Fuente
`app/src/main/java/com/dev/mandadito/data/repository/CategoryRepository.kt`

### Tabla: `categories`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID de la categoría (PK) |
| colmado_id | UUID | ID del colmado (FK) |
| name | VARCHAR(50) | Nombre de la categoría |
| description | TEXT | Descripción |
| icon | VARCHAR(50) | Icono (nombre) |
| color | VARCHAR(7) | Color hex |
| is_active | BOOLEAN | Estado activo |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Última actualización |

### Endpoints

#### Listar Categorías por Colmado
```http
GET /rest/v1/categories?colmado_id=eq.[colmado-id]
Authorization: Bearer [token]
```

#### Listar Categorías Activas
```http
GET /rest/v1/categories?colmado_id=eq.[colmado-id]&is_active=eq.true
Authorization: Bearer [token]
```

#### Obtener Categoría por ID
```http
GET /rest/v1/categories?id=eq.[category-id]
Authorization: Bearer [token]
```

#### Crear Categoría
```http
POST /rest/v1/categories
Authorization: Bearer [token]
Content-Type: application/json

{
    "colmado_id": "uuid-colmado",
    "name": "Bebidas",
    "description": "Bebidas y jugos",
    "icon": "local_drink",
    "color": "#FF5722"
}
```

#### Actualizar Categoría
```http
PATCH /rest/v1/categories?id=eq.[category-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "name": "Bebidas Actualizado",
    "color": "#E91E63"
}
```

#### Eliminar Categoría
```http
DELETE /rest/v1/categories?id=eq.[category-id]
Authorization: Bearer [token]
```

---

## Carrito de Compras

### Fuente
`app/src/main/java/comdev/mandadito/data/repository/CartRepository.kt`

### Tabla: `carts`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID del carrito (PK) |
| user_id | UUID | ID del usuario (FK) |
| colmado_id | UUID | ID del colmado (FK) |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Última actualización |

### Tabla: `cart_items`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID del item (PK) |
| cart_id | UUID | ID del carrito (FK) |
| product_id | UUID | ID del producto (FK) |
| quantity | INT | Cantidad |
| price_at_add | DECIMAL | Precio al momento de agregar |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Última actualización |

### Vistas

- `view_cart_summary`: Resumen de carritos por usuario
- `view_cart_items`: Items del carrito con detalles de productos

### Endpoints

#### Ver Resumen de Carritos
```http
GET /rest/v1/view_cart_summary?user_id=eq.[user-id]
Authorization: Bearer [token]
```

#### Ver Items de Carrito
```http
GET /rest/v1/view_cart_items?cart_id=eq.[cart-id]
Authorization: Bearer [token]
```

### Funciones RPC

#### Agregar al Carrito
```sql
SELECT add_to_cart(
    product_uuid => 'uuid-producto',
    user_uuid => 'uuid-usuario'
);
```

#### Actualizar Cantidad
```sql
SELECT update_cart_quantity(
    item_uuid => 'uuid-item',
    new_qty => 3
);
```

#### Eliminar del Carrito
```sql
SELECT remove_from_cart(
    item_uuid => 'uuid-item'
);
```

#### Vaciar Carrito
```http
DELETE /rest/v1/cart_items?cart_id=eq.[cart-id]
Authorization: Bearer [token]
```

---

## Pedidos

### Fuente
`app/src/main/java/com/dev/mandadito/data/repository/OrderRepository.kt`

### Tabla: `orders`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID del pedido (PK) |
| order_number | VARCHAR(20) | Número de orden |
| user_id | UUID | ID del cliente (FK) |
| colmado_id | UUID | ID del colmado (FK) |
| address_id | UUID | ID de dirección (FK) |
| delivery_user_id | UUID | ID del delivery (FK) |
| status | VARCHAR(20) | Estado (pending, confirmed, in_delivery, delivered, cancelled) |
| subtotal | DECIMAL | Subtotal |
| delivery_fee | DECIMAL | Costo de delivery |
| total | DECIMAL | Total |
| verification_code | VARCHAR(10) | Código de verificación de entrega |
| customer_notes | TEXT | Notas del cliente |
| payment_intent_id | VARCHAR | ID de PaymentIntent de Stripe |
| paid_at | TIMESTAMP | Fecha de pago |
| delivered_at | TIMESTAMP | Fecha de entrega |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Última actualización |

### Tabla: `order_items`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID del item (PK) |
| order_id | UUID | ID del pedido (FK) |
| product_id | UUID | ID del producto (FK) |
| quantity | INT | Cantidad |
| price_at_order | DECIMAL | Precio al momento de ordenar |

### Endpoints

#### Crear Pedido (Edge Function)
```http
POST /functions/v1/orders-create
Authorization: Bearer [token]
Content-Type: application/json

{
    "userId": "uuid-usuario",
    "cartId": "uuid-carrito",
    "addressId": "uuid-direccion",
    "deliveryFee": 50.00,
    "customerNotes": "Llamar al llegar"
}
```

**Respuesta:**
```json
{
    "success": true,
    "orderId": "uuid-pedido",
    "orderNumber": "ORD-2024-001",
    "clientSecret": "pi_xxx_secret_xxx",
    "message": "Orden creada exitosamente"
}
```

#### Obtener Pedidos del Colmado (Edge Function)
```http
GET /functions/v1/orders-get-colmado?colmadoId=[colmado-id]
Authorization: Bearer [token]
```

#### Obtener Pedidos del Delivery (Edge Function)
```http
GET /functions/v1/orders-get-delivery?deliveryUserId=[delivery-user-id]
Authorization: Bearer [token]
```

#### Actualizar Estado del Pedido
```http
PATCH /rest/v1/orders?id=eq.[order-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "status": "in_delivery"
}
```

#### Asignar Delivery
```http
PATCH /rest/v1/orders?id=eq.[order-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "delivery_user_id": "uuid-delivery",
    "status": "in_delivery"
}
```

#### Verificar Código de Entrega
```http
PATCH /rest/v1/orders?id=eq.[order-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "status": "delivered",
    "delivered_at": "2024-01-15T10:30:00Z"
}
```

#### Confirmar Pago (Edge Function)
```http
POST /functions/v1/orders-confirm-payment/[order-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "userId": "uuid-usuario",
    "cartId": "uuid-carrito",
    "paymentIntentId": "pi_xxx"
}
```

#### Cancelar Pedido (Edge Function)
```http
POST /functions/v1/orders-cancel/[order-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "userId": "uuid-usuario",
    "reason": "Cliente canceló"
}
```

---

## Direcciones

### Fuente
`app/src/main/java/com/dev/mandadito/data/repository/AddressRepository.kt`

### Tabla: `addresses`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID de la dirección (PK) |
| user_id | UUID | ID del usuario (FK) |
| name | VARCHAR(100) | Nombre de la dirección |
| street | VARCHAR(200) | Calle y número |
| apartment | VARCHAR(100) | Apartamento/Casa |
| city | VARCHAR(100) | Ciudad |
| postal_code | VARCHAR(20) | Código postal |
| latitude | DOUBLE | Latitud |
| longitude | DOUBLE | Longitud |
| place_id | VARCHAR | ID de Google Place |
| is_default | BOOLEAN | Es dirección predeterminada |
| created_at | TIMESTAMP | Fecha de creación |
| updated_at | TIMESTAMP | Última actualización |

### Endpoints

#### Listar Direcciones del Usuario
```http
GET /rest/v1/addresses?user_id=eq.[user-id]
Authorization: Bearer [token]
Order: is_default.desc, created_at.desc
```

#### Obtener Dirección por ID
```http
GET /rest/v1/addresses?id=eq.[address-id]
Authorization: Bearer [token]
```

#### Crear Dirección
```http
POST /rest/v1/addresses
Authorization: Bearer [token]
Content-Type: application/json

{
    "user_id": "uuid-usuario",
    "name": "Casa",
    "street": "Av. Abraham Lincoln 123",
    "city": "Santo Domingo",
    "latitude": 18.4861,
    "longitude": -69.9312,
    "is_default": true
}
```

#### Actualizar Dirección
```http
PATCH /rest/v1/addresses?id=eq.[address-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "name": "Trabajo",
    "street": "Av. Winston Churchill 456"
}
```

#### Establecer como Predeterminada
```http
PATCH /rest/v1/addresses?user_id=eq.[user-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "is_default": false
}
```

```http
PATCH /rest/v1/addresses?id=eq.[address-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "is_default": true
}
```

#### Eliminar Dirección
```http
DELETE /rest/v1/addresses?id=eq.[address-id]
Authorization: Bearer [token]
```

---

## Pagos (Stripe)

### Fuente
`app/src/main/java/com/dev/mandadito/data/repository/PaymentRepository.kt`
`app/src/main/java/com/dev/mandadito/data/repository/StripeRepository.kt`

### Endpoints (Edge Functions)

#### Obtener Configuración de Stripe
```http
GET /functions/v1/stripe-config
Authorization: Bearer [token]
```

**Respuesta:**
```json
{
    "publishableKey": "pk_test_xxx",
    "merchantId": "merchant.com.mandadito",
    "countryCode": "DO"
}
```

#### Crear Cuenta Stripe Connect
```http
POST /functions/v1/stripe-connect-create
Authorization: Bearer [token]
Content-Type: application/json

{
    "colmadoId": "uuid-colmado",
    "email": "vendedor@email.com",
    "businessName": "Mi Colmado"
}
```

**Respuesta:**
```json
{
    "success": true,
    "accountId": "acct_xxx",
    "onboardingUrl": "https://connect.stripe.com/..."
}
```

#### Verificar Estado de Cuenta Stripe
```http
POST /functions/v1/stripe-connect-status
Authorization: Bearer [token]
Content-Type: application/json

{
    "colmadoId": "uuid-colmado"
}
```

**Respuesta:**
```json
{
    "success": true,
    "accountId": "acct_xxx",
    "onboardingCompleted": true,
    "chargesEnabled": true,
    "payoutsEnabled": true
}
```

#### Refrescar Link de Onboarding
```http
POST /functions/v1/stripe-connect-refresh
Authorization: Bearer [token]
Content-Type: application/json

{
    "colmadoId": "uuid-colmado"
}
```

---

## Notificaciones

### Fuente
`app/src/main/java/com/dev/mandadito/data/repository/NotificationRepository.kt`

### Tabla: `notifications`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID de la notificación (PK) |
| user_id | UUID | ID del usuario (FK) |
| title | VARCHAR(100) | Título |
| message | TEXT | Mensaje |
| type | VARCHAR(50) | Tipo (order, delivery, promo, system) |
| reference_id | UUID | ID de referencia (pedido, etc.) |
| is_read | BOOLEAN | Ha sido leída |
| created_at | TIMESTAMP | Fecha de creación |

### Endpoints

#### Listar Notificaciones del Usuario
```http
GET /rest/v1/notifications?user_id=eq.[user-id]
Authorization: Bearer [token]
Order: created_at.desc
```

#### Marcar como Leída
```http
PATCH /rest/v1/notifications?id=eq.[notification-id]
Authorization: Bearer [token]
Content-Type: application/json

{
    "is_read": true
}
```

#### Marcar Todas como Leídas
```http
PATCH /rest/v1/notifications?user_id=eq.[user-id]&is_read=eq.false
Authorization: Bearer [token]
Content-Type: application/json

{
    "is_read": true
}
```

#### Eliminar Notificación
```http
DELETE /rest/v1/notifications?id=eq.[notification-id]
Authorization: Bearer [token]
```

#### Eliminar Notificaciones Leídas
```http
DELETE /rest/v1/notifications?user_id=eq.[user-id]&is_read=eq.true
Authorization: Bearer [token]
```

---

## Google Places

### Fuente
`app/src/main/java/com/dev/mandadito/data/repository/AddressRepository.kt`

### API de Autocompletado

```http
POST https://maps.googleapis.com/maps/api/place/autocomplete/json
Content-Type: application/json

{
    "input": "Av. Abraham",
    "sessiontoken": "session-uuid",
    "country": "do"
}
```

**Respuesta:**
```json
{
    "predictions": [
        {
            "place_id": "ChIJ...",
            "description": "Av. Abraham Lincoln, Santo Domingo",
            "primary_text": "Av. Abraham Lincoln",
            "secondary_text": "Santo Domingo"
        }
    ]
}
```

### API de Detalles de Lugar

```http
POST https://maps.googleapis.com/maps/api/place/details/json
Content-Type: application/json

{
    "placeid": "ChIJ...",
    "sessiontoken": "session-uuid",
    "fields": ["address_components", "geometry"]
}
```

**Respuesta:**
```json
{
    "result": {
        "formatted_address": "Av. Abraham Lincoln 123, Santo Domingo",
        "geometry": {
            "location": {
                "lat": 18.4861,
                "lng": -69.9312
            }
        },
        "address_components": [...]
    }
}
```

---

## Edge Functions

### Fuente
`app/src/main/java/com/dev/mandadito/config/AppConfig.kt`

### Lista de Edge Functions

| Función | Propósito |
|---------|-----------|
| `create-user` | Crear usuarios con roles (Admin) |
| `update-profile` | Actualizar perfil con avatar |
| `orders-create` | Crear pedidos desde carrito |
| `orders-confirm-payment` | Confirmar pago en backend |
| `orders-cancel` | Cancelar pedido |
| `stripe-config` | Obtener configuración de Stripe |
| `stripe-connect-create` | Crear cuenta Stripe Connect |
| `stripe-connect-status` | Verificar estado de Stripe |
| `stripe-connect-refresh` | Refrescar link de onboarding |

### Headers Requeridos

```http
Authorization: Bearer [access-token]
apikey: [supabase-anon-key]
Content-Type: application/json
```

---

## Almacenamiento

### Fuente
`app/src/main/java/com/dev/mandadito/data/repository/ProductRepository.kt`

### Buckets de Storage

| Bucket | Propósito | Acceso |
|--------|-----------|--------|
| `products` | Imágenes de productos | Público |
| `avatars` | Avatares de usuarios | Público |
| `colmados` | Logos de colmados | Público |

### Endpoints

#### Subir Imagen de Producto
```http
POST /storage/v1/object/products/[product-id]/image_[index].jpg
Authorization: Bearer [service-role-key]
Content-Type: image/jpeg

[body: binary data]
```

#### Obtener URL Pública
```
https://[project-id].supabase.co/storage/v1/object/public/products/[product-id]/image_0.jpg
```

#### Eliminar Imagen
```http
DELETE /storage/v1/object/products/[product-id]/image_[index].jpg
Authorization: Bearer [service-role-key]
```

---

## Roles y Permisos

### Roles del Sistema

| Rol | Descripción | Permisos |
|-----|-------------|----------|
| `admin` | Administrador del sistema | Acceso total |
| `seller` | Vendedor/Dueño de colmado | Gestionar su colmado |
| `delivery` | Repartidor | Ver y aceptar entregas |
| `client` | Cliente | Comprar y ver pedidos |

### Tabla: `roles`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID del rol (PK) |
| name | VARCHAR(20) | Nombre del rol |
| description | TEXT | Descripción |

### Tabla: `user_roles`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| user_id | UUID | ID del usuario (FK) |
| role_id | UUID | ID del rol (FK) |
| created_at | TIMESTAMP | Fecha de asignación |

### Tabla: `user_colmado`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | ID del registro (PK) |
| user_id | UUID | ID del usuario (FK) |
| colmado_id | UUID | ID del colmado (FK) |
| role_in_colmado | VARCHAR | Rol dentro del colmado |

---

## Códigos de Estado de Pedido

| Estado | Descripción |
|--------|-------------|
| `pending` | Pendiente de confirmación |
| `confirmed` | Confirmado por el vendedor |
| `in_delivery` | En proceso de entrega |
| `delivered` | Entregado |
| `cancelled` | Cancelado |

---

## Respuestas de Error Comunes

```json
{
    "error": "Error de conexión. Verifica tu internet"
}
```

```json
{
    "error": "Correo electrónico o contraseña incorrectos"
}
```

```json
{
    "error": "Tu cuenta ha sido bloqueada. Contacta al administrador"
}
```

```json
{
    "error": "No tienes permisos para realizar esta acción"
}
```

---

## Seguridad

### Row Level Security (RLS)

Todas las tablas tienen políticas RLS habilitadas:

- **profiles**: Acceso propio y admin
- **addresses**: Acceso propio del usuario
- **orders**: Acceso propio, seller del colmado, y delivery asignado
- **colmados**: Acceso público (lectura), seller propio (escritura)
- **products**: Acceso público (lectura), seller del colmado (escritura)

### Autenticación

- JWT tokens con tiempo de expiración
- Refresh tokens para renovación automática
- Token almacenado encriptado localmente

---

## Actualizaciones en Tiempo Real

### Fuente
`app/src/main/java/comdev/mandadito/data/network/SupabaseClient.kt`

### Suscripciones Realtime

```kotlin
supabase.realtime.channel("orders")
    .subscribe { status ->
        when (status) {
            is Channel.Status.Subscribed -> {
                // Suscripción exitosa
            }
            is Channel.Status.Unsubscribed -> {
                // Desconectado
            }
        }
    }
```

### Eventos Soportados

- `INSERT`: Nuevos registros
- `UPDATE`: Actualizaciones de registros
- `DELETE`: Eliminación de registros

### Canales Comunes

| Canal | Eventos | Uso |
|-------|---------|-----|
| `orders` | INSERT, UPDATE | Notificaciones de pedidos |
| `notifications` | INSERT | Nuevas notificaciones |
| `products` | UPDATE | Cambios de stock |

---

## Notas de Implementación

### Timeouts

```kotlin
connectTimeout = 30 segundos
readTimeout = 30 segundos
writeTimeout = 30 segundos
```

### Caché

- **Categorías**: 15 minutos TTL
- **Colmados**: 15 minutos TTL
- **Carritos**: 5 minutos TTL
- **Offline Support**: Fallback a Room Database

### Reintentos

- **Auth**: 3 intentos con delay exponencial
- **Perfil**: 3 intentos para verificar creación
- **Rol**: 3 intentos para obtener rol

---

## Documentación Relacionada

- [Supabase Documentation](https://supabase.com/docs)
- [Stripe Connect](https://stripe.com/docs/connect)
- [Google Places API](https://developers.google.com/maps/documentation/places)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Supabase Kotlin SDK](https://supabase.com/docs/guides/getting-started/quick-starts/kotlin)
