-- =====================================================
-- ESQUEMA DE ÓRDENES Y PAGOS - STRIPE CONNECT
-- =====================================================

-- =====================================================
-- TABLA: stripe_accounts
-- Almacena las cuentas de Stripe Connect de cada colmado
-- =====================================================
CREATE TABLE public.stripe_accounts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  colmado_id UUID NOT NULL REFERENCES public.colmados(id) ON DELETE CASCADE,

  -- IDs de Stripe
  stripe_account_id TEXT NOT NULL UNIQUE, -- acct_xxxxx

  -- Estado del onboarding
  onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
  charges_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  payouts_enabled BOOLEAN NOT NULL DEFAULT FALSE,

  -- URLs de onboarding
  onboarding_url TEXT, -- URL para completar onboarding
  onboarding_expires_at TIMESTAMPTZ,

  -- Metadata
  country TEXT DEFAULT 'DO', -- República Dominicana
  currency TEXT DEFAULT 'DOP',
  business_type TEXT, -- individual, company

  -- Timestamps
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  -- Constraint: un colmado solo tiene una cuenta Stripe
  UNIQUE(colmado_id)
);

-- Índice para búsquedas rápidas por stripe_account_id
CREATE INDEX idx_stripe_accounts_stripe_id ON public.stripe_accounts(stripe_account_id);

-- =====================================================
-- TABLA: orders
-- Almacena los pedidos de los clientes
-- =====================================================
CREATE TABLE public.orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Relaciones
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  colmado_id UUID NOT NULL REFERENCES public.colmados(id) ON DELETE RESTRICT,
  address_id UUID NOT NULL REFERENCES public.addresses(id) ON DELETE RESTRICT,
  delivery_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL, -- Delivery asignado

  -- Números de orden
  order_number TEXT NOT NULL UNIQUE, -- ORD-20250121-001

  -- Estado del pedido
  status TEXT NOT NULL DEFAULT 'pending' CHECK (
    status IN (
      'pending',           -- Esperando pago
      'payment_processing', -- Procesando pago
      'paid',              -- Pagado, esperando preparación
      'preparing',         -- Colmado preparando
      'ready_for_pickup',  -- Listo para recoger
      'in_delivery',       -- En camino
      'delivered',         -- Entregado
      'cancelled',         -- Cancelado
      'refunded'           -- Reembolsado
    )
  ),

  -- Montos (en DOP)
  subtotal DOUBLE PRECISION NOT NULL CHECK (subtotal >= 0),
  delivery_fee DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (delivery_fee >= 0),
  platform_fee DOUBLE PRECISION NOT NULL DEFAULT 0 CHECK (platform_fee >= 0), -- Tu comisión
  total DOUBLE PRECISION NOT NULL CHECK (total >= 0),

  -- Notas
  customer_notes TEXT,
  delivery_notes TEXT,
  cancellation_reason TEXT,

  -- Timestamps
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  paid_at TIMESTAMPTZ,
  delivered_at TIMESTAMPTZ,
  cancelled_at TIMESTAMPTZ
);

-- Índices
CREATE INDEX idx_orders_user_id ON public.orders(user_id);
CREATE INDEX idx_orders_colmado_id ON public.orders(colmado_id);
CREATE INDEX idx_orders_status ON public.orders(status);
CREATE INDEX idx_orders_delivery_user_id ON public.orders(delivery_user_id);
CREATE INDEX idx_orders_created_at ON public.orders(created_at DESC);

-- =====================================================
-- TABLA: order_items
-- Items específicos de cada pedido
-- =====================================================
CREATE TABLE public.order_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Relaciones
  order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
  product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE RESTRICT,

  -- Snapshot del producto al momento de la compra
  product_name TEXT NOT NULL,
  product_price DOUBLE PRECISION NOT NULL CHECK (product_price >= 0),
  product_image_url TEXT,

  -- Cantidad
  quantity INTEGER NOT NULL CHECK (quantity > 0),

  -- Subtotal (precio * cantidad)
  subtotal DOUBLE PRECISION NOT NULL CHECK (subtotal >= 0),

  -- Timestamps
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índices
CREATE INDEX idx_order_items_order_id ON public.order_items(order_id);
CREATE INDEX idx_order_items_product_id ON public.order_items(product_id);

