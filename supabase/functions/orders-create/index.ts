import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.3'
import Stripe from 'https://esm.sh/stripe@14.11.0?target=deno'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    console.log('📦 orders-create called')

    // Crear cliente de Supabase
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      {
        global: {
          headers: { Authorization: req.headers.get('Authorization')! },
        },
      }
    )

    // Inicializar Stripe
    const stripeSecretKey = Deno.env.get('STRIPE_SECRET_KEY')
    if (!stripeSecretKey) {
      throw new Error('STRIPE_SECRET_KEY not configured')
    }
    const stripe = new Stripe(stripeSecretKey, {
      apiVersion: '2023-10-16',
      httpClient: Stripe.createFetchHttpClient(),
    })

    // Obtener datos del request
    const { userId, cartId, addressId, deliveryFee = 0, customerNotes } = await req.json()
    console.log('📥 Request data:', { userId, cartId, addressId, deliveryFee })

    if (!userId || !cartId || !addressId) {
      return new Response(
        JSON.stringify({
          success: false,
          message: 'Faltan campos requeridos: userId, cartId, addressId',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400,
        }
      )
    }

    // 1. Obtener resumen del carrito (usando la vista view_cart_summary)
    console.log('🔍 PASO 1: Obteniendo carrito...')
    const { data: cartSummary, error: cartError } = await supabaseClient
      .from('view_cart_summary')
      .select('*')
      .eq('cart_id', cartId)
      .single()

    if (cartError || !cartSummary) {
      console.error('❌ Error obteniendo carrito:', cartError)
      return new Response(
        JSON.stringify({
          success: false,
          message: 'Carrito no encontrado',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400,
        }
      )
    }

    // 1.1. Obtener items del carrito (usando la vista view_cart_items)
    console.log('🔍 PASO 1.1: Obteniendo items del carrito...')
    const { data: cartItems, error: itemsError } = await supabaseClient
      .from('view_cart_items')
      .select('*')
      .eq('cart_id', cartId)

    if (itemsError || !cartItems || cartItems.length === 0) {
      console.error('❌ Error obteniendo items:', itemsError)
      return new Response(
        JSON.stringify({
          success: false,
          message: 'El carrito está vacío',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400,
        }
      )
    }

    console.log(`✅ Carrito obtenido con ${cartItems.length} items`)
    console.log(`   Subtotal (de vista): ${cartSummary.subtotal}`)

    const colmadoId = cartSummary.colmado_id
    const subtotal = cartSummary.subtotal

    // 2. Cancelar órdenes pendientes previas
    console.log('🔍 PASO 2: Cancelando órdenes pendientes previas...')
    const { data: pendingOrders } = await supabaseClient
      .from('orders')
      .select('id, order_number')
      .eq('user_id', userId)
      .eq('colmado_id', colmadoId)
      .in('status', ['pending', 'payment_processing'])

    if (pendingOrders && pendingOrders.length > 0) {
      console.log(`⚠️ Cancelando ${pendingOrders.length} órdenes pendientes...`)
      await supabaseClient
        .from('orders')
        .update({
          status: 'cancelled',
          cancelled_at: new Date().toISOString(),
          cancellation_reason: 'Orden reemplazada por nuevo intento de pago',
        })
        .in('id', pendingOrders.map((o: any) => o.id))
    }

    // 3. Calcular total
    const total = subtotal + deliveryFee
    console.log(`💰 PASO 3: Total calculado: ${total}`)

    // 4. Crear orden en Supabase
    console.log('📝 PASO 4: Creando orden...')
    const { data: order, error: orderError } = await supabaseClient
      .from('orders')
      .insert({
        user_id: userId,
        colmado_id: colmadoId,
        address_id: addressId,
        status: 'pending',
        subtotal: subtotal,
        delivery_fee: deliveryFee,
        platform_fee: 0,
        total: total,
        customer_notes: customerNotes,
      })
      .select()
      .single()

    if (orderError || !order) {
      console.error('❌ Error creando orden:', orderError)
      return new Response(
        JSON.stringify({
          success: false,
          message: `Error creando orden: ${orderError?.message}`,
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 500,
        }
      )
    }

    console.log(`✅ Orden creada: ${order.order_number}`)

    // 5. Crear order_items
    console.log('📝 PASO 5: Creando items de la orden...')
    const orderItems = cartItems.map((item: any) => ({
      order_id: order.id,
      product_id: item.product_id,
      product_name: item.product_name,
      product_price: item.price,
      product_image_url: item.image_urls?.[0] || null,
      quantity: item.quantity,
      subtotal: item.total,
    }))

    const { error: orderItemsError } = await supabaseClient
      .from('order_items')
      .insert(orderItems)

    if (orderItemsError) {
      console.error('❌ Error creando items:', orderItemsError)
      // Cancelar la orden si fallan los items
      await supabaseClient
        .from('orders')
        .update({ status: 'cancelled', cancellation_reason: 'Error creando items' })
        .eq('id', order.id)

      return new Response(
        JSON.stringify({
          success: false,
          message: 'Error creando items de la orden',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 500,
        }
      )
    }

    console.log('✅ Items creados')

    // 6. Obtener cuenta Stripe del colmado
    console.log('💳 PASO 6: Obteniendo cuenta Stripe del colmado...')
    const { data: stripeAccount, error: stripeAccountError } = await supabaseClient
      .from('stripe_accounts')
      .select('id, stripe_account_id, charges_enabled')
      .eq('colmado_id', colmadoId)
      .single()

    if (stripeAccountError || !stripeAccount) {
      console.error('❌ Colmado no tiene cuenta Stripe configurada')
      await supabaseClient
        .from('orders')
        .update({
          status: 'cancelled',
          cancellation_reason: 'Colmado no tiene cuenta Stripe configurada'
        })
        .eq('id', order.id)

      return new Response(
        JSON.stringify({
          success: false,
          message: 'El vendedor no ha configurado su cuenta de pagos',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400,
        }
      )
    }

    // Verificar que charges esté habilitado
    if (!stripeAccount.charges_enabled) {
      console.error('❌ Colmado no ha completado su onboarding')
      await supabaseClient
        .from('orders')
        .update({
          status: 'cancelled',
          cancellation_reason: 'El colmado no ha completado su onboarding de Stripe'
        })
        .eq('id', order.id)

      return new Response(
        JSON.stringify({
          success: false,
          message: 'El vendedor no ha completado su onboarding de Stripe',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400,
        }
      )
    }

    // 7. Crear PaymentIntent con Stripe Connect
    console.log('💳 PASO 7: Creando PaymentIntent...')
    const amountInCents = Math.floor(total * 100) // DOP en centavos (truncar como int() de Python)
    // Usar variable de entorno o 5% por defecto
    const feeEnv = Deno.env.get('STRIPE_PLATFORM_FEE_PERCENT')
    const platformFeePercent = feeEnv ? parseFloat(feeEnv) : 5
    const platformFeeCents = Math.floor(amountInCents * (platformFeePercent / 100)) // % de comisión (truncar)
    const transferAmountCents = amountInCents - platformFeeCents // Lo que recibe el colmado

    console.log(`💰 Montos: total=${amountInCents}, fee=${platformFeeCents}, transfer=${transferAmountCents}`)

    try {
      const paymentIntent = await stripe.paymentIntents.create({
        amount: amountInCents,
        currency: 'usd', // Usar USD porque Stripe no soporta DOP directamente
        payment_method_types: ['card'],
        transfer_data: {
          destination: stripeAccount.stripe_account_id,
          amount: transferAmountCents, // Monto sin comisión de plataforma
        },
        metadata: {
          order_id: order.id,
          order_number: order.order_number,
          user_id: userId,
          colmado_id: colmadoId,
          platform_fee_cents: platformFeeCents.toString(),
        },
        description: `Pedido ${order.order_number} - ${cartSummary.colmado_name}`,
      })

      console.log(`✅ PaymentIntent creado: ${paymentIntent.id}`)

      // 7.1. Guardar registro en tabla payments
      console.log('💾 Guardando registro de pago...')
      const { error: paymentError } = await supabaseClient
        .from('payments')
        .insert({
          order_id: order.id,
          user_id: userId,
          colmado_id: colmadoId,
          stripe_account_id: stripeAccount.id,
          stripe_payment_intent_id: paymentIntent.id,
          amount: amountInCents,
          platform_fee_amount: platformFeeCents,
          transfer_amount: transferAmountCents,
          currency: 'DOP',
          status: 'pending',
          client_secret: paymentIntent.client_secret,
        })

      if (paymentError) {
        console.error('⚠️ Error guardando payment:', paymentError)
      } else {
        console.log('✅ Payment record creado')
      }

      // 8. Actualizar orden con payment_processing
      await supabaseClient
        .from('orders')
        .update({ status: 'payment_processing' })
        .eq('id', order.id)

      // 9. Retornar respuesta exitosa
      return new Response(
        JSON.stringify({
          success: true,
          orderId: order.id,
          orderNumber: order.order_number,
          clientSecret: paymentIntent.client_secret,
          amount: total,
          message: 'Orden creada. Proceder con pago.',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 200,
        }
      )
    } catch (stripeError: any) {
      console.error('❌ Error en Stripe:', stripeError)

      // Cancelar la orden
      await supabaseClient
        .from('orders')
        .update({
          status: 'cancelled',
          cancellation_reason: `Error en Stripe: ${stripeError.message}`
        })
        .eq('id', order.id)

      return new Response(
        JSON.stringify({
          success: false,
          message: `Error creando PaymentIntent: ${stripeError.message}`,
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 500,
        }
      )
    }
  } catch (error: any) {
    console.error('❌ Error general:', error)
    return new Response(
      JSON.stringify({
        success: false,
        message: error.message,
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 500,
      }
    )
  }
})
