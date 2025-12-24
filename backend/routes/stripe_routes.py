"""
Rutas de Stripe Connect
Endpoints para manejar cuentas Stripe y onboarding
"""
from flask import Blueprint, request, jsonify, redirect
from services.stripe_service import StripeService

stripe_bp = Blueprint('stripe', __name__, url_prefix='/stripe')
stripe_service = StripeService()

# =====================================================
# CREAR CUENTA STRIPE CONNECT
# =====================================================
@stripe_bp.route('/connect/create', methods=['POST'])
def create_connect_account():
    """
    POST /stripe/connect/create
    Body: {
        "colmado_id": "uuid",
        "email": "seller@example.com",
        "business_name": "Mi Colmado"
    }
    """
    try:
        data = request.get_json()

        colmado_id = data.get('colmado_id')
        email = data.get('email')
        business_name = data.get('business_name')

        if not all([colmado_id, email, business_name]):
            return jsonify({
                'success': False,
                'message': 'Faltan campos requeridos: colmado_id, email, business_name'
            }), 400

        result = stripe_service.create_express_account(
            colmado_id=colmado_id,
            email=email,
            business_name=business_name
        )

        status_code = 200 if result['success'] else 400
        return jsonify(result), status_code

    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error: {str(e)}'
        }), 500

# =====================================================
# REFRESCAR LINK DE ONBOARDING
# =====================================================
@stripe_bp.route('/connect/refresh-link', methods=['POST'])
def refresh_onboarding_link():
    """
    POST /stripe/connect/refresh-link
    Body: {
        "colmado_id": "uuid"
    }
    """
    try:
        data = request.get_json()
        colmado_id = data.get('colmado_id')

        if not colmado_id:
            return jsonify({
                'success': False,
                'message': 'colmado_id es requerido'
            }), 400

        result = stripe_service.refresh_onboarding_link(colmado_id)

        status_code = 200 if result['success'] else 400
        return jsonify(result), status_code

    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error: {str(e)}'
        }), 500

# =====================================================
# VERIFICAR ESTADO DE CUENTA
# =====================================================
@stripe_bp.route('/connect/status', methods=['POST'])
def check_account_status():
    """
    POST /stripe/connect/status
    Body: {
        "colmado_id": "uuid"
    }
    """
    try:
        data = request.get_json()
        colmado_id = data.get('colmado_id')

        if not colmado_id:
            return jsonify({
                'success': False,
                'message': 'colmado_id es requerido'
            }), 400

        result = stripe_service.check_account_status(colmado_id)

        status_code = 200 if result['success'] else 400
        return jsonify(result), status_code

    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Error: {str(e)}'
        }), 500

# =====================================================
# ONBOARDING - URLS DE RETORNO
# =====================================================
@stripe_bp.route('/onboarding/complete', methods=['GET'])
def onboarding_complete():
    """
    GET /stripe/onboarding/complete
    URL a la que Stripe redirige cuando el onboarding es exitoso
    """
    # Aquí podrías redirigir a tu app Android usando un deep link
    # o mostrar una página web de éxito
    return """
    <html>
        <body style="font-family: Arial; text-align: center; padding: 50px;">
            <h1>✅ ¡Onboarding Completado!</h1>
            <p>Tu cuenta Stripe ha sido configurada exitosamente.</p>
            <p>Ya puedes recibir pagos en tu colmado.</p>
            <p>Puedes cerrar esta ventana.</p>
        </body>
    </html>
    """

@stripe_bp.route('/onboarding/refresh', methods=['GET'])
def onboarding_refresh():
    """
    GET /stripe/onboarding/refresh
    URL a la que Stripe redirige si el link expira
    """
    return """
    <html>
        <body style="font-family: Arial; text-align: center; padding: 50px;">
            <h1>⏰ Link Expirado</h1>
            <p>El link de onboarding ha expirado.</p>
            <p>Por favor, solicita un nuevo link desde la aplicación.</p>
        </body>
    </html>
    """

# =====================================================
# OBTENER PUBLISHABLE KEY
# =====================================================
@stripe_bp.route('/config', methods=['GET'])
def get_stripe_config():
    """
    GET /stripe/config
    Retorna la Publishable Key para Android
    """
    from config import Config
    return jsonify({
        'publishableKey': Config.STRIPE_PUBLISHABLE_KEY,
        'currency': Config.STRIPE_CURRENCY
    })
