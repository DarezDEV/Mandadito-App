-- Agregar campo de código de verificación a orders
-- Este código se genera cuando el pedido pasa a IN_DELIVERY
-- y se usa para confirmar la entrega

ALTER TABLE public.orders
ADD COLUMN IF NOT EXISTS verification_code TEXT;

    -- Crear función para generar código de verificación de 6 dígitos
CREATE OR REPLACE FUNCTION generate_verification_code()
RETURNS TEXT AS $$
BEGIN
  RETURN LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
END;
$$ LANGUAGE plpgsql;

-- Crear trigger para generar código cuando se asigna delivery
CREATE OR REPLACE FUNCTION set_verification_code_on_delivery()
RETURNS TRIGGER AS $$
BEGIN
  -- Solo generar código si el estado cambia a in_delivery y no hay código
  IF NEW.status = 'in_delivery' AND (OLD.status != 'in_delivery' OR OLD.status IS NULL) AND NEW.verification_code IS NULL THEN
    NEW.verification_code := generate_verification_code();
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_set_verification_code ON public.orders;
CREATE TRIGGER trigger_set_verification_code
  BEFORE UPDATE ON public.orders
  FOR EACH ROW
  EXECUTE FUNCTION set_verification_code_on_delivery();

-- Comentario en la columna
COMMENT ON COLUMN public.orders.verification_code IS 'Código de 6 dígitos para verificar entrega. Se genera automáticamente cuando el pedido pasa a in_delivery';
