-- ==========================================
-- SCRIPT PARA ELIMINAR LA COLUMNA 'direccion'
-- ==========================================
-- Ejecuta este script en el SQL Editor de Supabase para eliminar
-- la columna 'direccion' de la tabla 'profiles'

-- 1. Eliminar la columna direccion de la tabla profiles
ALTER TABLE public.profiles DROP COLUMN IF EXISTS direccion;

-- 2. Verificar que la columna fue eliminada
-- SELECT column_name, data_type 
-- FROM information_schema.columns 
-- WHERE table_schema = 'public' 
--   AND table_name = 'profiles';

