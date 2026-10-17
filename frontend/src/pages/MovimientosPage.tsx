// Historial de movimientos de inventario con filtros por variante, tipo y fechas.
import { useState } from 'react';
import { useMovimientos, type FiltroMovimientos } from '../hooks/useApi';
import { etiqueta, formatFechaHora, formatNumero } from '../lib/format';
import {
  Boton, Cabecera, Cargando, Celda, ErrorAlCargar, EtiquetaEstado, Fila, Paginador, Tabla, Vacio,
} from '../components/ui';

export default function MovimientosPage() {
  const [filtros, setFiltros] = useState<FiltroMovimientos>({ page: 0, size: 20 });
  const [varianteId, setVarianteId] = useState('');
  const { data, isLoading, error, refetch } = useMovimientos(filtros);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">Movimientos de inventario</h1>

      <div className="flex flex-wrap gap-2">
        <input
          aria-label="Filtrar por ID de variante"
          className="w-40 rounded-lg border border-gray-300 px-3 py-2 text-sm"
          placeholder="ID variante"
          value={varianteId}
          onChange={(e) => setVarianteId(e.target.value)} />
        <select
          aria-label="Filtrar por tipo"
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          value={filtros.tipo ?? ''}
          onChange={(e) => setFiltros((f) => ({ ...f, tipo: e.target.value || undefined, page: 0 }))}>
          <option value="">Todos los tipos</option>
          <option value="ENTRADA">Entrada</option>
          <option value="SALIDA">Salida</option>
          <option value="AJUSTE">Ajuste</option>
          <option value="DEVOLUCION">Devolución</option>
        </select>
        <label className="flex items-center gap-1 text-sm text-gray-700">
          Desde
          <input type="date" className="rounded-lg border border-gray-300 px-2 py-1.5 text-sm"
            value={filtros.desde ?? ''}
            onChange={(e) => setFiltros((f) => ({ ...f, desde: e.target.value || undefined, page: 0 }))} />
        </label>
        <label className="flex items-center gap-1 text-sm text-gray-700">
          Hasta
          <input type="date" className="rounded-lg border border-gray-300 px-2 py-1.5 text-sm"
            value={filtros.hasta ?? ''}
            onChange={(e) => setFiltros((f) => ({ ...f, hasta: e.target.value || undefined, page: 0 }))} />
        </label>
        <Boton variante="secundario" onClick={() => setFiltros((f) => ({ ...f, varianteId: varianteId ? Number(varianteId) : undefined, page: 0 }))}>
          Aplicar
        </Boton>
      </div>

      {isLoading && <Cargando />}
      {error && <ErrorAlCargar error={error} onReintentar={() => void refetch()} />}
      {data && (
        <>
          {data.content.length === 0 ? (
            <Vacio mensaje="No hay movimientos registrados con estos filtros. Las compras recibidas, ventas, devoluciones y ajustes generan movimientos automáticamente." />
          ) : (
            <Tabla>
              <Cabecera>
                <Fila>
                  <Celda>Fecha</Celda><Celda>Tipo</Celda><Celda>Producto / SKU</Celda>
                  <Celda>Talla · Color</Celda><Celda>Cantidad</Celda><Celda>Antes → Después</Celda>
                  <Celda>Motivo</Celda><Celda>Vínculo</Celda>
                </Fila>
              </Cabecera>
              <tbody>
                {data.content.map((m) => (
                  <Fila key={m.id}>
                    <Celda>{formatFechaHora(m.fecha)}</Celda>
                    <Celda><EtiquetaEstado valor={m.tipo} texto={etiqueta(m.tipo)} /></Celda>
                    <Celda className="max-w-[220px] truncate">{m.productoNombre}<span className="block font-mono text-xs text-gray-500">{m.sku}</span></Celda>
                    <Celda>{m.talla} · {m.color}</Celda>
                    <Celda className={m.cantidad >= 0 ? 'font-semibold text-emerald-700' : 'font-semibold text-red-700'}>
                      {m.cantidad >= 0 ? `+${formatNumero(m.cantidad)}` : formatNumero(m.cantidad)}
                    </Celda>
                    <Celda>{formatNumero(m.existenciasAntes)} → {formatNumero(m.existenciasDespues)}</Celda>
                    <Celda className="max-w-[200px] truncate" >{m.motivo}</Celda>
                    <Celda className="text-xs text-gray-600">
                      {m.ventaId ? `Venta #${m.ventaId}` : m.compraId ? `Compra #${m.compraId}` : m.devolucionId ? `Devolución #${m.devolucionId}` : 'Manual'}
                    </Celda>
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
