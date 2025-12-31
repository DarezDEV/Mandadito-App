# Script para probar endpoints de Stripe Connect

Write-Host "=== PRUEBA DE STRIPE CONNECT ===" -ForegroundColor Green
Write-Host ""

# 1. Refrescar link de onboarding
Write-Host "1. Refrescando link de onboarding..." -ForegroundColor Yellow
$body = @{
    colmado_id = "d9be59e4-e1c1-4781-9073-d44f89a65480"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "http://localhost:5000/stripe/connect/refresh-link" -Method POST -Body $body -ContentType "application/json"
    $result = $response.Content | ConvertFrom-Json
    Write-Host "[OK] Link de onboarding:" -ForegroundColor Green
    Write-Host $result.onboarding_url
    Write-Host ""
} catch {
    Write-Host "[ERROR]" $_.Exception.Message -ForegroundColor Red
    Write-Host ""
}

# 2. Verificar estado de cuenta
Write-Host "2. Verificando estado de cuenta..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:5000/stripe/connect/status" -Method POST -Body $body -ContentType "application/json"
    $result = $response.Content | ConvertFrom-Json
    Write-Host "[OK] Estado de la cuenta:" -ForegroundColor Green
    Write-Host "  Account ID: $($result.account_id)"
    Write-Host "  Onboarding completado: $($result.onboarding_completed)"
    Write-Host "  Cobros habilitados: $($result.charges_enabled)"
    Write-Host "  Pagos habilitados: $($result.payouts_enabled)"
    Write-Host ""
} catch {
    Write-Host "[ERROR]" $_.Exception.Message -ForegroundColor Red
    Write-Host ""
}

# 3. Obtener configuracion de Stripe
Write-Host "3. Obteniendo configuracion de Stripe..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:5000/stripe/config" -Method GET
    $result = $response.Content | ConvertFrom-Json
    Write-Host "[OK] Configuracion:" -ForegroundColor Green
    Write-Host "  Publishable Key: $($result.publishable_key.Substring(0,20))..."
    Write-Host "  Currency: $($result.currency)"
    Write-Host ""
} catch {
    Write-Host "[ERROR]" $_.Exception.Message -ForegroundColor Red
    Write-Host ""
}

Write-Host "=== FIN DE PRUEBAS ===" -ForegroundColor Green
