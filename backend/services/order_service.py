"""
Servicio de Órdenes
Maneja la creación y gestión de órdenes
"""
from services.supabase_service import SupabaseService
from services.stripe_service import StripeService

class OrderService:
    """Servicio para manejar operaciones de órdenes"""

    def __init__(self):
        self.supabase = SupabaseService()
        self.stripe_service = StripeService()

    def create_order_from_cart(
        self,
        user_id: str,
        cart_id: str,
        address_id: str,
        delivery_fee: float = 0.0,
        customer_notes: str = None
    ):
        """Crea una orden a partir de un carrito"""
        try:
            print("=" * 60)
            print(f"[ORDER SERVICE] 📦 Iniciando creación de orden")
            print(f"[ORDER SERVICE]   - user_id: {user_id}")
            print(f"[ORDER SERVICE]   - cart_id: {cart_id}")
            print(f"[ORDER SERVICE]   - address_id: {address_id}")
            print("=" * 60)
            
            # 1. Obtener carrito con items
            print(f"[ORDER SERVICE] 🔍 PASO 1: Obteniendo carrito desde Supabase...")
            try:
                cart = self.supabase.get_cart_with_items(cart_id)
                print(f"[ORDER SERVICE] ✅ Carrito obtenido exitosamente")
                print(f"[ORDER SERVICE]    Carrito: {cart}")
            except Exception as e:
                print(f"[ORDER SERVICE] ❌ ERROR en PASO 1: {str(e)}")
                import traceback
                traceback.print_exc()
                return {
                    'success': False,
                    'message': f'Error obteniendo carrito: {str(e)}'
                }
            
            if not cart:
                print(f"[ORDER SERVICE] ❌ Carrito no encontrado")
                return {
                    'success': False,
                    'message': 'Carrito no encontrado'
                }

            if not cart.get('items') or len(cart['items']) == 0:
                print(f"[ORDER SERVICE] ❌ Carrito vacío")
                return {
                    'success': False,
                    'message': 'El carrito está vacío'
                }

            colmado_id = cart['colmado_id']
            subtotal = cart['subtotal']
            print(f"[ORDER SERVICE]    - colmado_id: {colmado_id}")
            print(f"[ORDER SERVICE]    - subtotal: {subtotal}")
            print(f"[ORDER SERVICE]    - items: {len(cart['items'])}")

            # 2. Calcular totales
            total = subtotal + delivery_fee
            print(f"[ORDER SERVICE] 💰 PASO 2: Total calculado: {total}")

            # 3. Crear orden en Supabase
            print(f"[ORDER SERVICE] 📝 PASO 3: Creando orden en Supabase...")
            order_data = {
                'user_id': user_id,
                'colmado_id': colmado_id,
                'address_id': address_id,
                'status': 'pending',
                'subtotal': subtotal,
                'delivery_fee': delivery_fee,
                'platform_fee': 0,
                'total': total,
                'customer_notes': customer_notes,
            }
            
            try:
                order = self.supabase.create_order(order_data)
                print(f"[ORDER SERVICE] ✅ Orden creada en Supabase")
                print(f"[ORDER SERVICE]    Order: {order}")
            except Exception as e:
                print(f"[ORDER SERVICE] ❌ ERROR en PASO 3: {str(e)}")
                import traceback
                traceback.print_exc()
                return {
                    'success': False,
                    'message': f'Error creando orden en base de datos: {str(e)}'
                }
        
            if not order:
                return {
                    'success': False,
                    'message': 'Error creando orden'
                }

            order_id = order['id']
            order_number = order['order_number']

            # 4. Crear order_items (snapshot de los productos)
            order_items = []
            for item in cart['items']:
                order_items.append({
                    'order_id': order_id,
                    'product_id': item['product_id'],
                    'product_name': item['product_name'],
                    'product_price': item['price'],
                    'product_image_url': item['image_urls'][0] if item.get('image_urls') else None,
                    'quantity': item['quantity'],
                    'subtotal': item['price'] * item['quantity'],
                })

            self.supabase.create_order_items(order_items)

            # 5. Crear PaymentIntent en Stripe
            payment_result = self.stripe_service.create_payment_intent(
                order_id=order_id,
                amount_dop=total,
                colmado_id=colmado_id,
                user_id=user_id,
                description=f'Pedido {order_number} - {cart["colmado_name"]}'
            )

            if not payment_result['success']:
                # Si falla Stripe, cancelar la orden
                self.supabase.update_order(order_id, {
                    'status': 'cancelled',
                    'cancellation_reason': payment_result['message']
                })
                return payment_result

            # 6. Actualizar orden con status payment_processing
            self.supabase.update_order(order_id, {
                'status': 'payment_processing'
            })

            # 7. Vaciar el carrito
            self.supabase.clear_cart(cart_id)

            # 8. Retornar información para el cliente
            return {
                'success': True,
                'order_id': order_id,
                'order_number': order_number,
                'client_secret': payment_result['client_secret'],
                'amount': total,
                'message': 'Orden creada. Proceder con pago.'
            }

        except Exception as e:
            return {
                'success': False,
                'message': f'Error creando orden: {str(e)}'
            }

    def get_order_details(self, order_id: str, user_id: str):
        """
        Obtiene los detalles completos de una orden

        Args:
            order_id: ID de la orden
            user_id: ID del usuario (para verificar permisos)

        Returns:
            dict con detalles de la orden
        """
        try:
            # Obtener orden
            order = self.supabase.get_order(order_id)
            if not order:
                return {
                    'success': False,
                    'message': 'Orden no encontrada'
                }

            # Verificar que el usuario sea dueño de la orden
            if order['user_id'] != user_id:
                return {
                    'success': False,
                    'message': 'No tienes permiso para ver esta orden'
                }

            # Obtener items
            items = self.supabase.get_order_items(order_id)

            # Obtener colmado
            colmado = self.supabase.get_colmado(order['colmado_id'])

            return {
                'success': True,
                'order': {
                    **order,
                    'items': items,
                    'colmado': colmado
                }
            }

        except Exception as e:
            return {
                'success': False,
                'message': f'Error obteniendo orden: {str(e)}'
            }

    def cancel_order(self, order_id: str, user_id: str, reason: str = None):
        """
        Cancela una orden (solo si no ha sido pagada)

        Args:
            order_id: ID de la orden
            user_id: ID del usuario
            reason: Razón de cancelación

        Returns:
            dict con resultado
        """
        try:
            # Obtener orden
            order = self.supabase.get_order(order_id)
            if not order:
                return {
                    'success': False,
                    'message': 'Orden no encontrada'
                }

            # Verificar permisos
            if order['user_id'] != user_id:
                return {
                    'success': False,
                    'message': 'No tienes permiso para cancelar esta orden'
                }

            # Solo se puede cancelar si está pending o payment_processing
            if order['status'] not in ['pending', 'payment_processing']:
                return {
                    'success': False,
                    'message': f'No se puede cancelar una orden con estado: {order["status"]}'
                }

            # Actualizar orden
            self.supabase.update_order(order_id, {
                'status': 'cancelled',
                'cancelled_at': 'now()',
                'cancellation_reason': reason or 'Cancelado por el usuario'
            })

            return {
                'success': True,
                'message': 'Orden cancelada exitosamente'
            }

        except Exception as e:
            return {
                'success': False,
                'message': f'Error cancelando orden: {str(e)}'
            }
