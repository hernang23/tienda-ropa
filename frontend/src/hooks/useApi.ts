// Acceso a datos por dominio: consultas y mutaciones TanStack Query conectadas
// exclusivamente a endpoints reales del backend (/api/v1/*).
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, setToken } from '../lib/apiClient';
import type {
  AjusteRequest, AperturaCajaRequest, ActualizarUsuarioRequest, AuditoriaDto, CategoriaDto,
  ClienteDto, ClienteRequest, CierreCajaRequest, ConfiguracionDto, ConfiguracionRequest,
  CompraDto, CompraRequest, CrearUsuarioRequest, DashboardDto, DevolucionDto, DevolucionRequest,
  LoginResponse, MarcaDto, MovimientoCajaDto, MovimientoDto, MovimientoManualRequest, NombreDescActivoRequest,
  PagoRequest, ProductoActualizarRequest, ProductoCrearRequest, ProductoDto, ProveedorDto,
  ProveedorRequest, ResumenInventarioDto, SimulacionVenta, UsuarioDto, VarianteActualizarRequest,
  VarianteCrearRequest, VarianteDto, VentaDto, VentaRequest, VentaResumenDto, Page,
} from '../types/api';

const qs = (params: Record<string, unknown>): string => {
  const usp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') usp.set(k, String(v));
  });
  const s = usp.toString();
  return s ? `?${s}` : '';
};

// ---------------- Auth / usuarios ----------------
export const authKeys = { perfil: ['auth', 'perfil'] as const };

export function useLogin() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (creds: { username: string; password: string }): Promise<LoginResponse> => {
      const { data } = await api.post<LoginResponse>('/auth/login', creds);
      setToken(data.token);
      qc.setQueryData(authKeys.perfil, data.usuario);
      return data;
    },
  });
}

export async function logoutApi(): Promise<void> {
  try { await api.post('/auth/logout'); } finally { setToken(null); }
}

export function usePerfil(enabled = true) {
  return useQuery({
    queryKey: authKeys.perfil,
    queryFn: async () => (await api.get<UsuarioDto>('/auth/perfil')).data,
    enabled,
    staleTime: 5 * 60_000,
  });
}

export function useCambiarPassword() {
  return useMutation({
    mutationFn: async (body: { passwordActual: string; passwordNuevo: string }) =>
      (await api.put('/auth/password', body)).data,
  });
}

export function useUsuarios(page = 0, size = 20) {
  return useQuery({
    queryKey: ['usuarios', page, size],
    queryFn: async () => (await api.get<Page<UsuarioDto>>(`/usuarios${qs({ page, size })}`)).data,
  });
}

export function useCrearUsuario() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: CrearUsuarioRequest) => (await api.post<UsuarioDto>('/usuarios', body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['usuarios'] }),
  });
}

export function useActualizarUsuario() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: number; body: ActualizarUsuarioRequest }) =>
      (await api.put<UsuarioDto>(`/usuarios/${id}`, body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['usuarios'] }),
  });
}

export function useAlternarUsuarioActivo() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => (await api.patch<UsuarioDto>(`/usuarios/${id}/activo`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['usuarios'] }),
  });
}

// ---------------- Catálogo ----------------
export const catalogoKeys = {
  categorias: ['categorias'] as const,
  marcas: ['marcas'] as const,
  productos: ['productos'] as const,
  producto: (id: number) => ['productos', id] as const,
};

export function useCategorias(soloActivas = false) {
  return useQuery({
    queryKey: [...catalogoKeys.categorias, soloActivas],
    queryFn: async () => (await api.get<CategoriaDto[]>(`/categorias${qs({ soloActivas })}`)).data,
  });
}

export function useGuardarCategoria() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id?: number; body: NombreDescActivoRequest }) =>
      id ? (await api.put<CategoriaDto>(`/categorias/${id}`, body)).data
         : (await api.post<CategoriaDto>('/categorias', body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categorias'] }),
  });
}

export function useMarcas(soloActivas = false) {
  return useQuery({
    queryKey: [...catalogoKeys.marcas, soloActivas],
    queryFn: async () => (await api.get<MarcaDto[]>(`/marcas${qs({ soloActivas })}`)).data,
  });
}

export function useGuardarMarca() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id?: number; body: NombreDescActivoRequest }) =>
      id ? (await api.put<MarcaDto>(`/marcas/${id}`, body)).data
         : (await api.post<MarcaDto>('/marcas', body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['marcas'] }),
  });
}

export interface FiltroProductos { q?: string; categoriaId?: number; marcaId?: number; soloActivos?: boolean; page?: number; size?: number; }

export function useProductos(f: FiltroProductos) {
  return useQuery({
    queryKey: [...catalogoKeys.productos, f],
    queryFn: async () => (await api.get<Page<ProductoDto>>(`/productos${qs({ ...f })}`)).data,
  });
}

