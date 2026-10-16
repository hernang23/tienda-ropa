# Estado real del proyecto (auditoría de publicación)

Fecha de auditoría: 2026-10-16. Elaborado al preparar la publicación del repositorio;
documenta el estado VERIFICABLE en disco, sin afirmar resultados no ejecutados aquí.

## Verificado presente en el repositorio

| Componente | Estado | Evidencia |
|---|---|---|
| Backend código fuente | ✅ Completo y versionado | 91+ archivos Java: entidades, repositorios, DTO, mappers, servicios, 11 controladores, seguridad JWT, handler global, OpenAPI |
| Migraciones Flyway | ✅ Presentes | `V1__esquema_inicial.sql` (23 tablas), `V2__datos_semilla.sql` (roles + admin con hash BCrypt documentado) |
| Compilación backend | ✅ Registrada por sesiones previas | `mvn clean compile` exitoso; artefactos históricos existían en `backend/target` (excluidos del repositorio) |
| Pruebas backend | ⚠️ NO verificables | El directorio `backend/src/test` está vacío en este snapshot. El perfil `test` (H2) existe en `application.yml`, pero no hay suites que ejecutar |
| Frontend | ⚠️ Pendiente de reconstrucción | Solo base versionada: `package.json` (React 18/Vite/TS/TanStack Query/RHF/Zod/Recharts/Vitest), `vite.config.ts`, `index.html`. Los directorios `src/*` están vacíos en este snapshot |
| Docker | ✅ Creados y revisados estáticamente | `docker-compose.yml`, `backend/Dockerfile` (multi-stage, usuario sin privilegios), `frontend/Dockerfile` + `nginx.conf`, `.env.example` |
| Documentación | ✅ Completa | `README.md`, `docs/arquitectura.md`, `docs/api-contracts.md` (rutas verificadas contra los controladores), `docs/security.md`, `docs/diagrama-er.md` |
| Secretos | ✅ Ninguno real versionado | `.env` ignorado; `.env.example` solo con valores ilustrativos |

## Limitaciones honestas

1. **Docker no disponible en el entorno de desarrollo:** no se pudo ejecutar
   `docker compose config/up/build`. La validación fue estática (sintaxis YAML, nombres
   de servicio coherentes con `nginx.conf` y variables de `application.yml`).
2. **Sin GitHub credentials en el entorno:** la creación/push del repositorio remoto debe
   realizarla el propietario con un token PAT o SSH (instrucciones en README/informe).
3. **Pruebas automatizadas:** no deben citarse como "pasadas" las que no fueron ejecutadas.
   Las fases previas reportaron builds correctos; este documento fija únicamente lo comprobable.

## Orden recomendado de finalización

1. Reconstruir `frontend/src` (api client, tipos, layout, páginas conectadas a `/api/v1`).
2. Añadir suites JUnit 5 (servicios de venta/compra/devolución/caja + MockMvc de seguridad) y ejecutar `mvn clean test`.
3. Ejecutar `docker compose up -d --build` en un host con Docker y validar el flujo completo.
