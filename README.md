# tienda-ropa — Sistema de gestión para una tienda de ropa

Aplicación web completa (frontend + backend + base de datos) para administrar una tienda
de ropa: productos con variantes por talla/color, inventario trazable, proveedores,
clientes, compras con recepción de mercancía, punto de venta, pagos, devoluciones,
caja con cierre diario, dashboard, reportes, usuarios con roles y auditoría.

Todo el sistema está en español y usa pesos colombianos (COP). Los importes se manejan
con `BigDecimal`/`NUMERIC` (nunca float/double).

## Tecnologías

| Capa | Tecnología | Versión |
|---|---|---|
| Frontend | React + TypeScript (estricto), Vite, Tailwind-style CSS propio, React Router, React Hook Form, Zod, TanStack Query, Axios, Recharts, Vitest + React Testing Library | React 18.3, Vite 5.4, TS 5.6 |
| Backend | Java 17 LTS, Spring Boot 3.3.x, Spring Web, Spring Data JPA/Hibernate, Spring Security + JWT, Jakarta Validation, Maven, springdoc-openapi | SB 3.3.5 |
| Base de datos | PostgreSQL + Flyway (migraciones versionadas) | PostgreSQL 16 |
| Infraestructura | Docker Compose, variables de entorno vía `.env` | — |

## Estructura del proyecto

```
tienda-ropa/
├── backend/                  # API REST Spring Boot (/api/v1)
│   ├── src/main/java/com/tienda/ropa/
│   │   ├── config/           # Seguridad, OpenAPI, programación de tareas
│   │   ├── controller/       # 11 controladores REST (sin lógica de negocio)
│   │   ├── dto/              # Registros (records) de entrada/salida con validación
│   │   ├── entity/           # 21 entidades JPA
│   │   ├── enums/            # Estados y tipos del dominio
│   │   ├── exception/        # Excepciones de dominio + handler global
│   │   ├── mapper/           # Mappers explícitos entidad <-> DTO
│   │   ├── repository/       # Spring Data JPA (incluye locking pesimista)
│   │   ├── security/         # Filtro JWT, servicio de tokens, revocación
│   │   └── service/          # Reglas de negocio (ventas, compras, caja, etc.)
│   ├── src/main/resources/db/migration/   # V1 esquema, V2 semilla
│   └── Dockerfile
├── frontend/                 # SPA React + Vite
│   ├── src/api/              # Cliente Axios centralizado
│   ├── src/components/       # Componentes reutilizables
│   ├── src/hooks/            # Hooks de TanStack Query
│   ├── src/pages/            # Páginas por módulo
│   ├── src/types/            # Tipos e interfaces de la API
│   ├── src/validations/      # Esquemas Zod
│   └── Dockerfile, nginx.conf
├── docs/                     # arquitectura.md, api-contracts.md, security.md, diagrama-er.md
├── docker-compose.yml
├── .env.example
└── README.md
```

## Requisitos previos

- **Opción Docker:** Docker Engine + Docker Compose plugin.
- **Opción local:** JDK 17+, Maven 3.9+, Node.js 20+, npm 10+ y un PostgreSQL 16 accesible
  (o `docker compose up db` solo para la base).

## Configuración de variables de entorno

```bash
cp .env.example .env
# Edite .env: defina DB_PASSWORD y genere un JWT_SECRET propio:
openssl rand -base64 48
```

