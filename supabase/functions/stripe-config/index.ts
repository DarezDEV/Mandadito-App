import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'

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
    console.log('📡 stripe-config called')

    // Obtener la publishable key de Stripe desde variables de entorno
    const stripePublishableKey = Deno.env.get('STRIPE_PUBLISHABLE_KEY')

    if (!stripePublishableKey) {
      throw new Error('STRIPE_PUBLISHABLE_KEY not configured')
    }

    // Retornar la configuración
    return new Response(
      JSON.stringify({
        publishableKey: stripePublishableKey,
        currency: 'usd',
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200,
      }
    )
  } catch (error) {
    console.error('❌ Error in stripe-config:', error)
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
