# Contratos de API — tienda-ropa (`/api/v1`)

Base: `http://localhost:8080/api/v1`. Documentación interactiva (Swagger UI):
`http://localhost:8080/swagger-ui.html`; OpenAPI JSON: `http://localhost:8080/api-docs`.

## Convenciones

- **Autenticación:** header `Authorization: Bearer <JWT>` en todos los endpoints salvo
  `POST /auth/login`. Tokens inválidos/expirados/revocados → `401`.
- **Autorización:** se indica el rol requerido; sin permiso → `403`.
- **Errores:** cuerpo uniforme
  ```json
  { "timestamp": "...", "status": 409, "error": "CONFLICTO", "message": "...", "detalles": [ {"campo": "...", "mensaje": "..."} ] }
  ```
  Códigos usados: 200, 201, 204, 400 (validación), 401, 403, 404, 409 (conflicto/regla de negocio), 500.
- **Paginación:** parámetros Spring `page`, `size` (máx. 100), `sort`. Respuesta `Page<T>`.
- **Moneda:** COP; importes como números decimales (JSON) respaldados por `NUMERIC(14,2)`.

## Autenticación y usuarios (`AuthController`)

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/auth/login` | público | `{username,password}` → `{token, tipo:"Bearer", expiraEn, usuario:{id,nombre,roles}}` |
| POST | `/auth/logout` | autenticado | Revoca el `jti` del token actual → 204 |
| GET | `/auth/perfil` | autenticado | Datos del usuario autenticado |
| PUT | `/auth/password` | autenticado | `{passwordActual, passwordNueva}` → 204 |
| GET | `/usuarios` | ADMIN | Página de usuarios (sin hashes) |
| POST | `/usuarios` | ADMIN | Crear usuario con roles |
| PUT | `/usuarios/{id}` | ADMIN | Actualizar datos/roles/contraseña |
| PATCH | `/usuarios/{id}/activo?activo=false` | ADMIN | Activar/desactivar cuenta |

Ejemplo login:
```
POST /api/v1/auth/login   {"username":"admin","password":"Admin123!"}
200 {"token":"eyJ...","tipo":"Bearer","expiraEn":"2026-10-17T18:00:00Z","usuario":{"id":1,"nombre":"Administrador","roles":["ADMIN"]}}
```

## Catálogo (`CatalogoController`) — productos, variantes, categorías, marcas

| Método | Ruta | Rol | Notas |
|---|---|---|---|
| GET | `/productos?q=&categoriaId=&marcaId=&estado=&page=&size=` | autenticado | Búsqueda por nombre/SKU/código de barras |
| POST | `/productos` | ADMIN, INVENTARIO | 201; valida unicidad y categoría/marca existentes |
| GET | `/productos/{id}` | autenticado | Incluye lista de variantes |
| PUT | `/productos/{id}` | ADMIN, INVENTARIO | Edición |
| DELETE | `/productos/{id}` | ADMIN, INVENTARIO | Desactivación lógica (409 si tiene historial y está activo aún) |
| GET | `/productos/{productoId}/variantes` | autenticado | |
| POST | `/productos/{productoId}/variantes` | ADMIN, INVENTARIO | SKU único; talla+color no duplicados (409) |
| PUT | `/variantes/{varianteId}` | ADMIN, INVENTARIO | Precio/costo/stock mínimo; el stock NO se edita aquí (usar ajustes) |
| GET | `/variantes/{varianteId}/historial-precios` | autenticado | |
| GET/POST/PUT | `/categorias`, `/categorias/{id}` | lectura: autenticado; escritura: ADMIN, INVENTARIO | Nombre único |
| GET/POST/PUT | `/marcas`, `/marcas/{id}` | idem | Nombre único |

## Inventario (`InventarioController`)

| Método | Ruta | Rol | Notas |
|---|---|---|---|
| GET | `/inventario/resumen` | ADMIN, INVENTARIO | Valor a costo, unidades por categoría |
| GET | `/inventario/stock-bajo` | ADMIN, INVENTARIO | Variantes ≤ stock mínimo |
| GET | `/inventario/agotados` | ADMIN, INVENTARIO | Existencias = 0 |
| GET | `/inventario/movimientos?varianteId=&tipo=&desde=&hasta=&page=` | ADMIN, INVENTARIO | Historial trazable |
| POST | `/inventario/variantes/{varianteId}/ajuste` | ADMIN, INVENTARIO | `{cantidadDelta,motivo}` motivo obligatorio; nunca deja stock < 0 (409) |

## Proveedores y clientes (`PersonaController`)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/proveedores?q=&page=` | autenticado |
| POST/PUT | `/proveedores`, `/proveedores/{id}` | ADMIN, INVENTARIO |
| GET | `/proveedores/{id}/compras` | ADMIN, INVENTARIO |
| GET | `/clientes?q=&page=` | autenticado |
| POST/PUT | `/clientes`, `/clientes/{id}` | autenticado (VENDEDOR puede crear) |
| GET | `/clientes/{id}/historial` | autenticado |

