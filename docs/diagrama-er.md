# Diagrama Entidad-Relación — tienda-ropa

Generado a partir de las tablas reales de `V1__esquema_inicial.sql` (23 tablas).
Los importes monetarios son `NUMERIC(14,2)`; las cantidades `INTEGER` con `CHECK > 0`;
el stock tiene `CHECK >= 0`. Las cardinalidades reflejan las claves foráneas del esquema.

```mermaid
erDiagram
    USUARIO ||--o{ USUARIO_ROL : "tiene"
    ROL ||--o{ USUARIO_ROL : "asignado a"
    USUARIO ||--o{ TOKEN_REVOCADO : "emite"
    USUARIO ||--o{ REGISTRO_AUDITORIA : "genera"

    CATEGORIA ||--o{ PRODUCTO : "clasifica"
    MARCA ||--o{ PRODUCTO : "pertenece"
    PRODUCTO ||--o{ VARIANTE_PRODUCTO : "tiene (talla+color, SKU unico)"
    VARIANTE_PRODUCTO ||--o{ HISTORIAL_PRECIO : "registra precios"

    PROVEEDOR ||--o{ COMPRA : "origina"
    COMPRA ||--o{ DETALLE_COMPRA : "contiene"
    VARIANTE_PRODUCTO ||--o{ DETALLE_COMPRA : "adquirida en"

    CLIENTE ||--o{ VENTA : "compra (opcional)"
    USUARIO ||--o{ VENTA : "vende"
    VENTA ||--o{ DETALLE_VENTA : "contiene (precio historico congelado)"
    VARIANTE_PRODUCTO ||--o{ DETALLE_VENTA : "vendido en"
    VENTA ||--o{ PAGO_VENTA : "pagada por (multi-metodo)"
    CAJA ||--o{ PAGO_VENTA : "asociada (caja_id)"

    VENTA ||--o{ DEVOLUCION : "devuelta por"
    DEVOLUCION ||--o{ DETALLE_DEVOLUCION : "contiene"
    DETALLE_VENTA ||--o{ DETALLE_DEVOLUCION : "base de"
    VARIANTE_PRODUCTO ||--o{ DETALLE_DEVOLUCION : "regresa"

    VARIANTE_PRODUCTO ||--o{ MOVIMIENTO_INVENTARIO : "traza"
    USUARIO ||--o{ MOVIMIENTO_INVENTARIO : "responsable"
    MOVIMIENTO_INVENTARIO }o--|| VENTA : "referencia (nullable)"
    MOVIMIENTO_INVENTARIO }o--|| COMPRA : "referencia (nullable)"
    MOVIMIENTO_INVENTARIO }o--|| DEVOLUCION : "referencia (nullable)"

    USUARIO ||--o{ CAJA : "abre/cierra"
    CAJA ||--o{ MOVIMIENTO_CAJA : "ingresos/egresos manuales"
    USUARIO ||--o{ MOVIMIENTO_CAJA : "registra"

    CONFIGURACION_TIENDA ||--|| CONFIGURACION_TIENDA : "fila unica id=1"

    USUARIO {
        bigint id PK
        varchar username UK
        varchar password_hash "BCrypt"
        boolean activo
        timestamp last_login
    }
    ROL { bigint id PK
        varchar nombre UK "ADMIN|VENDEDOR|INVENTARIO"
    }
    PRODUCTO {
        bigint id PK
        varchar nombre
        bigint categoria_id FK
        bigint marca_id FK
        boolean activo
    }
    VARIANTE_PRODUCTO {
        bigint id PK
        bigint producto_id FK
        varchar sku UK
        varchar codigo_barras
        varchar talla
        varchar color
        numeric precio_venta
        numeric costo
        int existencias "CHECK >= 0"
        int stock_minimo
        unique(producto_id, talla, color)
    }
    VENTA {
        bigint id PK
        varchar numero UK "V-AAAAMM-##### via NumeroService"
        bigint cliente_id FK "nullable (ocasional)"
        bigint usuario_id FK
        numeric subtotal descuento impuesto total
        varchar estado "PENDIENTE|PAGADA|ANULADA"
        varchar cliente_ref UK "idempotencia"
    }
    DETALLE_VENTA {
        bigint id PK
        bigint venta_id FK
        bigint variante_id FK
        int cantidad "CHECK > 0"
        numeric precio_unitario "congelado"
    }
    PAGO_VENTA {
        bigint id PK
        bigint venta_id FK
        varchar metodo "EFECTIVO|TARJETA|TRANSFERENCIA|OTRO"
        numeric importe
        varchar referencia
        bigint caja_id FK
    }
    COMPRA {
        bigint id PK
        varchar numero UK
        bigint proveedor_id FK
        varchar estado "BORRADOR|PENDIENTE|RECIBIDA|CANCELADA"
        timestamp fecha_recepcion
    }
    MOVIMIENTO_INVENTARIO {
        bigint id PK
        bigint variante_id FK
        varchar tipo "ENTRADA|SALIDA|AJUSTE|DEVOLUCION"
        int cantidad
        int existencias_anteriores
        int existencias_posteriores
        varchar motivo
        bigint usuario_id FK
    }
    DEVOLUCION {
        bigint id PK
        bigint venta_id FK
        varchar motivo
        varchar estado
        numeric monto_reembolso
        varchar cliente_ref UK "idempotencia"
    }
    CAJA {
        bigint id PK
        bigint usuario_id FK
        numeric fondo_inicial
        varchar estado "ABIERTA|CERRADA"
        numeric efectivo_esperado efectivo_contado diferencia
    }
```

## Reglas de integridad aplicadas en el esquema

- `variante_producto`: UNIQUE(sku), UNIQUE(producto_id, talla, color), CHECK(existencias >= 0).
- `venta` / `compra` / `devolucion`: UNIQUE(numero o cliente_ref) para consecutivos e idempotencia.
- `detalle_venta`, `detalle_compra`, `detalle_devolucion`: CHECK(cantidad > 0); FK RESTRICT para preservar historial.
- `movimiento_inventario`: FKs de referencia (venta/compra/devolucion) opcionales con ON DELETE SET NULL implícito por política RESTRICT en padres históricos.
- Borrados físicos no permitidos sobre registros comerciales: se usan banderas `activo`/estados.
