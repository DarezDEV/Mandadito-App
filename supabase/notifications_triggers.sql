-- =====================================================
-- SISTEMA DE NOTIFICACIONES AUTOMÁTICAS
-- Mandadito App - Triggers y Funciones
-- =====================================================

-- =====================================================
-- FUNCIONES HELPER
-- =====================================================

-- Función para crear notificación
CREATE OR REPLACE FUNCTION create_notification(
    p_user_id UUID,
    p_type TEXT,
    p_title TEXT,
    p_message TEXT,
    p_is_push BOOLEAN DEFAULT FALSE
) RETURNS UUID AS $$
DECLARE
    notification_id UUID;
BEGIN
    INSERT INTO notifications (user_id, type, title, message, is_push, created_at)
    VALUES (p_user_id, p_type, p_title, p_message, p_is_push, NOW())
    RETURNING id INTO notification_id;

    RETURN notification_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- 1. NOTIFICACIÓN: NUEVO PEDIDO PARA VENDEDOR
-- =====================================================

CREATE OR REPLACE FUNCTION notify_seller_new_order()
RETURNS TRIGGER AS $$
DECLARE
    seller_user_id UUID;
    customer_name TEXT;
BEGIN
    -- Obtener el seller_id del colmado
    SELECT seller_id INTO seller_user_id
    FROM colmados
    WHERE id = NEW.colmado_id;

    -- Obtener nombre del cliente
    SELECT nombre INTO customer_name
    FROM profiles
    WHERE id = NEW.user_id;

    -- Crear notificación para el vendedor
    PERFORM create_notification(
        seller_user_id,
        'NEW_ORDER',
        'Nuevo Pedido #' || NEW.order_number,
        customer_name || ' realizó un pedido por RD$' || NEW.total::TEXT,
        TRUE -- Es notificación push
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_notify_seller_new_order ON orders;
CREATE TRIGGER trigger_notify_seller_new_order
    AFTER INSERT ON orders
    FOR EACH ROW
    EXECUTE FUNCTION notify_seller_new_order();

-- =====================================================
-- 2. NOTIFICACIÓN: CAMBIO DE ESTADO DEL PEDIDO
-- =====================================================

CREATE OR REPLACE FUNCTION notify_customer_order_status()
RETURNS TRIGGER AS $$
DECLARE
    status_message TEXT;
    status_type TEXT;
BEGIN
    -- Solo notificar si el estado cambió
    IF OLD.status IS DISTINCT FROM NEW.status THEN

        -- Determinar mensaje según el estado
        CASE NEW.status
            WHEN 'paid' THEN
                status_message := 'Tu pago ha sido confirmado';
                status_type := 'PAYMENT';
            WHEN 'preparing' THEN
                status_message := 'Tu pedido está siendo preparado';
                status_type := 'ORDER';
            WHEN 'ready_for_pickup' THEN
                status_message := 'Tu pedido está listo para ser recogido';
                status_type := 'ORDER';
            WHEN 'in_delivery' THEN
                status_message := 'Tu pedido está en camino. Código: ' || NEW.verification_code;
                status_type := 'ORDER';
            WHEN 'delivered' THEN
                status_message := 'Tu pedido ha sido entregado. ¡Gracias por tu compra!';
                status_type := 'ORDER_DELIVERED';
            WHEN 'cancelled' THEN
                status_message := 'Tu pedido ha sido cancelado';
                status_type := 'ALERT';
            ELSE
                RETURN NEW; -- No notificar para otros estados
        END CASE;

        -- Crear notificación para el cliente
        PERFORM create_notification(
            NEW.user_id,
            status_type,
            'Pedido #' || NEW.order_number,
            status_message,
            TRUE
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_notify_customer_order_status ON orders;
CREATE TRIGGER trigger_notify_customer_order_status
    AFTER UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION notify_customer_order_status();

-- =====================================================
-- 3. NOTIFICACIÓN: ASIGNACIÓN DE DELIVERY
-- =====================================================

CREATE OR REPLACE FUNCTION notify_delivery_assignment()
RETURNS TRIGGER AS $$
DECLARE
    colmado_name TEXT;
    customer_address TEXT;
BEGIN
    -- Solo notificar si se asignó un delivery (cambió de NULL a un ID)
    IF OLD.delivery_user_id IS NULL AND NEW.delivery_user_id IS NOT NULL THEN

        -- Obtener nombre del colmado
        SELECT name INTO colmado_name
        FROM colmados
        WHERE id = NEW.colmado_id;

        -- Obtener dirección del cliente
        SELECT formatted_address INTO customer_address
        FROM addresses
        WHERE id = NEW.address_id;

        -- Crear notificación para el delivery
        PERFORM create_notification(
            NEW.delivery_user_id,
            'NEW_ORDER',
            'Nueva Entrega Asignada',
            'Recoger en ' || colmado_name || ' - Entregar en ' || customer_address,
            TRUE
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_notify_delivery_assignment ON orders;
CREATE TRIGGER trigger_notify_delivery_assignment
    AFTER UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION notify_delivery_assignment();

-- =====================================================
-- 4. NOTIFICACIÓN: PAGO CONFIRMADO PARA VENDEDOR
-- =====================================================

CREATE OR REPLACE FUNCTION notify_seller_payment_confirmed()
RETURNS TRIGGER AS $$
DECLARE
    seller_user_id UUID;
    order_number TEXT;
    payment_amount NUMERIC;
BEGIN
    -- Solo notificar cuando el pago cambia a succeeded
    IF OLD.status IS DISTINCT FROM NEW.status AND NEW.status = 'succeeded' THEN

        -- Obtener información del pedido
        SELECT
            c.seller_id,
            o.order_number,
            o.total
        INTO
            seller_user_id,
            order_number,
            payment_amount
        FROM orders o
        JOIN colmados c ON o.colmado_id = c.id
        WHERE o.id = NEW.order_id;

        -- Crear notificación para el vendedor
        PERFORM create_notification(
            seller_user_id,
            'PAYMENT',
            'Pago Recibido',
            'Pago confirmado para pedido #' || order_number || ' - RD$' || payment_amount::TEXT,
            TRUE
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_notify_seller_payment_confirmed ON payments;
CREATE TRIGGER trigger_notify_seller_payment_confirmed
    AFTER UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION notify_seller_payment_confirmed();

-- =====================================================
-- 5. NOTIFICACIÓN: STOCK BAJO
-- =====================================================

CREATE OR REPLACE FUNCTION notify_seller_low_stock()
RETURNS TRIGGER AS $$
DECLARE
    seller_user_id UUID;
    notification_type TEXT;
    notification_title TEXT;
BEGIN
    -- Solo verificar cuando el stock disminuye
    IF NEW.stock < OLD.stock THEN

        -- Obtener el seller_id del colmado
        SELECT seller_id INTO seller_user_id
        FROM colmados
        WHERE id = NEW.colmado_id;

        -- Determinar si es stock bajo o sin stock
        IF NEW.stock = 0 THEN
            notification_type := 'OUT_OF_STOCK';
            notification_title := 'Producto Sin Stock';

            -- Crear notificación
            PERFORM create_notification(
                seller_user_id,
                notification_type,
                notification_title,
                'El producto "' || NEW.name || '" se ha agotado',
                TRUE
            );
        ELSIF NEW.stock <= NEW.min_stock AND NEW.stock > 0 THEN
            notification_type := 'LOW_STOCK';
            notification_title := 'Stock Bajo';

            -- Crear notificación
            PERFORM create_notification(
                seller_user_id,
                notification_type,
                notification_title,
                'El producto "' || NEW.name || '" tiene solo ' || NEW.stock::TEXT || ' unidades',
                FALSE -- No es push, solo informativa
            );
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_notify_seller_low_stock ON products;
CREATE TRIGGER trigger_notify_seller_low_stock
    AFTER UPDATE ON products
    FOR EACH ROW
    WHEN (NEW.stock < OLD.stock)
    EXECUTE FUNCTION notify_seller_low_stock();

-- =====================================================
-- 6. NOTIFICACIÓN: ENTREGA CONFIRMADA (PARA VENDEDOR)
-- =====================================================

CREATE OR REPLACE FUNCTION notify_seller_delivery_completed()
RETURNS TRIGGER AS $$
DECLARE
    seller_user_id UUID;
    delivery_name TEXT;
BEGIN
    -- Solo notificar cuando el estado cambia a delivered
    IF OLD.status IS DISTINCT FROM NEW.status AND NEW.status = 'delivered' THEN

        -- Obtener seller_id del colmado
        SELECT seller_id INTO seller_user_id
        FROM colmados
        WHERE id = NEW.colmado_id;

        -- Obtener nombre del delivery (si existe)
        IF NEW.delivery_user_id IS NOT NULL THEN
            SELECT nombre INTO delivery_name
            FROM profiles
            WHERE id = NEW.delivery_user_id;
        ELSE
            delivery_name := 'El cliente';
        END IF;

        -- Crear notificación para el vendedor
        PERFORM create_notification(
            seller_user_id,
            'ORDER_DELIVERED',
            'Entrega Completada',
            delivery_name || ' confirmó la entrega del pedido #' || NEW.order_number,
            FALSE
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_notify_seller_delivery_completed ON orders;
CREATE TRIGGER trigger_notify_seller_delivery_completed
    AFTER UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION notify_seller_delivery_completed();

-- =====================================================
-- HABILITAR REALTIME PARA NOTIFICACIONES
-- =====================================================

ALTER PUBLICATION supabase_realtime ADD TABLE notifications;

-- =====================================================
-- FIN DEL SCRIPT
-- =====================================================

COMMIT;
