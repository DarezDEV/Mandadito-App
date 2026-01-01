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
    console.log('📋 orders-get-colmado called')

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

    // Obtener colmadoId del query params
    const url = new URL(req.url)
    const colmadoId = url.searchParams.get('colmadoId')

    console.log('📥 Request params:', { colmadoId })

    if (!colmadoId) {
      return new Response(
        JSON.stringify({
          success: false,
          message: 'colmadoId es requerido',
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400,
        }
      )
    }

    // Obtener todas las órdenes del colmado con sus detalles
    console.log('🔍 Obteniendo órdenes del colmado...')
    const { data: orders, error: ordersError } = await supabaseClient
      .from('orders')
      .select(`
        *,
        items:order_items(*),
        address:addresses(*)
      `)
      .eq('colmado_id', colmadoId)
      .order('created_at', { ascending: false })

    if (ordersError) {
      console.error('❌ Error obteniendo órdenes:', ordersError)
      return new Response(
        JSON.stringify({
          success: false,
          message: `Error obteniendo órdenes: ${ordersError.message}`,
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 500,
        }
      )
    }

    console.log(`✅ ${orders?.length || 0} órdenes encontradas`)

    // Transformar a formato esperado
    const formattedOrders = (orders || []).map((order: any) => ({
      order: {
        id: order.id,
        orderNumber: order.order_number,
        userId: order.user_id,
        colmadoId: order.colmado_id,
        addressId: order.address_id,
        deliveryUserId: order.delivery_user_id,
        status: order.status,
        subtotal: order.subtotal,
        deliveryFee: order.delivery_fee,
        platformFee: order.platform_fee,
        total: order.total,
        customerNotes: order.customer_notes,
        deliveryNotes: order.delivery_notes,
        cancellationReason: order.cancellation_reason,
        createdAt: order.created_at,
        updatedAt: order.updated_at,
        paidAt: order.paid_at,
        deliveredAt: order.delivered_at,
        cancelledAt: order.cancelled_at,
      },
      items: (order.items || []).map((item: any) => ({
        id: item.id,
        orderId: item.order_id,
        productId: item.product_id,
        productName: item.product_name,
        productPrice: item.product_price,
        productImageUrl: item.product_image_url,
        quantity: item.quantity,
        subtotal: item.subtotal,
      })),
      address: order.address ? {
        id: order.address.id,
        street: order.address.street,
        city: order.address.city,
        province: order.address.province,
        postalCode: order.address.postal_code,
        country: order.address.country,
        reference: order.address.reference,
      } : null,
    }))

    // Retornar órdenes
    return new Response(
      JSON.stringify({
        success: true,
        orders: formattedOrders,
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
