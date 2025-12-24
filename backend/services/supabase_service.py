"""
Servicio de Supabase
Cliente para interactuar con Supabase con configuración HTTP explícita
"""
from supabase import create_client, Client
from config import Config

class SupabaseService:
    _instance = None
    _client: Client = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)

            print("=" * 60)
            print("[SUPABASE] Inicializando cliente...")
            print(f"[SUPABASE] URL: {Config.SUPABASE_URL}")
            print("=" * 60)

            cls._client = create_client(
                Config.SUPABASE_URL,
                Config.SUPABASE_SERVICE_ROLE_KEY
            )

            print("[SUPABASE] ✅ Cliente creado correctamente")
            print("=" * 60)

        return cls._instance

    @property
    def client(self):
        return self._client


    # =====================================================
    # STRIPE ACCOUNTS
    # =====================================================

    def get_stripe_account_by_colmado(self, colmado_id: str):
        """Obtiene la cuenta Stripe de un colmado"""
        try:
            response = self._client.table('stripe_accounts')\
                .select('*')\
                .eq('colmado_id', colmado_id)\
                .execute()
            return response.data[0] if response.data and len(response.data) > 0 else None
        except Exception:
            return None

    def create_stripe_account(self, data: dict):
        """Crea una nueva cuenta Stripe"""
        response = self._client.table('stripe_accounts')\
            .insert(data)\
            .execute()
        return response.data[0] if response.data else None

    def update_stripe_account(self, account_id: str, data: dict):
        """Actualiza una cuenta Stripe"""
        response = self._client.table('stripe_accounts')\
            .update(data)\
            .eq('id', account_id)\
            .execute()
        return response.data[0] if response.data else None

    # =====================================================
    # ORDERS
    # =====================================================

    def get_order(self, order_id: str):
        """Obtiene una orden por ID"""
        response = self._client.table('orders')\
            .select('*')\
            .eq('id', order_id)\
            .single()\
            .execute()
        return response.data if response.data else None

    def create_order(self, data: dict):
        """Crea una nueva orden"""
        response = self._client.table('orders')\
            .insert(data)\
            .execute()
        return response.data[0] if response.data else None

    def update_order(self, order_id: str, data: dict):
        """Actualiza una orden"""
        response = self._client.table('orders')\
            .update(data)\
            .eq('id', order_id)\
            .execute()
        return response.data[0] if response.data else None

    # =====================================================
    # ORDER ITEMS
    # =====================================================

    def create_order_items(self, items: list):
        """Crea múltiples items de orden"""
        response = self._client.table('order_items')\
            .insert(items)\
            .execute()
        return response.data if response.data else []

    def get_order_items(self, order_id: str):
        """Obtiene los items de una orden"""
        response = self._client.table('order_items')\
            .select('*')\
            .eq('order_id', order_id)\
            .execute()
        return response.data if response.data else []

    # =====================================================
    # PAYMENTS
    # =====================================================

    def create_payment(self, data: dict):
        """Crea un registro de pago"""
        response = self._client.table('payments')\
            .insert(data)\
            .execute()
        return response.data[0] if response.data else None

    def update_payment(self, payment_id: str, data: dict):
        """Actualiza un registro de pago"""
        response = self._client.table('payments')\
            .update(data)\
            .eq('id', payment_id)\
            .execute()
        return response.data[0] if response.data else None

    def get_payment_by_intent(self, payment_intent_id: str):
        """Obtiene un pago por PaymentIntent ID"""
        response = self._client.table('payments')\
            .select('*')\
            .eq('stripe_payment_intent_id', payment_intent_id)\
            .single()\
            .execute()
        return response.data if response.data else None

    # =====================================================
    # CART
    # =====================================================

    def get_cart_with_items(self, cart_id: str):
        """Obtiene un carrito con sus items"""
        # Obtener resumen del carrito
        cart_response = self._client.table('view_cart_summary')\
            .select('*')\
            .eq('cart_id', cart_id)\
            .single()\
            .execute()

        if not cart_response.data:
            return None

        # Obtener items del carrito
        items_response = self._client.table('view_cart_items')\
            .select('*')\
            .eq('cart_id', cart_id)\
            .execute()

        cart = cart_response.data
        cart['items'] = items_response.data or []

        return cart

    def clear_cart(self, cart_id: str):
        """Vacía un carrito (elimina todos los items)"""
        response = self._client.table('cart_items')\
            .delete()\
            .eq('cart_id', cart_id)\
            .execute()
        return response

    # =====================================================
    # COLMADOS
    # =====================================================

    def get_colmado(self, colmado_id: str):
        """Obtiene información de un colmado"""
        response = self._client.table('colmados')\
            .select('*')\
            .eq('id', colmado_id)\
            .single()\
            .execute()
        return response.data if response.data else None