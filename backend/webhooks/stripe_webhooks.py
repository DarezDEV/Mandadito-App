"""
Webhooks de Stripe
Maneja eventos de Stripe en tiempo real
"""
from flask import Blueprint, request, jsonify
import stripe
from config import Config
from services.stripe_service import StripeService

webhook_bp = Blueprint('webhooks', __name__, url_prefix='/webhooks')
stripe_service = StripeService()

# =====================================================
# WEBHOOK ENDPOINT
# =====================================================
@webhook_bp.route('/stripe', methods=['POST'])
def stripe_webhook():
    """
    POST /webhooks/stripe
    Recibe eventos de Stripe en tiempo real

    Eventos importantes:
    - payment_intent.succeeded: Pago exitoso
    - payment_intent.payment_failed: Pago fallido
    - account.updated: Cuenta Stripe actualizada
    """
    payload = request.data
    sig_header = request.headers.get('Stripe-Signature')

    try:
        # Verificar firma del webhook (IMPORTANTE: previene ataques)
        event = stripe.Webhook.construct_event(
            payload, sig_header, Config.STRIPE_WEBHOOK_SECRET
        )

    except ValueError as e:
        # Payload inválido
        print(f'❌ Webhook error: {str(e)}')
        return jsonify({'error': 'Invalid payload'}), 400

    except stripe.error.SignatureVerificationError as e:
        # Firma inválida
        print(f'❌ Webhook signature verification failed: {str(e)}')
        return jsonify({'error': 'Invalid signature'}), 400

    # Obtener tipo de evento
    event_type = event['type']
    event_data = event['data']['object']

    print(f'📨 Webhook recibido: {event_type}')

    # =====================================================
    # PAGO EXITOSO
    # =====================================================
    if event_type == 'payment_intent.succeeded':
        payment_intent = event_data
        print(f'✅ Pago exitoso: {payment_intent.id}')

        result = stripe_service.handle_payment_succeeded(payment_intent)

        if result['success']:
            print(f'✅ Orden {result["order_id"]} marcada como pagada')
        else:
            print(f'❌ Error procesando pago: {result["message"]}')

    # =====================================================
    # PAGO FALLIDO
    # =====================================================
    elif event_type == 'payment_intent.payment_failed':
        payment_intent = event_data
        print(f'❌ Pago fallido: {payment_intent.id}')

        result = stripe_service.handle_payment_failed(payment_intent)

        if result['success']:
            print(f'✅ Orden {result["order_id"]} marcada como cancelada')
        else:
            print(f'❌ Error procesando fallo: {result["message"]}')

    # =====================================================
    # CUENTA ACTUALIZADA
    # =====================================================
    elif event_type == 'account.updated':
        account = event_data
        print(f'🔄 Cuenta Stripe actualizada: {account.id}')

        # Aquí podrías actualizar el estado de la cuenta en Supabase
        # Por ejemplo, si se completa el onboarding

    # =====================================================
    # TRANSFERENCIA CREADA
    # =====================================================
    elif event_type == 'transfer.created':
        transfer = event_data
        print(f'💸 Transferencia creada: {transfer.id} → {transfer.amount / 100} DOP')

    # =====================================================
    # OTROS EVENTOS
    # =====================================================
    else:
        print(f'ℹ️ Evento no manejado: {event_type}')

    # IMPORTANTE: Siempre retornar 200 para confirmar recepción
    return jsonify({'success': True}), 200

# =====================================================
# HEALTH CHECK
# =====================================================
@webhook_bp.route('/health', methods=['GET'])
def health_check():
    """
    GET /webhooks/health
    Verifica que el servidor de webhooks esté funcionando
    """
    return jsonify({
        'status': 'healthy',
        'message': 'Webhook server is running'
    }), 200
