import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.3'
import Stripe from 'https://esm.sh/stripe@14.11.0?target=deno'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type, stripe-signature',
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    console.log('🪝 stripe-webhook called')

    // Inicializar Stripe
    const stripeSecretKey = Deno.env.get('STRIPE_SECRET_KEY')
    const webhookSecret = Deno.env.get('STRIPE_WEBHOOK_SECRET')

    if (!stripeSecretKey || !webhookSecret) {
      throw new Error('STRIPE_SECRET_KEY or STRIPE_WEBHOOK_SECRET not configured')
    }

    const stripe = new Stripe(stripeSecretKey, {
      apiVersion: '2023-10-16',
      httpClient: Stripe.createFetchHttpClient(),
    })

    // Obtener la firma del webhook
    const signature = req.headers.get('stripe-signature')
    if (!signature) {
      console.error('❌ No signature header')
      return new Response('No signature', { status: 400 })
    }

    // Obtener el body como texto
    const body = await req.text()

    // Verificar la firma del webhook
    let event: Stripe.Event
    try {
      event = stripe.webhooks.constructEvent(body, signature, webhookSecret)
      console.log(`✅ Webhook verificado: ${event.type}`)
    } catch (err: any) {
      console.error(`❌ Webhook signature verification failed: ${err.message}`)
      return new Response(`Webhook Error: ${err.message}`, { status: 400 })
    }

    // Crear cliente de Supabase (admin)
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // Manejar diferentes tipos de eventos
    switch (event.type) {
      case 'payment_intent.succeeded': {
        const paymentIntent = event.data.object as Stripe.PaymentIntent
        console.log(`💰 Payment succeeded: ${paymentIntent.id}`)

        // Obtener order_id de los metadata
        const orderId = paymentIntent.metadata.order_id

        if (!orderId) {
          console.error('❌ No order_id in metadata')
          break
        }

        // 1. Actualizar registro de payment
        const { error: paymentUpdateError } = await supabaseAdmin
          .from('payments')
          .update({
            status: 'succeeded',
            succeeded_at: new Date().toISOString(),
            stripe_charge_id: paymentIntent.latest_charge,
            payment_method_type: paymentIntent.payment_method_types?.[0] || null,
            amount_captured: paymentIntent.amount_received,
          })
          .eq('stripe_payment_intent_id', paymentIntent.id)

        if (paymentUpdateError) {
          console.error('❌ Error updating payment:', paymentUpdateError)
        } else {
          console.log('✅ Payment record updated')
        }

        // 2. Actualizar la orden a 'paid'
        const { error: updateError } = await supabaseAdmin
          .from('orders')
          .update({
            status: 'paid',
            paid_at: new Date().toISOString(),
          })
          .eq('id', orderId)

        if (updateError) {
          console.error('❌ Error updating order:', updateError)
        } else {
          console.log(`✅ Order ${orderId} marked as paid`)
          // NOTA: El carrito se vacía cuando el cliente llama a orders-confirm-payment
          // No se vacía aquí para evitar condiciones de carrera
        }
        break
      }

      case 'payment_intent.payment_failed': {
        const paymentIntent = event.data.object as Stripe.PaymentIntent
        console.log(`❌ Payment failed: ${paymentIntent.id}`)

        const orderId = paymentIntent.metadata.order_id

        if (orderId) {
          // Obtener información del error
          const errorMessage = paymentIntent.last_payment_error?.message || 'El pago falló'
          const errorCode = paymentIntent.last_payment_error?.code || null

          // 1. Actualizar registro de payment
          await supabaseAdmin
            .from('payments')
            .update({
              status: 'failed',
              failed_at: new Date().toISOString(),
              error_message: errorMessage,
              error_code: errorCode,
            })
            .eq('stripe_payment_intent_id', paymentIntent.id)

          console.log('✅ Payment record updated to failed')

          // 2. Actualizar la orden a 'cancelled'
          await supabaseAdmin
            .from('orders')
            .update({
              status: 'cancelled',
              cancelled_at: new Date().toISOString(),
              cancellation_reason: `Pago fallido: ${errorMessage}`,
            })
            .eq('id', orderId)

          console.log(`❌ Order ${orderId} marked as cancelled (payment failed)`)
        }
        break
      }

      case 'payment_intent.canceled': {
        const paymentIntent = event.data.object as Stripe.PaymentIntent
        console.log(`🚫 Payment canceled: ${paymentIntent.id}`)

        const orderId = paymentIntent.metadata.order_id

        if (orderId) {
          // Actualizar la orden a 'cancelled'
          await supabaseAdmin
            .from('orders')
            .update({
              status: 'cancelled',
              cancelled_at: new Date().toISOString(),
              cancellation_reason: 'Pago cancelado',
            })
            .eq('id', orderId)

          console.log(`🚫 Order ${orderId} marked as cancelled`)
        }
        break
      }

      case 'charge.refunded': {
        const charge = event.data.object as Stripe.Charge
        console.log(`💸 Charge refunded: ${charge.id}`)

        // Buscar la orden por el payment_intent
        const { data: order } = await supabaseAdmin
          .from('orders')
          .select('id')
          .eq('status', 'paid')
          .limit(1)
          .single()

        if (order) {
          await supabaseAdmin
            .from('orders')
            .update({
              status: 'refunded',
              cancellation_reason: 'Reembolsado',
            })
            .eq('id', order.id)

          console.log(`💸 Order refunded`)
        }
        break
      }

      default:
        console.log(`ℹ️ Unhandled event type: ${event.type}`)
    }

    return new Response(JSON.stringify({ received: true }), {
      headers: { 'Content-Type': 'application/json' },
      status: 200,
    })
  } catch (error: any) {
    console.error('❌ Error processing webhook:', error)
    return new Response(
      JSON.stringify({
        error: error.message,
      }),
      {
        headers: { 'Content-Type': 'application/json' },
        status: 500,
      }
    )
  }
})
