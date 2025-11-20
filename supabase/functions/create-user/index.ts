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

    // Obtener datos (sin avatar_base64, ahora se sube directamente desde Android)
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

    await new Promise(resolve => setTimeout(resolve, 500))

    // Obtener ID del rol
    const { data: roleId, error: roleError } = await supabase
      .from('roles')
      .select('id')
      .eq('name', role)
      .single()

    if (roleError || !roleId) {
      console.error('❌ Error obteniendo rol:', roleError)
      return new Response(JSON.stringify({ success: false, error: 'Rol no encontrado' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    console.log('🎭 Asignando rol:', role, 'con ID:', roleId.id, 'para usuario:', newUser.user.id)

    // Eliminar cualquier rol existente que el trigger pudo haber asignado
    const { error: deleteError } = await supabase
      .from('user_roles')
      .delete()
      .eq('user_id', newUser.user.id)

    if (deleteError) {
      console.warn('⚠️ Error eliminando roles existentes (puede ser normal):', deleteError.message)
    }

    // Asignar el rol correcto
    const { error: insertError } = await supabase
      .from('user_roles')
      .insert({
        user_id: newUser.user.id,
        role_id: roleId.id
      })

    if (insertError) {
      console.error('❌ Error insertando rol:', insertError)
      return new Response(JSON.stringify({ success: false, error: `Error al asignar rol: ${insertError.message}` }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    console.log('✅ Rol asignado exitosamente')

    // Crear perfil (avatar_url será null, se actualizará desde Android después)
    await supabase.from('profiles').upsert({
      id: newUser.user.id,
      email: newUser.user.email!,
      nombre,
      avatar_url: null, // Se actualizará desde Android después de subir la imagen
      activo: true
    })

    return new Response(JSON.stringify({
      success: true,
      user: {
        id: newUser.user.id,
        email: newUser.user.email!,
        nombre,
        avatar_url: null, // Se actualizará desde Android después de subir la imagen
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