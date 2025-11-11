// supabase/functions/create-user/index.ts
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

Deno.serve(async (req) => {
  try {
    // === FIX CRÍTICO: OBTENER APIKEY DEL HEADER ===
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

    // Validar que el token pertenece a un admin
    const { data: { user }, error: authError } = await supabase.auth.getUser(token)
    if (authError || !user) {
      return new Response(JSON.stringify({ success: false, error: 'Token inválido o expirado' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    // Verificar que sea admin
    const { data: roleData } = await supabase
      .from('user_roles')
      .select('roles!inner(name)')
      .eq('user_id', user.id)
      .single()

    if (roleData?.roles?.name !== 'admin') {
      return new Response(JSON.stringify({ success: false, error: 'Acceso denegado: solo admins' }), {
        status: 403,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    const { email, password, nombre, telefono, role } = await req.json()

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

    // Crear usuario con metadata que indica que NO debe asignar rol automático
    // El trigger verificará esta metadata y no asignará "client" automáticamente
    const { data: newUser, error } = await supabase.auth.admin.createUser({
      email,
      password,
      email_confirm: true,
      user_metadata: { 
        nombre, 
        telefono,
        skip_auto_role: true,  // Flag para que el trigger no asigne "client"
        role: role  // Rol que se asignará manualmente
      }
    })

    if (error || !newUser?.user) {
      return new Response(JSON.stringify({ success: false, error: error?.message || 'Error al crear usuario' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    // Esperar un poco para que el trigger termine de crear el perfil
    await new Promise(resolve => setTimeout(resolve, 300))

    // Obtener el ID del rol a asignar
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

    // Asignar el rol correcto
    // El trigger NO asignará "client" porque skip_auto_role está en metadata
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

    await supabase.from('profiles').upsert({
      id: newUser.user.id,
      email: newUser.user.email!,
      nombre,
      activo: true
    })

    return new Response(JSON.stringify({
      success: true,
      user: {
        id: newUser.user.id,
        email: newUser.user.email!,
        nombre,
        telefono: telefono || null,
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