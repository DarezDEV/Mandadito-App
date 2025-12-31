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
    // Initialize Supabase
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!;
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    // Parse request body
    const { colmado_id } = await req.json();

    console.log('[stripe-connect-status] Request:', { colmado_id });

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

    if (fetchError && fetchError.code !== 'PGRST116') {
      throw fetchError;
    }

    // If no account exists, return success with has_account=false
    if (!accountData) {
      console.log('[stripe-connect-status] No account found');
      return new Response(
        JSON.stringify({
          success: true,
          has_account: false,
          onboarding_completed: false,
          charges_enabled: false,
          payouts_enabled: false,
          account_id: null,
          onboarding_url: null,
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        }
      );
    }

    console.log('[stripe-connect-status] Account found, fetching from Stripe...');

    // Initialize Stripe
    const stripeSecretKey = Deno.env.get('STRIPE_SECRET_KEY');
    if (!stripeSecretKey) {
      throw new Error('STRIPE_SECRET_KEY not configured');
    }
    const stripe = new Stripe(stripeSecretKey, {
      apiVersion: '2023-10-16',
    });

    // Get updated account info from Stripe
    const account = await stripe.accounts.retrieve(accountData.stripe_account_id);

    console.log('[stripe-connect-status] Stripe account retrieved:', {
      id: account.id,
      details_submitted: account.details_submitted,
      charges_enabled: account.charges_enabled,
      payouts_enabled: account.payouts_enabled,
    });

    // Update database with latest info
    const { error: updateError } = await supabase
      .from('stripe_accounts')
      .update({
        onboarding_completed: account.details_submitted,
        charges_enabled: account.charges_enabled,
        payouts_enabled: account.payouts_enabled,
        updated_at: new Date().toISOString(),
      })
      .eq('id', accountData.id);

    if (updateError) {
      console.error('[stripe-connect-status] Error updating database:', updateError);
    }

    return new Response(
      JSON.stringify({
        success: true,
        has_account: true,
        account_id: account.id,
        onboarding_completed: account.details_submitted,
        charges_enabled: account.charges_enabled,
        payouts_enabled: account.payouts_enabled,
        onboarding_url: accountData.onboarding_url,
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      }
    );

  } catch (error: any) {
    console.error('[stripe-connect-status] Error:', error);
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
