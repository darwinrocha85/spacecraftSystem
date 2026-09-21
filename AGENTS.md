# spacecraftSystem Backend — AGENTS.md

> Proyecto independiente. Abrir opencode con cwd en `spacecraftSystem/`, nunca en `Projects/`.
> Stack: Java 17, Spring Boot 3.3.5, Maven, SQLite en archivo (`./data/spacecraft.db`), caché Caffeine.

## Cómo correr
- `.\mvnw.cmd spring-boot:run` → `http://localhost:8080` (`server.port=${PORT:8080}`)
- Requiere BankIn en :8000 y taller-backend en :8001 para el flujo completo
- Sin `.env.example`: la plantilla es `spacecraftSystem.env`. En PowerShell se setean con
  `$env:CLAVE="valor"` antes del `mvnw`. Claves: `BANKIN_API_BASE` (default `localhost:8000`),
  `BANKIN_API_KEY`, `TALLER_API_BASE`, `TALLER_INTERNAL_API_KEY`, `MAIL_HOST/PORT/USERNAME/PASSWORD`,
  `SQLITE_DB_PATH`, `PORT`
- Sin `MAIL_*`: los emails fallan en silencio (solo `WARN` en consola), la compra NO se rompe

## Integraciones (contratos externos)
- BankIn: `POST /transactions/purchase` + `POST /transactions/{id}/reverse` (ver `BANKIN-INTEGRATION.md`
  en esta raíz — es un stub, el canónico vive en `Bankin/docs/`). Uso en `BankInPaymentService.java`.
- Taller: `POST /api/internal/repairs` con `X-Internal-Api-Key` (siempre exigida).
- CORS (`CorsConfig.java`, path `/api/**`): permite `localhost:5173-5176` + dominios Firebase.
  Nuevo frontend = editar `allowedOrigins` + commit/push + esperar redeploy de Render.

## Deploy
- Dockerfile multietapa → Render `spacecraftsystem.onrender.com` (auto-deploy on push, sin `render.yaml`)

## No hacer
- No usar ni regenerar `docs/archive/fase2-backend.ps1` / `fase3-backend.ps1` /
  `phase1-backend.ps1`: generadores obsoletos, el código ya vive en `src/`. Editar Java directo.
- No cambiar el prefijo `/api`: todos los frontends dependen de él.
- No commitear `data/*.db` ni secretos (`spacecraftSystem.env` con valores reales).
- No tocar `CorsConfig.java` para un solo frontend sin revisar la lista completa.
