-- =====================================================
-- ESQUEMA DE BASE DE DATOS PARA PRODUCTOS Y CATEGORÍAS
-- =====================================================
-- Ejecuta este script en el SQL Editor de Supabase

-- =====================================================
-- PASO 1: CREAR TABLAS
-- =====================================================

-- Tabla de Categorías
CREATE TABLE IF NOT EXISTS public.categories (
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
CREATE TABLE IF NOT EXISTS public.products (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
  stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
  image_url TEXT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Relación Producto-Categoría (muchos a muchos)
CREATE TABLE IF NOT EXISTS public.product_categories (
  product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  category_id UUID NOT NULL REFERENCES public.categories(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (product_id, category_id)
);

-- =====================================================
-- PASO 2: ÍNDICES
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_categories_name ON public.categories(name);
CREATE INDEX IF NOT EXISTS idx_categories_is_active ON public.categories(is_active);
CREATE INDEX IF NOT EXISTS idx_products_name ON public.products(name);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON public.products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_price ON public.products(price);
CREATE INDEX IF NOT EXISTS idx_product_categories_product ON public.product_categories(product_id);
CREATE INDEX IF NOT EXISTS idx_product_categories_category ON public.product_categories(category_id);

-- =====================================================
-- PASO 3: FUNCIONES Y TRIGGERS
-- =====================================================
CREATE OR REPLACE FUNCTION public.update_categories_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION public.update_products_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_categories_updated_at ON public.categories;
CREATE TRIGGER trigger_update_categories_updated_at
  BEFORE UPDATE ON public.categories
  FOR EACH ROW EXECUTE FUNCTION public.update_categories_updated_at();

DROP TRIGGER IF EXISTS trigger_update_products_updated_at ON public.products;
CREATE TRIGGER trigger_update_products_updated_at
  BEFORE UPDATE ON public.products
  FOR EACH ROW EXECUTE FUNCTION public.update_products_updated_at();

-- =====================================================
-- PASO 4: VISTAS
-- =====================================================
CREATE OR REPLACE VIEW public.products_with_categories AS
SELECT
  p.id,
  p.name,
  p.description,
  p.price,
  p.stock,
  p.image_url,
  p.is_active,
  p.created_at,
  p.updated_at,
  COALESCE(
    json_agg(
      json_build_object(
        'id', c.id,
        'name', c.name,
        'description', c.description,
        'icon', c.icon,
        'color', c.color
      )
    ) FILTER (WHERE c.id IS NOT NULL),
    '[]'::json
  ) AS categories
FROM public.products p
LEFT JOIN public.product_categories pc ON p.id = pc.product_id
LEFT JOIN public.categories c ON pc.category_id = c.id
GROUP BY p.id;

-- =====================================================
-- PASO 5: SEGURIDAD (RLS)
-- =====================================================
ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product_categories ENABLE ROW LEVEL SECURITY;

-- Select
CREATE POLICY IF NOT EXISTS "Anyone can view active categories"
  ON public.categories FOR SELECT
  TO public
  USING (is_active = true);

CREATE POLICY IF NOT EXISTS "Authenticated users can view all categories"
  ON public.categories FOR SELECT
  TO authenticated
  USING (true);

CREATE POLICY IF NOT EXISTS "Anyone can view active products"
  ON public.products FOR SELECT
  TO public
  USING (is_active = true);

CREATE POLICY IF NOT EXISTS "Authenticated users can view all products"
  ON public.products FOR SELECT
  TO authenticated
  USING (true);

CREATE POLICY IF NOT EXISTS "Authenticated users can view product categories"
  ON public.product_categories FOR SELECT
  TO authenticated
  USING (true);

-- Insert/Update/Delete (admins + sellers)
CREATE POLICY IF NOT EXISTS "Admins and sellers manage categories"
  ON public.categories FOR ALL
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name IN ('admin','seller')
    )
  );

CREATE POLICY IF NOT EXISTS "Admins and sellers manage products"
  ON public.products FOR ALL
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name IN ('admin','seller')
    )
  );

CREATE POLICY IF NOT EXISTS "Admins and sellers manage product categories"
  ON public.product_categories FOR ALL
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.user_roles ur
      JOIN public.roles r ON ur.role_id = r.id
      WHERE ur.user_id = auth.uid() AND r.name IN ('admin','seller')
    )
  );

-- Service role full access
CREATE POLICY IF NOT EXISTS "Service role full access categories"
  ON public.categories FOR ALL
  TO service_role
  USING (true);

CREATE POLICY IF NOT EXISTS "Service role full access products"
  ON public.products FOR ALL
  TO service_role
  USING (true);

CREATE POLICY IF NOT EXISTS "Service role full access product categories"
  ON public.product_categories FOR ALL
  TO service_role
  USING (true);

-- =====================================================
-- PASO 6: DATOS DE EJEMPLO
-- =====================================================
INSERT INTO public.categories (name, description, icon, color)
VALUES
  ('Bebidas', 'Refrescos, jugos, agua, etc.', '🥤', '#FF6B6B'),
  ('Snacks', 'Papas, galletas, dulces, etc.', '🍿', '#4ECDC4'),
  ('Lácteos', 'Leche, queso, yogurt, etc.', '🥛', '#45B7D1'),
  ('Frutas y Verduras', 'Productos frescos', '🥬', '#96CEB4'),
  ('Carnes', 'Pollo, res, cerdo, etc.', '🥩', '#FFEAA7'),
  ('Panadería', 'Pan, pasteles, etc.', '🍞', '#DDA15E')
ON CONFLICT (name) DO NOTHING;
