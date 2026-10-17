// Historial de ventas con filtros y enlace al detalle.
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useVentas, type FiltroVentas } from '../hooks/useApi';
import { etiqueta, formatCOP, formatFechaHora } from '../lib/format';
import {
  Boton, Cabecera, Cargando, Celda, ErrorAlCargar, EtiquetaEstado, Fila, Paginador, Tabla, Vacio,
} from '../components/ui';

export default function VentasPage() {
  const [filtros, setFiltros] = useState<FiltroVentas>({ page: 0, size: 15 });
  const [q, setQ] = useState('');
  const { data, isLoading, error, refetch } = useVentas(filtros);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Ventas</h1>
        <Link to="/pos"><Boton>+ Nueva venta (POS)</Boton></Link>
      </div>

      <div className="flex flex-wrap gap-2">
        <input aria-label="Buscar por número o cliente" className="w-full max-w-xs rounded-lg border border-gray-300 px-3 py-2 text-sm"
          placeholder="Número de venta o cliente…" value={q} onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && setFiltros((f) => ({ ...f, q, page: 0 }))} />
        <select aria-label="Filtrar por estado" className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          value={filtros.estado ?? ''}
          onChange={(e) => setFiltros((f) => ({ ...f, estado: e.target.value || undefined, page: 0 }))}>
          <option value="">Todos los estados</option>
          <option value="COMPLETA">Completa</option>
          <option value="PARCIAL">Parcial</option>
          <option value="PENDIENTE_PAGO">Pendiente de pago</option>
          <option value="ANULADA">Anulada</option>
          <option value="CON_DEVOLUCION">Con devolución</option>
        </select>
        <label className="flex items-center gap-1 text-sm text-gray-700">Desde
          <input type="date" className="rounded-lg border border-gray-300 px-2 py-1.5 text-sm" value={filtros.desde ?? ''}
            onChange={(e) => setFiltros((f) => ({ ...f, desde: e.target.value || undefined, page: 0 }))} /></label>
        <label className="flex items-center gap-1 text-sm text-gray-700">Hasta
          <input type="date" className="rounded-lg border border-gray-300 px-2 py-1.5 text-sm" value={filtros.hasta ?? ''}
            onChange={(e) => setFiltros((f) => ({ ...f, hasta: e.target.value || undefined, page: 0 }))} /></label>
        <Boton variante="secundario" onClick={() => setFiltros((f) => ({ ...f, q, page: 0 }))}>Buscar</Boton>
      </div>

      {isLoading && <Cargando />}
      {error && <ErrorAlCargar error={error} onReintentar={() => void refetch()} />}
      {data && (
        <>
          {data.content.length === 0 ? <Vacio mensaje="No hay ventas que coincidan con los filtros." /> : (
            <Tabla>
              <Cabecera>
                <Fila><Celda>Número</Celda><Celda>Fecha</Celda><Celda>Cliente</Celda><Celda>Vendedor</Celda><Celda>Total</Celda><Celda>Pagado</Celda><Celda>Estado</Celda></Fila>
              </Cabecera>
              <tbody>
                {data.content.map((v) => (
                  <Fila key={v.id}>
                    <Celda><Link to={`/ventas/${v.id}`} className="font-mono text-marca-700 hover:underline">{v.numero}</Link></Celda>
                    <Celda>{formatFechaHora(v.fecha)}</Celda>
                    <Celda>{v.clienteNombre ?? 'Consumidor final'}</Celda>
                    <Celda>{v.vendedorNombre}</Celda>
                    <Celda className="font-semibold">{formatCOP(v.total)}</Celda>
                    <Celda>{formatCOP(v.totalPagado)}</Celda>
                    <Celda><EtiquetaEstado valor={v.estado} texto={etiqueta(v.estado)} /></Celda>
                  </Fila>
                ))}
              </tbody>
            </Tabla>
          )}
          <Paginador pagina={data.number} totalPaginas={data.totalPages} alCambiar={(p) => setFiltros((f) => ({ ...f, page: p }))} />
        </>
      )}
    </div>
  );
}