-- =====================================================
-- TABLA: payments
-- Almacena información de pagos con Stripe
-- =====================================================
CREATE TABLE public.payments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  -- Relaciones
  order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  colmado_id UUID NOT NULL REFERENCES public.colmados(id) ON DELETE RESTRICT,
  stripe_account_id UUID REFERENCES public.stripe_accounts(id) ON DELETE SET NULL,

  -- IDs de Stripe
  stripe_payment_intent_id TEXT NOT NULL UNIQUE, -- pi_xxxxx
  stripe_charge_id TEXT UNIQUE, -- ch_xxxxx (se llena después del pago)
  stripe_transfer_id TEXT UNIQUE, -- tr_xxxxx (transferencia al colmado)

  -- Montos (en centavos de DOP)
  amount INTEGER NOT NULL CHECK (amount > 0), -- Monto total en centavos
  amount_captured INTEGER DEFAULT 0, -- Monto capturado
  platform_fee_amount INTEGER DEFAULT 0, -- Tu comisión en centavos
  transfer_amount INTEGER DEFAULT 0, -- Monto transferido al colmado

  -- Moneda
  currency TEXT NOT NULL DEFAULT 'DOP',

  -- Estado del pago
  status TEXT NOT NULL DEFAULT 'pending' CHECK (
    status IN (
      'pending',           -- Esperando pago
      'processing',        -- Procesando
      'requires_action',   -- Requiere acción del usuario (3D Secure)
      'succeeded',         -- Exitoso
      'failed',            -- Falló
      'cancelled',         -- Cancelado
      'refunded'           -- Reembolsado
    )
  ),

  -- Método de pago
  payment_method_type TEXT, -- card, oxxo, etc.
  card_brand TEXT, -- visa, mastercard
  card_last4 TEXT, -- Últimos 4 dígitos

  -- Errores
  error_code TEXT,
  error_message TEXT,

  -- Metadata
  client_secret TEXT, -- Para PaymentSheet en Android
  receipt_url TEXT, -- URL del recibo de Stripe

  -- Timestamps
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  succeeded_at TIMESTAMPTZ,
  failed_at TIMESTAMPTZ
);

-- Índices
CREATE INDEX idx_payments_order_id ON public.payments(order_id);
CREATE INDEX idx_payments_user_id ON public.payments(user_id);
CREATE INDEX idx_payments_stripe_payment_intent_id ON public.payments(stripe_payment_intent_id);
CREATE INDEX idx_payments_status ON public.payments(status);

-- =====================================================
-- TABLA: order_status_history
-- Historial de cambios de estado de las órdenes
-- =====================================================
CREATE TABLE public.order_status_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,

  -- Estado
  from_status TEXT,
  to_status TEXT NOT NULL,

  -- Quién cambió el estado
  changed_by_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,

  -- Notas
  notes TEXT,

  -- Timestamp
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Índice
CREATE INDEX idx_order_status_history_order_id ON public.order_status_history(order_id);

-- =====================================================
-- TRIGGERS: updated_at automático
-- =====================================================

-- Función reutilizable
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger para stripe_accounts
CREATE TRIGGER trigger_update_stripe_accounts_updated_at
  BEFORE UPDATE ON public.stripe_accounts
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

-- Trigger para orders
CREATE TRIGGER trigger_update_orders_updated_at
  BEFORE UPDATE ON public.orders
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

-- Trigger para payments
CREATE TRIGGER trigger_update_payments_updated_at
  BEFORE UPDATE ON public.payments
  FOR EACH ROW
  EXECUTE FUNCTION public.update_updated_at_column();

-- =====================================================
-- TRIGGER: Generar order_number automáticamente
-- =====================================================
CREATE OR REPLACE FUNCTION public.generate_order_number()
RETURNS TRIGGER AS $$
DECLARE
  date_part TEXT;
  sequence_num INTEGER;
  new_order_number TEXT;
