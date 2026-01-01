import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const html = `<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Link Expirado - Mandadito</title>
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
      animation: gradientShift 10s ease infinite;
    }
    @keyframes gradientShift {
      0%, 100% { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
      50% { background: linear-gradient(135deg, #f5576c 0%, #f093fb 100%); }
    }
    .container {
      background: white;
      border-radius: 20px;
      padding: 50px;
      max-width: 600px;
      text-align: center;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      animation: slideUp 0.6s ease-out;
    }
    @keyframes slideUp {
      from {
        opacity: 0;
        transform: translateY(30px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
    .icon {
      font-size: 80px;
      margin-bottom: 20px;
      animation: pulse 2s ease infinite;
    }
    @keyframes pulse {
      0%, 100% { transform: scale(1); }
      50% { transform: scale(1.1); }
    }
    h1 {
      color: #2d3748;
      font-size: 32px;
      margin-bottom: 15px;
      font-weight: 700;
    }
    p {
      color: #4a5568;
      font-size: 18px;
      line-height: 1.6;
      margin-bottom: 15px;
    }
    .warning {
      background: #fffaf0;
      border-left: 4px solid #ed8936;
      padding: 15px;
      margin: 25px 0;
      border-radius: 8px;
      text-align: left;
    }
    .warning strong {
      color: #c05621;
      font-size: 16px;
    }
    .instructions {
      background: #f7fafc;
      padding: 20px;
      border-radius: 10px;
      margin: 20px 0;
      text-align: left;
    }
    .instructions strong {
      color: #2d3748;
      font-size: 18px;
      display: block;
      margin-bottom: 10px;
    }
    .instructions ol {
      margin-left: 20px;
      margin-top: 10px;
    }
    .instructions li {
      margin: 8px 0;
      color: #4a5568;
      line-height: 1.6;
    }
    .footer {
      font-size: 14px;
      color: #a0aec0;
      margin-top: 30px;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="icon">⏰</div>
    <h1>Link de Onboarding Expirado</h1>
    <p>El enlace de configuración de Stripe ha expirado.</p>

    <div class="warning">
      <strong>⚠️ No te preocupes</strong><br>
      Los enlaces de onboarding expiran rápidamente por seguridad.<br>
      Puedes obtener un nuevo enlace fácilmente.
    </div>

    <div class="instructions">
      <strong>Pasos para continuar:</strong>
      <ol>
        <li>Regresa a la aplicación Mandadito</li>
        <li>Toca el botón "¿El enlace no funciona? Refrescar"</li>
        <li>Se generará un nuevo enlace válido</li>
        <li>Continúa con tu configuración</li>
      </ol>
    </div>

    <p class="footer">
      Puedes cerrar esta ventana de forma segura.
    </p>
  </div>
</body>
</html>`;

serve((_req) => {
  return new Response(html, {
    status: 200,
    headers: new Headers({
      'Content-Type': 'text/html; charset=utf-8',
      'Cache-Control': 'no-cache',
      'X-Content-Type-Options': 'nosniff',
    }),
  });
});
