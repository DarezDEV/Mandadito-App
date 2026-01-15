import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.39.3'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    console.log('📊 finance-report called')

    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      {
        global: {
          headers: { Authorization: req.headers.get('Authorization')! },
        },
      }
    )

    // Obtener todas las ordenes con informacion del colmado
    const { data: orders, error: ordersError } = await supabaseClient
      .from('orders')
      .select(`
        id,
        order_number,
        status,
        subtotal,
        delivery_fee,
        platform_fee,
        total,
        created_at,
        paid_at,
        delivered_at,
        colmado_id,
        colmados!inner(name)
      `)
      .order('created_at', { ascending: false })
      .limit(100)

    if (ordersError) {
      console.error('Error fetching orders:', ordersError)
      return new Response(
        JSON.stringify({ success: false, message: ordersError.message }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
      )
    }

    console.log(`Ordenes encontradas: ${orders?.length}`)

    // Obtener pagos para enriquecimiento
    const orderIds = orders?.map(o => o.id) ?? []
    const { data: payments, error: paymentsError } = await supabaseClient
      .from('payments')
      .select('*')
      .in('order_id', orderIds)

    if (paymentsError) {
      console.error('Error fetching payments:', paymentsError)
    }

    // Crear mapa de pagos
    const paymentMap = new Map()
    payments?.forEach(p => paymentMap.set(p.order_id, p))

    // Enrich orders con nombre del colmado y datos de pago
    const enrichedOrders = orders?.map(o => {
      const payment = paymentMap.get(o.id)
      const colmadoName = Array.isArray(o.colmados) 
        ? o.colmados[0]?.name 
        : (o.colmados as any)?.name || 'Sin nombre'
      
      return {
        ...o,
        colmado_name: colmadoName,
        payment_status: payment?.status || 'no_payment',
        payment_id: payment?.id || null,
        stripe_payment_intent_id: payment?.stripe_payment_intent_id || null,
        card_brand: payment?.card_brand || null,
        card_last4: payment?.card_last4 || null,
        error_message: payment?.error_message || null,
        succeeded_at: payment?.succeeded_at || null,
        failed_at: payment?.failed_at || null
      }
    }) ?? []

    // Contar por estado
    const successfulOrders = enrichedOrders.filter(o => 
      o.status === 'paid' || o.status === 'delivered' || o.status === 'preparing'
    )
    const failedOrders = enrichedOrders.filter(o => 
      o.status === 'cancelled' || o.payment_status === 'failed'
    )
    const pendingOrders = enrichedOrders.filter(o => 
      o.status === 'pending' || o.status === 'payment_processing'
    )

    // Calcular metricas
    const totalRevenue = successfulOrders.reduce((sum, o) => sum + (o.total || 0), 0)
    const totalPlatformFees = successfulOrders.reduce((sum, o) => sum + (o.platform_fee || 0), 0)
    const totalTransfers = successfulOrders.reduce((sum, o) => sum + ((o.total || 0) - (o.platform_fee || 0)), 0)
    const totalOrdersCount = successfulOrders.length
    const averageOrderValue = totalOrdersCount > 0 ? totalRevenue / totalOrdersCount : 0

    // Ingresos por colmado
    const colmadoGroups = successfulOrders.reduce((acc, o) => {
      if (!acc[o.colmado_id]) {
        acc[o.colmado_id] = {
          colmadoId: o.colmado_id,
          colmadoName: o.colmado_name,
          totalRevenue: 0,
          platformFees: 0,
          orderCount: 0
        }
      }
      acc[o.colmado_id].totalRevenue += o.total || 0
      acc[o.colmado_id].platformFees += o.platform_fee || 0
      acc[o.colmado_id].orderCount += 1
      return acc
    }, {} as Record<string, any>)

    const revenueByColmado = Object.values(colmadoGroups)
      .map((c: any) => ({
        ...c,
        netRevenue: c.totalRevenue - c.platformFees,
        successfulPayments: c.orderCount,
        failedPayments: enrichedOrders.filter(o => o.colmado_id === c.colmadoId && o.status === 'cancelled').length
      }))
      .sort((a: any, b: any) => b.totalRevenue - a.totalRevenue)

    // Ingresos diarios (ultimos 30 dias)
    const last30Days = new Date()
    last30Days.setDate(last30Days.getDate() - 30)

    const dailyGroups = enrichedOrders
      .filter(o => 
        (o.status === 'paid' || o.status === 'delivered' || o.status === 'preparing') && 
        new Date(o.created_at) >= last30Days
      )
      .reduce((acc, o) => {
        const date = o.created_at.split('T')[0]
        if (!acc[date]) {
          acc[date] = { date, revenue: 0, platformFees: 0, orders: 0 }
        }
        acc[date].revenue += o.total || 0
        acc[date].platformFees += o.platform_fee || 0
        acc[date].orders += 1
        return acc
      }, {} as Record<string, any>)

    const dailyRevenue = Object.values(dailyGroups)
      .map((d: any) => ({
        ...d,
        successfulPayments: d.orders,
        failedPayments: enrichedOrders.filter(o => 
          o.created_at.split('T')[0] === d.date && o.status === 'cancelled'
        ).length
      }))
      .sort((a: any, b: any) => a.date.localeCompare(b.date))

    // Funcion para mapear estado de orden a estado de pago
    function mapOrderStatusToPaymentStatus(orderStatus: string): string {
      const successfulStatuses = ['paid', 'delivered', 'preparing', 'ready_for_pickup', 'in_delivery']
      const failedStatuses = ['cancelled', 'refunded']
      
      if (successfulStatuses.includes(orderStatus)) return 'succeeded'
      if (failedStatuses.includes(orderStatus)) return 'failed'
      return 'pending'
    }

    // Resumen financiero
    const summary = {
      totalRevenue,
      totalPlatformFees,
      totalTransfers,
      totalOrders: totalOrdersCount,
      successfulPayments: successfulOrders.length,
      failedPayments: failedOrders.length,
      pendingPayments: pendingOrders.length,
      averageOrderValue,
      activeColmados: Object.keys(colmadoGroups).length
    }

    // Generar pagos desde ordenes para el historial
    const recentPayments = enrichedOrders.map(o => ({
      id: o.payment_id || o.id,
      order_id: o.id,
      order_number: o.order_number,
      stripe_payment_intent_id: o.stripe_payment_intent_id || '',
      stripe_charge_id: null,
      stripe_transfer_id: null,
      amount: Math.round((o.total || 0) * 100),
      amount_captured: Math.round((o.total || 0) * 100),
      platform_fee_amount: Math.round((o.platform_fee || 0) * 100),
      transfer_amount: Math.round(((o.total || 0) - (o.platform_fee || 0)) * 100),
      currency: 'DOP',
      status: mapOrderStatusToPaymentStatus(o.status),
      payment_method_type: null,
      card_brand: o.card_brand,
      card_last4: o.card_last4,
      error_code: null,
      error_message: o.error_message,
      created_at: o.created_at,
      succeeded_at: o.succeeded_at || o.paid_at,
      failed_at: o.failed_at,
      colmado_id: o.colmado_id,
      colmado_name: o.colmado_name
    }))

    console.log(`✅ Reporte: ${enrichedOrders.length} ordenes, ${revenueByColmado.length} colmados`)
    console.log(`   Resumen: ingresos=${totalRevenue}, comisiones=${totalPlatformFees}, exitosas=${successfulOrders.length}`)

    return new Response(
      JSON.stringify({
        success: true,
        summary,
        dailyRevenue,
        revenueByColmado,
        recentPayments: recentPayments.slice(0, 50),
        debugOrders: enrichedOrders.slice(0, 5) // Para verificar datos
      }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
    )

  } catch (error: any) {
    console.error('Error:', error)
    return new Response(
      JSON.stringify({ success: false, message: error.message }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
    )
  }
})
