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
    console.log('❌ orders-cancel called')

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
    // URL format: /functions/v1/orders-cancel/{orderId}
    const url = new URL(req.url)
    const pathParts = url.pathname.split('/')
    const orderId = pathParts[pathParts.indexOf('orders-cancel') + 1]

    // Obtener datos del request
    const { userId, reason } = await req.json()
    console.log('📥 Request data:', { orderId, userId, reason })

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

    // 2. Verificar que la orden se puede cancelar (no está pagada o completada)
    if (order.status === 'paid' || order.status === 'completed' || order.status === 'cancelled') {
      return new Response(
        JSON.stringify({
          success: false,
          message: `No se puede cancelar una orden con estado: ${order.status}`,
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400,
        }
      )
    }

    // 3. Actualizar estado de la orden a 'cancelled'
    const { error: updateError } = await supabaseClient
      .from('orders')
      .update({
        status: 'cancelled',
        cancelled_at: new Date().toISOString(),
        cancellation_reason: reason || 'Cancelada por el usuario',
      })
      .eq('id', orderId)

    if (updateError) {
      console.error('❌ Error cancelando orden:', updateError)
      return new Response(
        JSON.stringify({
          success: false,
          message: 'Error cancelando orden',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 500,
        }
      )
    }

    console.log('✅ Orden cancelada')

    // 4. Retornar éxito
    return new Response(
      JSON.stringify({
        success: true,
        message: 'Orden cancelada exitosamente',
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
