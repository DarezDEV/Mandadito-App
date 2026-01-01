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
    const { colmado_id, email, business_name } = await req.json();

    console.log('[stripe-connect-create] Request:', { colmado_id, email, business_name });

    // Validate required fields
    if (!colmado_id || !email || !business_name) {
      return new Response(
        JSON.stringify({
          success: false,
          message: 'Missing required fields: colmado_id, email, business_name',
        }),
        {
          status: 400,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        }
      );
    }

    // Check if account already exists
    const { data: existingAccount, error: fetchError } = await supabase
      .from('stripe_accounts')
      .select('*')
      .eq('colmado_id', colmado_id)
      .single();

    if (fetchError && fetchError.code !== 'PGRST116') {
      throw fetchError;
    }

    if (existingAccount) {
      console.log('[stripe-connect-create] Account already exists');
      return new Response(
        JSON.stringify({
          success: false,
          message: 'Este colmado ya tiene una cuenta Stripe',
          account_id: existingAccount.stripe_account_id,
        }),
        {
          status: 400,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        }
      );
    }

    console.log('[stripe-connect-create] Creating Stripe Express account...');

    // Create Stripe Express account
    const account = await stripe.accounts.create({
      type: 'express',
      country: 'US',
      email: email,
      business_type: 'individual',
      capabilities: {
        card_payments: { requested: true },
        transfers: { requested: true },
      },
      business_profile: {
        name: business_name,
        product_description: 'Venta de productos de colmado',
      },
    });

    console.log('[stripe-connect-create] Stripe account created:', account.id);

    // Create account link for onboarding
    const accountLink = await stripe.accountLinks.create({
      account: account.id,
      refresh_url: `${supabaseUrl}/functions/v1/stripe-onboarding-refresh`,
      return_url: `${supabaseUrl}/functions/v1/stripe-onboarding-complete`,
      type: 'account_onboarding',
    });

    console.log('[stripe-connect-create] Onboarding link created');

    // Save to Supabase
    const { error: insertError } = await supabase
      .from('stripe_accounts')
      .insert({
        colmado_id: colmado_id,
        stripe_account_id: account.id,
        onboarding_completed: false,
        charges_enabled: false,
        payouts_enabled: false,
        onboarding_url: accountLink.url,
        country: 'US',
        currency: 'USD',
        business_type: 'individual',
      });

    if (insertError) {
      console.error('[stripe-connect-create] Error saving to database:', insertError);
      throw insertError;
    }

    console.log('[stripe-connect-create] Account saved to database');

    return new Response(
      JSON.stringify({
        success: true,
        account_id: account.id,
        onboarding_url: accountLink.url,
        message: 'Cuenta creada. Completar onboarding.',
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      }
    );

  } catch (error: any) {
    console.error('[stripe-connect-create] Error:', error);
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
