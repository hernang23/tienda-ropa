// Detalle de venta: ítems, pagos, devoluciones asociadas y anulación autorizada.
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../hooks/useAuth';
import { useAnularVenta, useDevolucionesDeVenta, useRegistrarDevolucion, useVenta } from '../hooks/useApi';
import { errorMessage } from '../lib/apiClient';
import { etiqueta, formatCOP, formatFechaHora, formatNumero } from '../lib/format';
import { num, type DevolucionItemRequest } from '../types/api';
import {
  Boton, Cabecera, Campo, Cargando, Celda, Confirmar, ErrorAlCargar, EtiquetaEstado, Fila,
  Modal, Tabla, Vacio, claseInput, notificar,
} from '../components/ui';

const esquemaDevolucion = z.object({ motivo: z.string().min(5, 'Describa el motivo (mínimo 5 caracteres).') });

export default function VentaDetallePage() {
  const { id } = useParams();
  const ventaId = Number(id);
  const { tieneRol } = useAuth();
  const { data: venta, isLoading, error, refetch } = useVenta(ventaId);
  const devoluciones = useDevolucionesDeVenta(ventaId);
  const anular = useAnularVenta();
  const registrarDevolucion = useRegistrarDevolucion();
  const [confAnular, setConfAnular] = useState(false);
  const [modalDev, setModalDev] = useState(false);

  const { register, handleSubmit, formState: { errors }, reset } = useForm<{ motivo: string }>({
    resolver: zodResolver(esquemaDevolucion),
  });

  if (isLoading) return <Cargando />;
  if (error) return <ErrorAlCargar error={error} onReintentar={() => void refetch()} />;
  if (!venta) return <Vacio />;

  const puedeDevolver = tieneRol('ADMIN', 'VENDEDOR') && (venta.estado === 'COMPLETA' || venta.estado === 'PARCIAL' || venta.estado === 'CON_DEVOLUCION');
  const puedeAnular = tieneRol('ADMIN') && venta.estado !== 'ANULADA';

  async function onSubmitDevolucion(v: { motivo: string }) {
    // Devuelve todas las unidades no devueltas de cada línea (el backend limita por saldo pendiente).
    const items: DevolucionItemRequest[] = venta.detalles
      .map((d) => ({ detalleVentaId: d.id, cantidad: Math.max(0, d.cantidad - d.cantidadDevuelta) }))
      .filter((i) => i.cantidad > 0);
    if (items.length === 0) { notificar.error('No hay cantidades pendientes de devolución en esta venta.'); return; }
    try {
      const dev = await registrarDevolucion.mutateAsync({ ventaId, body: { motivo: v.motivo, items } });
      notificar.exito(`Devolución ${dev.numero} registrada. Reintegro: ${formatCOP(dev.montoTotal)}.`);
      setModalDev(false); reset({ motivo: '' });
      void refetch();
      void devoluciones.refetch();
    } catch (e) { notificar.error(errorMessage(e)); }
  }

  return (
    <div className="space-y-5">
      <div>
        <Link to="/ventas" className="text-sm text-marca-700 hover:underline">← Ventas</Link>
        <div className="mt-1 flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-bold text-gray-900">Venta {venta.numero}</h1>
          <EtiquetaEstado valor={venta.estado} texto={etiqueta(venta.estado)} />
        </div>
        <p className="mt-1 text-sm text-gray-600">
          {formatFechaHora(venta.fecha)} · Vendedor: {venta.vendedorNombre} · Cliente: {venta.clienteNombre ?? 'Consumidor final'}
          {venta.cajaApertura ? ` · Caja abierta: ${formatFechaHora(venta.cajaApertura)}` : ''}
        </p>
      </div>

      <Tabla>
        <Cabecera>
          <Fila><Celda>Producto / SKU</Celda><Celda>Talla</Celda><Celda>Color</Celda><Celda>Cant.</Celda><Celda>Precio unit.</Celda><Celda>Descuento</Celda><Celda>Subtotal</Celda></Fila>
        </Cabecera>
        <tbody>
          {venta.detalles.map((d) => (
            <Fila key={d.id}>
              <Celda>{d.productoNombre}<span className="block font-mono text-xs text-gray-500">{d.sku}</span></Celda>
              <Celda>{d.talla}</Celda><Celda>{d.color}</Celda>
              <Celda>{formatNumero(d.cantidad)}{d.cantidadDevuelta > 0 && <span className="ml-1 text-xs text-red-600">({formatNumero(d.cantidadDevuelta)} dev.)</span>}</Celda>
              <Celda>{formatCOP(d.precioUnitario)}</Celda>
              <Celda>{d.descuentoPct > 0 ? `${d.descuentoPct}%` : '—'}</Celda>
              <Celda className="font-medium">{formatCOP(d.subtotal)}</Celda>
            </Fila>
          ))}
        </tbody>
      </Tabla>

      <div className="grid gap-5 md:grid-cols-2">
        <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
          <h2 className="font-semibold text-gray-800">Totales</h2>
          <p className="mt-2 flex justify-between text-sm"><span>Subtotal</span><span>{formatCOP(venta.subtotal)}</span></p>
          <p className="flex justify-between text-sm text-red-700"><span>Descuentos</span><span>−{formatCOP(venta.totalDescuento)}</span></p>
          <p className="flex justify-between text-base font-bold"><span>Total</span><span>{formatCOP(venta.total)}</span></p>
          <p className="flex justify-between text-sm text-emerald-700"><span>Pagado</span><span>{formatCOP(venta.totalPagado)}</span></p>
          {num(venta.total) - num(venta.totalPagado) > 0.001 && (
            <p className="flex justify-between text-sm font-medium text-amber-700"><span>Saldo</span><span>{formatCOP(num(venta.total) - num(venta.totalPagado))}</span></p>
          )}
        </div>
        <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
          <h2 className="font-semibold text-gray-800">Pagos</h2>
          {venta.pagos.length === 0 ? <Vacio mensaje="Sin pagos registrados." /> : (
            <ul className="mt-2 divide-y divide-gray-100 text-sm">
              {venta.pagos.map((p) => (
                <li key={p.id} className="flex items-center justify-between py-1.5">
                  <span>{etiqueta(p.metodo)}{p.referencia && <span className="ml-1 text-xs text-gray-500">({p.referencia})</span>}</span>
                  <span className="font-medium">{formatCOP(p.monto)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
        <h2 className="font-semibold text-gray-800">Devoluciones asociadas</h2>
        {devoluciones.isLoading && <Cargando />}
        {devoluciones.data && (devoluciones.data.length === 0
          ? <Vacio mensaje="Esta venta no tiene devoluciones." />
          : (
            <ul className="mt-2 divide-y divide-gray-100 text-sm">
              {devoluciones.data.map((dv) => (
                <li key={dv.id} className="flex flex-wrap items-center justify-between gap-2 py-2">
                  <span className="font-mono">{dv.numero} · {formatFechaHora(dv.fecha)}</span>
                  <span className="text-gray-600">{dv.motivo}</span>
                  <span className="flex items-center gap-2">
                    <EtiquetaEstado valor={dv.tipo} texto={etiqueta(dv.tipo)} />
                    <strong>{formatCOP(dv.montoTotal)}</strong>
                  </span>
                </li>
              ))}
            </ul>
          ))}
      </div>

      <div className="flex flex-wrap gap-2">
        {puedeDevolver && <Boton variante="secundario" onClick={() => setModalDev(true)}>Registrar devolución</Boton>}
        {puedeAnular && <Boton variante="peligro" onClick={() => setConfAnular(true)}>Anular venta</Boton>}
      </div>

      <Confirmar
        abierto={confAnular} titulo="Anular venta" peligroso
        descripcion="La anulación devuelve todas las unidades al inventario, revierte la entrada de caja asociada y marca la venta como ANULADA. Requiere rol ADMIN y queda auditada."
        confirmarTexto="Anular venta" cargando={anular.isPending}
        onCancelar={() => setConfAnular(false)}
        onConfirmar={async () => {
          try { await anular.mutateAsync(ventaId); notificar.exito('Venta anulada.'); void refetch(); }
          catch (e) { notificar.error(errorMessage(e)); }
          setConfAnular(false);
        }}
      />

      <Modal abierto={modalDev} titulo="Registrar devolución" ancho="max-w-md" onClose={() => setModalDev(false)}>
        <form onSubmit={handleSubmit(onSubmitDevolucion)} className="space-y-4">
          <p className="text-sm text-gray-600">
            Se devolverán las cantidades pendientes de cada línea:
          </p>
          <ul className="rounded-lg border border-gray-200 p-3 text-sm">
            {venta.detalles.map((d) => {
              const pendiente = Math.max(0, d.cantidad - d.cantidadDevuelta);
              return pendiente > 0 ? (
                <li key={d.id} className="py-0.5">{d.productoNombre} {d.talla}/{d.color} — {formatNumero(pendiente)} u</li>
              ) : null;
            })}
          </ul>
          <Campo etiqueta="Motivo *" error={errors.motivo?.message}>
            <textarea rows={3} className={claseInput(!!errors.motivo)} {...register('motivo')} autoFocus />
          </Campo>
          <p className="text-xs text-gray-500">El backend valida que la cantidad devuelta no exceda la vendida, reingresa el producto al inventario con movimiento auditable y registra la salida de caja del reintegro.</p>
          <div className="flex justify-end gap-2">
            <Boton type="button" variante="secundario" onClick={() => setModalDev(false)}>Cancelar</Boton>
            <Boton type="submit" cargando={registrarDevolucion.isPending}>Registrar devolución</Boton>
          </div>
        </form>
      </Modal>
    </div>
  );
}
