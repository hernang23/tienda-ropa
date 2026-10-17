-- ============================================================
-- V2: Datos de referencia y usuario administrador inicial.
-- El hash corresponde a la contrasena: Admin123!  (BCrypt, fuerza 10)
-- CAMBIAR esta contrasena en produccion mediante el perfil de usuario.
-- ============================================================

INSERT INTO rol (nombre, descripcion) VALUES
  ('ADMIN', 'Administrador: acceso completo al sistema'),
  ('VENDEDOR', 'Vendedor: punto de venta, clientes y consulta de productos'),
  ('INVENTARIO', 'Encargado de inventario: productos, compras y existencias');

-- Usuario administrador inicial (hash BCrypt de 'Admin123!')
INSERT INTO usuario (username, password_hash, nombre_completo, correo, activo)
VALUES ('admin', '$2a$10$ZqZlkaUewvfyNL7c5iLsb.sRiB/H0xAMpTNrRy8P9Dq0NQh7uWPoG', 'Administrador General', 'admin@tienda.local', TRUE);

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id FROM usuario u, rol r WHERE u.username='admin' AND r.nombre='ADMIN';

INSERT INTO configuracion_tienda (nombre_comercial, direccion, telefono, correo, nit, texto_comprobante)
VALUES ('Boutique Andina', 'Calle 10 # 24-35, Bogota', '+57 601 555 0100', 'contacto@boutiqueandina.co', 'NIT 900.123.456-7',
        'Gracias por su compra. Cambios dentro de los 30 dias conservando el tiquete.');

INSERT INTO categoria (nombre, descripcion) VALUES
  ('Camisetas', 'Camisetas basicas y estampadas'),
  ('Pantalones', 'Pantalones jeans y casual'),
  ('Vestidos', 'Vestidos para todas las ocasiones'),
  ('Chaquetas', 'Chaquetas y abrigos'),
  ('Accesorios', 'Bufandas, gorros y otros accesorios');

INSERT INTO marca (nombre, descripcion) VALUES
  ('Andina Wear', 'Marca nacional'),
  ('UrbanFit', 'Ropa urbana casual'),
  ('Eleganza', 'Linea formal y elegante');
