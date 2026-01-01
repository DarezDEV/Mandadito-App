// deno-lint-ignore-file no-explicit-any
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import Stripe from "https://esm.sh/stripe@14.11.0?target=deno";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3";

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  try {
    // Initialize Stripe
    const stripeSecretKey = Deno.env.get('STRIPE_SECRET_KEY');
    if (!stripeSecretKey) {
      throw new Error('STRIPE_SECRET_KEY not configured');
    }
    const stripe = new Stripe(stripeSecretKey, {
      apiVersion: '2023-10-16',
    });

    // Initialize Supabase
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!;
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // Parse request body
    const { colmado_id } = await req.json();

    console.log('[stripe-connect-refresh] Request:', { colmado_id });

    // Validate required field
    if (!colmado_id) {
      return new Response(
        JSON.stringify({
          success: false,
          message: 'colmado_id es requerido',
        }),
        {
          status: 400,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        }
      );
    }

    // Get account from database
    const { data: accountData, error: fetchError } = await supabase
      .from('stripe_accounts')
      .select('*')
      .eq('colmado_id', colmado_id)
      .single();

    if (fetchError || !accountData) {
      return new Response(
        JSON.stringify({
          success: false,
          message: 'Cuenta Stripe no encontrada',
        }),
        {
          status: 404,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        }
      );
    }

    console.log('[stripe-connect-refresh] Creating new account link...');

    // Create new account link
    const accountLink = await stripe.accountLinks.create({
      account: accountData.stripe_account_id,
      refresh_url: `${supabaseUrl}/functions/v1/stripe-onboarding-refresh`,
      return_url: `${supabaseUrl}/functions/v1/stripe-onboarding-complete`,
      type: 'account_onboarding',
    });

    console.log('[stripe-connect-refresh] Account link created');

    // Update database
    const { error: updateError } = await supabase
      .from('stripe_accounts')
      .update({
        onboarding_url: accountLink.url,
        updated_at: new Date().toISOString(),
      })
      .eq('id', accountData.id);

    if (updateError) {
      console.error('[stripe-connect-refresh] Error updating database:', updateError);
    }

    return new Response(
      JSON.stringify({
        success: true,
        onboarding_url: accountLink.url,
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      }
    );

  } catch (error: any) {
    console.error('[stripe-connect-refresh] Error:', error);
    return new Response(
      JSON.stringify({
        success: false,
        message: error.message || 'Internal server error',
      }),
      {
        status: 500,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      }
    );
  }
});
