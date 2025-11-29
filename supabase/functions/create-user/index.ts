// supabase/functions/create-user/index.ts
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

Deno.serve(async (req) => {
  try {
    console.log('🚀 create-user Edge Function ejecutándose...')
    console.log('📦 Versión: 2.0 - Permite sellers crear deliveries')
    
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

    // Validar usuario autenticado
    const { data: { user }, error: authError } = await supabase.auth.getUser(token)
    if (authError || !user) {
      return new Response(JSON.stringify({ success: false, error: 'Token inválido' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    // Obtener datos del request
    const { email, password, nombre, telefono, role, avatar_base64 } = await req.json()

    if (!email || !password || !nombre || !role) {
      return new Response(JSON.stringify({ success: false, error: 'Faltan campos requeridos' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    // Obtener el rol del usuario que hace la petición
    // Usar service_role para evitar problemas de RLS
    console.log(`🔍 Verificando rol del usuario: ${user.id} (${user.email})`)
    
    const { data: roleData, error: roleQueryError } = await supabase
      .from('user_roles')
      .select('roles!inner(name)')
      .eq('user_id', user.id)
      .maybeSingle()

    if (roleQueryError) {
      console.error('❌ Error consultando roles:', JSON.stringify(roleQueryError, null, 2))
      return new Response(JSON.stringify({ success: false, error: 'Error al verificar permisos' }), {
        status: 500,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    if (!roleData || !roleData.roles) {
      console.error('❌ No se encontró rol para el usuario:', user.id)
      console.error('   Datos recibidos:', JSON.stringify(roleData, null, 2))
      return new Response(JSON.stringify({ success: false, error: 'No se pudo verificar tu rol. Contacta al administrador.' }), {
        status: 403,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    const userRole = roleData.roles.name
    const isAdmin = userRole === 'admin'
    const isSeller = userRole === 'seller'

    console.log(`✅ Usuario autenticado: ${user.email}`)
    console.log(`✅ Rol del usuario: ${userRole}`)
    console.log(`✅ Rol a crear: ${role}`)
    console.log(`✅ isAdmin: ${isAdmin}, isSeller: ${isSeller}`)

    // ✅ VALIDACIÓN DE PERMISOS - REORGANIZADA
    // Primero verificar qué rol se quiere crear
    if (role === 'delivery') {
      // Sellers y admins pueden crear deliveries
      if (!isSeller && !isAdmin) {
        console.error(`❌ Acceso denegado: ${userRole} no puede crear deliveries`)
        return new Response(JSON.stringify({
          success: false,
          error: 'Solo sellers y admins pueden crear deliveries'
        }), {
          status: 403,
          headers: { 'Content-Type': 'application/json' }
        })
      }
      console.log(`✅ Permiso concedido: ${userRole} puede crear delivery`)
    } else if (role === 'seller' || role === 'client' || role === 'admin') {
      // Solo admins pueden crear otros roles (seller, client, admin)
      if (!isAdmin) {
        console.error(`❌ Acceso denegado: ${userRole} no puede crear rol ${role}`)
        return new Response(JSON.stringify({
          success: false,
          error: 'Solo admins pueden crear usuarios con este rol'
        }), {
          status: 403,
          headers: { 'Content-Type': 'application/json' }
        })
      }
      console.log(`✅ Permiso concedido: Admin puede crear ${role}`)
    } else {
      console.error(`❌ Rol inválido: ${role}`)
      return new Response(JSON.stringify({
        success: false,
        error: 'Rol inválido'
      }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    // Crear usuario
    console.log(`👤 Creando usuario: ${email}`)
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
      console.error('❌ Error creando usuario:', error)
      return new Response(JSON.stringify({
        success: false,
        error: error?.message || 'Error al crear usuario'
      }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    console.log(`✅ Usuario creado: ${newUser.user.id}`)

    // Esperar a que los triggers se ejecuten
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

    console.log(`🎭 Asignando rol: ${role} (ID: ${roleId.id})`)

    // Eliminar cualquier rol existente que el trigger pudo haber asignado
    const { error: deleteError } = await supabase
      .from('user_roles')
      .delete()
      .eq('user_id', newUser.user.id)

    if (deleteError) {
      console.warn('⚠️ Error eliminando roles existentes:', deleteError.message)
    }

    // Asignar el rol correcto
    const { error: insertError } = await supabase
      .from('user_roles')
      .insert({
        user_id: newUser.user.id,
        role_id: roleId.id
      })

    if (insertError) {
      console.error('❌ Error asignando rol:', insertError)
      return new Response(JSON.stringify({
        success: false,
        error: `Error al asignar rol: ${insertError.message}`
      }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    }

    console.log('✅ Rol asignado correctamente')

    // Procesar avatar si existe
    let avatarUrl = null
    if (avatar_base64) {
      try {
        console.log('📸 Procesando avatar...')

        // Extraer datos base64
        const base64Data = avatar_base64.split(',')[1]
        const mimeType = avatar_base64.split(';')[0].split(':')[1]
        const extension = mimeType.split('/')[1]

        // Decodificar base64
        const binaryString = atob(base64Data)
        const bytes = new Uint8Array(binaryString.length)
        for (let i = 0; i < binaryString.length; i++) {
          bytes[i] = binaryString.charCodeAt(i)
        }

        const fileName = `${newUser.user.id}/avatar.${extension}`

        // Subir a Storage
        const { error: uploadError } = await supabase.storage
          .from('profile-pictures')
          .upload(fileName, bytes, {
            contentType: mimeType,
            upsert: true
          })

        if (uploadError) {
          console.error('❌ Error subiendo avatar:', uploadError)
        } else {
          avatarUrl = `${Deno.env.get('SUPABASE_URL')}/storage/v1/object/public/profile-pictures/${fileName}`
          console.log('✅ Avatar subido:', avatarUrl)
        }
      } catch (avatarError) {
        console.error('⚠️ Error procesando avatar:', avatarError)
        // Continuar sin avatar
      }
    }

    // Crear/actualizar perfil
    const { error: profileError } = await supabase.from('profiles').upsert({
      id: newUser.user.id,
      email: newUser.user.email!,
      nombre,
      avatar_url: avatarUrl,
      activo: true
    })

    if (profileError) {
      console.warn('⚠️ Error creando perfil:', profileError.message)
    }

    console.log('✅ Usuario creado completamente')

    return new Response(JSON.stringify({
      success: true,
      user: {
        id: newUser.user.id,
        email: newUser.user.email!,
        nombre,
        avatar_url: avatarUrl,
        role,
        activo: true
      },
      message: 'Usuario creado exitosamente'
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    })

  } catch (err) {
    console.error('💥 Error en create-user:', err)
    return new Response(JSON.stringify({
      success: false,
      error: 'Error interno del servidor',
      details: err.message
    }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    })
  }
})