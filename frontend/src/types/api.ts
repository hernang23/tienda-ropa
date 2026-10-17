// Tipos e interfaces del frontend alineados con los DTO reales del backend
// (com.tienda.ropa.dto.*). Los montos llegan como string o número JSON; se normalizan
// mediante `num()` en utilidades. Las fechas son ISO strings generados por el backend.

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export const EMPTY_PAGE: Page<never> = {
  content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true,
};

// ---------- Autenticación / usuarios ----------
export interface UsuarioDto {
  id: number;
  username: string;
  nombreCompleto: string;
  correo?: string | null;
  activo: boolean;
  roles: string[];
  ultimoAcceso?: string | null;
  creadoEn?: string | null;
}

export interface LoginResponse {
  token: string;
  usuario: UsuarioDto;
}

export interface CrearUsuarioRequest {
  username: string;
  password: string;
  nombreCompleto: string;
  correo?: string;
  roles: string[];
}

export interface ActualizarUsuarioRequest {
  nombreCompleto: string;
  correo?: string;
  roles: string[];
}

// ---------- Catálogo ----------
export interface CategoriaDto { id: number; nombre: string; descripcion?: string | null; activo: boolean; creadoEn?: string | null; }
export interface MarcaDto { id: number; nombre: string; descripcion?: string | null; activo: boolean; creadoEn?: string | null; }

export interface VarianteDto {
  id: number; productoId: number; sku: string; codigoBarras?: string | null;
  talla: string; color: string; precioVenta: number | string; costoUnitario: number | string;
  existencias: number; stockMinimo: number; activo: boolean; bajoMinimo: boolean; agotado: boolean;
}

export interface ProductoDto {
  id: number; nombre: string; descripcion?: string | null;
  categoriaId?: number | null; categoriaNombre?: string | null;
  marcaId?: number | null; marcaNombre?: string | null;
  genero?: string | null; tipoPrenda?: string | null; temporada?: string | null;
  activo: boolean; imagenUrl?: string | null;
  creadoEn?: string | null; actualizadoEn?: string | null;
  variantes: VarianteDto[]; totalExistencias: number;
}

export interface VarianteCrearRequest {
  sku: string; codigoBarras?: string; talla: string; color: string;
  precioVenta: number; costoUnitario: number; existenciasIniciales: number;
  stockMinimo?: number; activo?: boolean;
}

export interface VarianteActualizarRequest {
  codigoBarras?: string; precioVenta: number; costoUnitario: number;
  stockMinimo?: number; activo?: boolean;
}

export interface ProductoCrearRequest {
  nombre: string; descripcion?: string; categoriaId?: number; marcaId?: number;
  genero?: string; tipoPrenda?: string; temporada?: string; imagenUrl?: string;
  variantes?: VarianteCrearRequest[];
}

export interface ProductoActualizarRequest {
  nombre: string; descripcion?: string; categoriaId?: number; marcaId?: number;
  genero?: string; tipoPrenda?: string; temporada?: string; imagenUrl?: string; activo?: boolean;
}

export interface NombreDescActivoRequest { nombre: string; descripcion?: string; activo?: boolean; }

// ---------- Inventario ----------
export interface MovimientoDto {
  id: number; varianteId: number; sku: string; productoNombre: string; talla: string; color: string;
  tipo: string; cantidad: number; existenciasAntes: number; existenciasDespues: number;
  motivo: string; usuarioId: number; fecha: string;
  ventaId?: number | null; compraId?: number | null; devolucionId?: number | null;
}

export interface AjusteRequest { delta: number; motivo: string; }

export interface ResumenInventarioDto {
  valorAlCosto: number | string; unidadesTotales: number;
  variantesBajoMinimo: number; variantesAgotadas: number;
}

// ---------- Personas ----------
export interface ProveedorDto {
  id: number; nombre: string; nit?: string | null; contacto?: string | null; telefono?: string | null;
  correo?: string | null; direccion?: string | null; ciudad?: string | null; observaciones?: string | null;
  activo: boolean; creadoEn?: string | null;
}

export interface ProveedorRequest {
  nombre: string; nit?: string; contacto?: string; telefono?: string; correo?: string;
  direccion?: string; ciudad?: string; observaciones?: string; activo?: boolean;
}

export interface ClienteDto {
  id: number; tipo: string; nombre: string; documento?: string | null; correo?: string | null;
  telefono?: string | null; direccion?: string | null; activo: boolean; creadoEn?: string | null;
}

export interface ClienteRequest {
  tipo: string; nombre: string; documento?: string; correo?: string; telefono?: string;
  direccion?: string; activo?: boolean;
}

export interface VentaResumenDto { id: number; numero: string; fecha: string; total: number | string; estado: string; }

// ---------- Compras ----------
export interface DetalleCompraDto {
  id: number; varianteId: number; sku: string; productoNombre: string; talla: string; color: string;
  cantidad: number; costoUnitario: number | string; descuento: number | string; impuestos: number | string; subtotal: number | string;
}

export interface CompraDto {
  id: number; numero: string; proveedorId: number; proveedorNombre: string; estado: string;
  fechaRegistro: string; fechaRecepcion?: string | null;
  subtotal: number | string; descuento: number | string; impuestos: number | string; total: number | string;
  observaciones?: string | null; usuarioId: number; detalles: DetalleCompraDto[];
}

export interface DetalleCompraRequest {
  varianteId: number; cantidad: number; costoUnitario: number; descuento?: number; impuestos?: number;
}

