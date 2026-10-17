# Arquitectura — tienda-ropa

## Visión general

Sistema web de tres capas desacopladas por una API REST versionada (`/api/v1`):

```
[Navegador SPA React] --HTTP/JSON--> [Backend Spring Boot] --JDBC/Hibernate--> [PostgreSQL]
```

- **Frontend** (`frontend/`): SPA en React 18 + TypeScript estricto (Vite). Acceso a la API
  centralizado en `src/api/client.ts` (Axios con interceptor que adjunta el token JWT y
  normaliza errores). Estado de servidor con TanStack Query (caché + invalidación por
  mutación). Formularios con React Hook Form + Zod. Gráficos con Recharts. Navegación con
  React Router; rutas protegidas por sesión y por rol.
- **Backend** (`backend/`): Spring Boot 3.3 sobre Java 17, organizado por capas:
  - `controller`: expone endpoints REST, valida entrada (`@Valid`) y delega. Sin lógica de negocio. No devuelve entidades JPA.
  - `service`: reglas de negocio y transacciones (`@Transactional`). Cálculo de importes, validación de stock, idempotencia, trazabilidad.
  - `repository`: Spring Data JPA. Consultas JPQL/SQL nativas paginadas; `findByIdForUpdate` (bloqueo pesimista `PESSIMISTIC_WRITE`) para variantes afectadas por ventas/recepciones.
  - `entity`: 21 entidades JPA; dinero como `BigDecimal`; fechas como `Instant`/`LocalDateTime` con zona `America/Bogota`.
  - `dto`: Java records inmutables con anotaciones Jakarta Validation.
  - `mapper`: transformaciones explícitas entidad↔DTO (`Mappers`).
  - `security`: emisión/validación de JWT HS256 (`JwtService`), filtro (`JwtAuthFilter`), revocación de tokens persistida (`TokenRevocado`).
  - `exception`: excepciones de dominio (`BusinessException`, `ConflictException`, `ResourceNotFoundException`, `InsufficientStockException`) y `GlobalExceptionHandler` con formato uniforme `{timestamp,status,error,message,detalles[]}`. Sin trazas al cliente.
  - `config`: `SecurityConfig` (estados sin sesión, CORS estricto, BCrypt), OpenAPI, salud Actuator.
- **Base de datos**: PostgreSQL 16. Esquema gobernado exclusivamente por Flyway
  (`V1__esquema_inicial.sql`, `V2__datos_semilla.sql`); Hibernate solo valida (`ddl-auto: validate`).

## Módulos de negocio

| Módulo | Servicio principal | Reglas clave |
|---|---|---|
| Autenticación/Usuarios | `AuthService` | BCrypt, roles ADMIN/VENDEDOR/INVENTARIO, logout revoca el jti, cambio de contraseña exige clave actual |
| Catálogo | `CatalogoService` | SKU único, no duplicar combinación talla+color por producto, desactivación en lugar de borrado físico |
| Inventario | `InventarioService` | Todo cambio de stock genera `MovimientoInventario` con existencias anteriores/posteriores, motivo obligatorio en ajustes, stock nunca negativo |
| Compras | `CompraService` | Borrador no toca inventario; recepción incrementa una sola vez (estado `RECIBIDA` irreversible); cancelación solo desde BORRADOR/PENDIENTE |
| Ventas/POS | `VentaService` | Importes calculados en el servidor; disponibilidad revalidada con bloqueo pesimista; número consecutivo único vía `NumeroService` (tabla secuencia con fila bloqueada); idempotencia por `clienteRef` único; anulación devuelve stock con movimiento trazable |
| Devoluciones | `DevolucionService` | Cantidad devuelta ≤ vendida − ya devuelta; entrada a inventario solo si la prenda es apta; reembolsos documentados |
| Caja | `CajaService` | Una caja abierta por turno; movimientos manuales con motivo; cierre calcula esperado vs contado sin duplicar ventas/pagos (los pagos se consultan del período) |
| Reportes/Dashboard | `ReporteService` | Indicadores agregados con SQL sobre datos persistidos; ganancia bruta = ingresos − costo de lo vendido |
| Auditoría | `AuditoriaService` | Registro de acciones sensibles (venta, anulación, ajuste, cambio de configuración, login) con usuario, entidad y detalle |

## Concurrency e integridad

- Bloqueo pesimista sobre `variante_producto` al descontar/incrementar stock dentro de la transacción de venta/recepción.
- Restricciones `CHECK` en BD (cantidades > 0, totales ≥ 0, stock ≥ 0) como última línea de defensa.
- Unicidad garantizada en BD: SKU, número de venta, combinaciones talla/color por producto, referencia de idempotencia.

## Verificaciones realizadas (estado honesto)

- Backend: `mvn clean compile` y empaquetado ejecutados correctamente en el entorno de desarrollo. Las pruebas automatizadas del backend se ejecutan con perfil `test` (H2 en memoria) mediante `mvn test`; los resultados concretos de la última ejecución se registran en el informe del entregable.
- Frontend: verificación de tipos (`tsc --noEmit`), pruebas Vitest y `npm run build` ejecutados según registro del proyecto.
- Docker: `docker-compose.yml` validado estáticamente; su construcción completa requiere Docker Engine en el host del usuario.
