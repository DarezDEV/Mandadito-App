-- ==========================================
-- ESQUEMA COMPLETO PARA GESTIÓN DE USUARIOS
-- ==========================================
-- Este esquema permite:
-- 1. Administradores pueden gestionar todos los usuarios (crear, editar, eliminar, deshabilitar)
-- 2. Usuarios bloqueados no pueden hacer login
-- 3. Trigger automático para crear perfiles
-- 4. Asignación automática de rol 'client' al registrarse

-- ==========================================
-- 1) TABLAS
-- ==========================================

-- Tabla de roles
create table if not exists public.roles (
  id serial primary key,
  name text not null unique check (name in ('client','seller','delivery','admin'))
);

-- Tabla de perfiles
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null unique,
  nombre text not null,
  telefono text,
  activo boolean not null default true,
  created_at timestamptz not null default now()
);

-- Tabla de roles de usuario
create table if not exists public.user_roles (
  user_id uuid not null references auth.users(id) on delete cascade,
  role_id int not null references public.roles(id) on delete restrict,
  primary key (user_id, role_id)
);

-- ==========================================
-- 2) SEED ROLES
-- ==========================================

insert into public.roles (name) values
  ('client'), ('seller'), ('delivery'), ('admin')
on conflict (name) do nothing;

-- ==========================================
-- 3) FUNCIONES AUXILIARES
-- ==========================================

-- Función para verificar si un usuario es administrador
create or replace function public.is_admin(user_id uuid)
returns boolean as $$
begin
  return exists (
    select 1
    from public.user_roles ur
    join public.roles r on ur.role_id = r.id
    where ur.user_id = user_id
      and r.name = 'admin'
  );
end;
$$ language plpgsql security definer;

-- Función para verificar si un usuario es administrador (versión sin parámetros para uso en RLS)
create or replace function public.is_current_user_admin()
returns boolean as $$
begin
  return exists (
    select 1
    from public.user_roles ur
    join public.roles r on ur.role_id = r.id
    where ur.user_id = auth.uid()
      and r.name = 'admin'
  );
end;
$$ language plpgsql security definer;

-- ==========================================
-- 4) TRIGGERS
-- ==========================================

-- Función del trigger para crear perfil automáticamente
create or replace function public.handle_new_user()
returns trigger as $$
declare
  client_role_id int;
begin
  -- Intentar insertar el perfil con los datos disponibles
  insert into public.profiles (id, email, nombre, telefono, activo)
  values (
    new.id, 
    new.email, 
    coalesce(new.raw_user_meta_data->>'nombre', split_part(new.email, '@', 1)),
    nullif(new.raw_user_meta_data->>'telefono', ''),
    true
  );

  -- Asignar rol 'client' por defecto
  select id into client_role_id
  from public.roles
  where name = 'client'
  limit 1;

  if client_role_id is not null then
    insert into public.user_roles (user_id, role_id)
    values (new.id, client_role_id)
    on conflict do nothing;
  end if;

  return new;
exception
  when others then
    -- Log del error para depuración
    raise warning 'Error al crear perfil para usuario %: %', new.id, sqlerrm;
    -- Retornar new para que el usuario se cree aunque el perfil falle
    return new;
end;
$$ language plpgsql security definer;

-- Crear el trigger si no existe
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

-- ==========================================
-- 5) ACTIVAR RLS
-- ==========================================

alter table public.roles enable row level security;
alter table public.profiles enable row level security;
alter table public.user_roles enable row level security;

-- ==========================================
-- 6) POLÍTICAS RLS
-- ==========================================

-- ==========================================
-- POLÍTICAS PARA ROLES
-- ==========================================

-- Permitir leer roles a usuarios autenticados
drop policy if exists "roles_select_auth" on public.roles;
create policy "roles_select_auth"
  on public.roles for select
  to authenticated
  using (true);

-- ==========================================
-- POLÍTICAS PARA PROFILES
-- ==========================================

-- Permitir leer el propio perfil
drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own"
  on public.profiles for select
  to authenticated
  using (id = auth.uid());

-- Permitir leer todos los perfiles a administradores
drop policy if exists "profiles_select_admin" on public.profiles;
create policy "profiles_select_admin"
  on public.profiles for select
  to authenticated
  using (public.is_current_user_admin());

-- Permitir insertar el propio perfil
drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own"
  on public.profiles for insert
  to authenticated
  with check (id = auth.uid());

-- Permitir insertar perfiles a administradores (para crear usuarios)
drop policy if exists "profiles_insert_admin" on public.profiles;
create policy "profiles_insert_admin"
  on public.profiles for insert
  to authenticated
  with check (public.is_current_user_admin());

-- Permitir actualizar el propio perfil
drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own"
  on public.profiles for update
  to authenticated
  using (id = auth.uid())
  with check (id = auth.uid());

