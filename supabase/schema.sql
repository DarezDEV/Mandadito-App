-- =====================================================
-- ESQUEMA COMPLETO DE BASE DE DATOS - MANDADITO APP
-- Incluye: Usuarios, Roles, Colmados, Productos, Direcciones, CARRITO
-- =====================================================

-- =====================================================
-- PASO 1: LIMPIEZA COMPLETA
-- =====================================================

-- Deshabilitar RLS temporalmente
ALTER TABLE IF EXISTS public.profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.user_roles DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.roles DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.colmados DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.user_colmado DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.categories DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.products DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.product_categories DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.product_images DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.addresses DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.carts DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.cart_items DISABLE ROW LEVEL SECURITY;

-- Eliminar vistas
DROP VIEW IF EXISTS public.colmados_with_owner CASCADE;
DROP VIEW IF EXISTS public.products_with_categories CASCADE;
DROP VIEW IF EXISTS public.deliveries_view CASCADE;
DROP VIEW IF EXISTS public.view_cart_summary CASCADE;
DROP VIEW IF EXISTS public.view_cart_items CASCADE;

-- Eliminar triggers existentes
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
DROP TRIGGER IF EXISTS on_colmado_deactivation ON public.colmados;
DROP TRIGGER IF EXISTS on_colmado_deletion ON public.colmados;
DROP TRIGGER IF EXISTS trigger_update_colmados_updated_at ON public.colmados;
DROP TRIGGER IF EXISTS trigger_update_categories_updated_at ON public.categories;
DROP TRIGGER IF EXISTS trigger_update_products_updated_at ON public.products;
DROP TRIGGER IF EXISTS trigger_check_max_images ON public.product_images;
DROP TRIGGER IF EXISTS update_addresses_updated_at ON public.addresses;
DROP TRIGGER IF EXISTS trigger_update_carts_updated_at ON public.carts;
DROP TRIGGER IF EXISTS trigger_update_cart_items_updated_at ON public.cart_items;

-- Eliminar funciones existentes
DROP FUNCTION IF EXISTS public.handle_new_user() CASCADE;
DROP FUNCTION IF EXISTS public.handle_colmado_deactivation() CASCADE;
DROP FUNCTION IF EXISTS public.handle_colmado_deletion() CASCADE;
DROP FUNCTION IF EXISTS public.update_colmados_updated_at() CASCADE;
DROP FUNCTION IF EXISTS public.update_categories_updated_at() CASCADE;
DROP FUNCTION IF EXISTS public.update_products_updated_at() CASCADE;
DROP FUNCTION IF EXISTS public.update_updated_at_column() CASCADE;
DROP FUNCTION IF EXISTS public.check_max_product_images() CASCADE;
DROP FUNCTION IF EXISTS public.check_product_has_category() CASCADE;

-- Eliminar políticas existentes
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
        EXECUTE format('DROP POLICY IF EXISTS %I ON public.%I', 'all_policies', r.tablename);
    END LOOP;

    -- Storage policies
    FOR r IN (SELECT policyname FROM pg_policies WHERE schemaname = 'storage' AND tablename = 'objects') LOOP
        EXECUTE 'DROP POLICY IF EXISTS "' || r.policyname || '" ON storage.objects';
    END LOOP;
END $$;

-- Eliminar tablas en orden correcto (por dependencias)
DROP TABLE IF EXISTS public.cart_items CASCADE;
DROP TABLE IF EXISTS public.carts CASCADE;
DROP TABLE IF EXISTS public.product_images CASCADE;
DROP TABLE IF EXISTS public.product_categories CASCADE;
DROP TABLE IF EXISTS public.products CASCADE;
DROP TABLE IF EXISTS public.categories CASCADE;
DROP TABLE IF EXISTS public.addresses CASCADE;
DROP TABLE IF EXISTS public.user_colmado CASCADE;
DROP TABLE IF EXISTS public.colmados CASCADE;
DROP TABLE IF EXISTS public.user_roles CASCADE;
DROP TABLE IF EXISTS public.profiles CASCADE;
DROP TABLE IF EXISTS public.roles CASCADE;

-- =====================================================
-- PASO 2: CREAR TABLAS
-- =====================================================

-- Tabla de roles
CREATE TABLE public.roles (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE CHECK (name IN ('client', 'seller', 'delivery', 'admin'))
);

-- Tabla de perfiles
CREATE TABLE public.profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email TEXT NOT NULL UNIQUE,
  nombre TEXT NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  avatar_url TEXT
);

