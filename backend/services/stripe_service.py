"""
Servicio de Stripe Connect
Maneja todas las operaciones de Stripe: cuentas, pagos, transferencias
"""
import stripe
from config import Config
from services.supabase_service import SupabaseService

# Configurar Stripe con la API key
stripe.api_key = Config.STRIPE_SECRET_KEY

class StripeService:
    """Servicio para manejar operaciones de Stripe Connect"""

    def __init__(self):
        self.supabase = SupabaseService()

    # =====================================================
    # STRIPE CONNECT - CUENTAS
    # =====================================================

    def create_express_account(self, colmado_id: str, email: str, business_name: str):
        """
        Crea una cuenta Express de Stripe Connect para un colmado

        Args:
            colmado_id: ID del colmado en Supabase
            email: Email del vendedor
            business_name: Nombre del negocio

        Returns:
            dict con account_id y onboarding_url
        """
        try:
            print(f"[DEBUG] Step 1: Checking existing account for colmado_id: {colmado_id}")
            # Verificar si ya existe una cuenta
            existing = self.supabase.get_stripe_account_by_colmado(colmado_id)
            print(f"[DEBUG] Step 2: Existing account check complete: {existing}")
            if existing:
                return {
                    'success': False,
                    'message': 'Este colmado ya tiene una cuenta Stripe',
                    'account_id': existing['stripe_account_id']
                }

            print(f"[DEBUG] Step 3: Creating Stripe Express account for {email}")
            # Crear cuenta Express en Stripe
            account = stripe.Account.create(
                type='express',
                country='US',  # USA (DO no soportado por Stripe Connect)
                email=email,
                business_type='individual',  # o 'company'
                capabilities={
                    'card_payments': {'requested': True},
                    'transfers': {'requested': True},
                },
                business_profile={
                    'name': business_name,
                    'product_description': 'Venta de productos de colmado',
                },
            )

            # Crear AccountLink para onboarding
            account_link = stripe.AccountLink.create(
                account=account.id,
                refresh_url=f"{Config.BASE_URL}/stripe/onboarding/refresh",
                return_url=f"{Config.BASE_URL}/stripe/onboarding/complete",
                type='account_onboarding',
            )

            # Guardar en Supabase
            self.supabase.create_stripe_account({
                'colmado_id': colmado_id,
                'stripe_account_id': account.id,
                'onboarding_completed': False,
                'charges_enabled': False,
                'payouts_enabled': False,
                'onboarding_url': account_link.url,
                'onboarding_expires_at': None,  # AccountLinks expiran en ~5 minutos
                'country': 'US',
                'currency': 'USD',
                'business_type': 'individual'
            })

            return {
                'success': True,
                'account_id': account.id,
                'onboarding_url': account_link.url,
                'message': 'Cuenta creada. Completar onboarding.'
            }

        except stripe.error.StripeError as e:
            return {
                'success': False,
                'message': f'Error de Stripe: {str(e)}'
            }
        except Exception as e:
            return {
                'success': False,
                'message': f'Error: {str(e)}'
            }

    def refresh_onboarding_link(self, colmado_id: str):
        """
        Genera un nuevo link de onboarding (los links expiran rápido)

        Args:
            colmado_id: ID del colmado

        Returns:
            dict con nuevo onboarding_url
        """
        try:
            # Obtener cuenta de Supabase
            account_data = self.supabase.get_stripe_account_by_colmado(colmado_id)
            if not account_data:
                return {
                    'success': False,
                    'message': 'Cuenta Stripe no encontrada'
                }

            stripe_account_id = account_data['stripe_account_id']

            # Crear nuevo AccountLink
            account_link = stripe.AccountLink.create(
                account=stripe_account_id,
                refresh_url=f"{Config.BASE_URL}/stripe/onboarding/refresh",
                return_url=f"{Config.BASE_URL}/stripe/onboarding/complete",
                type='account_onboarding',
            )

            # Actualizar en Supabase
            self.supabase.update_stripe_account(account_data['id'], {
                'onboarding_url': account_link.url
            })

            return {
                'success': True,
                'onboarding_url': account_link.url
            }

        except Exception as e:
            return {
                'success': False,
                'message': f'Error: {str(e)}'
            }

    def check_account_status(self, colmado_id: str):
        """
        Verifica el estado de una cuenta Stripe

        Args:
            colmado_id: ID del colmado

        Returns:
            dict con estado de la cuenta
        """
        try:
            # Obtener cuenta de Supabase
            account_data = self.supabase.get_stripe_account_by_colmado(colmado_id)
            if not account_data:
                return {
                    'success': False,
                    'message': 'Cuenta Stripe no encontrada'
                }

            stripe_account_id = account_data['stripe_account_id']

            # Obtener información actualizada de Stripe
            account = stripe.Account.retrieve(stripe_account_id)

            # Actualizar en Supabase
            self.supabase.update_stripe_account(account_data['id'], {
                'onboarding_completed': account.details_submitted,
                'charges_enabled': account.charges_enabled,
                'payouts_enabled': account.payouts_enabled,
            })

            return {
                'success': True,
                'account_id': account.id,
                'onboarding_completed': account.details_submitted,
                'charges_enabled': account.charges_enabled,
                'payouts_enabled': account.payouts_enabled,
                'requirements': account.requirements.currently_due if hasattr(account.requirements, 'currently_due') else []
            }

        except Exception as e:
            return {
                'success': False,
                'message': f'Error: {str(e)}'
            }

    # =====================================================
    # PAGOS - PAYMENT INTENTS
    # =====================================================

    def create_payment_intent(
        self,
        order_id: str,
        amount_dop: float,
        colmado_id: str,
        user_id: str,
        description: str = None
    ):
        """
        Crea un PaymentIntent con transferencia directa al colmado

        Args:
            order_id: ID de la orden
            amount_dop: Monto total en pesos (DOP)
            colmado_id: ID del colmado
            user_id: ID del usuario
            description: Descripción del pago

        Returns:
            dict con client_secret y payment_intent_id
        """
        try:
            # Obtener cuenta Stripe del colmado
            stripe_account = self.supabase.get_stripe_account_by_colmado(colmado_id)
            if not stripe_account:
                return {
                    'success': False,
                    'message': 'El colmado no tiene cuenta Stripe configurada'
                }

            if not stripe_account['charges_enabled']:
                return {
                    'success': False,
                    'message': 'El colmado no ha completado su onboarding de Stripe'
                }

            stripe_account_id = stripe_account['stripe_account_id']

            # Convertir DOP a centavos (Stripe trabaja en centavos)
            amount_cents = int(amount_dop * 100)

            # Calcular comisión de plataforma (tu ganancia)
            platform_fee_cents = int(amount_cents * (Config.STRIPE_PLATFORM_FEE_PERCENT / 100))

            # Monto que recibirá el colmado
            transfer_amount_cents = amount_cents - platform_fee_cents

            # Crear PaymentIntent
            payment_intent = stripe.PaymentIntent.create(
                amount=amount_cents,
                currency='usd',
                payment_method_types=['card'],
                description=description or f'Pedido {order_id}',

                # CLAVE: Transferir automáticamente al colmado
                transfer_data={
                    'destination': stripe_account_id,
                    'amount': transfer_amount_cents,  # Monto sin tu comisión
                },

                # Metadata para tracking
                metadata={
                    'order_id': order_id,
                    'user_id': user_id,
                    'colmado_id': colmado_id,
                    'platform_fee_cents': platform_fee_cents,
                }
            )

            # Guardar en Supabase
            payment_record = self.supabase.create_payment({
                'order_id': order_id,
                'user_id': user_id,
                'colmado_id': colmado_id,
                'stripe_account_id': stripe_account['id'],
                'stripe_payment_intent_id': payment_intent.id,
                'amount': amount_cents,
                'platform_fee_amount': platform_fee_cents,
                'transfer_amount': transfer_amount_cents,
                'currency': 'DOP',
                'status': 'pending',
                'client_secret': payment_intent.client_secret,
            })

            return {
                'success': True,
                'payment_intent_id': payment_intent.id,
                'client_secret': payment_intent.client_secret,
                'amount': amount_dop,
                'platform_fee': platform_fee_cents / 100,
                'transfer_amount': transfer_amount_cents / 100,
                'payment_record_id': payment_record['id']
            }

        except stripe.error.StripeError as e:
            return {
                'success': False,
                'message': f'Error de Stripe: {str(e)}'
            }
        except Exception as e:
            return {
                'success': False,
                'message': f'Error: {str(e)}'
            }

    # =====================================================
    # WEBHOOKS - MANEJO DE EVENTOS
    # =====================================================

    def handle_payment_succeeded(self, payment_intent):
        """
        Maneja el evento payment_intent.succeeded

        Args:
            payment_intent: Objeto PaymentIntent de Stripe

        Returns:
            dict con resultado
        """
        try:
            payment_intent_id = payment_intent.id

            # Buscar el pago en Supabase
            payment_record = self.supabase.get_payment_by_intent(payment_intent_id)
            if not payment_record:
                return {
                    'success': False,
                    'message': f'Pago no encontrado: {payment_intent_id}'
                }

            # Actualizar registro de pago
            self.supabase.update_payment(payment_record['id'], {
                'status': 'succeeded',
                'succeeded_at': 'now()',
                'stripe_charge_id': payment_intent.latest_charge,
                'payment_method_type': payment_intent.payment_method_types[0] if payment_intent.payment_method_types else None,
                'amount_captured': payment_intent.amount_received,
            })

            # Actualizar orden a 'paid'
            order_id = payment_record['order_id']
            self.supabase.update_order(order_id, {
                'status': 'paid',
                'paid_at': 'now()',
            })

            return {
                'success': True,
                'message': f'Pago procesado exitosamente',
                'order_id': order_id
            }

        except Exception as e:
            return {
                'success': False,
                'message': f'Error procesando pago: {str(e)}'
            }

    def handle_payment_failed(self, payment_intent):
        """
        Maneja el evento payment_intent.payment_failed

        Args:
            payment_intent: Objeto PaymentIntent de Stripe

        Returns:
            dict con resultado
        """
        try:
            payment_intent_id = payment_intent.id

            # Buscar el pago en Supabase
            payment_record = self.supabase.get_payment_by_intent(payment_intent_id)
            if not payment_record:
                return {
                    'success': False,
                    'message': f'Pago no encontrado: {payment_intent_id}'
                }

            # Obtener error
            error_message = None
            error_code = None
            if hasattr(payment_intent, 'last_payment_error') and payment_intent.last_payment_error:
                error_message = payment_intent.last_payment_error.message
                error_code = payment_intent.last_payment_error.code

            # Actualizar registro de pago
            self.supabase.update_payment(payment_record['id'], {
                'status': 'failed',
                'failed_at': 'now()',
                'error_message': error_message,
                'error_code': error_code,
            })

            # Actualizar orden a 'cancelled'
            order_id = payment_record['order_id']
            self.supabase.update_order(order_id, {
                'status': 'cancelled',
                'cancelled_at': 'now()',
                'cancellation_reason': f'Pago fallido: {error_message}',
            })

            return {
                'success': True,
                'message': f'Pago fallido procesado',
                'order_id': order_id
            }

        except Exception as e:
            return {
                'success': False,
                'message': f'Error procesando fallo de pago: {str(e)}'
            }
