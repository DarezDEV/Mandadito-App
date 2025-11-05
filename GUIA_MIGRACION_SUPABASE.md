# Guía de Migración y Configuración de Supabase

## Resumen de Cambios

Se ha mejorado el sistema de gestión de usuarios en Supabase para permitir que los administradores gestionen completamente los usuarios (crear, editar, eliminar, deshabilitar) y que los usuarios bloqueados no puedan hacer login.

## Cambios Realizados

### 1. SQL Mejorado (`supabase_schema.sql`)

El nuevo SQL incluye:

- **Funciones auxiliares**: `is_admin()` e `is_current_user_admin()` para verificar si un usuario es administrador
- **Políticas RLS completas**: Permiten a los administradores gestionar todos los usuarios
- **Trigger mejorado**: Crea automáticamente el perfil y asigna el rol 'client' por defecto
- **Verificación de cuenta bloqueada**: El código de la app verifica el campo `activo` antes y después del login

### 2. Configuración de Cliente Admin

Se agregó soporte para el cliente admin de Supabase:

- **AppConfig.kt**: Agregada `SUPABASE_SERVICE_ROLE_KEY`
- **build.gradle.kts**: Agregada la lectura de `SUPABASE_SERVICE_ROLE_KEY` desde `local.properties`
- **SupabaseClient.kt**: Agregado `adminClient` que usa la SERVICE_ROLE_KEY

### 3. Código de Login Mejorado

El código en `AuthRepository.kt` ya incluye la verificación de cuenta bloqueada:

- Verifica el campo `activo` antes de autenticar
- Verifica nuevamente después de autenticar
- Muestra el mensaje: "Tu cuenta ha sido bloqueada. Por favor, contacta con tu proveedor para más información."

## Instrucciones de Instalación

### Paso 1: Configurar Supabase

1. Abre tu proyecto en Supabase Dashboard
2. Ve a SQL Editor
3. Ejecuta el contenido completo de `supabase_schema.sql`
4. Verifica que todas las políticas se crearon correctamente

### Paso 2: Configurar SERVICE_ROLE_KEY

1. Ve a Supabase Dashboard → Settings → API
2. Copia la **Service Role Key** (NO la Anon Key)
3. Agrega la siguiente línea a tu archivo `local.properties`:

```properties
SUPABASE_SERVICE_ROLE_KEY=tu_service_role_key_aqui
```

**⚠️ IMPORTANTE**: 
- NUNCA subas el archivo `local.properties` a Git
- NUNCA expongas la SERVICE_ROLE_KEY en código público
- La SERVICE_ROLE_KEY tiene permisos completos sobre tu base de datos

### Paso 3: Verificar Configuración

1. Asegúrate de que tu `local.properties` tenga:
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`
   - `SUPABASE_SERVICE_ROLE_KEY` (nuevo)

2. Reconstruye el proyecto:
   ```bash
   ./gradlew clean build
   ```

## Funcionalidades Implementadas

### Para Administradores

✅ **Crear usuarios**: Pueden crear usuarios con cualquier rol (excepto delivery)
✅ **Editar usuarios**: Pueden actualizar nombre, teléfono, dirección
✅ **Eliminar usuarios**: Pueden eliminar usuarios completamente
✅ **Deshabilitar/Habilitar usuarios**: Pueden bloquear/desbloquear cuentas usando el campo `activo`

### Para Usuarios Bloqueados

✅ **Verificación en login**: Si `activo = false`, el login se rechaza
✅ **Mensaje claro**: "Tu cuenta ha sido bloqueada. Por favor, contacta con tu proveedor para más información."

## Políticas RLS Explicadas

### Perfiles (profiles)

- **Usuarios normales**: Solo pueden leer/actualizar su propio perfil
- **Administradores**: Pueden leer/actualizar/eliminar todos los perfiles

### Roles de Usuario (user_roles)

- **Usuarios normales**: Solo pueden asignarse el rol 'client' a sí mismos
- **Administradores**: Pueden gestionar todos los roles de todos los usuarios

### Roles (roles)

- **Todos los usuarios autenticados**: Pueden leer los roles disponibles

## Verificación de Funcionamiento

### 1. Verificar que el trigger funciona

Crea un nuevo usuario desde la app y verifica que:
- Se crea el perfil automáticamente
- Se asigna el rol 'client' automáticamente

### 2. Verificar políticas de administrador

Como administrador, intenta:
- Ver todos los usuarios
- Crear un nuevo usuario
- Editar un usuario existente
- Deshabilitar un usuario
- Habilitar un usuario deshabilitado
- Eliminar un usuario

### 3. Verificar bloqueo de cuenta

1. Deshabilita un usuario desde el panel de administración
2. Intenta hacer login con ese usuario
3. Debe aparecer el mensaje de cuenta bloqueada

## Troubleshooting

### Error: "Cliente admin no disponible"

- Verifica que `SUPABASE_SERVICE_ROLE_KEY` esté en `local.properties`
- Verifica que la key sea correcta (debe ser la Service Role Key, no la Anon Key)
- Reconstruye el proyecto después de agregar la key

### Error: "permission denied for table profiles"

- Ejecuta el SQL completo de `supabase_schema.sql` nuevamente
- Verifica que las políticas RLS se crearon correctamente
- Verifica que el usuario actual tiene el rol 'admin' en `user_roles`

### El trigger no crea el perfil

- Verifica que el trigger existe: `SELECT * FROM pg_trigger WHERE tgname = 'on_auth_user_created';`
- Verifica que la función existe: `SELECT * FROM pg_proc WHERE proname = 'handle_new_user';`
- Revisa los logs de Supabase para ver errores del trigger

### Usuarios bloqueados pueden hacer login

- Verifica que el código en `AuthRepository.kt` está verificando el campo `activo`
- Verifica que el campo `activo` se está actualizando correctamente en la base de datos
- Verifica que la verificación se hace antes del login (líneas 193-213 en AuthRepository.kt)

## Notas Importantes

1. **Seguridad**: La SERVICE_ROLE_KEY tiene permisos completos. Úsala solo en el cliente admin y nunca la expongas.

2. **RLS**: Todas las tablas tienen RLS activado. Las políticas controlan quién puede hacer qué.

3. **Trigger**: El trigger se ejecuta automáticamente cuando se crea un usuario en `auth.users`. Si falla, el usuario se crea pero el perfil no.

4. **Rol por defecto**: Todos los usuarios nuevos reciben el rol 'client' automáticamente.

5. **Administradores**: Para convertir un usuario en administrador, necesitas asignarle el rol 'admin' manualmente en la base de datos o usar el código de administración.

