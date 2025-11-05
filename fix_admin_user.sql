-- ==========================================
-- SCRIPT PARA ARREGLAR USUARIO ADMIN CREADO DESDE SUPABASE
-- ==========================================
-- Ejecuta este script en el SQL Editor de Supabase después de crear un usuario admin
-- Reemplaza 'TU_EMAIL_AQUI' con el email de tu usuario admin

-- 1. Obtener el ID del usuario admin (reemplaza el email)
-- SELECT id, email FROM auth.users WHERE email = 'TU_EMAIL_AQUI';

-- 2. Crear el perfil si no existe (reemplaza el ID y los datos)
-- INSERT INTO public.profiles (id, email, nombre, activo)
-- SELECT 
--   id,
--   email,
--   COALESCE(raw_user_meta_data->>'nombre', split_part(email, '@', 1)),
--   true
-- FROM auth.users
-- WHERE email = 'TU_EMAIL_AQUI'
-- ON CONFLICT (id) DO UPDATE SET
--   activo = true,
--   nombre = COALESCE(EXCLUDED.nombre, profiles.nombre);

-- 3. Obtener el ID del rol 'admin'
-- SELECT id FROM public.roles WHERE name = 'admin';

-- 4. Asignar el rol admin al usuario (reemplaza user_id y role_id)
-- INSERT INTO public.user_roles (user_id, role_id)
-- SELECT 
--   u.id,
--   r.id
-- FROM auth.users u
-- CROSS JOIN public.roles r
-- WHERE u.email = 'TU_EMAIL_AQUI'
--   AND r.name = 'admin'
-- ON CONFLICT (user_id, role_id) DO NOTHING;

-- ==========================================
-- SCRIPT COMPLETO (TODO EN UNO)
-- ==========================================
-- Reemplaza 'TU_EMAIL_AQUI' con el email de tu usuario admin y ejecuta:

DO $$
DECLARE
  v_user_id uuid;
  v_admin_role_id int;
BEGIN
  -- Obtener el ID del usuario
  SELECT id INTO v_user_id
  FROM auth.users
  WHERE email = 'TU_EMAIL_AQUI';
  
  IF v_user_id IS NULL THEN
    RAISE EXCEPTION 'Usuario con email TU_EMAIL_AQUI no encontrado';
  END IF;
  
  -- Crear o actualizar el perfil
  INSERT INTO public.profiles (id, email, nombre, activo)
  SELECT 
    id,
    email,
    COALESCE(raw_user_meta_data->>'nombre', split_part(email, '@', 1)),
    true
  FROM auth.users
  WHERE id = v_user_id
  ON CONFLICT (id) DO UPDATE SET
    activo = true;
  
  -- Obtener el ID del rol admin
  SELECT id INTO v_admin_role_id
  FROM public.roles
  WHERE name = 'admin';
  
  IF v_admin_role_id IS NULL THEN
    RAISE EXCEPTION 'Rol admin no encontrado. Ejecuta el seed de roles primero.';
  END IF;
  
  -- Asignar el rol admin
  INSERT INTO public.user_roles (user_id, role_id)
  VALUES (v_user_id, v_admin_role_id)
  ON CONFLICT (user_id, role_id) DO NOTHING;
  
  RAISE NOTICE 'Usuario admin configurado correctamente. ID: %, Rol: %', v_user_id, v_admin_role_id;
END $$;

