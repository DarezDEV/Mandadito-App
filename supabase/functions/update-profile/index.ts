// Edge Function: update-profile
// Ruta en Supabase: supabase/functions/update-profile/index.ts

// deno-lint-ignore-file no-explicit-any
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.38.4";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

interface UpdateProfileRequest {
  nombre: string;
  email: string;
  avatar_base64?: string;
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    console.log("🔷 Update Profile Edge Function - Inicio");

    // 1. Obtener el token de autenticación
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      throw new Error("No authorization header");
    }

    // 2. Crear cliente de Supabase con SERVICE_ROLE_KEY
    const supabaseAdmin = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
      {
        auth: {
          autoRefreshToken: false,
          persistSession: false,
        },
      }
    );

    // 3. Validar el token usando el cliente admin
    // Extraer el token del header (formato: "Bearer TOKEN")
    const token = authHeader.replace("Bearer ", "");

    console.log("🔍 Validando token...");

    // Obtener el usuario desde el token usando el cliente admin
    const {
      data: { user },
      error: userError,
    } = await supabaseAdmin.auth.getUser(token);

    if (userError || !user) {
      console.error("❌ Error obteniendo usuario:", userError?.message);
      throw new Error("Usuario no autenticado");
    }

    console.log(`✅ Usuario autenticado: ${user.id}`);

    // 5. Parsear el body de la request
    const requestData: UpdateProfileRequest = await req.json();
    const { nombre, email, avatar_base64 } = requestData;

    console.log(`📝 Datos a actualizar: nombre=${nombre}, email=${email}`);

    if (!nombre || !email) {
      throw new Error("Nombre y email son requeridos");
    }

    // 6. Validar formato de email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      throw new Error("Formato de email inválido");
    }

    let avatarUrl: string | null = null;

    // 7. Si hay avatar, subirlo a Storage
    if (avatar_base64) {
      try {
        console.log("📸 Procesando avatar...");

        // Extraer el contenido base64 (remover el prefijo data:image/jpeg;base64,)
        const base64Data = avatar_base64.split(",")[1];

        // Convertir base64 a bytes
        const binaryString = atob(base64Data);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
          bytes[i] = binaryString.charCodeAt(i);
        }

        // Generar nombre único para el archivo
        const fileName = `${user.id}_${Date.now()}.jpg`;
        const filePath = `avatars/${fileName}`;

        console.log(`📤 Subiendo avatar: ${filePath}`);

        // Subir a Storage
        const { data: uploadData, error: uploadError } = await supabaseAdmin
          .storage
          .from("avatars")
          .upload(filePath, bytes, {
            contentType: "image/jpeg",
            upsert: true,
          });

        if (uploadError) {
          console.error("❌ Error subiendo avatar:", uploadError);
          throw uploadError;
        }

        console.log("✅ Avatar subido exitosamente");

        // Obtener URL pública
        const { data: urlData } = supabaseAdmin
          .storage
          .from("avatars")
          .getPublicUrl(filePath);

        avatarUrl = urlData.publicUrl;
        console.log(`🖼️ URL pública del avatar: ${avatarUrl}`);

        // Eliminar avatar anterior si existe
        try {
          const { data: profile } = await supabaseAdmin
            .from("profiles")
            .select("avatar_url")
            .eq("id", user.id)
            .single();

          if (profile?.avatar_url) {
            const oldPath = profile.avatar_url.split("/").pop();
            if (oldPath) {
              await supabaseAdmin
                .storage
                .from("avatars")
                .remove([`avatars/${oldPath}`]);
              console.log("🗑️ Avatar anterior eliminado");
            }
          }
        } catch (deleteError) {
          console.warn("⚠️ No se pudo eliminar avatar anterior:", deleteError);
        }
      } catch (avatarError: any) {
        console.error("❌ Error procesando avatar:", avatarError);
        // Continuar sin avatar en caso de error
        avatarUrl = null;
      }
    }

    // 8. Actualizar email en auth.users si cambió
    if (email !== user.email) {
      console.log(`📧 Actualizando email: ${user.email} -> ${email}`);

      const { error: updateAuthError } = await supabaseAdmin.auth.admin
        .updateUserById(user.id, {
          email: email,
        });

      if (updateAuthError) {
        console.error("❌ Error actualizando email en auth:", updateAuthError);
        throw new Error(`Error actualizando email: ${updateAuthError.message}`);
      }

      console.log("✅ Email actualizado en auth.users");
    }

    // 9. Preparar datos para actualizar en profiles
    const updateData: any = {
      nombre: nombre,
      email: email,
    };

    if (avatarUrl) {
      updateData.avatar_url = avatarUrl;
    }

    // 10. Actualizar tabla profiles
    console.log("📝 Actualizando tabla profiles...");

    const { data: profileData, error: profileError } = await supabaseAdmin
      .from("profiles")
      .update(updateData)
      .eq("id", user.id)
      .select()
      .single();

    if (profileError) {
      console.error("❌ Error actualizando perfil:", profileError);
      throw new Error(`Error actualizando perfil: ${profileError.message}`);
    }

    console.log("✅ Perfil actualizado exitosamente");

    // 11. Retornar respuesta exitosa
    return new Response(
      JSON.stringify({
        success: true,
        user: profileData,
        message: "Perfil actualizado exitosamente",
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      }
    );
  } catch (error: any) {
    console.error("❌ Error en update-profile:", error);

    return new Response(
      JSON.stringify({
        success: false,
        error: error.message || "Error desconocido",
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 400,
      }
    );
  }
});
