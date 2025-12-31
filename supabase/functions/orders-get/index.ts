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
    console.log('📋 orders-get called')

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
    const url = new URL(req.url)
    const pathParts = url.pathname.split('/')
    const orderId = pathParts[pathParts.length - 1]
    const userId = url.searchParams.get('userId')

    console.log('📥 Request params:', { orderId, userId })

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

    // Obtener orden con sus detalles
    const { data: order, error: orderError } = await supabaseClient
      .from('orders')
      .select(`
        *,
        items:order_items(*),
        colmado:colmados(*),
        address:addresses(*)
      `)
      .eq('id', orderId)
      .eq('user_id', userId)
      .single()

    if (orderError || !order) {
      console.error('❌ Error obteniendo orden:', orderError)
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

    // Retornar orden
    return new Response(
      JSON.stringify({
        success: true,
        order: {
          id: order.id,
          orderNumber: order.order_number,
          status: order.status,
          total: order.total,
          subtotal: order.subtotal,
          deliveryFee: order.delivery_fee,
          platformFee: order.platform_fee,
          customerNotes: order.customer_notes,
          createdAt: order.created_at,
          items: order.items,
          colmado: order.colmado,
          address: order.address,
        },
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
