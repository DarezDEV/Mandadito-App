-- ================================================================
-- FIX: Agregar colmado_id a la tabla categories
-- ================================================================
-- Este script agrega la columna colmado_id que falta en la tabla categories
-- Ejecuta esto en el SQL Editor de Supabase
-- ================================================================

-- 1. Agregar la columna colmado_id (nullable primero)
ALTER TABLE public.categories
ADD COLUMN IF NOT EXISTS colmado_id UUID;

-- 2. Si tienes categorías existentes, necesitas asignarles un colmado_id
-- Opción A: Si NO tienes categorías existentes, puedes hacer la columna NOT NULL directamente
-- Opción B: Si TIENES categorías, primero asígnalas a un colmado específico

-- Ejemplo de cómo asignar categorías existentes a un colmado:
-- UPDATE public.categories
-- SET colmado_id = 'TU-COLMADO-ID-AQUI'
-- WHERE colmado_id IS NULL;

-- 3. Hacer la columna NOT NULL (descomenta después de asignar colmado_id a todas las categorías)
-- ALTER TABLE public.categories
-- ALTER COLUMN colmado_id SET NOT NULL;

-- 4. Agregar foreign key constraint
ALTER TABLE public.categories
ADD CONSTRAINT IF NOT EXISTS categories_colmado_id_fkey
FOREIGN KEY (colmado_id) REFERENCES public.colmados(id) ON DELETE CASCADE;

-- 5. Agregar índice para mejorar rendimiento
CREATE INDEX IF NOT EXISTS idx_categories_colmado_id
ON public.categories(colmado_id);

-- 6. Remover el constraint UNIQUE del nombre (si existe) ya que ahora el nombre puede repetirse entre colmados diferentes
ALTER TABLE public.categories
DROP CONSTRAINT IF EXISTS categories_name_key;

-- 7. Agregar nuevo constraint UNIQUE combinando nombre y colmado_id
ALTER TABLE public.categories
ADD CONSTRAINT IF NOT EXISTS categories_name_colmado_unique
UNIQUE (name, colmado_id);

-- 8. Actualizar RLS policies si es necesario
-- Eliminar políticas viejas
DROP POLICY IF EXISTS "Public categories are viewable by everyone" ON public.categories;
DROP POLICY IF EXISTS "Sellers can create categories" ON public.categories;
DROP POLICY IF EXISTS "Sellers can update their categories" ON public.categories;
DROP POLICY IF EXISTS "Sellers can delete their categories" ON public.categories;

-- Crear nuevas políticas basadas en colmado_id
CREATE POLICY "Anyone can view active categories" ON public.categories
  FOR SELECT
  USING (is_active = true);

CREATE POLICY "Sellers can view all their colmado categories" ON public.categories
  FOR SELECT TO authenticated
  USING (
    colmado_id IN (
      SELECT id FROM public.colmados WHERE seller_id = auth.uid()
    )
  );

CREATE POLICY "Sellers can create categories for their colmados" ON public.categories
  FOR INSERT TO authenticated
  WITH CHECK (
    colmado_id IN (
      SELECT id FROM public.colmados WHERE seller_id = auth.uid()
    )
  );

CREATE POLICY "Sellers can update their colmado categories" ON public.categories
  FOR UPDATE TO authenticated
  USING (
    colmado_id IN (
      SELECT id FROM public.colmados WHERE seller_id = auth.uid()
    )
  )
  WITH CHECK (
    colmado_id IN (
      SELECT id FROM public.colmados WHERE seller_id = auth.uid()
    )
  );

CREATE POLICY "Sellers can delete their colmado categories" ON public.categories
  FOR DELETE TO authenticated
  USING (
    colmado_id IN (
      SELECT id FROM public.colmados WHERE seller_id = auth.uid()
    )
  );

-- ================================================================
-- NOTA IMPORTANTE:
-- ================================================================
-- Si la columna sigue siendo nullable, necesitas ejecutar el paso 3
-- después de asignar un colmado_id a todas las categorías existentes.
--
-- Si NO tienes categorías existentes, puedes ejecutar esto directamente:
-- ALTER TABLE public.categories ALTER COLUMN colmado_id SET NOT NULL;
-- ================================================================
