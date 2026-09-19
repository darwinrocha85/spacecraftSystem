# spacecraftSystem

API REST en **Spring Boot 3 / Java 17** que administra una flota de naves espaciales: alta y
edición de naves, apertura como museo (visitas por franja horaria) o teatro (funciones con
asientos), venta de entradas con cobro real vía BankIn, y el envío de una nave al taller de
reparación.

## Stack
- Spring Boot 3, Java 17, Maven
- H2 en memoria (se reinicia en cada arranque; datos de demo en `data.sql`)
- Caché con Caffeine, manejo centralizado de excepciones

## Cómo correr en local
```bash
./mvnw spring-boot:run     # Windows: mvnw.cmd spring-boot:run
```
Levanta en `http://localhost:8080`. Consola H2: `http://localhost:8080/h2-console`.

Para el flujo completo (compra con cobro real, taller) necesita además `Bankin` (puerto 8000)
y `spacecraft-taller-backend` (puerto 8001) corriendo.

## Áreas principales
| Área | Qué hace |
|---|---|
| Flota | CRUD de naves |
| Museo / Teatro | Disponibilidad, venta y cancelación de entradas, cobro real vía BankIn |
| Taller | Envía la nave al taller (`spacecraft-taller-backend`), revierte ventas activas, la recibe de vuelta |
| Marketing | Endpoint de solo lectura para la landing pública |
| Dashboard | Ingresos, ocupación y estado de la flota para el panel admin |
| Email | Confirmación de compra/cancelación, best-effort (nunca bloquea la operación) |

## Variables de entorno
Se configuran en un archivo `.env` local (no versionado): credenciales de BankIn, la clave
interna compartida con el taller y, opcionalmente, credenciales SMTP para el email. Ningún
valor real vive en este repo.

## Repos relacionados
Paneles: [spacecraftSystem-frontend](../spacecraftSystem-frontend),
[spacecraft-tickets-frontend](../spacecraft-tickets-frontend). Taller:
[spacecraft-taller-backend](../spacecraft-taller-backend),
[spacecraft-taller-frontend](../spacecraft-taller-frontend). Landing:
[spacecraft-events-landing](../spacecraft-events-landing).
