import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.3'

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
    console.log('✅ orders-confirm-payment called')

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

    // Obtener orderId de la URL
    // URL format: /functions/v1/orders-confirm-payment/{orderId}
    const url = new URL(req.url)
    const pathParts = url.pathname.split('/')
    const orderId = pathParts[pathParts.indexOf('orders-confirm-payment') + 1]

    // Obtener datos del request
    const { userId, cartId, paymentIntentId } = await req.json()
    console.log('📥 Request data:', { orderId, userId, cartId, paymentIntentId })

    if (!userId) {
      return new Response(
        JSON.stringify({
          success: false,
          message: 'userId es requerido',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400,
        }
      )
    }

    // 1. Verificar que la orden existe y pertenece al usuario
    const { data: order, error: orderError } = await supabaseClient
      .from('orders')
      .select('*')
      .eq('id', orderId)
      .eq('user_id', userId)
      .single()

    if (orderError || !order) {
      console.error('❌ Orden no encontrada:', orderError)
      return new Response(
        JSON.stringify({
          success: false,
          message: 'Orden no encontrada',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 404,
        }
      )
    }

    console.log(`✅ Orden encontrada: ${order.order_number}`)

    // 2. Verificar que la orden esté en estado válido para confirmar
    if (order.status !== 'pending' && order.status !== 'payment_processing') {
      return new Response(
        JSON.stringify({
          success: false,
          message: `La orden ya tiene estado: ${order.status}`,
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400,
        }
      )
    }

    // 3. Actualizar estado de la orden a 'paid'
    const { error: updateError } = await supabaseClient
      .from('orders')
      .update({
        status: 'paid',
        paid_at: new Date().toISOString(),
      })
      .eq('id', orderId)

    if (updateError) {
      console.error('❌ Error actualizando orden:', updateError)
      return new Response(
        JSON.stringify({
          success: false,
          message: 'Error confirmando pago',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 500,
        }
      )
    }

    console.log('✅ Orden actualizada a estado: paid')

    // 3.1. Actualizar registro de payment si existe paymentIntentId
    if (paymentIntentId) {
      console.log(`💳 Actualizando payment record: ${paymentIntentId}`)
      const { error: paymentUpdateError } = await supabaseClient
        .from('payments')
        .update({
          status: 'succeeded',
          succeeded_at: new Date().toISOString(),
        })
        .eq('stripe_payment_intent_id', paymentIntentId)

      if (paymentUpdateError) {
        console.error('⚠️ Error actualizando payment:', paymentUpdateError)
      } else {
        console.log('✅ Payment record actualizado')
      }
    }

    // 3. Vaciar el carrito si se proporcionó cartId
    if (cartId) {
      console.log(`🗑️ Vaciando carrito: ${cartId}`)

      // Eliminar items del carrito
      await supabaseClient
        .from('cart_items')
        .delete()
        .eq('cart_id', cartId)

      // Actualizar carrito
      await supabaseClient
        .from('carts')
        .update({ updated_at: new Date().toISOString() })
        .eq('id', cartId)

      console.log('✅ Carrito vaciado')
    }

    // 4. Retornar éxito
    return new Response(
      JSON.stringify({
        success: true,
        message: 'Pago confirmado exitosamente',
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200,
      }
    )
  } catch (error: any) {
    console.error('❌ Error:', error)
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
