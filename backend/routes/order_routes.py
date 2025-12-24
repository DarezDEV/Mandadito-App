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
    Body: {
        "user_id": "uuid",
        "cart_id": "uuid",
        "address_id": "uuid",
        "delivery_fee": 50.0,
        "customer_notes": "Sin cebolla"
    }

    Returns: {
        "success": true,
        "order_id": "uuid",
        "order_number": "ORD-20250121-001",
        "client_secret": "pi_xxx_secret_xxx",
        "amount": 350.50
    }
    """
    try:
        data = request.get_json()

        user_id = data.get('userId')
        cart_id = data.get('cartId')
        address_id = data.get('addressId')
        delivery_fee = data.get('deliveryFee', 0.0)
        customer_notes = data.get('customerNotes')

        if not all([user_id, cart_id, address_id]):
            return jsonify({
                'success': False,
                'message': 'Faltan campos requeridos: user_id, cart_id, address_id'
            }), 400

        result = order_service.create_order_from_cart(
            user_id=user_id,
            cart_id=cart_id,
            address_id=address_id,
            delivery_fee=float(delivery_fee),
            customer_notes=customer_notes
        )

        status_code = 200 if result['success'] else 400
        return jsonify(result), status_code

    except Exception as e:
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
    GET /orders/<order_id>?user_id=uuid

    Returns: {
        "success": true,
        "order": {
            "id": "uuid",
            "order_number": "ORD-20250121-001",
            "status": "paid",
            "total": 350.50,
            "items": [...],
            "colmado": {...}
        }
    }
    """
    try:
        user_id = request.args.get('user_id')

        if not user_id:
            return jsonify({
                'success': False,
                'message': 'user_id es requerido'
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
# CANCELAR ORDEN
# =====================================================
@order_bp.route('/<order_id>/cancel', methods=['POST'])
def cancel_order(order_id):
    """
    POST /orders/<order_id>/cancel
    Body: {
        "user_id": "uuid",
        "reason": "Ya no lo necesito"
    }
    """
    try:
        data = request.get_json()
        user_id = data.get('user_id')
        reason = data.get('reason')

        if not user_id:
            return jsonify({
                'success': False,
                'message': 'user_id es requerido'
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