export interface CompraRequest {
  proveedorId: number; descuento?: number; impuestos?: number; observaciones?: string;
  detalles: DetalleCompraRequest[];
}

// ---------- Ventas ----------
export interface DetalleVentaDto {
  id: number; varianteId: number; sku: string; productoNombre: string; talla: string; color: string;
  cantidad: number; precioUnitario: number | string; descuento: number | string; impuestos: number | string;
  subtotal: number | string; devuelto: number;
}

export interface PagoDto {
  id: number; metodo: string; monto: number | string; referencia?: string | null; estado: string; fecha: string;
}

export interface VentaDto {
  id: number; numero: string; clienteId?: number | null; clienteNombre?: string | null; usuarioId: number;
  fecha: string; subtotal: number | string; descuento: number | string; impuestos: number | string;
  total: number | string; estado: string; estadoPago: string; observaciones?: string | null;
  detalles: DetalleVentaDto[]; pagos: PagoDto[];
}

export interface ItemCarritoRequest { varianteId: number; cantidad: number; descuentoUnidad?: number; }
export interface PagoRequest { metodo: string; monto: number; referencia?: string; }

export interface VentaRequest {
  clienteId?: number; items: ItemCarritoRequest[]; descuentoGlobal?: number; observaciones?: string;
  pagos?: PagoRequest[]; claveIdempotencia?: string; pendienteDePago?: boolean;
}

export interface SimulacionVenta {
  subtotal: number | string; descuento: number | string; impuestos: number | string; total: number | string;
  [clave: string]: unknown;
}

// ---------- Devoluciones ----------
export interface DetalleDevolucionDto {
  id: number; detalleVentaId: number; sku: string; productoNombre: string; talla: string; color: string;
  cantidad: number; regresaInventario: boolean; estadoFisico: string;
  montoReembolso: number | string; observacion?: string | null;
}

export interface DevolucionDto {
  id: number; numero: string; ventaId: number; ventaNumero: string; fecha: string; motivo: string;
  tipo: string; estado: string; usuarioId: number; totalReembolsado: number | string;
  detalles: DetalleDevolucionDto[];
}

export interface ItemDevolucionRequest {
  detalleVentaId: number; cantidad: number; regresaInventario: boolean;
  estadoFisico: 'BUENO' | 'DANADO'; montoReembolso?: number; observacion?: string;
}

export interface DevolucionRequest {
  ventaId: number; motivo: string; tipo: 'REEMBOLSO' | 'CAMBIO' | 'SALDO_FAVOR';
  items: ItemDevolucionRequest[]; claveIdempotencia?: string;
}

// ---------- Caja ----------
export interface MovimientoCajaDto {
  id: number; tipo: string; monto: number | string; motivo: string; fecha: string; usuarioId: number;
}

export interface PagoResumenMetodoDto { metodo: string; total: number | string; }

export interface CajaDto {
  id: number; numero: string; abiertoPor: number; cerradoPor?: number | null;
  apertura: string; cierre?: string | null;
  fondoInicial: number | string; efectivoEsperado: number | string;
  efectivoContado?: number | string | null; diferencia?: number | string | null;
  estado: string; observaciones?: string | null;
  movimientos: MovimientoCajaDto[]; ventasPorMetodo: PagoResumenMetodoDto[];
}

export interface AperturaCajaRequest { fondoInicial: number; observaciones?: string; }
export interface CierreCajaRequest { efectivoContado: number; observaciones?: string; }
export interface MovimientoManualRequest { tipo: 'INGRESO' | 'EGRESO'; monto: number; motivo: string; }

// ---------- Reportes / configuración / auditoría ----------
export interface PuntoSerieDto { fecha: string; total: number | string; transacciones: number; }
export interface ProductoMasVendidoDto { sku: string; producto: string; talla: string; color: string; unidades: number; ingresos: number | string; }

export interface DashboardDto {
  ventasHoy: number | string; ventasMes: number | string; transaccionesHoy: number;
  unidadesVendidasMes: number; ticketPromedioMes: number | string; gananciaBrutaMes: number | string;
  valorInventarioCosto: number | string; comprasMes: number; variantesBajoMinimo: number;
  variantesAgotadas: number; serieUltimos30Dias: PuntoSerieDto[]; topProductos: ProductoMasVendidoDto[];
  ingresosPorMetodoMes: PagoResumenMetodoDto[];
}

export interface ConfiguracionDto {
  id: number; nombreComercial: string; logoUrl?: string | null; direccion?: string | null;
  telefono?: string | null; correo?: string | null; nit?: string | null; moneda: string;
  zonaHoraria: string; ivaPorcentaje: number | string; descuentoMaxVendedor: number | string;
  descuentoMaxAdmin: number | string; stockMinimoDefault: number; metodosPago: string;
  permitirClienteOcasional: boolean; textoComprobante?: string | null;
}

export interface ConfiguracionRequest {
  nombreComercial?: string; logoUrl?: string; direccion?: string; telefono?: string; correo?: string;
  nit?: string; ivaPorcentaje?: number; descuentoMaxVendedor?: number; descuentoMaxAdmin?: number;
  stockMinimoDefault?: number; metodosPago?: string; permitirClienteOcasional?: boolean; textoComprobante?: string;
}

export interface AuditoriaDto {
  id: number; usuarioId?: number | null; accion: string; entidad: string;
  entidadId?: string | null; detalle?: string | null; ip?: string | null; fecha: string;
}

// Utilidades de conversión monetaria (el backend puede enviar string o número)
export const num = (v: number | string | null | undefined): number =>
  v == null ? 0 : typeof v === 'number' ? v : Number(v);
