-- =====================================================
-- SCHEMA MAESTRO COMPLETO - MANDADITO APP
-- VERSIÓN LIMPIA Y FUNCIONAL
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
ALTER TABLE IF EXISTS public.notifications DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.stripe_accounts DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.orders DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.order_items DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.payments DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.order_status_history DISABLE ROW LEVEL SECURITY;

-- Eliminar vistas
DROP VIEW IF EXISTS public.colmados_with_owner CASCADE;
DROP VIEW IF EXISTS public.products_with_categories CASCADE;
DROP VIEW IF EXISTS public.deliveries_view CASCADE;
DROP VIEW IF EXISTS public.view_cart_summary CASCADE;
DROP VIEW IF EXISTS public.view_cart_items CASCADE;
DROP VIEW IF EXISTS public.orders_full CASCADE;

-- Eliminar triggers
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
DROP TRIGGER IF EXISTS trigger_update_stripe_accounts_updated_at ON public.stripe_accounts;
DROP TRIGGER IF EXISTS trigger_update_orders_updated_at ON public.orders;
DROP TRIGGER IF EXISTS trigger_update_payments_updated_at ON public.payments;
DROP TRIGGER IF EXISTS trigger_generate_order_number ON public.orders;
DROP TRIGGER IF EXISTS trigger_track_order_status_change ON public.orders;

-- Eliminar funciones
DROP FUNCTION IF EXISTS public.handle_new_user() CASCADE;
DROP FUNCTION IF EXISTS public.handle_colmado_deactivation() CASCADE;
DROP FUNCTION IF EXISTS public.handle_colmado_deletion() CASCADE;
DROP FUNCTION IF EXISTS public.update_updated_at_column() CASCADE;
DROP FUNCTION IF EXISTS public.check_max_product_images() CASCADE;
DROP FUNCTION IF EXISTS public.check_product_has_images() CASCADE;
DROP FUNCTION IF EXISTS public.check_product_has_category() CASCADE;
DROP FUNCTION IF EXISTS public.generate_order_number() CASCADE;
DROP FUNCTION IF EXISTS public.track_order_status_change() CASCADE;

-- Eliminar TODAS las políticas de TODAS las tablas
DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP
      EXECUTE 'DROP POLICY IF EXISTS ALL ON public.' || quote_ident(r.tablename);
      FOR r IN (SELECT policyname, tablename FROM pg_policies WHERE schemaname = 'public') LOOP
          EXECUTE 'DROP POLICY IF EXISTS ' || quote_ident(r.policyname) || ' ON public.' || quote_ident(r.tablename);
      END LOOP;
  END LOOP;

  -- Storage policies
  FOR r IN (SELECT policyname FROM pg_policies WHERE schemaname = 'storage' AND tablename = 'objects') LOOP
      EXECUTE 'DROP POLICY IF EXISTS ' || quote_ident(r.policyname) || ' ON storage.objects';
  END LOOP;
END $$;

-- =====================================================
-- PASO 2: CREAR TABLAS
-- =====================================================

-- Tabla de roles
CREATE TABLE IF NOT EXISTS public.roles (
id SERIAL PRIMARY KEY,
name TEXT NOT NULL UNIQUE CHECK (name IN ('client', 'seller', 'delivery', 'admin'))
);

-- Tabla de perfiles
CREATE TABLE IF NOT EXISTS public.profiles (
id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
email TEXT NOT NULL UNIQUE,
nombre TEXT NOT NULL,
activo BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
avatar_url TEXT
);

-- Tabla de relación usuarios-roles
CREATE TABLE IF NOT EXISTS public.user_roles (
user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
role_id INT NOT NULL REFERENCES public.roles(id) ON DELETE RESTRICT,
PRIMARY KEY (user_id, role_id)
);

