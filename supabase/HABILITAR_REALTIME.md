# GUÍA PARA HABILITAR REALTIME EN SUPABASE

## Problema
El sistema de actualizaciones en tiempo real (Realtime) no funciona porque la replicación de Supabase no está habilitada para las tablas de pedidos.

## Solución
Ejecuta el script SQL en tu base de datos de Supabase.

### Método 1: SQL Editor en Supabase Dashboard (Recomendado)

1. Ve a https://supabase.com/dashboard
2. Selecciona tu proyecto
3. En el menú lateral, ve a **SQL Editor**
4. Crea un **New Query**
5. Copia y pega el siguiente código:

```sql
-- Habilitar replicación para la tabla principal de pedidos
ALTER PUBLICATION supabase_realtime ADD TABLE orders;

-- Habilitar replicación para order_items
ALTER PUBLICATION supabase_realtime ADD TABLE order_items;
```

6. Haz clic en **Run** para ejecutar el query

### Método 2: Ejecutar el archivo existente

El archivo `supabase/enable_orders_realtime.sql` ya existe en el proyecto. Puedes ejecutarlo usando:

1. **Con CLI de Supabase**:
   ```bash
   supabase db push --db-url postgresql://USER:PASSWORD@HOST:PORT/postgres
   ```

2. **Copiar el contenido** del archivo y ejecutarlo en el SQL Editor de Supabase

## Verificar que Realtime está habilitado

Después de ejecutar el script, verifica que funcionó ejecutando:

```sql
-- Verificar qué tablas tienen replicación habilitada
SELECT * FROM pg_publication_tables WHERE pubname = 'supabase_realtime';
```

Deberías ver `orders` y `order_items` en la lista.

## Qué funcionará después de habilitar Realtime

✅ **Cliente**: Verá actualizaciones de sus pedidos en tiempo real
   - Cuando el vendedor cambia el estado a "Preparando"
   - Cuando se asigna un delivery
   - Cuando el pedido es entregado

✅ **Vendedor**: Verá nuevos pedidos y cambios de estado en tiempo real
   - Cuando un cliente crea un pedido nuevo
   - Actualización automática de la lista de pedidos

✅ **Delivery**: Verá asignaciones de pedidos en tiempo real
   - Cuando un vendedor le asigna un pedido
   - Actualizaciones del estado del pedido

## Si Realtime sigue sin funcionar después de esto

1. **Reinicia la aplicación** en el dispositivo/emulador
2. **Verifica los logs** de la aplicación - deberías ver mensajes como:
   - `🔴 Configurando Realtime para cliente: [user-id]`
   - `📡 Evento Realtime: Insert/Update/Delete`
   - `✅ Realtime activo en canal: [channel-id]`

3. **Verifica la conexión a internet** en el dispositivo

4. **Verifica que el usuario está autenticado** - Realtime requiere un usuario autenticado

## Logs útiles para debugging

Los ViewModels de la aplicación tienen logs detallados para Realtime:

### ClientOrdersViewModel
```
ClientOrdersViewModel: 🔴 Configurando Realtime para cliente: [user-id]
ClientOrdersViewModel: ✅ Realtime activo en canal: client_orders_[timestamp]
ClientOrdersViewModel: 📡 Evento Realtime: Update
```

### SellerOrdersViewModel
```
SellerOrdersViewModel: 🔴 Configurando Realtime para colmado: [colmado-id]
SellerOrdersViewModel: ✅ Realtime activo en canal: seller_orders_[timestamp]
SellerOrdersViewModel: 📡 Evento Realtime: Insert
```

### DeliveryOrdersViewModel
```
DeliveryOrdersViewModel: 🔴 Iniciando suscripción Realtime para delivery: [user-id]
DeliveryOrdersViewModel: ✅ Suscripción Realtime activa en canal: orders_delivery_[user-id]
DeliveryOrdersViewModel: 📡 Cambio detectado en orders: Update
```

## Notas Técnicas

- Los ViewModels ya tienen toda la lógica de suscripción a Realtime implementada
- Se suscriben a TODOS los cambios en la tabla `orders` y filtran los eventos relevantes en Kotlin
- Los canales se crean con nombres únicos para evitar conflictos
- Las suscripciones se limpian automáticamente cuando el ViewModel se destruye (onCleared)
- La configuración de Supabase ya incluye el módulo de Realtime en `SupabaseClient.kt`

## Problemas Comunes

### "Realtime no funciona inmediatamente"
- Después de habilitar la replicación, puede tardar unos segundos hasta 1 minuto en empezar a funcionar
- Es normal debido al tiempo de propagación en Supabase

### "Solo funciona a veces"
- Verifica la estabilidad de tu conexión a internet
- Los websockets de Realtime requieren una conexión estable

### "Los logs no aparecen"
- Verifica que el usuario está autenticado (SupabaseClient.client.auth.currentUserOrNull()?.id)
- Verifica que el ViewModel se está inicializando correctamente

## Documentación Oficial

- [Supabase Realtime - PostgreSQL Changes](https://supabase.com/docs/guides/realtime/postgres-changes)
- [Supabase Kotlin - Realtime](https://github.com/supabase-community/supabase-kt#realtime)