BEGIN
  -- Formato: ORD-20250121-001
  date_part := TO_CHAR(NOW(), 'YYYYMMDD');

  -- Contar órdenes del día
  SELECT COUNT(*) + 1 INTO sequence_num
  FROM public.orders
  WHERE order_number LIKE 'ORD-' || date_part || '-%';

  -- Generar número con padding
  new_order_number := 'ORD-' || date_part || '-' || LPAD(sequence_num::TEXT, 3, '0');

  NEW.order_number := new_order_number;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_generate_order_number
  BEFORE INSERT ON public.orders
  FOR EACH ROW
  WHEN (NEW.order_number IS NULL)
  EXECUTE FUNCTION public.generate_order_number();

-- =====================================================
-- TRIGGER: Registrar cambios de estado en el historial
-- =====================================================
CREATE OR REPLACE FUNCTION public.track_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
  IF OLD.status IS DISTINCT FROM NEW.status THEN
    INSERT INTO public.order_status_history (
      order_id,
      from_status,
      to_status,
      changed_by_user_id
    ) VALUES (
      NEW.id,
      OLD.status,
      NEW.status,
      auth.uid() -- Usuario actual de Supabase
    );
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_track_order_status_change
  AFTER UPDATE ON public.orders
  FOR EACH ROW
  EXECUTE FUNCTION public.track_order_status_change();

-- =====================================================
-- ROW LEVEL SECURITY (RLS)
-- =====================================================

-- Habilitar RLS
ALTER TABLE public.stripe_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_status_history ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- POLÍTICAS: stripe_accounts
-- =====================================================

-- Vendedores pueden ver su propia cuenta
CREATE POLICY "Sellers can view own stripe account"
  ON public.stripe_accounts FOR SELECT
  TO authenticated
  USING (
    colmado_id IN (
      SELECT uc.colmado_id FROM public.user_colmado uc
      WHERE uc.user_id = auth.uid()
    )
  );

-- Solo service_role puede crear/actualizar cuentas Stripe (desde backend)
CREATE POLICY "Service role can manage stripe accounts"
  ON public.stripe_accounts FOR ALL
  TO service_role
  USING (true)
  WITH CHECK (true);

-- =====================================================
-- POLÍTICAS: orders
-- =====================================================

-- Clientes pueden ver sus propias órdenes
CREATE POLICY "Customers can view own orders"
  ON public.orders FOR SELECT
  TO authenticated
  USING (user_id = auth.uid());

-- Vendedores pueden ver órdenes de su colmado
CREATE POLICY "Sellers can view colmado orders"
  ON public.orders FOR SELECT
  TO authenticated
  USING (
    colmado_id IN (
      SELECT uc.colmado_id FROM public.user_colmado uc
      WHERE uc.user_id = auth.uid()
    )
  );

-- Deliveries pueden ver órdenes asignadas
CREATE POLICY "Deliveries can view assigned orders"
  ON public.orders FOR SELECT
  TO authenticated
  USING (delivery_user_id = auth.uid());

-- Clientes pueden crear órdenes
CREATE POLICY "Customers can create orders"
  ON public.orders FOR INSERT
  TO authenticated
  WITH CHECK (user_id = auth.uid());

-- Vendedores pueden actualizar estado de sus órdenes
CREATE POLICY "Sellers can update colmado orders"
  ON public.orders FOR UPDATE
  TO authenticated
  USING (
    colmado_id IN (
      SELECT uc.colmado_id FROM public.user_colmado uc
      WHERE uc.user_id = auth.uid()
    )
  );

-- Deliveries pueden actualizar sus órdenes asignadas
CREATE POLICY "Deliveries can update assigned orders"
  ON public.orders FOR UPDATE
  TO authenticated
  USING (delivery_user_id = auth.uid());

-- Service role tiene acceso total (webhooks)
CREATE POLICY "Service role can manage orders"
  ON public.orders FOR ALL
  TO service_role
  USING (true)
  WITH CHECK (true);

-- =====================================================
-- POLÍTICAS: order_items
-- =====================================================