export function useProducto(id: number | null) {
  return useQuery({
    queryKey: id != null ? catalogoKeys.producto(id) : ['productos', 'none'],
    queryFn: async () => (await api.get<ProductoDto>(`/productos/${id}`)).data,
    enabled: id != null,
  });
}

export function useCrearProducto() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: ProductoCrearRequest) => (await api.post<ProductoDto>('/productos', body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['productos'] }),
  });
}

export function useActualizarProducto() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id: number; body: ProductoActualizarRequest }) =>
      (await api.put<ProductoDto>(`/productos/${id}`, body)).data,
    onSuccess: (_d, v) => {
      qc.invalidateQueries({ queryKey: ['productos'] });
      qc.invalidateQueries({ queryKey: catalogoKeys.producto(v.id) });
    },
  });
}

/** Desactivación lógica (el backend impide borrar con historial). */
export function useDesactivarProducto() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => (await api.delete(`/productos/${id}`)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['productos'] }),
  });
}

export function useVariantes(productoId: number | null) {
  return useQuery({
    queryKey: ['variantes', productoId],
    queryFn: async () => (await api.get<VarianteDto[]>(`/productos/${productoId}/variantes`)).data,
    enabled: productoId != null,
  });
}

export function useCrearVariante() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ productoId, body }: { productoId: number; body: VarianteCrearRequest }) =>
      (await api.post<VarianteDto>(`/productos/${productoId}/variantes`, body)).data,
    onSuccess: (_d, v) => {
      qc.invalidateQueries({ queryKey: ['variantes', v.productoId] });
      qc.invalidateQueries({ queryKey: ['productos'] });
      qc.invalidateQueries({ queryKey: catalogoKeys.producto(v.productoId) });
    },
  });
}

export function useActualizarVariante() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ varianteId, body }: { varianteId: number; body: VarianteActualizarRequest }) =>
      (await api.put<VarianteDto>(`/variantes/${varianteId}`, body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['variantes'] });
      qc.invalidateQueries({ queryKey: ['productos'] });
      qc.invalidateQueries({ queryKey: ['inventario'] });
    },
  });
}

export function useHistorialPrecios(varianteId: number | null) {
  return useQuery({
    queryKey: ['historial-precios', varianteId],
    queryFn: async () => (await api.get<Record<string, unknown[]>>(`/variantes/${varianteId}/historial-precios`)).data,
    enabled: varianteId != null,
  });
}

// ---------------- Inventario ----------------
export interface FiltroInventario { q?: string; talla?: string; color?: string; bajoMinimo?: boolean; agotado?: boolean; page?: number; size?: number; }

export function useInventario(f: FiltroInventario) {
  return useQuery({
    queryKey: ['inventario', f],
    queryFn: async () => (await api.get<Page<VarianteDto>>(`/inventario${qs({ ...f })}`)).data,
  });
}

export interface FiltroMovimientos { varianteId?: number; tipo?: string; desde?: string; hasta?: string; page?: number; size?: number; }

export function useMovimientos(f: FiltroMovimientos) {
  return useQuery({
    queryKey: ['movimientos', f],
    queryFn: async () => (await api.get<Page<MovimientoDto>>(`/inventario/movimientos${qs({ ...f })}`)).data,
  });
}

export function useAjusteInventario() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ varianteId, body }: { varianteId: number; body: AjusteRequest }) =>
      (await api.post<MovimientoDto>(`/inventario/variantes/${varianteId}/ajuste`, body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inventario'] });
      qc.invalidateQueries({ queryKey: ['movimientos'] });
      qc.invalidateQueries({ queryKey: ['productos'] });
      qc.invalidateQueries({ queryKey: ['reportes'] });
    },
  });
}

export function useResumenInventario() {
  return useQuery({
    queryKey: ['inventario', 'resumen'],
    queryFn: async () => (await api.get<ResumenInventarioDto>('/inventario/resumen')).data,
  });
}

// ---------------- Proveedores / clientes ----------------
export function useProveedores(q?: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['proveedores', q, page, size],
    queryFn: async () => (await api.get<Page<ProveedorDto>>(`/proveedores${qs({ q, page, size })}`)).data,
  });
}

export function useGuardarProveedor() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id?: number; body: ProveedorRequest }) =>
      id ? (await api.put<ProveedorDto>(`/proveedores/${id}`, body)).data
         : (await api.post<ProveedorDto>('/proveedores', body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['proveedores'] }),
  });
}

export function useComprasDeProveedor(id: number | null) {
  return useQuery({
    queryKey: ['proveedores', id, 'compras'],
    queryFn: async () => (await api.get<Page<CompraDto>>(`/proveedores/${id}/compras`)).data,
    enabled: id != null,
  });
}

