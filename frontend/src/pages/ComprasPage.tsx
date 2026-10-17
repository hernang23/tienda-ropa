// Registro de compras con líneas por variante; se envían a /compras (estado BORRADOR hasta recibir).
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useCambiarEstadoCompra, useCompras, useCrearCompra, useProductos, useProveedores, useRecibirCompra } from '../hooks/useApi';
import { errorMessage } from '../lib/apiClient';
import { etiqueta, formatCOP, formatFechaHora, formatNumero } from '../lib/format';
import { num, type DetalleCompraRequest } from '../types/api';
import {
  Boton, Cabecera, Campo, Cargando, Celda, Confirmar, ErrorAlCargar, EtiquetaEstado, Fila,
  Modal, Paginador, Tabla, Vacio, claseInput, notificar,
} from '../components/ui';

interface LineaNueva { varianteId: number; sku: string; producto: string; cantidad: number; costo: number; }

export default function ComprasPage() {
  const { tieneRol } = useAuth();
  const [q, setQ] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [page, setPage] = useState(0);
  const { data, isLoading, error, refetch } = useCompras(busqueda || undefined, undefined, page, 15);
  const cambiarEstado = useCambiarEstadoCompra();
  const recibir = useRecibirCompra();
  const [idRecibir, setIdRecibir] = useState<number | null>(null);

  // ---- Modal de nueva compra ----
  const [modal, setModal] = useState(false);
  const [proveedorId, setProveedorId] = useState<number | null>(null);
  const [observaciones, setObservaciones] = useState('');
  const [lineas, setLineas] = useState<LineaNueva[]>([]);
  const [buscarProd, setBuscarProd] = useState('');
  const productos = useProductos({ q: buscarProd, soloActivos: true, size: 8 });
  const crear = useCrearCompra();

  const proveedores = useProveedores(undefined, 0, 100);

  function agregarLinea(varianteId: number, sku: string, producto: string, costo: number) {
    setLineas((ls) => ls.some((l) => l.varianteId === varianteId) ? ls : [...ls, { varianteId, sku, producto, cantidad: 1, costo }]);
  }
  function actualizarLinea(i: number, cambios: Partial<LineaNueva>) {
    setLineas((ls) => ls.map((l, idx) => (idx === i ? { ...l, ...cambios } : l)));
  }

  const totalEstimado = lineas.reduce((acc, l) => acc + l.cantidad * l.costo, 0);

  async function guardarCompra(confirmarRecepcion: boolean) {
    if (!proveedorId) { notificar.error('Seleccione el proveedor.'); return; }
    if (lineas.length === 0) { notificar.error('Agregue al menos una línea de mercancía.'); return; }
    const detalles: DetalleCompraRequest[] = lineas.map((l) => ({ varianteId: l.varianteId, cantidad: l.cantidad, costoUnitario: l.costo }));
    try {
      const compra = await crear.mutateAsync({ proveedorId, observaciones: observaciones || undefined, detalles });
      if (confirmarRecepcion) {
        await recibir.mutateAsync(compra.id);
        notificar.exito(`Compra ${compra.numero} registrada y recibida. Inventario actualizado.`);
      } else {
        notificar.exito(`Compra ${compra.numero} registrada como borrador.`);
      }
      setModal(false);
      setLineas([]); setProveedorId(null); setObservaciones(''); setBuscarProd('');
      setPage(0);
      void refetch();
    } catch (e) { notificar.error(errorMessage(e)); }
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Compras</h1>
        {tieneRol('ADMIN', 'INVENTARIO') && <Boton onClick={() => setModal(true)}>+ Registrar compra</Boton>}
      </div>

      <div className="flex gap-2">
        <input aria-label="Buscar compras" className="w-full max-w-xs rounded-lg border border-gray-300 px-3 py-2 text-sm"
          placeholder="Número o proveedor…" value={q} onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && (setBusqueda(q), setPage(0))} />
        <Boton variante="secundario" onClick={() => { setBusqueda(q); setPage(0); }}>Buscar</Boton>
      </div>

      {isLoading && <Cargando />}
      {error && <ErrorAlCargar error={error} onReintentar={() => void refetch()} />}
      {data && (
        <>
          {data.content.length === 0 ? <Vacio mensaje="No hay compras registradas." /> : (
            <Tabla>
              <Cabecera>
                <Fila><Celda>Número</Celda><Celda>Proveedor</Celda><Celda>Fecha</Celda><Celda>Ítems</Celda><Celda>Total</Celda><Celda>Estado</Celda><Celda>Acciones</Celda></Fila>
              </Cabecera>
              <tbody>
                {data.content.map((c) => (
                  <Fila key={c.id}>
                    <Celda><Link to={`/compras/${c.id}`} className="font-mono text-marca-700 hover:underline">{c.numero}</Link></Celda>
                    <Celda>{c.proveedorNombre}</Celda>
                    <Celda>{formatFechaHora(c.fechaRegistro)}</Celda>
                    <Celda>{formatNumero(c.detalles.length)}</Celda>
                    <Celda className="font-semibold">{formatCOP(c.total)}</Celda>
                    <Celda><EtiquetaEstado valor={c.estado} texto={etiqueta(c.estado)} /></Celda>
                    <Celda>
                      <div className="flex gap-1">
                        {c.estado === 'BORRADOR' && (
                          <Boton variante="exito" onClick={() => setIdRecibir(c.id)}>Recibir</Boton>
                        )}
                        {c.estado === 'RECIBIDA' && (
                          <Boton variante="fantasma" onClick={async () => {
                            try { await cambiarEstado.mutateAsync({ id: c.id, estado: 'CANCELADA' }); notificar.info('Compra cancelada.'); }
                            catch (e) { notificar.error(errorMessage(e)); }
                          }}>Cancelar</Boton>
                        )}
                      </div>
                    </Celda>
                  </Fila>
                ))}
              </tbody>
            </Tabla>
          )}
          <Paginador pagina={data.number} totalPaginas={data.totalPages} alCambiar={setPage} />
        </>
      )}

      <Confirmar
        abierto={idRecibir != null}
        titulo="Recepción de mercancía"
        descripcion="Al confirmar la recepción se incrementarán las existencias de cada variante y se generará el movimiento de inventario correspondiente. Esta acción solo puede hacerse una vez."
        confirmarTexto="Confirmar recepción"
        cargando={recibir.isPending}
        onCancelar={() => setIdRecibir(null)}
        onConfirmar={async () => {
          if (idRecibir == null) return;
          try {
            const c = await recibir.mutateAsync(idRecibir);
            notificar.exito(`Compra ${c.numero} recibida. Mercancía disponible en inventario.`);
          } catch (e) { notificar.error(errorMessage(e)); }
          setIdRecibir(null);
        }}
      />

      {/* Modal nueva compra */}
      <Modal abierto={modal} titulo="Registrar compra a proveedor" ancho="max-w-3xl" onClose={() => setModal(false)}>
        <div className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Campo etiqueta="Proveedor *" hint="Debe existir un proveedor registrado.">
              <select className={claseInput(proveedorId == null)} value={proveedorId ?? ''} onChange={(e) => setProveedorId(e.target.value ? Number(e.target.value) : null)}>
                <option value="">Seleccione…</option>
                {(proveedores.data?.content ?? []).map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
              </select>
            </Campo>
            <Campo etiqueta="Observaciones">
              <input className={claseInput} value={observaciones} onChange={(e) => setObservaciones(e.target.value)} />
            </Campo>
          </div>

          <div>
            <Campo etiqueta="Agregar variantes (busque por nombre o SKU)">
              <input className={claseInput} placeholder="Camiseta, SKU…" value={buscarProd} onChange={(e) => setBuscarProd(e.target.value)} />
            </Campo>
            {productos.isLoading && <Cargando mensaje="Buscando productos…" />}
            {productos.data && (
              <ul className="mt-2 max-h-44 space-y-1 overflow-y-auto rounded-lg border border-gray-200 p-2">
                {productos.data.content.flatMap((p) =>
                  p.variantes.filter((v) => v.activo).map((v) => (
                    <li key={v.id} className="flex items-center justify-between gap-2 rounded px-2 py-1 text-sm hover:bg-gray-50">
                      <span className="min-w-0 truncate">
                        <strong>{p.nombre}</strong> · {v.talla}/{v.color} · <span className="font-mono text-xs">{v.sku}</span>
                        <span className="ml-2 text-xs text-gray-500">stock {formatNumero(v.existencias)}</span>
                      </span>
                      <Boton variante="secundario" onClick={() => agregarLinea(v.id, v.sku, `${p.nombre} ${v.talla}/${v.color}`, num(v.costoUnitario))}>
                        Agregar
                      </Boton>
                    </li>
                  )))}
                {productos.data.content.every((p) => p.variantes.length === 0) && (
                  <li className="px-2 py-3 text-sm text-gray-500">Los productos deben tener variantes activas.</li>
                )}
              </ul>
            )}
          </div>

          {lineas.length > 0 && (
            <Tabla>
              <Cabecera><Fila><Celda>Ítem</Celda><Celda>Cantidad</Celda><Celda>Costo unitario</Celda><Celda>Subtotal</Celda><Celda /></Fila></Cabecera>
              <tbody>
                {lineas.map((l, i) => (
                  <Fila key={l.varianteId}>
                    <Celda>{l.producto}<span className="block font-mono text-xs text-gray-500">{l.sku}</span></Celda>
                    <Celda><input type="number" min="1" className="w-20 rounded border border-gray-300 px-2 py-1 text-sm" value={l.cantidad}
                      onChange={(e) => actualizarLinea(i, { cantidad: Math.max(1, Number(e.target.value) || 1) })} /></Celda>
                    <Celda><input type="number" min="0" step="50" className="w-28 rounded border border-gray-300 px-2 py-1 text-sm" value={l.costo}
                      onChange={(e) => actualizarLinea(i, { costo: Math.max(0, Number(e.target.value) || 0) })} /></Celda>
                    <Celda>{formatCOP(l.cantidad * l.costo)}</Celda>
                    <Celda><Boton variante="fantasma" onClick={() => setLineas((ls) => ls.filter((_, idx) => idx !== i))}>Quitar</Boton></Celda>
                  </Fila>
                ))}
              </tbody>
            </Tabla>
          )}

          <p className="text-right text-sm font-semibold text-gray-800">Total estimado: {formatCOP(totalEstimado)}</p>

          <div className="flex flex-wrap justify-end gap-2">
            <Boton variante="secundario" onClick={() => setModal(false)}>Cancelar</Boton>
            <Boton variante="exito" cargando={crear.isPending || recibir.isPending} onClick={() => void guardarCompra(true)}>
              Guardar y recibir
            </Boton>
            <Boton cargando={crear.isPending} onClick={() => void guardarCompra(false)}>Guardar como borrador</Boton>
          </div>
        </div>
      </Modal>
    </div>
  );
}