-- Tabla de colmados
CREATE TABLE IF NOT EXISTS public.colmados (
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
CREATE TABLE IF NOT EXISTS public.user_colmado (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
colmado_id UUID NOT NULL REFERENCES public.colmados(id) ON DELETE CASCADE,
role_in_colmado TEXT NOT NULL CHECK (role_in_colmado IN ('owner', 'delivery')),
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
UNIQUE(user_id, colmado_id)
);

-- Tabla de Categorías (por colmado)
CREATE TABLE IF NOT EXISTS public.categories (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
colmado_id UUID NOT NULL REFERENCES public.colmados(id) ON DELETE CASCADE,
name TEXT NOT NULL,
description TEXT,
icon TEXT,
color TEXT,
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
UNIQUE(name, colmado_id) -- Nombre único por colmado
);

-- 🔥 Tabla de Productos CON colmado_id y min_stock
CREATE TABLE IF NOT EXISTS public.products (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
colmado_id UUID NOT NULL REFERENCES public.colmados(id) ON DELETE CASCADE,
name TEXT NOT NULL,
description TEXT,
price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
min_stock INTEGER NOT NULL DEFAULT 0 CHECK (min_stock >= 0),
image_urls TEXT[] DEFAULT ARRAY[]::TEXT[],
is_active BOOLEAN NOT NULL DEFAULT TRUE,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabla de imágenes de productos
CREATE TABLE IF NOT EXISTS public.product_images (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
image_url TEXT NOT NULL,
display_order INTEGER NOT NULL DEFAULT 0,
is_primary BOOLEAN DEFAULT FALSE,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
CONSTRAINT unique_product_order UNIQUE (product_id, display_order)
);

-- Tabla de relación Producto-Categoría
CREATE TABLE IF NOT EXISTS public.product_categories (
product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
category_id UUID NOT NULL REFERENCES public.categories(id) ON DELETE CASCADE,
PRIMARY KEY (product_id, category_id),
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabla de direcciones
CREATE TABLE IF NOT EXISTS public.addresses (
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
CREATE TABLE IF NOT EXISTS public.carts (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
colmado_id UUID NOT NULL REFERENCES public.colmados(id) ON DELETE CASCADE,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
UNIQUE(user_id, colmado_id)
);

-- Tabla de items del carrito
CREATE TABLE IF NOT EXISTS public.cart_items (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
cart_id UUID NOT NULL REFERENCES public.carts(id) ON DELETE CASCADE,
product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
UNIQUE(cart_id, product_id)
);

-- Tabla de notificaciones
CREATE TABLE IF NOT EXISTS public.notifications (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
title TEXT NOT NULL,
message TEXT NOT NULL,
type TEXT NOT NULL,
is_read BOOLEAN NOT NULL DEFAULT FALSE,
is_push BOOLEAN NOT NULL DEFAULT FALSE,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabla de cuentas Stripe
CREATE TABLE IF NOT EXISTS public.stripe_accounts (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
colmado_id UUID NOT NULL UNIQUE REFERENCES public.colmados(id) ON DELETE CASCADE,
stripe_account_id TEXT NOT NULL UNIQUE,
onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
charges_enabled BOOLEAN NOT NULL DEFAULT FALSE,
payouts_enabled BOOLEAN NOT NULL DEFAULT FALSE,
onboarding_url TEXT,
onboarding_expires_at TIMESTAMPTZ,
country TEXT DEFAULT 'DO',
currency TEXT DEFAULT 'DOP',
business_type TEXT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabla de órdenes
CREATE TABLE IF NOT EXISTS public.orders (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
colmado_id UUID NOT NULL REFERENCES public.colmados(id) ON DELETE RESTRICT,
address_id UUID NOT NULL REFERENCES public.addresses(id) ON DELETE RESTRICT,
delivery_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
order_number TEXT NOT NULL UNIQUE,
status TEXT NOT NULL DEFAULT 'pending' CHECK (
  status IN ('pending', 'payment_processing', 'paid', 'preparing', 'ready_for_pickup', 'in_delivery', 'delivered', 'cancelled', 'refunded')
),
subtotal DOUBLE PRECISION NOT NULL CHECK (subtotal >= 0),
delivery_fee DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (delivery_fee >= 0),
platform_fee DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (platform_fee >= 0),
total DOUBLE PRECISION NOT NULL CHECK (total >= 0),
customer_notes TEXT,
delivery_notes TEXT,
cancellation_reason TEXT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
paid_at TIMESTAMPTZ,
delivered_at TIMESTAMPTZ,
cancelled_at TIMESTAMPTZ
);

-- Tabla de items de órdenes
CREATE TABLE IF NOT EXISTS public.order_items (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE RESTRICT,
product_name TEXT NOT NULL,
product_price DOUBLE PRECISION NOT NULL CHECK (product_price >= 0),
product_image_url TEXT,
quantity INTEGER NOT NULL CHECK (quantity > 0),
subtotal DOUBLE PRECISION NOT NULL CHECK (subtotal >= 0),
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabla de pagos
CREATE TABLE IF NOT EXISTS public.payments (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
colmado_id UUID NOT NULL REFERENCES public.colmados(id) ON DELETE RESTRICT,
stripe_account_id UUID REFERENCES public.stripe_accounts(id) ON DELETE SET NULL,
stripe_payment_intent_id TEXT NOT NULL UNIQUE,
stripe_charge_id TEXT UNIQUE,
stripe_transfer_id TEXT UNIQUE,
amount INTEGER NOT NULL CHECK (amount > 0),
amount_captured INTEGER DEFAULT 0,
platform_fee_amount INTEGER DEFAULT 0,
transfer_amount INTEGER DEFAULT 0,
currency TEXT NOT NULL DEFAULT 'DOP',
status TEXT NOT NULL DEFAULT 'pending' CHECK (
  status IN ('pending', 'processing', 'requires_action', 'succeeded', 'failed', 'cancelled', 'refunded')
),
payment_method_type TEXT,
card_brand TEXT,
card_last4 TEXT,
error_code TEXT,
error_message TEXT,
client_secret TEXT,
receipt_url TEXT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
succeeded_at TIMESTAMPTZ,
failed_at TIMESTAMPTZ
);

-- Tabla de historial de estados
CREATE TABLE IF NOT EXISTS public.order_status_history (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
from_status TEXT,
to_status TEXT NOT NULL,
changed_by_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
notes TEXT,
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =====================================================
-- PASO 3: CREAR ÍNDICES
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_colmados_seller_id ON public.colmados(seller_id);
CREATE INDEX IF NOT EXISTS idx_colmados_is_active ON public.colmados(is_active);
CREATE INDEX IF NOT EXISTS idx_user_colmado_user_id ON public.user_colmado(user_id);
CREATE INDEX IF NOT EXISTS idx_user_colmado_colmado_id ON public.user_colmado(colmado_id);
CREATE INDEX IF NOT EXISTS idx_user_colmado_role ON public.user_colmado(role_in_colmado);
CREATE INDEX IF NOT EXISTS idx_user_colmado_lookup ON public.user_colmado(user_id, colmado_id, role_in_colmado);
CREATE INDEX IF NOT EXISTS idx_categories_name ON public.categories(name);
CREATE INDEX IF NOT EXISTS idx_categories_is_active ON public.categories(is_active);
CREATE INDEX IF NOT EXISTS idx_products_colmado_id ON public.products(colmado_id);
CREATE INDEX IF NOT EXISTS idx_products_name ON public.products(name);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON public.products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_price ON public.products(price);
CREATE INDEX IF NOT EXISTS idx_product_categories_product_id ON public.product_categories(product_id);
CREATE INDEX IF NOT EXISTS idx_product_categories_category_id ON public.product_categories(category_id);
CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON public.product_images(product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_primary ON public.product_images(product_id, is_primary) WHERE is_primary = TRUE;
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON public.user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_addresses_user_id ON public.addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_addresses_created_at ON public.addresses(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_addresses_user_default ON public.addresses(user_id, is_default) WHERE is_default = TRUE;
CREATE INDEX IF NOT EXISTS idx_carts_user_id ON public.carts(user_id);
CREATE INDEX IF NOT EXISTS idx_carts_colmado_id ON public.carts(colmado_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON public.cart_items(cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id ON public.cart_items(product_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON public.notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON public.notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON public.notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stripe_accounts_stripe_id ON public.stripe_accounts(stripe_account_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON public.orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_colmado_id ON public.orders(colmado_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON public.orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_delivery_user_id ON public.orders(delivery_user_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON public.orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON public.order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON public.order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON public.payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON public.payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_stripe_payment_intent_id ON public.payments(stripe_payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON public.payments(status);
CREATE INDEX IF NOT EXISTS idx_order_status_history_order_id ON public.order_status_history(order_id);

-- =====================================================
-- PASO 4: INSERTAR ROLES POR DEFECTO
-- =====================================================

INSERT INTO public.roles (name) VALUES ('client'), ('seller'), ('delivery'), ('admin')
ON CONFLICT (name) DO NOTHING;

-- =====================================================
-- PASO 5: CREAR FUNCIONES
-- =====================================================

-- Función genérica para updated_at
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Función para crear perfil al registrarse
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
      nombre = EXCLUDED.nombre;

role_to_assign := COALESCE(NEW.raw_user_meta_data->>'role', 'client');

SELECT id INTO target_role_id FROM public.roles WHERE name = role_to_assign LIMIT 1;
IF target_role_id IS NULL THEN
  SELECT id INTO target_role_id FROM public.roles WHERE name = 'client' LIMIT 1;
END IF;

INSERT INTO public.user_roles (user_id, role_id)
VALUES (NEW.id, target_role_id)
ON CONFLICT (user_id, role_id) DO NOTHING;

RETURN NEW;
END;
$$;

-- Función para activar/desactivar usuarios al cambiar estado del colmado
CREATE OR REPLACE FUNCTION public.handle_colmado_deactivation()
RETURNS TRIGGER
SECURITY DEFINER
SET search_path = public, auth
LANGUAGE plpgsql
AS $$
BEGIN
IF OLD.is_active = TRUE AND NEW.is_active = FALSE THEN
  UPDATE public.profiles SET activo = FALSE WHERE id = NEW.seller_id;
  UPDATE public.profiles SET activo = FALSE
  WHERE id IN (SELECT user_id FROM public.user_colmado WHERE colmado_id = NEW.id);
END IF;

IF OLD.is_active = FALSE AND NEW.is_active = TRUE THEN
  UPDATE public.profiles SET activo = TRUE WHERE id = NEW.seller_id;
  UPDATE public.profiles SET activo = TRUE
  WHERE id IN (SELECT user_id FROM public.user_colmado WHERE colmado_id = NEW.id);
END IF;

RETURN NEW;
END;
$$;

-- Función para eliminar usuarios al eliminar colmado
CREATE OR REPLACE FUNCTION public.handle_colmado_deletion()
RETURNS TRIGGER
SECURITY DEFINER
SET search_path = public, auth
LANGUAGE plpgsql
AS $$
DECLARE
delivery_user_ids UUID[];
seller_user_id UUID;
BEGIN
seller_user_id := OLD.seller_id;

SELECT ARRAY_AGG(user_id) INTO delivery_user_ids
FROM public.user_colmado WHERE colmado_id = OLD.id;

DELETE FROM public.user_colmado WHERE colmado_id = OLD.id;

IF delivery_user_ids IS NOT NULL AND array_length(delivery_user_ids, 1) > 0 THEN
  DELETE FROM auth.users WHERE id = ANY(delivery_user_ids);
END IF;

IF seller_user_id IS NOT NULL THEN
  DELETE FROM auth.users WHERE id = seller_user_id;
END IF;

RETURN OLD;
END;
$$;

-- Función para validar máximo 5 imágenes
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

-- Función para generar order_number
CREATE OR REPLACE FUNCTION public.generate_order_number()
RETURNS TRIGGER AS $$
DECLARE
date_part TEXT;
sequence_num INTEGER;
new_order_number TEXT;
BEGIN
date_part := TO_CHAR(NOW(), 'YYYYMMDD');

SELECT COUNT(*) + 1 INTO sequence_num
FROM public.orders
WHERE order_number LIKE 'ORD-' || date_part || '-%';

new_order_number := 'ORD-' || date_part || '-' || LPAD(sequence_num::TEXT, 3, '0');

NEW.order_number := new_order_number;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Función para tracking de cambios de estado
CREATE OR REPLACE FUNCTION public.track_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
IF OLD.status IS DISTINCT FROM NEW.status THEN
  INSERT INTO public.order_status_history (order_id, from_status, to_status, changed_by_user_id)
  VALUES (NEW.id, OLD.status, NEW.status, auth.uid());
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

CREATE TRIGGER trigger_update_stripe_accounts_updated_at
BEFORE UPDATE ON public.stripe_accounts
FOR EACH ROW
EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER trigger_update_orders_updated_at
BEFORE UPDATE ON public.orders
FOR EACH ROW
EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER trigger_update_payments_updated_at
BEFORE UPDATE ON public.payments
FOR EACH ROW
EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER trigger_generate_order_number
BEFORE INSERT ON public.orders
FOR EACH ROW
WHEN (NEW.order_number IS NULL)
EXECUTE FUNCTION public.generate_order_number();

CREATE TRIGGER trigger_track_order_status_change
AFTER UPDATE ON public.orders
FOR EACH ROW
EXECUTE FUNCTION public.track_order_status_change();

-- =====================================================
-- PASO 7: CREAR VISTAS
-- =====================================================

CREATE OR REPLACE VIEW public.colmados_with_owner AS
SELECT
c.*,
p.nombre as owner_name,
p.email as owner_email,
p.activo as owner_activo,
(SELECT COUNT(*) FROM public.user_colmado uc WHERE uc.colmado_id = c.id) as total_users
FROM public.colmados c
LEFT JOIN public.profiles p ON c.seller_id = p.id;

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

CREATE OR REPLACE VIEW public.orders_full AS
SELECT
o.id,
o.order_number,
o.status,
o.subtotal,
o.delivery_fee,
o.platform_fee,
o.total,
o.created_at,
o.paid_at,
o.delivered_at,
o.user_id,
up.nombre as customer_name,
up.email as customer_email,
a.phone as customer_phone,
o.colmado_id,
c.name as colmado_name,
c.address as colmado_address,
c.phone as colmado_phone,
o.address_id,
a.street as delivery_address,
a.city as delivery_city,
o.delivery_user_id,
ud.nombre as delivery_name,
p.stripe_payment_intent_id,
p.status as payment_status,
p.card_brand,
p.card_last4
FROM public.orders o
LEFT JOIN public.profiles up ON o.user_id = up.id
LEFT JOIN public.colmados c ON o.colmado_id = c.id
LEFT JOIN public.addresses a ON o.address_id = a.id
LEFT JOIN public.profiles ud ON o.delivery_user_id = ud.id
LEFT JOIN public.payments p ON o.id = p.order_id;

-- =====================================================
-- PASO 8: POLÍTICAS RLS
-- =====================================================

-- PROFILES
CREATE POLICY "authenticated_users_can_view_own_profile" ON public.profiles
FOR SELECT TO authenticated USING (auth.uid() = id);
CREATE POLICY "authenticated_users_can_update_own_profile" ON public.profiles
FOR UPDATE TO authenticated USING (auth.uid() = id) WITH CHECK (auth.uid() = id);
CREATE POLICY "authenticated_users_can_insert_own_profile" ON public.profiles
FOR INSERT TO authenticated WITH CHECK (auth.uid() = id);
CREATE POLICY "service_role_full_access_profiles" ON public.profiles
FOR ALL TO service_role USING (true) WITH CHECK (true);
CREATE POLICY "anon_can_insert_profiles" ON public.profiles
FOR INSERT TO anon WITH CHECK (true);

-- USER_ROLES
CREATE POLICY "authenticated_users_can_view_own_roles" ON public.user_roles
FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "authenticated_users_can_insert_own_role" ON public.user_roles
FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "service_role_full_access_user_roles" ON public.user_roles
FOR ALL TO service_role USING (true) WITH CHECK (true);
CREATE POLICY "anon_can_insert_user_roles" ON public.user_roles
FOR INSERT TO anon WITH CHECK (true);

-- ROLES
CREATE POLICY "authenticated_users_can_view_roles" ON public.roles
FOR SELECT TO authenticated USING (true);
CREATE POLICY "anon_can_view_roles" ON public.roles
FOR SELECT TO anon USING (true);
CREATE POLICY "service_role_full_access_roles" ON public.roles
FOR ALL TO service_role USING (true) WITH CHECK (true);

-- COLMADOS
CREATE POLICY "sellers_can_view_own_colmado" ON public.colmados
FOR SELECT TO authenticated USING (seller_id = auth.uid());
CREATE POLICY "sellers_can_insert_own_colmado" ON public.colmados
FOR INSERT TO authenticated WITH CHECK (seller_id = auth.uid());
CREATE POLICY "sellers_can_update_own_colmado" ON public.colmados
FOR UPDATE TO authenticated USING (seller_id = auth.uid()) WITH CHECK (seller_id = auth.uid());
CREATE POLICY "service_role_full_access_colmados" ON public.colmados
FOR ALL TO service_role USING (true) WITH CHECK (true);
CREATE POLICY "anon_can_insert_colmados" ON public.colmados
FOR INSERT TO anon WITH CHECK (true);
CREATE POLICY "public_can_view_active_colmados" ON public.colmados
FOR SELECT TO anon, authenticated USING (is_active = true);

-- USER_COLMADO
CREATE POLICY "sellers_can_view_own_colmado_users" ON public.user_colmado
FOR SELECT TO authenticated
USING (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()));
CREATE POLICY "sellers_can_add_deliveries_to_their_colmado" ON public.user_colmado
FOR INSERT TO authenticated
WITH CHECK (
  colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid())
  AND role_in_colmado = 'delivery'
);
CREATE POLICY "sellers_can_remove_deliveries_from_their_colmado" ON public.user_colmado
FOR DELETE TO authenticated
USING (
  colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid())
  AND role_in_colmado = 'delivery'
);
CREATE POLICY "service_role_full_access_user_colmado" ON public.user_colmado
FOR ALL TO service_role USING (true) WITH CHECK (true);
CREATE POLICY "anon_can_insert_user_colmado" ON public.user_colmado
FOR INSERT TO anon WITH CHECK (true);

-- CATEGORIES
CREATE POLICY "Anyone can view active categories" ON public.categories
FOR SELECT TO public USING (is_active = true);
CREATE POLICY "Authenticated users can view all categories" ON public.categories
FOR SELECT TO authenticated USING (true);
CREATE POLICY "Sellers can create categories for their colmados" ON public.categories
FOR INSERT TO authenticated
WITH CHECK (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()));
CREATE POLICY "Sellers can update their colmado categories" ON public.categories
FOR UPDATE TO authenticated
USING (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()))
WITH CHECK (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()));
CREATE POLICY "Sellers can delete their colmado categories" ON public.categories
FOR DELETE TO authenticated
USING (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()));
CREATE POLICY "Service role has full access to categories" ON public.categories
FOR ALL TO service_role USING (true);

-- PRODUCTS
CREATE POLICY "Anyone can view active products" ON public.products
FOR SELECT TO public USING (is_active = true);
CREATE POLICY "Authenticated users can view all products" ON public.products
FOR SELECT TO authenticated USING (true);
CREATE POLICY "Sellers can insert products to their colmados" ON public.products
FOR INSERT TO authenticated
WITH CHECK (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()));
CREATE POLICY "Sellers can update products from their colmados" ON public.products
FOR UPDATE TO authenticated
USING (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()))
WITH CHECK (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()));
CREATE POLICY "Sellers can delete products from their colmados" ON public.products
FOR DELETE TO authenticated
USING (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()));
CREATE POLICY "Service role has full access to products" ON public.products
FOR ALL TO service_role USING (true);

-- PRODUCT_CATEGORIES
CREATE POLICY "Authenticated users can view product categories" ON public.product_categories
FOR SELECT TO authenticated USING (true);
CREATE POLICY "Service role has full access to product categories" ON public.product_categories
FOR ALL TO service_role USING (true);

-- PRODUCT_IMAGES
CREATE POLICY "Anyone can view product images" ON public.product_images
FOR SELECT TO public USING (true);
CREATE POLICY "Admins and sellers can manage product images" ON public.product_images
FOR ALL TO authenticated
USING (EXISTS (SELECT 1 FROM public.user_roles ur JOIN public.roles r ON ur.role_id = r.id WHERE ur.user_id = auth.uid() AND r.name IN
('admin', 'seller')));
CREATE POLICY "Service role has full access to product images" ON public.product_images
FOR ALL TO service_role USING (true);

-- ADDRESSES
CREATE POLICY "Users can view own addresses" ON public.addresses
FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own addresses" ON public.addresses
FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own addresses" ON public.addresses
FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete own addresses" ON public.addresses
FOR DELETE USING (auth.uid() = user_id);

-- CARTS
CREATE POLICY "Users can view own carts" ON public.carts
FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "Users can insert own carts" ON public.carts
FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
CREATE POLICY "Users can update own carts" ON public.carts
FOR UPDATE TO authenticated USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY "Users can delete own carts" ON public.carts
FOR DELETE TO authenticated USING (user_id = auth.uid());
CREATE POLICY "Service role has full access to carts" ON public.carts
FOR ALL TO service_role USING (true) WITH CHECK (true);

-- CART_ITEMS
CREATE POLICY "Users can view items from own carts" ON public.cart_items
FOR SELECT TO authenticated
USING (cart_id IN (SELECT id FROM public.carts WHERE user_id = auth.uid()));
CREATE POLICY "Users can insert items to own carts" ON public.cart_items
FOR INSERT TO authenticated
WITH CHECK (cart_id IN (SELECT id FROM public.carts WHERE user_id = auth.uid()));
CREATE POLICY "Users can update items from own carts" ON public.cart_items
FOR UPDATE TO authenticated
USING (cart_id IN (SELECT id FROM public.carts WHERE user_id = auth.uid()))
WITH CHECK (cart_id IN (SELECT id FROM public.carts WHERE user_id = auth.uid()));
CREATE POLICY "Users can delete items from own carts" ON public.cart_items
FOR DELETE TO authenticated
USING (cart_id IN (SELECT id FROM public.carts WHERE user_id = auth.uid()));
CREATE POLICY "Service role has full access to cart items" ON public.cart_items
FOR ALL TO service_role USING (true) WITH CHECK (true);

-- NOTIFICATIONS
CREATE POLICY "Users can view own notifications" ON public.notifications
FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users can update own notifications" ON public.notifications
FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Service role can manage notifications" ON public.notifications
FOR ALL TO service_role USING (true) WITH CHECK (true);

-- STRIPE_ACCOUNTS
CREATE POLICY "Sellers can view own stripe account" ON public.stripe_accounts
FOR SELECT TO authenticated
USING (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()));
CREATE POLICY "Service role can manage stripe accounts" ON public.stripe_accounts
FOR ALL TO service_role USING (true) WITH CHECK (true);

-- ORDERS
CREATE POLICY "Customers can view own orders" ON public.orders
FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "Sellers can view colmado orders" ON public.orders
FOR SELECT TO authenticated
USING (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()));
CREATE POLICY "Customers can create orders" ON public.orders
FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
CREATE POLICY "Service role can manage orders" ON public.orders
FOR ALL TO service_role USING (true) WITH CHECK (true);

-- ORDER_ITEMS
CREATE POLICY "Customers can view own order items" ON public.order_items
FOR SELECT TO authenticated
USING (order_id IN (SELECT id FROM public.orders WHERE user_id = auth.uid()));
CREATE POLICY "Sellers can view colmado order items" ON public.order_items
FOR SELECT TO authenticated
USING (order_id IN (SELECT o.id FROM public.orders o WHERE o.colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid())));
CREATE POLICY "Service role can manage order items" ON public.order_items
FOR ALL TO service_role USING (true) WITH CHECK (true);

-- PAYMENTS
CREATE POLICY "Customers can view own payments" ON public.payments
FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY "Sellers can view colmado payments" ON public.payments
FOR SELECT TO authenticated
USING (colmado_id IN (SELECT id FROM public.colmados WHERE seller_id = auth.uid()));
CREATE POLICY "Service role can manage payments" ON public.payments
FOR ALL TO service_role USING (true) WITH CHECK (true);

-- ORDER_STATUS_HISTORY
CREATE POLICY "Customers can view own order history" ON public.order_status_history
FOR SELECT TO authenticated
USING (order_id IN (SELECT id FROM public.orders WHERE user_id = auth.uid()));
CREATE POLICY "Service role can view order history" ON public.order_status_history
FOR SELECT TO service_role USING (true);

-- STORAGE POLICIES
CREATE POLICY "Users can upload their own profile picture" ON storage.objects
FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'profile-pictures' AND (storage.foldername(name))[1] = auth.uid()::text);
CREATE POLICY "Users can update their own profile picture" ON storage.objects
FOR UPDATE TO authenticated
USING (bucket_id = 'profile-pictures' AND (storage.foldername(name))[1] = auth.uid()::text);
CREATE POLICY "Users can delete their own profile picture" ON storage.objects
FOR DELETE TO authenticated
USING (bucket_id = 'profile-pictures' AND (storage.foldername(name))[1] = auth.uid()::text);
CREATE POLICY "Public can view profile pictures" ON storage.objects
FOR SELECT TO public USING (bucket_id = 'profile-pictures');
CREATE POLICY "Admins and sellers can upload product images" ON storage.objects
FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'products' AND EXISTS (SELECT 1 FROM public.user_roles ur JOIN public.roles r ON ur.role_id = r.id WHERE ur.user_id =
auth.uid() AND r.name IN ('admin','seller')));
CREATE POLICY "Anyone can view product images" ON storage.objects
FOR SELECT TO public USING (bucket_id = 'products');

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
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stripe_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_status_history ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================
