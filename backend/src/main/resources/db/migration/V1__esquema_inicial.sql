-- ============================================================
-- V1: Esquema base - Tienda de Ropa (PostgreSQL)
-- Moneda: COP. Dinero siempre NUMERIC(12,2). Fechas con zona: timestamptz.
-- ============================================================

-- ---------- USUARIOS Y ROLES ----------
CREATE TABLE rol (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(50)  NOT NULL UNIQUE,      -- ADMIN, VENDEDOR, INVENTARIO
    descripcion     VARCHAR(200),
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE usuario (
    id                  BIGSERIAL PRIMARY KEY,
    username            VARCHAR(60)  NOT NULL UNIQUE,
    password_hash       VARCHAR(100) NOT NULL,
    nombre_completo     VARCHAR(120) NOT NULL,
    correo              VARCHAR(120),
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    ultimo_acceso       TIMESTAMPTZ,
    creado_en           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_username_no_vacio CHECK (btrim(username) <> '')
);

CREATE TABLE usuario_rol (
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id),
    rol_id       BIGINT NOT NULL REFERENCES rol(id),
    PRIMARY KEY (usuario_id, rol_id)
);

-- Token de revocación JWT (logout / cambio de contraseña)
CREATE TABLE token_revocado (
    id          BIGSERIAL PRIMARY KEY,
    jti         VARCHAR(64) NOT NULL UNIQUE,
    expira_en   TIMESTAMPTZ NOT NULL,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- CATÁLOGO ----------
CREATE TABLE categoria (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(300),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE marca (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(300),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE producto (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    descripcion     TEXT,
    categoria_id    BIGINT REFERENCES categoria(id),
    marca_id        BIGINT REFERENCES marca(id),
    genero          VARCHAR(20),                    -- MASCULINO, FEMENINO, UNISEX, INFANTIL
    tipo_prenda     VARCHAR(60),                    -- Camiseta, Pantalon, etc.
    temporada       VARCHAR(40),                    -- P/V 2026, etc.
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    imagen_url      VARCHAR(400),
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_producto_nombre ON producto USING gin (to_tsvector('spanish', coalesce(nombre,'') || ' ' || coalesce(descripcion,'')));
CREATE INDEX idx_producto_categoria ON producto(categoria_id);
CREATE INDEX idx_producto_activo ON producto(activo);

CREATE TABLE variante_producto (
    id              BIGSERIAL PRIMARY KEY,
    producto_id     BIGINT NOT NULL REFERENCES producto(id),
    sku             VARCHAR(60) NOT NULL UNIQUE,
    codigo_barras   VARCHAR(40) UNIQUE,
    talla           VARCHAR(15) NOT NULL,           -- S, M, L, XL, 28, 30...
    color           VARCHAR(30) NOT NULL,
    precio_venta    NUMERIC(12,2) NOT NULL CHECK (precio_venta >= 0),
    costo_unitario  NUMERIC(12,2) NOT NULL CHECK (costo_unitario >= 0),
    existencias     INTEGER NOT NULL DEFAULT 0 CHECK (existencias >= 0),
    stock_minimo    INTEGER NOT NULL DEFAULT 3 CHECK (stock_minimo >= 0),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    version         INTEGER NOT NULL DEFAULT 0,     -- @Version optimista (concurrencia)
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Impide combinaciones talla/color duplicadas por producto
    CONSTRAINT uq_variante_producto_talla_color UNIQUE (producto_id, talla, color)
);
CREATE INDEX idx_variante_sku ON variante_producto(sku);
CREATE INDEX idx_variante_barras ON variante_producto(codigo_barras);
CREATE INDEX idx_variante_producto ON variante_producto(producto_id);

-- Historial de precios
CREATE TABLE historial_precio (
    id            BIGSERIAL PRIMARY KEY,
    variante_id   BIGINT NOT NULL REFERENCES variante_producto(id),
    precio_anterior NUMERIC(12,2) NOT NULL,
    precio_nuevo    NUMERIC(12,2) NOT NULL,
    usuario_id    BIGINT REFERENCES usuario(id),
    creado_en     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- PROVEEDORES Y CLIENTES ----------
CREATE TABLE proveedor (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    nit             VARCHAR(20) UNIQUE,
    contacto        VARCHAR(120),
    telefono        VARCHAR(30),
    correo          VARCHAR(120),
    direccion       VARCHAR(200),
    ciudad          VARCHAR(80),
    observaciones   TEXT,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_proveedor_nombre ON proveedor(nombre);

CREATE TABLE cliente (
    id              BIGSERIAL PRIMARY KEY,
    tipo            VARCHAR(15) NOT NULL DEFAULT 'NATURAL' CHECK (tipo IN ('NATURAL','EMPRESA')),
    nombre          VARCHAR(150) NOT NULL,
    documento       VARCHAR(20) UNIQUE,
    correo          VARCHAR(120),
    telefono        VARCHAR(30),
    direccion       VARCHAR(200),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cliente_nombre ON cliente(nombre);

-- ---------- COMPRAS ----------
CREATE TABLE compra (
    id                BIGSERIAL PRIMARY KEY,
    numero            VARCHAR(30) NOT NULL UNIQUE,
    proveedor_id      BIGINT NOT NULL REFERENCES proveedor(id),
    estado            VARCHAR(20) NOT NULL DEFAULT 'BORRADOR'
                      CHECK (estado IN ('BORRADOR','PENDIENTE','RECIBIDA','CANCELADA')),
    fecha_registro    TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_recepcion   TIMESTAMPTZ,
    subtotal          NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    descuento         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    impuestos         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (impuestos >= 0),
    total             NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    observaciones     TEXT,
    usuario_id        BIGINT NOT NULL REFERENCES usuario(id),
    CONSTRAINT ck_compra_total CHECK (total = subtotal - descuento + impuestos)
);

CREATE TABLE detalle_compra (
    id              BIGSERIAL PRIMARY KEY,
    compra_id       BIGINT NOT NULL REFERENCES compra(id),
    variante_id     BIGINT NOT NULL REFERENCES variante_producto(id),
    cantidad        INTEGER NOT NULL CHECK (cantidad > 0),
    costo_unitario  NUMERIC(12,2) NOT NULL CHECK (costo_unitario >= 0),
    descuento       NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    impuestos       NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (impuestos >= 0),
    subtotal        NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0)
);
CREATE INDEX idx_detalle_compra_compra ON detalle_compra(compra_id);

-- ---------- VENTAS ----------
CREATE TABLE venta (
    id              BIGSERIAL PRIMARY KEY,
    numero          VARCHAR(30) NOT NULL UNIQUE,
    cliente_id      BIGINT REFERENCES cliente(id),      -- opcional: cliente ocasional
    usuario_id      BIGINT NOT NULL REFERENCES usuario(id),
    fecha           TIMESTAMPTZ NOT NULL DEFAULT now(),
    subtotal        NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
    descuento       NUMERIC(12,2) NOT NULL CHECK (descuento >= 0),
    impuestos       NUMERIC(12,2) NOT NULL CHECK (impuestos >= 0),
    total           NUMERIC(12,2) NOT NULL CHECK (total >= 0),
    estado          VARCHAR(15) NOT NULL DEFAULT 'COMPLETADA'
                    CHECK (estado IN ('PENDIENTE_PAGO','COMPLETADA','ANULADA')),
    estado_pago     VARCHAR(15) NOT NULL DEFAULT 'PAGADO'
                    CHECK (estado_pago IN ('PAGADO','PARCIAL','PENDIENTE')),
    observaciones   TEXT,
    caja_id         BIGINT,                              -- FK agregada despues de crear caja
    clave_idempotencia VARCHAR(64) UNIQUE,               -- evita doble registro por reintentos
    CONSTRAINT ck_venta_total CHECK (total = subtotal - descuento + impuestos),
    CONSTRAINT ck_venta_descuento CHECK (descuento <= subtotal)
);
CREATE INDEX idx_venta_fecha ON venta(fecha);
CREATE INDEX idx_venta_cliente ON venta(cliente_id);

CREATE TABLE detalle_venta (
    id                BIGSERIAL PRIMARY KEY,
    venta_id          BIGINT NOT NULL REFERENCES venta(id),
    variante_id       BIGINT NOT NULL REFERENCES variante_producto(id),
    cantidad          INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario   NUMERIC(12,2) NOT NULL CHECK (precio_unitario >= 0),  -- congelado en la venta
    descuento         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    impuestos         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (impuestos >= 0),
    subtotal          NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
    costo_unitario    NUMERIC(12,2) NOT NULL DEFAULT 0,                     -- para ganancia bruta
    devuelto          INTEGER NOT NULL DEFAULT 0 CHECK (devuelto >= 0 AND devuelto <= cantidad)
);
CREATE INDEX idx_detalle_venta_venta ON detalle_venta(venta_id);
CREATE INDEX idx_detalle_venta_variante ON detalle_venta(variante_id);

-- ---------- PAGOS ----------
CREATE TABLE pago_venta (
    id            BIGSERIAL PRIMARY KEY,
    venta_id      BIGINT NOT NULL REFERENCES venta(id),
    metodo        VARCHAR(20) NOT NULL CHECK (metodo IN ('EFECTIVO','TRANSFERENCIA','TARJETA','OTRO')),
    monto         NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    fecha         TIMESTAMPTZ NOT NULL DEFAULT now(),
    referencia    VARCHAR(80),
    estado        VARCHAR(15) NOT NULL DEFAULT 'REGISTRADO' CHECK (estado IN ('REGISTRADO','ANULADO')),
    usuario_id    BIGINT REFERENCES usuario(id)
);
CREATE INDEX idx_pago_venta ON pago_venta(venta_id);

-- ---------- INVENTARIO ----------
CREATE TABLE movimiento_inventario (
    id                BIGSERIAL PRIMARY KEY,
    variante_id       BIGINT NOT NULL REFERENCES variante_producto(id),
    tipo              VARCHAR(15) NOT NULL CHECK (tipo IN ('ENTRADA','SALIDA','AJUSTE','DEVOLUCION')),
    cantidad          INTEGER NOT NULL,               -- positiva; el signo lo define el tipo/regla
    existencias_antes INTEGER NOT NULL CHECK (existencias_antes >= 0),
    existencias_despues INTEGER NOT NULL CHECK (existencias_despues >= 0),
    motivo            VARCHAR(250) NOT NULL,
    usuario_id        BIGINT NOT NULL REFERENCES usuario(id),
    fecha             TIMESTAMPTZ NOT NULL DEFAULT now(),
    venta_id          BIGINT REFERENCES venta(id),
    compra_id         BIGINT REFERENCES compra(id),
    devolucion_id     BIGINT REFERENCES "devolucion"(id)
);
-- Nota: la FK a devolucion se agrega mas abajo (tabla creada despues)

-- ---------- DEVOLUCIONES ----------
CREATE TABLE devolucion (
    id              BIGSERIAL PRIMARY KEY,
    numero          VARCHAR(30) NOT NULL UNIQUE,
    venta_id        BIGINT NOT NULL REFERENCES venta(id),
    fecha           TIMESTAMPTZ NOT NULL DEFAULT now(),
    motivo          VARCHAR(250) NOT NULL,
    tipo            VARCHAR(15) NOT NULL DEFAULT 'REEMBOLSO' CHECK (tipo IN ('REEMBOLSO','CAMBIO','SALDO_FAVOR')),
    estado          VARCHAR(15) NOT NULL DEFAULT 'REGISTRADA' CHECK (estado IN ('REGISTRADA','ANULADA')),
    usuario_id      BIGINT NOT NULL REFERENCES usuario(id),
    total_reembolsado NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_reembolsado >= 0),
    clave_idempotencia VARCHAR(64) UNIQUE
);

CREATE TABLE detalle_devolucion (
    id                  BIGSERIAL PRIMARY KEY,
    devolucion_id       BIGINT NOT NULL REFERENCES devolucion(id),
    detalle_venta_id    BIGINT NOT NULL REFERENCES detalle_venta(id),
    cantidad            INTEGER NOT NULL CHECK (cantidad > 0),
    regresa_inventario  BOOLEAN NOT NULL DEFAULT TRUE,   -- FALSE si la prenda esta danada
    estado_fisico       VARCHAR(20) NOT NULL DEFAULT 'BUENO' CHECK (estado_fisico IN ('BUENO','DANADO')),
    monto_reembolso     NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (monto_reembolso >= 0),
    observacion         VARCHAR(250)
);

ALTER TABLE movimiento_inventario
    ADD CONSTRAINT fk_mov_devolucion FOREIGN KEY (devolucion_id) REFERENCES devolucion(id);

-- ---------- CAJA ----------
CREATE TABLE caja (
    id              BIGSERIAL PRIMARY KEY,
    numero          VARCHAR(30) NOT NULL UNIQUE,
    abierto_por     BIGINT NOT NULL REFERENCES usuario(id),
    cerrado_por     BIGINT REFERENCES usuario(id),
    apertura        TIMESTAMPTZ NOT NULL DEFAULT now(),
    cierre          TIMESTAMPTZ,
    fondo_inicial   NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (fondo_inicial >= 0),
    efectivo_esperado NUMERIC(12,2),
    efectivo_contado  NUMERIC(12,2),
    diferencia        NUMERIC(12,2),
    estado          VARCHAR(15) NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA','CERRADA')),
    observaciones   TEXT
);

ALTER TABLE venta ADD CONSTRAINT fk_venta_caja FOREIGN KEY (caja_id) REFERENCES caja(id);

CREATE TABLE movimiento_caja (
    id          BIGSERIAL PRIMARY KEY,
    caja_id     BIGINT NOT NULL REFERENCES caja(id),
    tipo        VARCHAR(10) NOT NULL CHECK (tipo IN ('INGRESO','EGRESO')),
    monto       NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    motivo      VARCHAR(250) NOT NULL,
    fecha       TIMESTAMPTZ NOT NULL DEFAULT now(),
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id)
);

-- ---------- CONFIGURACION ----------
CREATE TABLE configuracion_tienda (
    id                        BIGSERIAL PRIMARY KEY,
    nombre_comercial          VARCHAR(150) NOT NULL DEFAULT 'Mi Tienda de Ropa',
    logo_url                  VARCHAR(400),
    direccion                 VARCHAR(200),
    telefono                  VARCHAR(30),
    correo                    VARCHAR(120),
    nit                       VARCHAR(20),
    moneda                    VARCHAR(10) NOT NULL DEFAULT 'COP',
    zona_horaria              VARCHAR(50) NOT NULL DEFAULT 'America/Bogota',
    iva_porcentaje            NUMERIC(5,2) NOT NULL DEFAULT 19.00 CHECK (iva_porcentaje >= 0 AND iva_porcentaje <= 100),
    descuento_max_vendedor    NUMERIC(5,2) NOT NULL DEFAULT 10.00 CHECK (descuento_max_vendedor >= 0 AND descuento_max_vendedor <= 100),
    descuento_max_admin       NUMERIC(5,2) NOT NULL DEFAULT 50.00 CHECK (descuento_max_admin >= 0 AND descuento_max_admin <= 100),
    stock_minimo_default      INTEGER NOT NULL DEFAULT 3 CHECK (stock_minimo_default >= 0),
    metodos_pago              VARCHAR(200) NOT NULL DEFAULT 'EFECTIVO,TRANSFERENCIA,TARJETA,OTRO',
    permitir_cliente_ocasional BOOLEAN NOT NULL DEFAULT TRUE,
    texto_comprobante         TEXT
);

-- ---------- AUDITORIA ----------
CREATE TABLE registro_auditoria (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT REFERENCES usuario(id),
    accion      VARCHAR(60) NOT NULL,        -- LOGIN, VENTA_CREAR, PRODUCTO_EDITAR, ...
    entidad     VARCHAR(60),
    entidad_id  VARCHAR(40),
    detalle     TEXT,                         -- JSON sin datos sensibles
    ip          VARCHAR(45),
    fecha       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_auditoria_fecha ON registro_auditoria(fecha);
CREATE INDEX idx_auditoria_usuario ON registro_auditoria(usuario_id);
