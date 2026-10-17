// Historial de devoluciones con filtros. La creación se hace desde el detalle de la venta.
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDevoluciones, type FiltroDevoluciones } from '../hooks/useApi';
import { etiqueta, formatCOP, formatFechaHora, formatNumero } from '../lib/format';
import {
  Cabecera, Cargando, Celda, ErrorAlCargar, EtiquetaEstado, Fila, Paginador, Tabla, Vacio,
} from '../components/ui';

export default function DevolucionesPage() {
  const [filtros, setFiltros] = useState<FiltroDevoluciones>({ page: 0, size: 15 });
  const [q, setQ] = useState('');
  const { data, isLoading, error, refetch } = useDevoluciones(filtros);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">Devoluciones</h1>
      <p className="text-sm text-gray-600">
        Para registrar una devolución, ábrala desde el detalle de la venta correspondiente. Así se valida el saldo pendiente por línea y se reingresa el producto al inventario.
      </p>

      <div className="flex flex-wrap gap-2">
        <input aria-label="Buscar devoluciones" className="w-full max-w-xs rounded-lg border border-gray-300 px-3 py-2 text-sm"
          placeholder="Número o motivo…" value={q} onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && setFiltros((f) => ({ ...f, q, page: 0 }))} />
        <select aria-label="Filtrar por tipo" className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          value={filtros.tipo ?? ''} onChange={(e) => setFiltros((f) => ({ ...f, tipo: e.target.value || undefined, page: 0 }))}>
          <option value="">Todos los tipos</option>
          <option value="TOTAL">Total</option>
          <option value="PARCIAL">Parcial</option>
        </select>
        <label className="flex items-center gap-1 text-sm text-gray-700">Desde
          <input type="date" className="rounded-lg border border-gray-300 px-2 py-1.5 text-sm" value={filtros.desde ?? ''}
            onChange={(e) => setFiltros((f) => ({ ...f, desde: e.target.value || undefined, page: 0 }))} /></label>
        <label className="flex items-center gap-1 text-sm text-gray-700">Hasta
          <input type="date" className="rounded-lg border border-gray-300 px-2 py-1.5 text-sm" value={filtros.hasta ?? ''}
            onChange={(e) => setFiltros((f) => ({ ...f, hasta: e.target.value || undefined, page: 0 }))} /></label>
      </div>

      {isLoading && <Cargando />}
      {error && <ErrorAlCargar error={error} onReintentar={() => void refetch()} />}
      {data && (
        <>
          {data.content.length === 0 ? <Vacio mensaje="No hay devoluciones registradas." /> : (
            <Tabla>
              <Cabecera>
                <Fila><Celda>Número</Celda><Celda>Fecha</Celda><Celda>Venta</Celda><Celda>Tipo</Celda><Celda>Unidades</Celda><Celda>Monto</Celda><Celda>Motivo</Celda></Fila>
              </Cabecera>
              <tbody>
                {data.content.map((d) => (
                  <Fila key={d.id}>
                    <Celda className="font-mono">{d.numero}</Celda>
                    <Celda>{formatFechaHora(d.fecha)}</Celda>
                    <Celda><Link to={`/ventas/${d.ventaId}`} className="font-mono text-marca-700 hover:underline">{d.ventaNumero}</Link></Celda>
                    <Celda><EtiquetaEstado valor={d.tipo} texto={etiqueta(d.tipo)} /></Celda>
                    <Celda>{formatNumero(d.items.reduce((a, i) => a + i.cantidad, 0))}</Celda>
                    <Celda className="font-semibold text-red-700">{formatCOP(d.montoTotal)}</Celda>
                    <Celda className="max-w-[240px] truncate">{d.motivo}</Celda>
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