## Compras (`CompraController`) — rol ADMIN o INVENTARIO en todo el controlador

| Método | Ruta | Notas |
|---|---|---|
| GET | `/compras?estado=&proveedorId=&page=` | |
| POST | `/compras` | `{proveedorId,detalles:[{varianteId,cantidad,costoUnitario}],descuento,observaciones}` → estado BORRADOR/PENDIENTE; totales calculados en servidor |
| GET | `/compras/{id}` | Cabecera + detalles |
| PATCH | `/compras/{id}/estado` | Transición de estado controlada (BORRADOR→PENDIENTE, cancelación según reglas del servicio) |
| POST | `/compras/{id}/recepcion` | Solo PENDIENTE → RECIBIDA; incrementa stock una única vez con movimientos ENTRADA (409 si ya recibida) |

## Ventas (`VentaController`) — escritura: ADMIN, VENDEDOR

| Método | Ruta | Notas |
|---|---|---|
| POST | `/ventas` | `{clienteId?,items:[{varianteId,cantidad}],descuentoPorcentaje?,clienteRef?}`. El servidor recalcula todo; valida stock con bloqueo pesimista; 409 `InsufficientStock` si no hay existencias; idempotencia por `clienteRef` (reintento devuelve la venta existente) |
| GET | `/ventas?estado=&clienteId=&vendedorId=&desde=&hasta=&page=` | |
| GET | `/ventas/{id}` y `/ventas/numero/{numero}` | Detalle con ítems y pagos |
| POST | `/ventas/simular` | autenticado | Cálculo en servidor de subtotal/descuento/impuesto/total para el carrito antes de confirmar (sin persistir) |
| POST | `/ventas/{ventaId}/pagos` | `{metodo,importe,referencia?}`; suma de pagos ≤ total pendiente (409); al cubrir el total la venta pasa a PAGADA |
| POST | `/ventas/{ventaId}/anular` | `{motivo}`; devuelve stock con movimiento DEVOLUCION; trazabilidad conservada |

## Devoluciones (`DevolucionController`)

| Método | Ruta | Rol | Notas |
|---|---|---|---|
| POST | `/devoluciones` | ADMIN, VENDEDOR | `{ventaId,items:[{detalleVentaId,cantidad,regrasaInventario}],motivo,conReembolso,clienteRef?}`; valida cantidad ≤ vendida−devuelta (409); entrada a inventario solo si `regrasaInventario=true` |
| GET | `/devoluciones?...` | ADMIN, VENDEDOR | Listado paginado |
| GET | `/devoluciones/{id}` | autenticado | |
| POST | `/devoluciones/{id}/anular` | ADMIN | Anula la devolución (ruta sobre el id de devolución) y revierte sus efectos |

## Caja (`CajaController`) — ADMIN, VENDEDOR

| Método | Ruta | Notas |
|---|---|---|
| POST | `/caja/abrir` | `{fondoInicial}`; 409 si ya existe caja abierta del usuario |
| GET | `/caja/abierta` | Caja actual o 404 |
| GET | `/caja/{id}` | Cabecera de la caja |
| POST | `/caja/{cajaId}/movimientos` | `{tipo:INGRESO|EGRESO,importe,motivo}` |
| POST | `/caja/{cajaId}/cerrar` | `{efectivoContado}` → diferencia calculada; estado CERRADA. El resumen de pagos/efectivo esperado se obtiene de `GET /caja/abierta` y `GET /reportes/ingresos-por-metodo` |
| GET | `/caja?page=` | Historial de cierres |

## Reportes y dashboard (`ReporteController`) — autenticado

| Método | Ruta | Contenido |
|---|---|---|
| GET | `/reportes/dashboard` | Ventas día/mes, transacciones, unidades, ticket promedio, ganancia bruta estimada, más vendidos, stock bajo/agotados, compras, valor de inventario |
| GET | `/reportes/serie-ventas?desde=&hasta=&granularidad=dia|semana|mes` | Serie temporal |
| GET | `/reportes/productos-mas-vendidos?desde=&hasta=&limit=` | Top N |
| GET | `/reportes/ingresos-por-metodo?desde=&hasta=` | Por método de pago |

## Configuración (`ConfiguracionController`)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/configuracion` | autenticado |
| PUT | `/configuracion` | ADMIN (audita el cambio) |

## Auditoría (`AuthController`)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/auditoria?usuarioId=&desde=&hasta=&page=` | ADMIN |

> Nota: rutas exactas y esquemas de cuerpos pueden consultarse en vivo en `/api-docs`
> (OpenAPI generado automáticamente por springdoc).
