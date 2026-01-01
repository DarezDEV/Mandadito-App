-- ================================================================
-- DIAGNÓSTICO: Por qué no aparecen las categorías
-- ================================================================
-- Ejecuta estos queries UNO POR UNO en Supabase SQL Editor
-- ================================================================

-- 1️⃣ Ver TODAS las categorías que existen
SELECT
    id,
    colmado_id,
    name,
    is_active,
    created_at
FROM public.categories
ORDER BY created_at DESC;

-- 2️⃣ Ver los colmados que existen (para verificar el colmado_id)
SELECT
    id as colmado_id,
    seller_id,
    name,
    is_active
FROM public.colmados
ORDER BY created_at DESC;

-- 3️⃣ Ver si las categorías están activas
SELECT
    c.name as categoria,
    c.is_active as categoria_activa,
    c.colmado_id,
    col.name as colmado_nombre,
    col.is_active as colmado_activo
FROM public.categories c
LEFT JOIN public.colmados col ON c.colmado_id = col.id
ORDER BY c.created_at DESC;

-- 4️⃣ Contar categorías por colmado
SELECT
    col.name as colmado,
    col.id as colmado_id,
    COUNT(c.id) as total_categorias,
    COUNT(CASE WHEN c.is_active = true THEN 1 END) as categorias_activas
FROM public.colmados col
LEFT JOIN public.categories c ON c.colmado_id = col.id
GROUP BY col.id, col.name
ORDER BY col.created_at DESC;

-- ================================================================
-- POSIBLES PROBLEMAS Y SOLUCIONES:
-- ================================================================
--
-- PROBLEMA 1: La categoría tiene un colmado_id diferente al del usuario
-- SOLUCIÓN: Ejecuta esto para ver qué colmado_id deberías usar:
--   SELECT id, name FROM colmados WHERE seller_id = 'TU-USER-ID';
--
-- PROBLEMA 2: La categoría está marcada como inactiva
-- SOLUCIÓN: Activa la categoría:
--   UPDATE categories SET is_active = true WHERE id = 'CATEGORIA-ID';
--
-- PROBLEMA 3: El colmado está inactivo
-- SOLUCIÓN: Activa el colmado:
--   UPDATE colmados SET is_active = true WHERE id = 'COLMADO-ID';
--
-- ================================================================
