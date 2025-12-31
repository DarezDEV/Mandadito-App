-- ================================================================
-- ACTUALIZAR VISTA colmados_with_owner PARA INCLUIR STRIPE
-- ================================================================
-- Esta vista ahora incluirá información sobre si el colmado
-- tiene Stripe configurado y listo para recibir pagos
-- ================================================================

-- 1. Eliminar la vista anterior
DROP VIEW IF EXISTS public.colmados_with_owner CASCADE;

-- 2. Crear la vista actualizada con información de Stripe
CREATE OR REPLACE VIEW public.colmados_with_owner AS
SELECT
  -- Columnas de la tabla colmados (especificadas manualmente para evitar duplicados)
  c.id,
  c.seller_id,
  c.name,
  c.address,
  c.phone,
  c.description,
  c.is_active,
  c.created_at,
  c.updated_at,
  -- Información del dueño (profile)
  p.nombre as owner_name,
  p.email as owner_email,
  p.activo as owner_activo,
  -- Conteo de usuarios
  (SELECT COUNT(*) FROM public.user_colmado uc WHERE uc.colmado_id = c.id) as total_users,
  -- Información de Stripe (desde stripe_accounts)
  sa.stripe_account_id,
  COALESCE(sa.onboarding_completed, false) as stripe_onboarding_completed,
  COALESCE(sa.charges_enabled, false) as stripe_charges_enabled,
  COALESCE(sa.payouts_enabled, false) as stripe_payouts_enabled,
  -- Indica si el colmado está listo para recibir pagos (tiene Stripe completo)
  (sa.onboarding_completed = true AND sa.charges_enabled = true) as stripe_ready
FROM public.colmados c
LEFT JOIN public.profiles p ON c.seller_id = p.id
LEFT JOIN public.stripe_accounts sa ON c.id = sa.colmado_id;

-- 3. Recargar el schema cache
NOTIFY pgrst, 'reload schema';

-- ================================================================
-- VERIFICAR QUE LA VISTA SE CREÓ CORRECTAMENTE
-- ================================================================

-- Ver la estructura de la nueva vista
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'colmados_with_owner'
ORDER BY ordinal_position;

-- Ver colmados con estado de Stripe
SELECT
  id,
  name,
  is_active,
  stripe_account_id,
  stripe_onboarding_completed,
  stripe_charges_enabled,
  stripe_ready
FROM public.colmados_with_owner
LIMIT 10;

-- ================================================================
-- NOTA IMPORTANTE:
-- ================================================================
-- Los clientes solo verán colmados donde stripe_ready = true
-- Los sellers verán un mensaje pidiendo configurar Stripe si
-- stripe_ready = false
-- ================================================================