export function useClientes(q?: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['clientes', q, page, size],
    queryFn: async () => (await api.get<Page<ClienteDto>>(`/clientes${qs({ q, page, size })}`)).data,
  });
}

export function useGuardarCliente() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, body }: { id?: number; body: ClienteRequest }) =>
      id ? (await api.put<ClienteDto>(`/clientes/${id}`, body)).data
         : (await api.post<ClienteDto>('/clientes', body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['clientes'] }),
  });
}

export function useHistorialCliente(id: number | null) {
  return useQuery({
    queryKey: ['clientes', id, 'historial'],
    queryFn: async () => (await api.get<Page<VentaResumenDto>>(`/clientes/${id}/historial`)).data,
    enabled: id != null,
  });
}

// ---------------- Compras ----------------
export function useCompras(q?: string, estado?: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['compras', q, estado, page, size],
    queryFn: async () => (await api.get<Page<CompraDto>>(`/compras${qs({ q, estado, page, size })}`)).data,
  });
}

export function useCompra(id: number | null) {
  return useQuery({
    queryKey: ['compras', id],
    queryFn: async () => (await api.get<CompraDto>(`/compras/${id}`)).data,
    enabled: id != null,
  });
}

export function useCrearCompra() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: CompraRequest) => (await api.post<CompraDto>('/compras', body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['compras'] }),
  });
}

export function useCambiarEstadoCompra() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, estado }: { id: number; estado: string }) =>
      (await api.patch<CompraDto>(`/compras/${id}/estado${qs({ estado })}`)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['compras'] });
      qc.invalidateQueries({ queryKey: ['inventario'] });
    },
  });
}

/** Recepción de mercancía: incrementa existencias una sola vez. */
export function useRecibirCompra() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => (await api.post<CompraDto>(`/compras/${id}/recepcion`)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['compras'] });
      qc.invalidateQueries({ queryKey: ['inventario'] });
      qc.invalidateQueries({ queryKey: ['movimientos'] });
      qc.invalidateQueries({ queryKey: ['productos'] });
      qc.invalidateQueries({ queryKey: ['reportes'] });
    },
  });
}

// ---------------- Ventas / POS ----------------
export function useVentas(f: { q?: string; desde?: string; hasta?: string; estado?: string; page?: number; size?: number }) {
  return useQuery({
    queryKey: ['ventas', f],
    queryFn: async () => (await api.get<Page<VentaDto>>(`/ventas${qs({ ...f })}`)).data,
  });
}

export function useVenta(id: number | null) {
  return useQuery({
    queryKey: ['ventas', id],
    queryFn: async () => (await api.get<VentaDto>(`/ventas/${id}`)).data,
    enabled: id != null,
  });
}

export function useVentaPorNumero(numero: string | null) {
  return useQuery({
    queryKey: ['ventas', 'numero', numero],
    queryFn: async () => (await api.get<VentaDto>(`/ventas/numero/${encodeURIComponent(numero)}`)).data,
    enabled: numero != null && numero.length > 0,
  });
}

export function useSimularVenta() {
  return useMutation({
    mutationFn: async (body: VentaRequest) => (await api.post<SimulacionVenta>('/ventas/simular', body)).data,
  });
}

/** Confirmación real de la venta con clave de idempotencia para evitar duplicados. */
export function useCrearVenta() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: VentaRequest) => (await api.post<VentaDto>('/ventas', body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ventas'] });
      qc.invalidateQueries({ queryKey: ['inventario'] });
      qc.invalidateQueries({ queryKey: ['movimientos'] });
      qc.invalidateQueries({ queryKey: ['productos'] });
      qc.invalidateQueries({ queryKey: ['caja'] });
      qc.invalidateQueries({ queryKey: ['reportes'] });
    },
  });
}

export function useRegistrarPagos() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ ventaId, pagos }: { ventaId: number; pagos: PagoRequest[] }) =>
      (await api.post<VentaDto>(`/ventas/${ventaId}/pagos`, pagos)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ventas'] });
      qc.invalidateQueries({ queryKey: ['caja'] });
      qc.invalidateQueries({ queryKey: ['reportes'] });
    },
  });
}

export function useAnularVenta() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ ventaId, motivo }: { ventaId: number; motivo: string }) =>
      (await api.post<VentaDto>(`/ventas/${ventaId}/anular`, { motivo })).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ventas'] });
      qc.invalidateQueries({ queryKey: ['inventario'] });
      qc.invalidateQueries({ queryKey: ['movimientos'] });
      qc.invalidateQueries({ queryKey: ['caja'] });
      qc.invalidateQueries({ queryKey: ['reportes'] });
    },
  });
}

// ---------------- Devoluciones ----------------
export function useDevoluciones(page = 0, size = 20) {
  return useQuery({
    queryKey: ['devoluciones', page, size],
    queryFn: async () => (await api.get<Page<DevolucionDto>>(`/devoluciones${qs({ page, size })}`)).data,
  });
}

