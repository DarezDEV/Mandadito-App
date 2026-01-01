-- ================================================================
-- RECARGAR EL SCHEMA CACHE DE SUPABASE
-- ================================================================
-- Ejecuta esto en el SQL Editor de Supabase para forzar la
-- recarga del cache del esquema de PostgREST
-- ================================================================

-- Notificar a PostgREST que recargue el schema
NOTIFY pgrst, 'reload schema';

-- También puedes hacer esto manualmente en el Dashboard:
-- 1. Ve a Settings → API
-- 2. Busca "Reload Schema" o "Schema Cache"
-- 3. Haz clic en "Reload" o "Refresh"

-- ================================================================
-- VERIFICAR QUE LA COLUMNA EXISTE:
-- ================================================================

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'categories'
ORDER BY ordinal_position;

-- ================================================================
-- Deberías ver estas columnas:
-- - id (uuid)
-- - colmado_id (uuid) ← ESTA ES LA IMPORTANTE
-- - name (text)
-- - description (text)
-- - icon (text)
-- - color (text)
-- - is_active (boolean)
-- - created_at (timestamp with time zone)
-- - updated_at (timestamp with time zone)
-- ================================================================