-- Los clientes pueden ver items de sus órdenes
CREATE POLICY "Customers can view own order items"
  ON public.order_items FOR SELECT
  TO authenticated
  USING (
    order_id IN (
      SELECT id FROM public.orders WHERE user_id = auth.uid()
    )
  );

-- Vendedores pueden ver items de órdenes de su colmado
CREATE POLICY "Sellers can view colmado order items"
  ON public.order_items FOR SELECT
  TO authenticated
  USING (
    order_id IN (
      SELECT o.id FROM public.orders o
      INNER JOIN public.user_colmado uc ON o.colmado_id = uc.colmado_id
      WHERE uc.user_id = auth.uid()
    )
  );

-- Clientes pueden crear items al crear órdenes
CREATE POLICY "Customers can create order items"
  ON public.order_items FOR INSERT
  TO authenticated
  WITH CHECK (
    order_id IN (
      SELECT id FROM public.orders WHERE user_id = auth.uid()
    )
  );

-- Service role puede manejar todo
CREATE POLICY "Service role can manage order items"
  ON public.order_items FOR ALL
  TO service_role
  USING (true)
  WITH CHECK (true);

-- =====================================================
-- POLÍTICAS: payments
-- =====================================================

-- Clientes pueden ver sus propios pagos
CREATE POLICY "Customers can view own payments"
  ON public.payments FOR SELECT
  TO authenticated
  USING (user_id = auth.uid());

-- Vendedores pueden ver pagos de su colmado
CREATE POLICY "Sellers can view colmado payments"
  ON public.payments FOR SELECT
  TO authenticated
  USING (
    colmado_id IN (
      SELECT uc.colmado_id FROM public.user_colmado uc
      WHERE uc.user_id = auth.uid()
    )
  );

-- Solo service_role puede crear/actualizar pagos (desde backend/webhooks)
CREATE POLICY "Service role can manage payments"
  ON public.payments FOR ALL
  TO service_role
  USING (true)
  WITH CHECK (true);

-- =====================================================
-- POLÍTICAS: order_status_history
-- =====================================================

-- Clientes pueden ver historial de sus órdenes
CREATE POLICY "Customers can view own order history"
  ON public.order_status_history FOR SELECT
  TO authenticated
  USING (
    order_id IN (
      SELECT id FROM public.orders WHERE user_id = auth.uid()
    )
  );

-- Vendedores pueden ver historial de órdenes de su colmado
CREATE POLICY "Sellers can view colmado order history"
  ON public.order_status_history FOR SELECT
  TO authenticated
  USING (
    order_id IN (
      SELECT o.id FROM public.orders o
      INNER JOIN public.user_colmado uc ON o.colmado_id = uc.colmado_id
      WHERE uc.user_id = auth.uid()
    )
  );

-- Service role puede ver todo
CREATE POLICY "Service role can view order history"
  ON public.order_status_history FOR SELECT
  TO service_role
  USING (true);

-- =====================================================
-- VISTAS: Resúmenes útiles
-- =====================================================

-- Vista: Órdenes con información completa
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

  -- Cliente
  o.user_id,
  up.nombre as customer_name,
  up.email as customer_email,
  up.telefono as customer_phone,

  -- Colmado
  o.colmado_id,
  c.name as colmado_name,
  c.address as colmado_address,
  c.phone as colmado_phone,

  -- Dirección de entrega
  o.address_id,
  a.street_address as delivery_address,
  a.city as delivery_city,
  a.reference as delivery_reference,

  -- Delivery
  o.delivery_user_id,
  ud.nombre as delivery_name,
  ud.telefono as delivery_phone,

  -- Pago
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
-- COMENTARIOS (Documentación)
-- =====================================================

COMMENT ON TABLE public.stripe_accounts IS 'Cuentas de Stripe Connect de cada colmado';
COMMENT ON TABLE public.orders IS 'Pedidos de los clientes';
COMMENT ON TABLE public.order_items IS 'Productos dentro de cada pedido';
COMMENT ON TABLE public.payments IS 'Pagos procesados con Stripe';
COMMENT ON TABLE public.order_status_history IS 'Historial de cambios de estado de pedidos';

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================