export function useDevolucion(id: number | null) {
  return useQuery({
    queryKey: ['devoluciones', id],
    queryFn: async () => (await api.get<DevolucionDto>(`/devoluciones/${id}`)).data,
    enabled: id != null,
  });
}

export function useCrearDevolucion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: DevolucionRequest) => (await api.post<DevolucionDto>('/devoluciones', body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['devoluciones'] });
      qc.invalidateQueries({ queryKey: ['ventas'] });
      qc.invalidateQueries({ queryKey: ['inventario'] });
      qc.invalidateQueries({ queryKey: ['movimientos'] });
      qc.invalidateQueries({ queryKey: ['caja'] });
      qc.invalidateQueries({ queryKey: ['reportes'] });
    },
  });
}

export function useAnularDevolucion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) => (await api.post<DevolucionDto>(`/devoluciones/${id}/anular`)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['devoluciones'] });
      qc.invalidateQueries({ queryKey: ['inventario'] });
      qc.invalidateQueries({ queryKey: ['movimientos'] });
    },
  });
}

// ---------------- Caja ----------------
export function useCajas(page = 0, size = 20) {
  return useQuery({
    queryKey: ['cajas', page, size],
    queryFn: async () => (await api.get<Page<import('../types/api').CajaDto>>(`/caja${qs({ page, size })}`)).data,
  });
}

export function useCajaAbierta() {
  return useQuery({
    queryKey: ['caja', 'abierta'],
    queryFn: async () => (await api.get<import('../types/api').CajaDto | null>('/caja/abierta')).data,
  });
}

export function useCaja(id: number | null) {
  return useQuery({
    queryKey: ['caja', id],
    queryFn: async () => (await api.get<import('../types/api').CajaDto>(`/caja/${id}`)).data,
    enabled: id != null,
  });
}

export function useAbrirCaja() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: AperturaCajaRequest) => (await api.post<CajaDto>('/caja/abrir', body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['caja'] });
      qc.invalidateQueries({ queryKey: ['cajas'] });
    },
  });
}

export function useMovimientoCaja() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ cajaId, body }: { cajaId: number; body: MovimientoManualRequest }) =>
      (await api.post<MovimientoCajaDto>(`/caja/${cajaId}/movimientos`, body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['caja'] });
      qc.invalidateQueries({ queryKey: ['cajas'] });
    },
  });
}

export function useCerrarCaja() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ cajaId, body }: { cajaId: number; body: CierreCajaRequest }) =>
      (await api.post<CajaDto>(`/caja/${cajaId}/cerrar`, body)).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['caja'] });
      qc.invalidateQueries({ queryKey: ['cajas'] });
    },
  });
}

// ---------------- Reportes / configuración / auditoría ----------------
export function useDashboard() {
  return useQuery({
    queryKey: ['reportes', 'dashboard'],
    queryFn: async () => (await api.get<DashboardDto>('/reportes/dashboard')).data,
    refetchInterval: 60_000,
  });
}

export function useSerieVentas(desde: string, hasta: string) {
  return useQuery({
    queryKey: ['reportes', 'serie', desde, hasta],
    queryFn: async () => (await api.get<import('../types/api').PuntoSerieDto[]>(`/reportes/serie-ventas${qs({ desde, hasta })}`)).data,
  });
}

export function useProductosMasVendidos(desde: string, hasta: string) {
  return useQuery({
    queryKey: ['reportes', 'top', desde, hasta],
    queryFn: async () => (await api.get<import('../types/api').ProductoMasVendidoDto[]>(`/reportes/productos-mas-vendidos${qs({ desde, hasta })}`)).data,
  });
}

export function useIngresosPorMetodo(desde: string, hasta: string) {
  return useQuery({
    queryKey: ['reportes', 'metodos', desde, hasta],
    queryFn: async () => (await api.get<PagoResumen[]>(`/reportes/ingresos-por-metodo${qs({ desde, hasta })}`)).data,
  });
}

type PagoResumen = import('../types/api').PagoResumenMetodoDto;

export function useConfiguracion() {
  return useQuery({
    queryKey: ['configuracion'],
    queryFn: async () => (await api.get<ConfiguracionDto>('/configuracion')).data,
  });
}

export function useGuardarConfiguracion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (body: ConfiguracionRequest) => (await api.put<ConfiguracionDto>('/configuracion', body)).data,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['configuracion'] }),
  });
}

export function useAuditoria(page = 0, size = 30) {
  return useQuery({
    queryKey: ['auditoria', page, size],
    queryFn: async () => (await api.get<Page<AuditoriaDto>>(`/auditoria${qs({ page, size })}`)).data,
  });
}
