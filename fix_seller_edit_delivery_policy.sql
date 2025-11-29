-- =====================================================
-- FIX: Permitir a sellers editar perfiles de deliveries de su colmado
-- =====================================================

-- Política para que sellers puedan actualizar perfiles de deliveries de su colmado
CREATE POLICY "Sellers can update their colmado delivery profiles"
ON public.profiles
FOR UPDATE
TO authenticated
USING (
    -- El usuario a actualizar es un delivery del colmado del seller
    id IN (
        SELECT uc.user_id
        FROM public.user_colmado uc
        WHERE uc.role_in_colmado = 'delivery'
        AND uc.colmado_id IN (
            -- El colmado pertenece al seller (en user_colmado)
            SELECT colmado_id 
            FROM public.user_colmado 
            WHERE user_id = auth.uid() 
            AND role_in_colmado = 'owner'
        )
        OR
        -- O el colmado pertenece al seller (en colmados)
        uc.colmado_id IN (
            SELECT id 
            FROM public.colmados 
            WHERE seller_id = auth.uid()
        )
    )
)
WITH CHECK (
    -- Misma validación para WITH CHECK
    id IN (
        SELECT uc.user_id
        FROM public.user_colmado uc
        WHERE uc.role_in_colmado = 'delivery'
        AND uc.colmado_id IN (
            SELECT colmado_id 
            FROM public.user_colmado 
            WHERE user_id = auth.uid() 
            AND role_in_colmado = 'owner'
        )
        OR
        uc.colmado_id IN (
            SELECT id 
            FROM public.colmados 
            WHERE seller_id = auth.uid()
        )
    )
);

