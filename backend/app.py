"""
Servidor Flask - Backend de Mandadito
Maneja pagos con Stripe Connect y gestión de órdenes
"""
from flask import Flask, jsonify
from flask_cors import CORS
from config import Config

# Importar blueprints
from routes.stripe_routes import stripe_bp
from routes.order_routes import order_bp
from webhooks.stripe_webhooks import webhook_bp

# =====================================================
# CREAR APLICACIÓN FLASK
# =====================================================
def create_app():
    app = Flask(__name__)
    app.config.from_object(Config)

    # Habilitar CORS para Android
    CORS(app, resources={
        r"/*": {
            "origins": "*",  # En producción, especifica tu dominio
            "methods": ["GET", "POST", "PUT", "DELETE"],
            "allow_headers": ["Content-Type", "Authorization"]
        }
    })

    # Registrar blueprints
    app.register_blueprint(stripe_bp)
    app.register_blueprint(order_bp)
    app.register_blueprint(webhook_bp)

    # =====================================================
    # RUTA RAÍZ
    # =====================================================
    @app.route('/', methods=['GET'])
    def index():
        return jsonify({
            'name': 'Mandadito API',
            'version': '1.0.0',
            'status': 'running',
            'endpoints': {
                'stripe': {
                    'create_account': 'POST /stripe/connect/create',
                    'refresh_link': 'POST /stripe/connect/refresh-link',
                    'check_status': 'POST /stripe/connect/status',
                    'config': 'GET /stripe/config'
                },
                'orders': {
                    'create': 'POST /orders/create',
                    'get': 'GET /orders/<order_id>',
                    'cancel': 'POST /orders/<order_id>/cancel'
                },
                'webhooks': {
                    'stripe': 'POST /webhooks/stripe',
                    'health': 'GET /webhooks/health'
                }
            }
        })

    # =====================================================
    # MANEJO DE ERRORES
    # =====================================================
    @app.errorhandler(404)
    def not_found(error):
        return jsonify({
            'success': False,
            'message': 'Endpoint no encontrado'
        }), 404

    @app.errorhandler(500)
    def internal_error(error):
        return jsonify({
            'success': False,
            'message': 'Error interno del servidor'
        }), 500

    return app

# =====================================================
# EJECUTAR SERVIDOR
# =====================================================
if __name__ == '__main__':
    app = create_app()

    print('=' * 50)
    print('Mandadito Backend Server')
    print('=' * 50)
    print(f'URL: http://localhost:{Config.PORT}')
    print(f'Environment: {Config.ENV}')
    print(f'Stripe Mode: TEST')
    print('=' * 50)
    print('\nServer running...\n')

    app.run(
        host='0.0.0.0',
        port=Config.PORT,
        debug=Config.DEBUG
    )
