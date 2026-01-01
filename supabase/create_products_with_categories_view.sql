-- ================================================================
-- CREAR LA VISTA products_with_categories
-- ================================================================
-- Esta vista combina productos con sus categorías e imágenes
-- ================================================================

-- 1. Eliminar la vista si existe (para recrearla limpia)
DROP VIEW IF EXISTS public.products_with_categories CASCADE;

-- 2. Crear la vista
CREATE OR REPLACE VIEW public.products_with_categories AS
SELECT
  p.id,
  p.colmado_id,
  p.name,
  p.description,
  p.price,
  p.stock,
  p.min_stock,
  p.is_active,
  p.created_at,
  p.updated_at,
  COALESCE(
    (SELECT ARRAY_AGG(
      json_build_object(
        'id', pi.id,
        'url', pi.image_url,
        'order', pi.display_order,
        'is_primary', COALESCE(pi.is_primary, false)
      ) ORDER BY pi.display_order
    )
    FROM public.product_images pi
    WHERE pi.product_id = p.id),
    ARRAY[]::json[]
  ) as images,
  (SELECT pi.image_url
   FROM public.product_images pi
   WHERE pi.product_id = p.id
   ORDER BY pi.is_primary DESC, pi.display_order ASC
   LIMIT 1
  ) as image_url,
  COALESCE(
    (SELECT ARRAY_AGG(
      json_build_object(
        'id', c.id,
        'colmado_id', c.colmado_id,
        'name', c.name,
        'description', c.description,
        'icon', c.icon,
        'color', c.color,
        'is_active', c.is_active,
        'created_at', c.created_at,
        'updated_at', c.updated_at
      )
    )
    FROM public.categories c
    INNER JOIN public.product_categories pc ON c.id = pc.category_id
    WHERE pc.product_id = p.id),
    ARRAY[]::json[]
  ) as categories
FROM public.products p;

-- 3. Habilitar RLS en la vista
ALTER VIEW public.products_with_categories SET (security_invoker = on);

-- 4. Recargar el schema cache
NOTIFY pgrst, 'reload schema';

-- ================================================================
-- VERIFICAR QUE LA VISTA SE CREÓ CORRECTAMENTE
-- ================================================================

-- Ver todas las vistas disponibles
SELECT schemaname, viewname
FROM pg_views
WHERE schemaname = 'public'
  AND viewname = 'products_with_categories';

-- Ver el contenido de la vista (debe mostrar tu producto)
SELECT * FROM public.products_with_categories LIMIT 5;

-- ================================================================
-- ✅ Si ves tu producto con sus categorías, ¡está listo!
-- ================================================================
