-- =====================================================
-- HABILITAR REALTIME PARA TABLAS DE PEDIDOS
-- Mandadito App - Actualizaciones en tiempo real
-- =====================================================

-- Habilitar replicación para la tabla principal de pedidos
ALTER PUBLICATION supabase_realtime ADD TABLE orders;

-- Habilitar replicación para order_items (opcional, si se necesita tracking individual)
ALTER PUBLICATION supabase_realtime ADD TABLE order_items;

-- =====================================================
-- NOTA IMPORTANTE
-- =====================================================
-- 
-- Después de ejecutar este script, las actualizaciones en tiempo real
-- de los pedidos funcionarán para:
-- 
-- 1. CLIENTES: Verán actualizaciones de sus pedidos
-- 2. VENDEDORES: Verán nuevos pedidos y cambios de estado
-- 3. DELIVERIES: Verán asignaciones de pedidos y cambios
--
-- Los ViewModels ya tienen implementada la lógica de suscripción Realtime.
-- Solo necesitaba habilitar la replicación en Supabase.
-- =====================================================

COMMIT;