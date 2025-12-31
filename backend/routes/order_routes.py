"""
Rutas de Órdenes
Endpoints para crear y gestionar órdenes
"""
from flask import Blueprint, request, jsonify
from services.order_service import OrderService

order_bp = Blueprint('orders', __name__, url_prefix='/orders')
order_service = OrderService()

# =====================================================
# CREAR ORDEN DESDE CARRITO
# =====================================================
@order_bp.route('/create', methods=['POST'])
def create_order():
    """
    POST /orders/create
    Body (camelCase): {
        "userId": "uuid",
        "cartId": "uuid",
        "addressId": "uuid",
        "deliveryFee": 50.0,
        "customerNotes": "Sin cebolla"
    }

    Returns (camelCase): {
        "success": true,
        "orderId": "uuid",
        "orderNumber": "ORD-20250121-001",
        "clientSecret": "pi_xxx_secret_xxx",
        "amount": 350.50
    }
    """
    try:
        print("[DEBUG] 🚀 INICIO - Endpoint /orders/create llamado")
        print("[DEBUG] 📍 Paso 1: Obteniendo JSON del request...")
        data = request.get_json()
        print("[DEBUG] ✅ Paso 1 completado")

        print(f"[DEBUG] 📥 Request data recibida: {data}")

        # Recibir en camelCase desde Android
        user_id = data.get('userId')
        cart_id = data.get('cartId')
        address_id = data.get('addressId')
        delivery_fee = data.get('deliveryFee', 0.0)
        customer_notes = data.get('customerNotes')
        
        print(f"[DEBUG] 📝 Valores extraídos:")
        print(f"  - userId: {user_id}")
        print(f"  - cartId: {cart_id}")
        print(f"  - addressId: {address_id}")
        print(f"  - deliveryFee: {delivery_fee}")

        if not all([user_id, cart_id, address_id]):
            return jsonify({
                'success': False,
                'message': 'Faltan campos requeridos: userId, cartId, addressId'
            }), 400

        # Llamar al servicio
        print("[DEBUG] 📍 Paso 2: Llamando a order_service.create_order_from_cart...")
        result = order_service.create_order_from_cart(
            user_id=user_id,
            cart_id=cart_id,
            address_id=address_id,
            delivery_fee=float(delivery_fee),
            customer_notes=customer_notes
        )
        print("[DEBUG] ✅ Paso 2 completado")

        print(f"[DEBUG] 📦 Resultado del servicio: {result}")

        # IMPORTANTE: Convertir respuesta a camelCase para Android
        if result['success']:
            response = {
                'success': True,
                'orderId': result.get('order_id'),
                'orderNumber': result.get('order_number'),
                'clientSecret': result.get('client_secret'),
                'amount': result.get('amount'),
                'message': result.get('message')
            }
            print(f"[DEBUG] ✅ Response enviada: {response}")
        else:
            response = {
                'success': False,
                'message': result.get('message')
            }
            print(f"[DEBUG] ❌ Response con error: {response}")

        status_code = 200 if result['success'] else 400
        return jsonify(response), status_code

    except Exception as e:
        print(f"[ERROR] ❌ Exception: {str(e)}")
        import traceback
        traceback.print_exc()
        return jsonify({
            'success': False,
            'message': f'Error: {str(e)}'
        }), 500

# =====================================================
# OBTENER DETALLES DE ORDEN
# =====================================================
@order_bp.route('/<order_id>', methods=['GET'])
def get_order(order_id):
    """
    GET /orders/<order_id>?userId=uuid

    Returns (camelCase): {
        "success": true,
        "order": {
            "id": "uuid",
            "orderNumber": "ORD-20250121-001",
            "status": "paid",
            "total": 350.50,
            "items": [...],
            "colmado": {...}
        }
    }
    """
    try:
        # Recibir en camelCase
        user_id = request.args.get('userId')

        if not user_id:
            return jsonify({
                'success': False,
                'message': 'userId es requerido'
            }), 400

        result = order_service.get_order_details(
            order_id=order_id,
            user_id=user_id
        )

        status_code = 200 if result['success'] else 400
        return jsonify(result), status_code

    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error: {str(e)}'
        }), 500

# =====================================================
# CONFIRMAR PAGO (desde Android después de PaymentSheet exitoso)
# =====================================================
@order_bp.route('/<order_id>/confirm-payment', methods=['POST'])
def confirm_payment(order_id):
    """
    POST /orders/<order_id>/confirm-payment
    Body (camelCase): {
        "userId": "uuid",
        "cartId": "uuid",
        "paymentIntentId": "pi_xxx"
    }

    Este endpoint se llama desde Android después de que el PaymentSheet
    retorna exitoso, para actualizar el estado de la orden a 'paid'.
    """
    try:
        data = request.get_json()

        # Recibir en camelCase
        user_id = data.get('userId')
        cart_id = data.get('cartId')
        payment_intent_id = data.get('paymentIntentId')

        if not user_id:
            return jsonify({
                'success': False,
                'message': 'userId es requerido'
            }), 400

        result = order_service.confirm_payment(
            order_id=order_id,
            user_id=user_id,
            cart_id=cart_id,
            payment_intent_id=payment_intent_id
        )

        status_code = 200 if result['success'] else 400
        return jsonify(result), status_code

    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error: {str(e)}'
        }), 500

# =====================================================
# CANCELAR ORDEN
# =====================================================
@order_bp.route('/<order_id>/cancel', methods=['POST'])
def cancel_order(order_id):
    """
    POST /orders/<order_id>/cancel
    Body (camelCase): {
        "userId": "uuid",
        "reason": "Ya no lo necesito"
    }
    """
    try:
        data = request.get_json()
        
        # Recibir en camelCase
        user_id = data.get('userId')
        reason = data.get('reason')

        if not user_id:
            return jsonify({
                'success': False,
                'message': 'userId es requerido'
            }), 400

        result = order_service.cancel_order(
            order_id=order_id,
            user_id=user_id,
            reason=reason
        )

        status_code = 200 if result['success'] else 400
        return jsonify(result), status_code

    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error: {str(e)}'
        }), 500