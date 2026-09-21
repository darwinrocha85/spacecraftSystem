# BANKIN-INTEGRATION — referencia (no editar aquí)

El contrato canónico de cobro con BankIn vive en el repo `Bankin`:
`Bankin/docs/BANKIN-INTEGRATION.md`
(repo: https://github.com/darwinrocha85/Bankin).
Este archivo es solo un puntero para no duplicar 90 líneas en dos repos.

Resumen mínimo del contrato:
- Cobro: `POST {BANKIN_API_BASE}/transactions/purchase`
  `{"card_id": "...", "amount": 49.90, "note": "naveSpace Tickets"}` → `201`
- Reversa: `POST {BANKIN_API_BASE}/transactions/{id}/reverse` (sin body) → `200`
- Errores: `404` tarjeta/tx inexistente, `400` saldo/regla de negocio,
  `401` si falta `X-Api-Key` (solo cuando BankIn define `EXTERNAL_API_KEY`).
- URLs: local `http://localhost:8000`, producción `https://bankinback.onrender.com`

Ver `src/main/java/com/rocha/spacecraftmanagementsystem/service/BankInPaymentService.java`
para el uso desde este repo.