-- Tabla de relación usuarios-roles
CREATE TABLE public.user_roles (
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  role_id INT NOT NULL REFERENCES public.roles(id) ON DELETE RESTRICT,
  PRIMARY KEY (user_id, role_id)
);

-- Tabla de colmados
CREATE TABLE public.colmados (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  seller_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  address TEXT NOT NULL,
  phone TEXT NOT NULL,
  description TEXT,
  stripe_account_id TEXT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabla de relación usuarios-colmados
CREATE TABLE public.user_colmado (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  colmado_id UUID NOT NULL REFERENCES public.colmados(id) ON DELETE CASCADE,
  role_in_colmado TEXT NOT NULL CHECK (role_in_colmado IN ('owner', 'delivery')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(user_id, colmado_id)
);

-- Tabla de Categorías
CREATE TABLE public.categories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL UNIQUE,
  description TEXT,
  icon TEXT,
  color TEXT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabla de Productos
CREATE TABLE public.products (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
  stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
  image_urls TEXT[] DEFAULT ARRAY[]::TEXT[],
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabla de imágenes de productos
CREATE TABLE public.product_images (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  image_url TEXT NOT NULL,
  display_order INTEGER NOT NULL DEFAULT 0,
  is_primary BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT unique_product_order UNIQUE (product_id, display_order)
);

-- Tabla de relación Producto-Categoría (muchos a muchos)
CREATE TABLE public.product_categories (
  product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  category_id UUID NOT NULL REFERENCES public.categories(id) ON DELETE CASCADE,
  PRIMARY KEY (product_id, category_id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabla de direcciones
CREATE TABLE public.addresses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  phone TEXT NOT NULL,
  formatted_address TEXT NOT NULL,
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  place_id TEXT,
  street TEXT,
  address_extra TEXT,
  city TEXT,
  postal_code TEXT,
  is_manual BOOLEAN DEFAULT FALSE,
  is_default BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tabla de carritos
CREATE TABLE public.carts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  colmado_id UUID NOT NULL REFERENCES public.colmados(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(user_id, colmado_id)
);

-- Tabla de items del carrito
CREATE TABLE public.cart_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  cart_id UUID NOT NULL REFERENCES public.carts(id) ON DELETE CASCADE,
  product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(cart_id, product_id)
);

-- =====================================================
-- PASO 3: CREAR ÍNDICES
-- =====================================================

-- Índices para colmados
CREATE INDEX idx_colmados_seller_id ON public.colmados(seller_id);
CREATE INDEX idx_colmados_is_active ON public.colmados(is_active);

-- Índices para user_colmado
CREATE INDEX idx_user_colmado_user_id ON public.user_colmado(user_id);
CREATE INDEX idx_user_colmado_colmado_id ON public.user_colmado(colmado_id);
CREATE INDEX idx_user_colmado_role ON public.user_colmado(role_in_colmado);
CREATE INDEX idx_user_colmado_lookup ON public.user_colmado(user_id, colmado_id, role_in_colmado);

-- Índices para categorías
CREATE INDEX idx_categories_name ON public.categories(name);
CREATE INDEX idx_categories_is_active ON public.categories(is_active);

-- Índices para productos
CREATE INDEX idx_products_name ON public.products(name);
CREATE INDEX idx_products_is_active ON public.products(is_active);
CREATE INDEX idx_products_price ON public.products(price);

-- Índices para product_categories
CREATE INDEX idx_product_categories_product_id ON public.product_categories(product_id);
CREATE INDEX idx_product_categories_category_id ON public.product_categories(category_id);

-- Índices para product_images
CREATE INDEX idx_product_images_product_id ON public.product_images(product_id);
CREATE INDEX idx_product_images_primary ON public.product_images(product_id, is_primary) WHERE is_primary = TRUE;

-- Índices para user_roles
CREATE INDEX idx_user_roles_role_id ON public.user_roles(role_id);

-- Índices para addresses
CREATE INDEX idx_addresses_user_id ON public.addresses(user_id);
CREATE INDEX idx_addresses_created_at ON public.addresses(created_at DESC);
CREATE INDEX idx_addresses_user_default ON public.addresses(user_id, is_default) WHERE is_default = TRUE;
CREATE INDEX idx_addresses_location ON public.addresses USING gist(point(longitude, latitude));

-- Índices para carts
CREATE INDEX idx_carts_user_id ON public.carts(user_id);
CREATE INDEX idx_carts_colmado_id ON public.carts(colmado_id);
CREATE INDEX idx_carts_user_colmado ON public.carts(user_id, colmado_id);

-- Índices para cart_items
CREATE INDEX idx_cart_items_cart_id ON public.cart_items(cart_id);
CREATE INDEX idx_cart_items_product_id ON public.cart_items(product_id);
CREATE INDEX idx_cart_items_cart_product ON public.cart_items(cart_id, product_id);

-- =====================================================
-- PASO 4: INSERTAR ROLES POR DEFECTO
-- =====================================================

INSERT INTO public.roles (name) VALUES
  ('client'), ('seller'), ('delivery'), ('admin')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- PASO 5: CREAR FUNCIONES
-- =====================================================

-- Función 1: Crear perfil y asignar rol al registrar usuario
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
SECURITY DEFINER
SET search_path = public, auth
LANGUAGE plpgsql
AS $$
DECLARE
  target_role_id INT;
  user_nombre TEXT;
  role_to_assign TEXT;
BEGIN
  RAISE NOTICE '🚀 handle_new_user() - Nuevo usuario: % (%)', NEW.email, NEW.id;

  user_nombre := COALESCE(
    NEW.raw_user_meta_data->>'nombre',
    NEW.raw_user_meta_data->>'name',
    SPLIT_PART(NEW.email, '@', 1),
    'Usuario'
  );

  INSERT INTO public.profiles (id, email, nombre, activo, created_at)
  VALUES (NEW.id, NEW.email, user_nombre, TRUE, NOW())
  ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        nombre = EXCLUDED.nombre,
        activo = EXCLUDED.activo,
        created_at = EXCLUDED.created_at;

  RAISE NOTICE '✅ Perfil creado/actualizado: %', NEW.email;

  role_to_assign := COALESCE(NEW.raw_user_meta_data->>'role', 'client');
  RAISE NOTICE '🎭 Rol a asignar: %', role_to_assign;

  SELECT id INTO target_role_id FROM public.roles WHERE name = role_to_assign LIMIT 1;
  IF target_role_id IS NULL THEN
    RAISE WARNING '⚠️ Rol "%" no existe, usando "client" como fallback', role_to_assign;
    SELECT id INTO target_role_id FROM public.roles WHERE name = 'client' LIMIT 1;

    IF target_role_id IS NULL THEN
      INSERT INTO public.roles (name) VALUES ('client') RETURNING id INTO target_role_id;
      RAISE NOTICE '✅ Rol "client" creado con id %', target_role_id;
    END IF;
  END IF;

  INSERT INTO public.user_roles (user_id, role_id)
  VALUES (NEW.id, target_role_id)
  ON CONFLICT (user_id, role_id) DO NOTHING;

  RAISE NOTICE '✅ Rol % asignado a % (role_id: %)', role_to_assign, NEW.email, target_role_id;
  RETURN NEW;

EXCEPTION
  WHEN OTHERS THEN
    RAISE WARNING '❌ ERROR EN TRIGGER handle_new_user(): % - %', SQLERRM, SQLSTATE;
    RETURN NEW;
END;
$$;

-- Función 2: Desactivar/activar usuarios al cambiar estado del colmado
CREATE OR REPLACE FUNCTION public.handle_colmado_deactivation()
RETURNS TRIGGER
SECURITY DEFINER
SET search_path = public, auth
LANGUAGE plpgsql
AS $$
BEGIN
  IF OLD.is_active = TRUE AND NEW.is_active = FALSE THEN
    RAISE NOTICE '🚫 Desactivando usuarios del colmado: % (seller: %)', NEW.id, NEW.seller_id;

    UPDATE public.profiles SET activo = FALSE WHERE id = NEW.seller_id;
    RAISE NOTICE '  ✅ Seller desactivado: %', NEW.seller_id;

    UPDATE public.profiles SET activo = FALSE
    WHERE id IN (SELECT user_id FROM public.user_colmado WHERE colmado_id = NEW.id);

    RAISE NOTICE '✅ Todos los usuarios del colmado % desactivados', NEW.id;
  END IF;

  IF OLD.is_active = FALSE AND NEW.is_active = TRUE THEN
    RAISE NOTICE '✅ Activando usuarios del colmado: % (seller: %)', NEW.id, NEW.seller_id;

    UPDATE public.profiles SET activo = TRUE WHERE id = NEW.seller_id;
    RAISE NOTICE '  ✅ Seller activado: %', NEW.seller_id;

    UPDATE public.profiles SET activo = TRUE
    WHERE id IN (SELECT user_id FROM public.user_colmado WHERE colmado_id = NEW.id);

    RAISE NOTICE '✅ Todos los usuarios del colmado % activados', NEW.id;
  END IF;

  RETURN NEW;

EXCEPTION
  WHEN OTHERS THEN
    RAISE WARNING '❌ ERROR en handle_colmado_deactivation: % - %', SQLERRM, SQLSTATE;
    RETURN NEW;
END;
$$;

-- Función 3: Eliminar usuarios de auth.users al eliminar colmado
CREATE OR REPLACE FUNCTION public.handle_colmado_deletion()
RETURNS TRIGGER
SECURITY DEFINER
SET search_path = public, auth
LANGUAGE plpgsql
AS $$
DECLARE
  delivery_user_ids UUID[];
  seller_user_id UUID;
  total_deleted INT := 0;
BEGIN
  RAISE NOTICE '🗑️ Eliminando usuarios del colmado: % (seller: %)', OLD.id, OLD.seller_id;

  seller_user_id := OLD.seller_id;

  SELECT ARRAY_AGG(user_id) INTO delivery_user_ids
  FROM public.user_colmado WHERE colmado_id = OLD.id;

  DELETE FROM public.user_colmado WHERE colmado_id = OLD.id;
  RAISE NOTICE '  ✅ Relaciones en user_colmado eliminadas';

  IF delivery_user_ids IS NOT NULL AND array_length(delivery_user_ids, 1) > 0 THEN
    DELETE FROM auth.users WHERE id = ANY(delivery_user_ids);
    GET DIAGNOSTICS total_deleted = ROW_COUNT;
    RAISE NOTICE '  ✅ % usuarios delivery eliminados de auth.users: %', total_deleted, delivery_user_ids;
  END IF;

  IF seller_user_id IS NOT NULL THEN
    DELETE FROM auth.users WHERE id = seller_user_id;
    GET DIAGNOSTICS total_deleted = ROW_COUNT;
    IF total_deleted > 0 THEN
      RAISE NOTICE '  ✅ Seller eliminado de auth.users: %', seller_user_id;
    ELSE
      RAISE WARNING '  ⚠️  No se pudo eliminar seller: %', seller_user_id;
    END IF;
  END IF;

  RAISE NOTICE '✅ Colmado % y todos sus usuarios eliminados completamente', OLD.id;

  RETURN OLD;

EXCEPTION
  WHEN OTHERS THEN
    RAISE WARNING '❌ ERROR en handle_colmado_deletion: % - %', SQLERRM, SQLSTATE;
    RETURN OLD;
END;
$$;

-- Función 4: Actualizar updated_at genérica
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Función 5: Validar máximo 5 imágenes por producto
CREATE OR REPLACE FUNCTION public.check_max_product_images()
RETURNS TRIGGER AS $$
DECLARE
  image_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO image_count
  FROM public.product_images
  WHERE product_id = NEW.product_id;

  IF image_count >= 5 THEN
    RAISE EXCEPTION 'Un producto no puede tener más de 5 imágenes';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- PASO 6: CREAR TRIGGERS
-- =====================================================

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_user();

CREATE TRIGGER on_colmado_deactivation
  AFTER UPDATE ON public.colmados
  FOR EACH ROW
  WHEN (OLD.is_active IS DISTINCT FROM NEW.is_active)
  EXECUTE FUNCTION public.handle_colmado_deactivation();

CREATE TRIGGER on_colmado_deletion
  BEFORE DELETE ON public.colmados
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_colmado_deletion();

CREATE TRIGGER trigger_update_colmados_updated_at
  BEFORE UPDATE ON public.colmados
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER trigger_update_categories_updated_at
  BEFORE UPDATE ON public.categories
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER trigger_update_products_updated_at
  BEFORE UPDATE ON public.products
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER trigger_check_max_images
  BEFORE INSERT ON public.product_images
  FOR EACH ROW
  EXECUTE FUNCTION public.check_max_product_images();

CREATE TRIGGER update_addresses_updated_at
    BEFORE UPDATE ON public.addresses
    FOR EACH ROW
    EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER trigger_update_carts_updated_at
  BEFORE UPDATE ON public.carts
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER trigger_update_cart_items_updated_at
  BEFORE UPDATE ON public.cart_items
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

-- =====================================================
-- PASO 7: CREAR VISTAS
-- =====================================================

-- Vista: Colmados con información del dueño
CREATE OR REPLACE VIEW public.colmados_with_owner AS
SELECT
  c.*,
  p.nombre as owner_name,
  p.email as owner_email,
  p.activo as owner_activo,
  (SELECT COUNT(*) FROM public.user_colmado uc WHERE uc.colmado_id = c.id) as total_users
FROM public.colmados c
LEFT JOIN public.profiles p ON c.seller_id = p.id;

-- Vista: Productos con categorías e imágenes
CREATE OR REPLACE VIEW public.products_with_categories AS
SELECT
  p.id,
  p.name,
  p.description,
  p.price,
  p.stock,
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

-- Vista: Deliveries con su información
CREATE OR REPLACE VIEW public.deliveries_view AS
SELECT
    p.id,
    p.email,
    p.nombre,
    p.activo,
    p.avatar_url,
    uc.colmado_id,
    uc.role_in_colmado,
    uc.created_at as assigned_at
FROM
    public.profiles p
INNER JOIN
    public.user_roles ur ON p.id = ur.user_id
INNER JOIN
    public.roles r ON ur.role_id = r.id
LEFT JOIN
    public.user_colmado uc ON p.id = uc.user_id
WHERE
    r.name = 'delivery';

-- ========== VISTAS DEL CARRITO ==========

-- Vista: Resumen del carrito por usuario y colmado
CREATE OR REPLACE VIEW public.view_cart_summary AS
SELECT
    c.id as cart_id,
    c.user_id,
    c.colmado_id,
    col.name as colmado_name,
    col.address as colmado_address,
    col.description as colmado_description,
    COUNT(ci.id)::INTEGER as total_products,
    COALESCE(SUM(p.price * ci.quantity), 0)::DOUBLE PRECISION as subtotal,
    c.created_at,
    c.updated_at
FROM
    public.carts c
INNER JOIN
    public.colmados col ON c.colmado_id = col.id
LEFT JOIN
    public.cart_items ci ON c.id = ci.cart_id
LEFT JOIN
    public.products p ON ci.product_id = p.id
GROUP BY
    c.id, c.user_id, c.colmado_id, col.name, col.address, col.description, c.created_at, c.updated_at;

-- Vista: Items del carrito con detalles del producto
CREATE OR REPLACE VIEW public.view_cart_items AS
SELECT
    ci.id as cart_item_id,
    ci.cart_id,
    ci.product_id,
    p.name as product_name,
    p.price,
    p.image_urls,
    ci.quantity,
    (p.price * ci.quantity)::DOUBLE PRECISION as total
FROM
    public.cart_items ci
INNER JOIN
    public.products p ON ci.product_id = p.id;

-- =====================================================
-- PASO 8: CREAR POLÍTICAS DE SEGURIDAD (RLS)
-- =====================================================

-- ========== PROFILES ==========
CREATE POLICY "authenticated_users_can_view_own_profile" ON public.profiles
  FOR SELECT TO authenticated USING (auth.uid() = id);

CREATE POLICY "authenticated_users_can_update_own_profile" ON public.profiles
  FOR UPDATE TO authenticated USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

CREATE POLICY "authenticated_users_can_insert_own_profile" ON public.profiles
  FOR INSERT TO authenticated WITH CHECK (auth.uid() = id);

CREATE POLICY "admins_can_view_all_profiles" ON public.profiles
  FOR SELECT TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "admins_can_update_all_profiles" ON public.profiles
  FOR UPDATE TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "admins_can_delete_profiles" ON public.profiles
  FOR DELETE TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "service_role_full_access_profiles" ON public.profiles
  FOR ALL TO service_role USING (true) WITH CHECK (true);

CREATE POLICY "anon_can_insert_profiles" ON public.profiles
  FOR INSERT TO anon WITH CHECK (true);

-- ========== USER_ROLES ==========
CREATE POLICY "authenticated_users_can_view_own_roles" ON public.user_roles
  FOR SELECT TO authenticated USING (auth.uid() = user_id);

CREATE POLICY "authenticated_users_can_insert_own_role" ON public.user_roles
  FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

CREATE POLICY "admins_can_view_all_user_roles" ON public.user_roles
  FOR SELECT TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "admins_can_manage_all_user_roles" ON public.user_roles
  FOR ALL TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "service_role_full_access_user_roles" ON public.user_roles
  FOR ALL TO service_role USING (true) WITH CHECK (true);

CREATE POLICY "anon_can_insert_user_roles" ON public.user_roles
  FOR INSERT TO anon WITH CHECK (true);

-- ========== ROLES ==========
CREATE POLICY "authenticated_users_can_view_roles" ON public.roles
  FOR SELECT TO authenticated USING (true);

CREATE POLICY "anon_can_view_roles" ON public.roles
  FOR SELECT TO anon USING (true);

CREATE POLICY "only_admins_can_modify_roles" ON public.roles
  FOR ALL TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "service_role_full_access_roles" ON public.roles
  FOR ALL TO service_role USING (true) WITH CHECK (true);

-- ========== COLMADOS ==========
CREATE POLICY "sellers_can_view_own_colmado"
  ON public.colmados FOR SELECT TO authenticated USING (seller_id = auth.uid());

CREATE POLICY "sellers_can_insert_own_colmado"
  ON public.colmados FOR INSERT TO authenticated WITH CHECK (seller_id = auth.uid());

CREATE POLICY "sellers_can_update_own_colmado"
  ON public.colmados FOR UPDATE TO authenticated USING (seller_id = auth.uid()) WITH CHECK (seller_id = auth.uid());

CREATE POLICY "admins_can_view_all_colmados"
  ON public.colmados FOR SELECT TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "admins_can_manage_all_colmados"
  ON public.colmados FOR ALL TO authenticated USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "service_role_full_access_colmados"
  ON public.colmados FOR ALL TO service_role USING (true) WITH CHECK (true);

CREATE POLICY "anon_can_insert_colmados"
  ON public.colmados FOR INSERT TO anon WITH CHECK (true);

CREATE POLICY "public_can_view_active_colmados"
  ON public.colmados FOR SELECT TO anon, authenticated USING (is_active = true);

-- ========== USER_COLMADO ==========
CREATE POLICY "sellers_can_view_own_colmado_users"
  ON public.user_colmado FOR SELECT TO authenticated
  USING (
    colmado_id IN (
      SELECT id FROM public.colmados WHERE seller_id = auth.uid()
    )
  );

CREATE POLICY "sellers_can_add_deliveries_to_their_colmado"
  ON public.user_colmado FOR INSERT TO authenticated
  WITH CHECK (
    (
      colmado_id IN (
        SELECT colmado_id
        FROM public.user_colmado
        WHERE user_id = auth.uid() AND role_in_colmado = 'owner'
      )
      OR
      colmado_id IN (
        SELECT id FROM public.colmados WHERE seller_id = auth.uid()
      )
    )
    AND role_in_colmado = 'delivery'
  );

CREATE POLICY "sellers_can_view_their_colmado_deliveries"
  ON public.user_colmado FOR SELECT TO authenticated
  USING (
    colmado_id IN (
      SELECT colmado_id
      FROM public.user_colmado
      WHERE user_id = auth.uid() AND role_in_colmado = 'owner'
    )
    OR
    colmado_id IN (
      SELECT id FROM public.colmados WHERE seller_id = auth.uid()
    )
    OR
    user_id = auth.uid()
    OR
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "sellers_can_remove_deliveries_from_their_colmado"
  ON public.user_colmado FOR DELETE TO authenticated
  USING (
    (
      colmado_id IN (
        SELECT colmado_id
        FROM public.user_colmado
        WHERE user_id = auth.uid() AND role_in_colmado = 'owner'
      )
      OR
      colmado_id IN (
        SELECT id FROM public.colmados WHERE seller_id = auth.uid()
      )
    )
    AND role_in_colmado = 'delivery'
  );

CREATE POLICY "admins_can_view_all_colmado_users"
  ON public.user_colmado FOR ALL TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "service_role_full_access_user_colmado"
  ON public.user_colmado FOR ALL TO service_role
  USING (true) WITH CHECK (true);

CREATE POLICY "anon_can_insert_user_colmado"
  ON public.user_colmado FOR INSERT TO anon
  WITH CHECK (true);

-- ========== CATEGORIES ==========
CREATE POLICY "Anyone can view active categories"
  ON public.categories FOR SELECT
  TO public
  USING (is_active = true);

CREATE POLICY "Authenticated users can view all categories"
  ON public.categories FOR SELECT
  TO authenticated
  USING (true);

CREATE POLICY "Admins can manage categories"
  ON public.categories FOR ALL
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "Service role has full access to categories"
  ON public.categories FOR ALL
  TO service_role
  USING (true);

-- ========== PRODUCTS ==========
CREATE POLICY "Anyone can view active products"
  ON public.products FOR SELECT
  TO public
  USING (is_active = true);

CREATE POLICY "Authenticated users can view all products"
  ON public.products FOR SELECT
  TO authenticated
  USING (true);

CREATE POLICY "Admins can manage products"
  ON public.products FOR ALL
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "Service role has full access to products"
  ON public.products FOR ALL
  TO service_role
  USING (true);

-- ========== PRODUCT_CATEGORIES ==========
CREATE POLICY "Authenticated users can view product categories"
  ON public.product_categories FOR SELECT
  TO authenticated
  USING (true);

CREATE POLICY "Admins can manage product categories"
  ON public.product_categories FOR ALL
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name = 'admin'
    )
  );

CREATE POLICY "Service role has full access to product categories"
  ON public.product_categories FOR ALL
  TO service_role
  USING (true);

-- ========== PRODUCT_IMAGES ==========
CREATE POLICY "Anyone can view product images"
  ON public.product_images FOR SELECT
  TO public
  USING (true);

CREATE POLICY "Admins and sellers can manage product images"
  ON public.product_images FOR ALL
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name IN ('admin', 'seller')
    )
  );

CREATE POLICY "Service role has full access to product images"
  ON public.product_images FOR ALL
  TO service_role
  USING (true);

-- ========== ADDRESSES ==========
CREATE POLICY "Users can view own addresses"
    ON public.addresses FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own addresses"
    ON public.addresses FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own addresses"
    ON public.addresses FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own addresses"
    ON public.addresses FOR DELETE
    USING (auth.uid() = user_id);

-- ========== CARTS ==========
CREATE POLICY "Users can view own carts"
  ON public.carts FOR SELECT
  TO authenticated
  USING (user_id = auth.uid());

CREATE POLICY "Users can insert own carts"
  ON public.carts FOR INSERT
  TO authenticated
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update own carts"
  ON public.carts FOR UPDATE
  TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can delete own carts"
  ON public.carts FOR DELETE
  TO authenticated
  USING (user_id = auth.uid());

CREATE POLICY "Service role has full access to carts"
  ON public.carts FOR ALL
  TO service_role
  USING (true)
  WITH CHECK (true);

-- ========== CART_ITEMS ==========
CREATE POLICY "Users can view items from own carts"
  ON public.cart_items FOR SELECT
  TO authenticated
  USING (
    cart_id IN (
      SELECT id FROM public.carts WHERE user_id = auth.uid()
    )
  );

CREATE POLICY "Users can insert items to own carts"
  ON public.cart_items FOR INSERT
  TO authenticated
  WITH CHECK (
    cart_id IN (
      SELECT id FROM public.carts WHERE user_id = auth.uid()
    )
  );

CREATE POLICY "Users can update items from own carts"
  ON public.cart_items FOR UPDATE
  TO authenticated
  USING (
    cart_id IN (
      SELECT id FROM public.carts WHERE user_id = auth.uid()
    )
  )
  WITH CHECK (
    cart_id IN (
      SELECT id FROM public.carts WHERE user_id = auth.uid()
    )
  );

CREATE POLICY "Users can delete items from own carts"
  ON public.cart_items FOR DELETE
  TO authenticated
  USING (
    cart_id IN (
      SELECT id FROM public.carts WHERE user_id = auth.uid()
    )
  );

CREATE POLICY "Service role has full access to cart items"
  ON public.cart_items FOR ALL
  TO service_role
  USING (true)
  WITH CHECK (true);

-- ========== STORAGE: PROFILE PICTURES ==========
CREATE POLICY "Users can upload their own profile picture"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'profile-pictures'
  AND (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "Users can update their own profile picture"
ON storage.objects FOR UPDATE
TO authenticated
USING (
  bucket_id = 'profile-pictures'
  AND (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "Users can delete their own profile picture"
ON storage.objects FOR DELETE
TO authenticated
USING (
  bucket_id = 'profile-pictures'
  AND (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "Public can view profile pictures"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'profile-pictures');

CREATE POLICY "Service role full access profile pictures"
ON storage.objects FOR ALL
TO service_role
USING (bucket_id = 'profile-pictures');

-- ========== STORAGE: PRODUCT IMAGES ==========
CREATE POLICY "Admins and sellers can upload product images"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'products' AND
  EXISTS (
    SELECT 1 FROM public.user_roles ur
    JOIN public.roles r ON ur.role_id = r.id
    WHERE ur.user_id = auth.uid() AND r.name IN ('admin','seller')
  )
);

CREATE POLICY "Admins and sellers can update product images"
ON storage.objects FOR UPDATE
TO authenticated
USING (
  bucket_id = 'products' AND
  EXISTS (
    SELECT 1 FROM public.user_roles ur
    JOIN public.roles r ON ur.role_id = r.id
    WHERE ur.user_id = auth.uid() AND r.name IN ('admin','seller')
  )
);

CREATE POLICY "Admins and sellers can delete product images"
ON storage.objects FOR DELETE
TO authenticated
USING (
  bucket_id = 'products' AND
  EXISTS (
    SELECT 1 FROM public.user_roles ur
    JOIN public.roles r ON ur.role_id = r.id
    WHERE ur.user_id = auth.uid() AND r.name IN ('admin','seller')
  )
);

CREATE POLICY "Anyone can view product images"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'products');

-- =====================================================
-- PASO 9: HABILITAR RLS
-- =====================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.colmados ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_colmado ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_images ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.addresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.carts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cart_items ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- PASO 10: VALIDACIONES Y CONSTRAINTS
-- =====================================================

-- Validar formato de teléfono en addresses
ALTER TABLE public.addresses ADD CONSTRAINT check_phone_format
CHECK (phone ~ '^[0-9+\s\-()]+$');

-- Validar que latitude está en rango válido
ALTER TABLE public.addresses ADD CONSTRAINT check_latitude_range
CHECK (latitude >= -90 AND latitude <= 90);

-- Validar que longitude está en rango válido
ALTER TABLE public.addresses ADD CONSTRAINT check_longitude_range
CHECK (longitude >= -180 AND longitude <= 180);

-- Validar que solo haya una dirección predeterminada por usuario
CREATE UNIQUE INDEX idx_one_default_address_per_user
ON public.addresses(user_id)
WHERE is_default = TRUE;

-- Validar array de imágenes en products (1-5 imágenes)
ALTER TABLE public.products ADD CONSTRAINT check_image_urls_count
CHECK (array_length(image_urls, 1) IS NULL OR array_length(image_urls, 1) BETWEEN 1 AND 5);

-- Validar formato de color en categories (hex color)
ALTER TABLE public.categories ADD CONSTRAINT check_color_format
CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$');

-- Validar formato de teléfono en colmados
ALTER TABLE public.colmados ADD CONSTRAINT check_colmado_phone_format
CHECK (phone ~ '^[0-9+\s\-()]+$');

-- Validar cantidad mínima en cart_items
ALTER TABLE public.cart_items ADD CONSTRAINT check_minimum_quantity
CHECK (quantity > 0);

-- =====================================================
-- PASO 11: COMENTARIOS DE DOCUMENTACIÓN
-- =====================================================

COMMENT ON TABLE public.profiles IS 'Perfiles de usuarios del sistema';
COMMENT ON TABLE public.roles IS 'Roles disponibles: client, seller, delivery, admin';
COMMENT ON TABLE public.user_roles IS 'Relación muchos a muchos entre usuarios y roles';
COMMENT ON TABLE public.colmados IS 'Tiendas/colmados registrados en el sistema';
COMMENT ON TABLE public.user_colmado IS 'Relación entre usuarios y colmados (owner, delivery)';
COMMENT ON TABLE public.categories IS 'Categorías de productos';
COMMENT ON TABLE public.products IS 'Productos disponibles para venta';
COMMENT ON TABLE public.product_categories IS 'Relación muchos a muchos entre productos y categorías';
COMMENT ON TABLE public.product_images IS 'Imágenes de productos (máximo 5 por producto)';
COMMENT ON TABLE public.addresses IS 'Direcciones de entrega de usuarios';
COMMENT ON TABLE public.carts IS 'Carritos de compra por usuario y colmado';
COMMENT ON TABLE public.cart_items IS 'Items/productos en cada carrito';
COMMENT ON VIEW public.view_cart_summary IS 'Vista con resumen de carritos: total de productos y subtotal';
COMMENT ON VIEW public.view_cart_items IS 'Vista con detalles de items del carrito incluyendo info del producto';

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================