| Variable | Descripción |
|---|---|
| `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Credenciales de PostgreSQL |
| `DB_HOST`, `DB_PORT` | Host/puerto JDBC (en Docker: `db:5432`) |
| `SERVER_PORT` | Puerto HTTP del backend (defecto 8080) |
| `JWT_SECRET` | Secreto HS256 Base64 ≥256 bits (obligatorio en producción) |
| `JWT_EXPIRACION_MINUTOS` | Expiración del access token (defecto 480) |
| `CORS_ORIGINES` | Orígenes permitidos separados por coma |
| `VITE_API_URL` | URL base de la API usada por el navegador (`/api` en Docker) |
| `VITE_PROXY_TARGET` | Destino del proxy de desarrollo de Vite |

## Ejecución con Docker Compose (recomendada)

```bash
cp .env.example .env      # ajuste DB_PASSWORD y JWT_SECRET
docker compose up -d --build
```

- Frontend: http://localhost:3000
- API/Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs
- Salud: http://localhost:8080/actuator/health

Las migraciones de Flyway se aplican automáticamente al iniciar el backend.
Los datos persisten en el volumen `pgdata`.

## Ejecución local (desarrollo)

**Base de datos:**
```bash
docker compose up -d db     # o use un PostgreSQL propio
```

**Backend:**
```bash
cd backend
export DB_HOST=localhost DB_PORT=5432 DB_NAME=tienda_ropa DB_USER=postgres DB_PASSWORD=postgres
export JWT_SECRET="$(openssl rand -base64 48)"
mvn spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm ci
npm run dev    # http://localhost:5173 (proxy /api -> http://localhost:8080)
```

## Migraciones

Flyway gobierna el esquema (`ddl-auto: validate`). Migraciones en
`backend/src/main/resources/db/migration`:

- `V1__esquema_inicial.sql` — 24 tablas, FKs, CHECKs, índices, restricciones de unicidad.
- `V2__datos_semilla.sql` — roles, permisos de referencia, métodos de pago,
  configuración y usuario administrador.

Ejecución manual (si se desea): `mvn flyway:migrate` (requiere plugin configurado) o
simplemente iniciar la aplicación. Sobre una base limpia basta con arrancar el backend.

## Inicialización del administrador

La migración `V2` crea el usuario **admin** con contraseña de desarrollo `Admin123!`
(hash BCrypt; ver comentario en el SQL). **Para producción**:

1. Cambie la contraseña inmediatamente tras el primer inicio de sesión
   (`PUT /api/v1/auth/password` o Perfil → Cambiar contraseña en la interfaz), **o**
2. Antes de la primera ejecución, sustituya el hash de `V2__datos_semilla.sql`
   generando uno nuevo: `htpasswd -bnBC 10 "" 'SuClaveFuerte!' | tr -d ':\n'` (prefijo `$2y$`).

Ningún otro usuario semilla queda habilitado con credenciales conocidas.

## Roles

| Rol | Alcance |
|---|---|
| `ADMIN` | Acceso completo: usuarios, configuración, auditoría, anular ventas, todo lo demás |
| `VENDEDOR` | POS, consulta de catálogo/clientes, registrar ventas y devoluciones, caja |
| `INVENTARIO` | Productos, categorías/marcas, proveedores, compras/recepción, ajustes de inventario |

La autorización se aplica en el backend mediante `@PreAuthorize` en cada endpoint sensible.

## Pruebas

**Backend:**
```bash
cd backend
mvn clean test     # pruebas unitarias (JUnit 5) + perfil test con H2
mvn package        # genera target/*.jar
```

**Frontend:**
```bash
cd frontend
npx tsc --noEmit   # verificación de tipos
npm run test -- --run   # Vitest + React Testing Library
npm run build      # compilación de producción
```

## Documentación adicional

- `docs/arquitectura.md` — capas, módulos y flujo de datos.
- `docs/api-contracts.md` — endpoints reales, permisos y ejemplos.
- `docs/security.md` — autenticación JWT, revocación, CORS, manejo de errores.
- `docs/diagrama-er.md` — diagrama entidad-relación (Mermaid) del esquema real.

## Solución de problemas frecuentes

| Problema | Causa / Solución |
|---|---|
| `Defina DB_PASSWORD en .env` al ejecutar compose | Falta `cp .env.example .env` o variable vacía |
| Backend no inicia: `FATAL: password authentication failed` | `DB_USER/DB_PASSWORD` distintos a los usados al crear el volumen. `docker compose down -v` para reiniciar la BD |
| `Validate error: Schema-validation: missing table` | Flyway no ejecutó: revise logs; confirme que la BD apuntada es la correcta |
| CORS bloqueado en desarrollo | Añada su origen a `CORS_ORIGINES` (defecto incluye `http://localhost:5173`) |
| 401 tras un tiempo sin usar la app | El token expiró (`JWT_EXPIRACION_MINUTOS`); vuelva a iniciar sesión |
| Puerto 3000/8080 ocupado | Cambie el mapeo en `docker-compose.yml` |
| Ventas concurrentes fallan con 409 | Comportamiento esperado: el stock se valida con bloqueo pesimista en el servidor |

## Estado y limitaciones conocidas

Este repositorio refleja el estado funcional del proyecto. Consulte la sección
"Verificaciones realizadas" en `docs/arquitectura.md` para distinguir entre
funcionalidad implementada, compilada y probada automáticamente.
