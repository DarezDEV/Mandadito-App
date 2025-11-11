-- =====================================================
-- FIX: Modificar trigger para NO asignar "client" automáticamente
-- cuando se crea desde la función admin (con skip_auto_role en metadata)
-- =====================================================

-- Eliminar el trigger actual
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

-- Modificar la función del trigger para que verifique metadata
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
SECURITY DEFINER
SET search_path = public, auth
LANGUAGE plpgsql
AS $$
DECLARE
  client_role_id INT;
  user_nombre TEXT;
  skip_auto_role BOOLEAN;
BEGIN
  -- Log para debugging
  RAISE NOTICE '🚀 TRIGGER handle_new_user() iniciado - User: % (%)', NEW.email, NEW.id;

  -- Extraer nombre del metadata o email
  user_nombre := COALESCE(
    NEW.raw_user_meta_data->>'nombre',
    NEW.raw_user_meta_data->>'name',
    SPLIT_PART(NEW.email, '@', 1),
    'Usuario'
  );

  -- Verificar si hay metadata que indica que NO debe asignar rol automático
  skip_auto_role := COALESCE(
    (NEW.raw_user_meta_data->>'skip_auto_role')::boolean,
    false
  );

  -- Crear/actualizar perfil
  INSERT INTO public.profiles (id, email, nombre, activo, created_at)
  VALUES (
    NEW.id,
    NEW.email,
    user_nombre,
    TRUE,
    NOW()
  )
  ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    nombre = EXCLUDED.nombre,
    activo = EXCLUDED.activo,
    created_at = EXCLUDED.created_at;

  RAISE NOTICE '✅ PERFIL CREADO/ACTUALIZADO: %', NEW.email;

  -- SOLO asignar rol "client" si NO hay flag skip_auto_role
  -- Esto permite que la función admin asigne roles manualmente
  IF NOT skip_auto_role THEN
    -- Obtener/Crear rol 'client'
    SELECT id INTO client_role_id FROM public.roles WHERE name = 'client' LIMIT 1;
    
    IF client_role_id IS NULL THEN
      INSERT INTO public.roles (name) VALUES ('client') RETURNING id INTO client_role_id;
      RAISE NOTICE '✅ ROL CLIENT CREADO: %', client_role_id;
    END IF;

    -- Asignar rol "client" solo si no existe ya
    INSERT INTO public.user_roles (user_id, role_id)
    VALUES (NEW.id, client_role_id)
    ON CONFLICT (user_id, role_id) DO NOTHING;

    RAISE NOTICE '✅ ROL CLIENT ASIGNADO A: % (role_id: %)', NEW.email, client_role_id;
  ELSE
    RAISE NOTICE '⏭️  SKIP AUTO ROLE: No se asigna "client" automáticamente para %', NEW.email;
  END IF;

  RETURN NEW;
EXCEPTION
  WHEN OTHERS THEN
    RAISE WARNING '❌ ERROR EN TRIGGER handle_new_user(): % - %', SQLERRM, SQLSTATE;
    RETURN NEW;
END;
$$;

-- Recrear el trigger
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_user();

-- Permisos para la función
ALTER FUNCTION public.handle_new_user() OWNER TO postgres;
GRANT EXECUTE ON FUNCTION public.handle_new_user() TO anon, authenticated, service_role;

-- =====================================================
-- FIN DEL FIX
-- =====================================================