-- Permitir actualizar cualquier perfil a administradores
drop policy if exists "profiles_update_admin" on public.profiles;
create policy "profiles_update_admin"
  on public.profiles for update
  to authenticated
  using (public.is_current_user_admin())
  with check (public.is_current_user_admin());

-- Permitir eliminar el propio perfil (aunque no se recomienda)
drop policy if exists "profiles_delete_own" on public.profiles;
create policy "profiles_delete_own"
  on public.profiles for delete
  to authenticated
  using (id = auth.uid());

-- Permitir eliminar cualquier perfil a administradores
drop policy if exists "profiles_delete_admin" on public.profiles;
create policy "profiles_delete_admin"
  on public.profiles for delete
  to authenticated
  using (public.is_current_user_admin());

-- ==========================================
-- POLÍTICAS PARA USER_ROLES
-- ==========================================

-- Permitir leer los propios roles
drop policy if exists "user_roles_select_own" on public.user_roles;
create policy "user_roles_select_own"
  on public.user_roles for select
  to authenticated
  using (user_id = auth.uid());

-- Permitir leer todos los roles a administradores
drop policy if exists "user_roles_select_admin" on public.user_roles;
create policy "user_roles_select_admin"
  on public.user_roles for select
  to authenticated
  using (public.is_current_user_admin());

-- Permitir insertar solo el rol 'client' para sí mismo
drop policy if exists "user_roles_insert_client_only" on public.user_roles;
create policy "user_roles_insert_client_only"
  on public.user_roles for insert
  to authenticated
  with check (
    user_id = auth.uid()
    and exists (
      select 1 from public.roles r
      where r.id = role_id and r.name = 'client'
    )
  );

-- Permitir insertar cualquier rol a administradores
drop policy if exists "user_roles_insert_admin" on public.user_roles;
create policy "user_roles_insert_admin"
  on public.user_roles for insert
  to authenticated
  with check (public.is_current_user_admin());

-- Permitir actualizar los propios roles (solo client)
drop policy if exists "user_roles_update_own" on public.user_roles;
create policy "user_roles_update_own"
  on public.user_roles for update
  to authenticated
  using (user_id = auth.uid())
  with check (
    user_id = auth.uid()
    and exists (
      select 1 from public.roles r
      where r.id = role_id and r.name = 'client'
    )
  );

-- Permitir actualizar cualquier rol a administradores
drop policy if exists "user_roles_update_admin" on public.user_roles;
create policy "user_roles_update_admin"
  on public.user_roles for update
  to authenticated
  using (public.is_current_user_admin())
  with check (public.is_current_user_admin());

-- Permitir eliminar los propios roles
drop policy if exists "user_roles_delete_own" on public.user_roles;
create policy "user_roles_delete_own"
  on public.user_roles for delete
  to authenticated
  using (user_id = auth.uid());

-- Permitir eliminar cualquier rol a administradores
drop policy if exists "user_roles_delete_admin" on public.user_roles;
create policy "user_roles_delete_admin"
  on public.user_roles for delete
  to authenticated
  using (public.is_current_user_admin());

-- ==========================================
-- 7) VERIFICACIÓN DE CUENTA BLOQUEADA EN LOGIN
-- ==========================================
-- NOTA: Esta verificación se hace en el código de la app (AuthRepository.kt)
-- El código ya verifica el campo 'activo' antes y después del login.
-- Si 'activo' es false, el login se rechaza con el mensaje:
-- "Tu cuenta ha sido bloqueada. Por favor, contacta con tu proveedor para más información."

-- ==========================================
-- 8) PERMISOS ADICIONALES
-- ==========================================

-- Permitir que el trigger (security definer) pueda insertar perfiles
-- Esto es necesario porque el trigger se ejecuta con permisos del propietario
grant usage on schema public to postgres, anon, authenticated, service_role;
grant all on public.profiles to postgres, service_role;
grant all on public.user_roles to postgres, service_role;
grant all on public.roles to postgres, service_role;

-- ==========================================
-- 9) FUNCIÓN PARA CREAR USUARIOS (ALTERNATIVA A API ADMIN)
-- ==========================================
-- Esta función permite crear usuarios desde la app usando el cliente admin
-- Solo se ejecuta con SERVICE_ROLE_KEY (security definer)

create or replace function public.admin_create_user(
  p_email text,
  p_password text,
  p_nombre text,
  p_telefono text default null,
  p_direccion text default null,
  p_role_name text default 'client'
)
returns uuid
language plpgsql
security definer
as $$
declare
  v_user_id uuid;
  v_role_id int;
begin
  -- Esta función requiere que se llame desde el cliente admin (SERVICE_ROLE_KEY)
  -- Por ahora, dejamos que la app use la API admin directamente
  -- Si la API admin no funciona, podemos implementar esta función
  
  raise exception 'Esta función aún no está implementada. Usa la API admin directamente.';
  
  return v_user_id;
end;
$$;

-- ==========================================
-- FIN DEL ESQUEMA
-- ==========================================

