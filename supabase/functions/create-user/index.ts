// supabase/functions/create-user/index.ts
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

Deno.serve(async (req) => {
  try {
    // Validar API Key
    const apikey = req.headers.get('apikey')
    if (!apikey || apikey !== Deno.env.get('SUPABASE_ANON_KEY')) {
      return new Response(JSON.stringify({ success: false, error: 'API key inválida' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    const authHeader = req.headers.get('Authorization')
    if (!authHeader?.startsWith('Bearer ')) {
      return new Response(JSON.stringify({ success: false, error: 'No autorizado' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    const token = authHeader.replace('Bearer ', '')
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // Validar admin
    const { data: { user }, error: authError } = await supabase.auth.getUser(token)
    if (authError || !user) {
      return new Response(JSON.stringify({ success: false, error: 'Token inválido' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    const { data: roleData } = await supabase
      .from('user_roles')
      .select('roles!inner(name)')
      .eq('user_id', user.id)
      .single()

    if (roleData?.roles?.name !== 'admin') {
      return new Response(JSON.stringify({ success: false, error: 'Acceso denegado' }), {
        status: 403,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    // ✨ NUEVO: Obtener datos incluyendo avatar_base64
    const { email, password, nombre, telefono, role, avatar_base64 } = await req.json()

    if (!email || !password || !nombre || !role) {
      return new Response(JSON.stringify({ success: false, error: 'Faltan campos requeridos' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    if (role === 'delivery') {
      return new Response(JSON.stringify({ success: false, error: 'No se pueden crear delivery' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    // Crear usuario
    const { data: newUser, error } = await supabase.auth.admin.createUser({
      email,
      password,
      email_confirm: true,
      user_metadata: {
        nombre,
        telefono,
        skip_auto_role: true,
        role: role
      }
    })

    if (error || !newUser?.user) {
      return new Response(JSON.stringify({ success: false, error: error?.message || 'Error al crear usuario' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    await new Promise(resolve => setTimeout(resolve, 300))

    // Asignar rol
    const { data: roleId } = await supabase
      .from('roles')
      .select('id')
      .eq('name', role)
      .single()

    if (!roleId) {
      return new Response(JSON.stringify({ success: false, error: 'Rol no encontrado' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    const { error: insertError } = await supabase
      .from('user_roles')
      .insert({
        user_id: newUser.user.id,
        role_id: roleId.id
      })

    if (insertError) {
      return new Response(JSON.stringify({ success: false, error: `Error al asignar rol: ${insertError.message}` }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    // ✨ NUEVO: Subir avatar si existe
    let avatar_url = null
    if (avatar_base64) {
      try {
        // Decodificar base64
        const base64Data = avatar_base64.split(',')[1] || avatar_base64
        const binaryData = Uint8Array.from(atob(base64Data), c => c.charCodeAt(0))

        // Determinar extensión
        const mimeType = avatar_base64.match(/^data:(image\/\w+);base64,/)?.[1] || 'image/jpeg'
        const extension = mimeType.split('/')[1]

        // Subir a Storage
        const fileName = `${newUser.user.id}/avatar.${extension}`
        const { data: uploadData, error: uploadError } = await supabase.storage
          .from('profile-pictures')
          .upload(fileName, binaryData, {
            contentType: mimeType,
            upsert: true
          })

        if (uploadError) {
          console.error('Error subiendo avatar:', uploadError)
        } else {
          // Obtener URL pública
          const { data: urlData } = supabase.storage
            .from('profile-pictures')
            .getPublicUrl(fileName)

          avatar_url = urlData.publicUrl
        }
      } catch (avatarError) {
        console.error('Error procesando avatar:', avatarError)
        // No fallar la creación del usuario si falla el avatar
      }
    }

    // Actualizar perfil con avatar_url
    await supabase.from('profiles').upsert({
      id: newUser.user.id,
      email: newUser.user.email!,
      nombre,
      avatar_url,
      activo: true
    })

    return new Response(JSON.stringify({
      success: true,
      user: {
        id: newUser.user.id,
        email: newUser.user.email!,
        nombre,
        avatar_url,
        role,
        activo: true
      },
      message: 'Usuario creado exitosamente'
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    })

  } catch (err) {
    console.error('Error en create-user:', err)
    return new Response(JSON.stringify({ success: false, error: 'Error interno del servidor' }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    })
  }
})