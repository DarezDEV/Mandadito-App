"""
Configuración del backend Flask
"""
import os
from dotenv import load_dotenv

# Cargar variables de entorno
load_dotenv()

class Config:
    """Configuración de la aplicación"""

    # =========================
    # FLASK
    # =========================
    SECRET_KEY = os.getenv('SECRET_KEY', 'dev-secret-key-change-in-production')
    ENV = os.getenv('FLASK_ENV', 'development')
    DEBUG = ENV == 'development'
    PORT = int(os.getenv('PORT', 5000))

    # =========================
    # SUPABASE
    # =========================
    SUPABASE_URL = os.getenv('SUPABASE_URL')
    SUPABASE_KEY = os.getenv('SUPABASE_KEY')
    SUPABASE_SERVICE_ROLE_KEY = os.getenv('SUPABASE_SERVICE_ROLE_KEY')

    # =========================
    # STRIPE
    # =========================
    STRIPE_SECRET_KEY = os.getenv('STRIPE_SECRET_KEY')
    STRIPE_PUBLISHABLE_KEY = os.getenv('STRIPE_PUBLISHABLE_KEY')
    STRIPE_WEBHOOK_SECRET = os.getenv('STRIPE_WEBHOOK_SECRET')

    # ❌ ERROR ANTES: comentarios / moneda en mayúscula
    # ✅ CORRECTO:
    STRIPE_PLATFORM_FEE_PERCENT = float(os.getenv('STRIPE_PLATFORM_FEE_PERCENT', '5'))
    STRIPE_CURRENCY = os.getenv('STRIPE_CURRENCY', 'usd')  # SIEMPRE minúscula (USD para cuentas US)

    # =========================
    # URLS
    # =========================
    BASE_URL = os.getenv('BASE_URL', 'http://localhost:5000')

    # =========================
    # VALIDACIÓN
    # =========================
    @staticmethod
    def validate():
        required = [
            'SUPABASE_URL',
            'SUPABASE_SERVICE_ROLE_KEY',
            'STRIPE_SECRET_KEY',
            'STRIPE_PUBLISHABLE_KEY'
        ]

        missing = [key for key in required if not os.getenv(key)]

        if missing:
            raise RuntimeError(
                f"❌ Missing required environment variables: {', '.join(missing)}"
            )
print(">>> SUPABASE_URL RAW:", repr(Config.SUPABASE_URL))
# Validar al importar
Config.validate()
