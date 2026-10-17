# Seguridad — tienda-ropa

## Autenticación

- Tokens JWT **HS256** emitidos por `POST /api/v1/auth/login`. El secreto proviene
  exclusivamente de la variable de entorno `JWT_SECRET` (mínimo 256 bits, Base64).
- Expiración configurada con `JWT_EXPIRACION_MINUTOS` (defecto 480 min).
- **Revocación:** `POST /api/v1/auth/logout` persiste el `jti` del token en la tabla
  `token_revocado`; `JwtAuthFilter` rechaza tokens revocados o expirados en cada petición.
- Contraseñas con hash **BCrypt** (`SecurityConfig` → `BCryptPasswordEncoder`). Nunca se
  almacenan ni devuelven en texto plano; el campo `password_hash` no forma parte de ningún DTO de salida.
- Cambio de contraseña (`PUT /api/v1/auth/password`) exige verificar la contraseña actual.
- Recuperación por correo: no implementada (requiere proveedor SMTP); el flujo habilitado es
  el restablecimiento por un administrador (`PUT /api/v1/usuarios/{id}`).

## Autorización

- Roles: `ADMIN`, `VENDEDOR`, `INVENTARIO` (tabla `rol`, relación N:N con `usuario_rol`).
- Aplicada en el **backend** endpoint por endpoint con `@PreAuthorize("hasRole(...)")` /
  `hasAnyRole(...)`. Los permisos de lectura autenticada son mínimos; las operaciones de
  escritura están restringidas por rol (usuarios/configuración/auditoría/anular venta total:
  ADMIN; POS/devoluciones/caja: ADMIN+VENDEDOR; catálogo/compras/inventario/proveedores:
  ADMIN+INVENTARIO).
- El frontend oculta navegación por rol únicamente como UX; no es la frontera de seguridad.

## Protección de la aplicación

- **SQL Injection:** consultas parametrizadas vía Spring Data JPA/JPQL y native queries con parámetros nombrados. Sin concatenación de SQL.
- **XSS:** la SPA no renderiza HTML procedente de la API (React escapa por defecto); respuestas JSON con `Content-Type` adecuado.
- **CSRF:** deshabilitado deliberadamente porque la autenticación es *stateless* por header `Authorization: Bearer` (no cookies). No existe sesión por cookie que pueda forjarse.
- **CORS:** lista estricta configurable por `CORS_ORIGINES` (defecto `http://localhost:5173,http://localhost:3000`); sin `*`, sin credenciales cross-origin.
- **Cabeceras:** Spring Security añade `X-Content-Type-Options`, `X-Frame-Options/DENY` y caché no-store en endpoints de API.
- **Errores:** `GlobalExceptionHandler` mapea excepciones de dominio a 400/404/409/422 con mensajes seguros; los errores inesperados se registran internamente con `Logger` y devuelven 500 genérico (sin trazas ni detalles internos). `server.error.include-stacktrace=never`.
- **Paginación limitada:** los endpoints listados usan `Pageable` con tamaño máximo aplicado en servicio.
- **Datos financieros sensibles:** no se almacenan números de tarjeta ni CVV; solo método, importe, referencia opcional y estado.
- **Secretos:** fuera del repositorio; `.env` ignorado por Git; solo `.env.example` con valores ilustrativos. La imagen Docker corre el backend con usuario sin privilegios.
- **Auditoría:** `registro_auditoria` guarda usuario, acción, entidad, detalle y fecha de operaciones sensibles.
- **Salud:** `/actuator/health` expuesto sin detalles (`show-details: never`).

## Administración del administrador inicial

La migración V2 crea `admin` con contraseña de desarrollo `Admin123!`. Para producción debe
rotarse inmediatamente (perfil → cambiar contraseña) o sustituir el hash antes del primer
arranque. No se incluyen otras cuentas semilla activas.
