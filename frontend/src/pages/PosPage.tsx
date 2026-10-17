// Punto de venta: carrito, simulación autorizada por el backend y confirmación con idempotencia.
import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../hooks/useAuth';
import { useClientes, useCajaAbierta, useCrearVenta, useProductos, useSimularVenta } from '../hooks/useApi';
import { errorMessage } from '../lib/apiClient';
import { formatCOP, formatNumero } from '../lib/format';
import { num, type PagoRequest, type SimulacionVenta } from '../types/api';
import { Boton, Campo, Cargando, Modal, Vacio, claseInput, notificar } from '../components/ui';

interface LineaCarrito { varianteId: number; sku: string; nombre: string; talla: string; color: string; precioUnitario: number; cantidad: number; disponible: number; }

const esquemaPago = z.object({
  efectivo: z.coerce.number().min(0),
  electronico: z.coerce.number().min(0),
});

function uuid(): string {
  return crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export default function PosPage() {
  const { usuario, tieneRol } = useAuth();
  const puedeDescuentoAlto = tieneRol('ADMIN');
  const caja = useCajaAbierta();
  const simular = useSimularVenta();
  const crearVenta = useCrearVenta();

  // Búsqueda (nombre/SKU/código de barras) vía endpoint real de productos.
  const [busqueda, setBusqueda] = useState('');
  const busquedaDebounce = useDebounce(busqueda, 350);
  const productos = useProductos({ q: busquedaDebounce, soloActivos: true, size: 12 });

  const [carrito, setCarrito] = useState<LineaCarrito[]>([]);
  const [clienteId, setClienteId] = useState<number | null>(null);
  const [descuentoGlobalPct, setDescuentoGlobalPct] = useState(0);
  const clientes = useClientes(undefined, 0, 50);

  const [simulacion, setSimulacion] = useState<SimulacionVenta | null>(null);
  const [modalPago, setModalPago] = useState(false);
  const claveIdempotencia = useRef<string>(uuid());

  const { register, handleSubmit, watch, reset } = useForm<{ efectivo: number; electronico: number }>({
    resolver: zodResolver(esquemaPago),
    defaultValues: { efectivo: 0, electronico: 0 },
  });

  // Sincronizar descuento máximo permitido en UI (el backend lo revalida siempre).
  const maxDescuentoPermitido = simulacion?.maxDescuentoPct ?? (puedeDescuentoAlto ? undefined : undefined);

  // Simulación en servidor cuando cambia el carrito (precios definitivos del backend).
  const payload = useMemo(() => ({
    clienteId: clienteId ?? undefined,
    detalle: carrito.map((l) => ({ varianteId: l.varianteId, cantidad: l.cantidad, descuentoPct: 0 })),
    descuentoGlobalPct,
  }), [carrito, clienteId, descuentoGlobalPct]);

  useEffect(() => {
    if (carrito.length === 0) { setSimulacion(null); return; }
    let cancelado = false;
    const t = setTimeout(() => {
      simular.mutate(payload, {
        onSuccess: (s) => { if (!cancelado) setSimulacion(s); },
        onError: (e) => { if (!cancelado) { setSimulacion(null); notificar.error(errorMessage(e)); } },
      });
    }, 250);
    return () => { cancelado = true; clearTimeout(t); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [payload]);

  function agregar(varianteId: number, sku: string, nombre: string, talla: string, color: string, precio: number, disponible: number) {
    setCarrito((c) => {
      const existente = c.find((l) => l.varianteId === varianteId);
      if (existente) {
        if (existente.cantidad + 1 > disponible) { notificar.error(`No hay más existencias disponibles de ${sku}.`); return c; }
        return c.map((l) => (l.varianteId === varianteId ? { ...l, cantidad: l.cantidad + 1 } : l));
      }
      if (disponible <= 0) { notificar.error('Variante sin existencias.'); return c; }
      return [...c, { varianteId, sku, nombre, talla, color, precioUnitario: precio, cantidad: 1, disponible }];
    });
  }

  function cambiarCantidad(i: number, cantidad: number) {
    setCarrito((c) => c.flatMap((l, idx) => {
      if (idx !== i) return [l];
      if (cantidad <= 0) return [];
      if (cantidad > l.disponible) { notificar.error(`Máximo ${l.disponible} unidades disponibles.`); return [l]; }
      return [{ ...l, cantidad }];
    }));
  }

  async function confirmarVenta() {
    if (!caja.data) { notificar.error('Debe abrir la caja antes de vender.'); return; }
    if (carrito.length === 0) return;
    const v = { efectivo: Number(watch('efectivo')) || 0, electronico: Number(watch('electronico')) || 0 };
    const pagos: PagoRequest[] = [];
    if (v.efectivo > 0) pagos.push({ metodo: 'EFECTIVO', monto: v.efectivo, referencia: undefined });
    if (v.electronico > 0) pagos.push({ metodo: 'TARJETA', monto: v.electronico, referencia: undefined });
    if (pagos.length === 0) { notificar.error('Indique al menos un método de pago.'); return; }

    try {
      const venta = await crearVenta.mutateAsync({
        clienteId: clienteId ?? undefined,
        cajaId: caja.data.id,
        detalle: carrito.map((l) => ({ varianteId: l.varianteId, cantidad: l.cantidad, descuentoPct: 0 })),
        descuentoGlobalPct,
        pagos,
        claveIdempotencia: claveIdempotencia.current,
      });
      notificar.exito(`Venta ${venta.numero} confirmada. Total ${formatCOP(venta.total)}.`);
      setModalPago(false);
      setCarrito([]); setClienteId(null); setDescuentoGlobalPct(0); setSimulacion(null);
      reset({ efectivo: 0, electronico: 0 });
      claveIdempotencia.current = uuid(); // nueva clave para la siguiente venta
    } catch (e) {
      notificar.error(errorMessage(e));
      // Si la venta ya se registró (respuesta duplicada), la clave evita una segunda venta;
      // solo se regenera tras éxito o tras error de validación conocido.
    }
  }

  const totalSimulado = simulacion ? num(simulacion.total) : null;
  const sumasPagadas = (Number(watch('efectivo')) || 0) + (Number(watch('electronico')) || 0);
  const cambio = totalSimulado != null ? Math.max(0, (Number(watch('efectivo')) || 0) - Math.max(0, totalSimulado - (Number(watch('electronico')) || 0))) : 0;

  return (
    <div className="grid gap-5 lg:grid-cols-[1fr_420px]">
      {/* Columna izquierda: búsqueda */}
      <div className="space-y-3">
        <h1 className="text-2xl font-bold text-gray-900">Punto de venta</h1>

        {!caja.isLoading && !caja.data && (
          <div role="alert" className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
            La caja está cerrada. <a href="/caja" className="font-semibold underline">Abra la caja</a> antes de registrar ventas.
          </div>
        )}

        <input
          aria-label="Buscar producto, SKU o código de barras"
          className={claseInput()}
          placeholder="Escanee o escriba nombre / SKU / código de barras…"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          autoFocus />

        {productos.isLoading && <Cargando mensaje="Buscando productos…" />}
        {productos.error && <p role="alert" className="text-sm text-red-700">{errorMessage(productos.error)}</p>}
        {productos.data && (
          productos.data.content.length === 0
            ? <Vacio mensaje={busqueda ? 'Sin coincidencias. Verifique el nombre o SKU.' : 'No hay productos activos con variantes disponibles.'} />
            : (
              <ul className="grid gap-2 sm:grid-cols-2">
                {productos.data.content.map((p) => (
                  <li key={p.id} className="rounded-xl border border-gray-200 bg-white p-3 shadow-sm">
                    <p className="font-medium text-gray-900">{p.nombre}</p>
                    <div className="mt-2 flex flex-wrap gap-1.5">
                      {p.variantes.filter((v) => v.activo).map((v) => (
                        <button
                          key={v.id}
                          onClick={() => agregar(v.id, v.sku, p.nombre, v.talla, v.color, num(v.precioVenta), v.existencias)}
                          disabled={v.agotado}
                          title={`Stock: ${v.existencias}`}
                          className={`rounded-lg border px-2.5 py-1.5 text-left text-xs transition
                            ${v.agotado ? 'cursor-not-allowed border-gray-200 bg-gray-100 text-gray-400'
                              : 'border-marca-200 bg-marca-50 text-marca-900 hover:border-marca-400 hover:bg-marca-100'}`}>
                          <span className="block font-semibold">{v.talla} · {v.color}</span>
                          <span>{formatCOP(v.precioVenta)} — {v.agotado ? 'agotado' : `${v.existencias} disp.`}</span>
                        </button>
                      ))}
                      {p.variantes.length === 0 && <span className="text-xs text-gray-500">Sin variantes</span>}
                    </div>
                  </li>
                ))}
              </ul>
            )
        )}
      </div>

      {/* Columna derecha: carrito */}
      <aside className="h-fit rounded-xl border border-gray-200 bg-white p-4 shadow-sm lg:sticky lg:top-16">
        <h2 className="text-lg font-semibold text-gray-800">Carrito ({formatNumero(carrito.reduce((a, l) => a + l.cantidad, 0))} ítems)</h2>

        <div className="mt-3 space-y-3">
          <Campo etiqueta="Cliente (opcional)">
            <select className={claseInput()} value={clienteId ?? ''} onChange={(e) => setClienteId(e.target.value ? Number(e.target.value) : null)}>
              <option value="">Venta sin cliente</option>
              {(clientes.data?.content ?? []).map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </select>
          </Campo>

          <Campo etiqueta={`Descuento global (%)${maxDescuentoPermitido != null ? ` — máx. autorizado: ${maxDescuentoPermitido}%` : ''}`}
            hint={usuario?.roles.includes('VENDEDOR') && !puedeDescuentoAlto ? 'Los descuentos altos requieren rol ADMIN; el backend lo valida.' : undefined}>
            <input type="number" min="0" max={maxDescuentoPermitido ?? 100} step="0.5" className={claseInput()}
              value={descuentoGlobalPct}
              onChange={(e) => setDescuentoGlobalPct(Math.max(0, Number(e.target.value) || 0))} />
          </Campo>

          {carrito.length === 0 ? (
            <Vacio mensaje="Agregue productos desde la búsqueda." />
          ) : (
            <ul className="divide-y divide-gray-100">
              {carrito.map((l, i) => (
                <li key={l.varianteId} className="flex items-center justify-between gap-2 py-2 text-sm">
                  <div className="min-w-0">
                    <p className="truncate font-medium">{l.nombre}</p>
                    <p className="text-xs text-gray-500">{l.talla} · {l.color} · {formatCOP(l.precioUnitario)} c/u</p>
                  </div>
                  <div className="flex items-center gap-1">
                    <button aria-label="Restar uno" className="rounded border border-gray-300 px-2 py-0.5" onClick={() => cambiarCantidad(i, l.cantidad - 1)}>−</button>
                    <span className="w-8 text-center font-semibold">{l.cantidad}</span>
                    <button aria-label="Sumar uno" className="rounded border border-gray-300 px-2 py-0.5" onClick={() => cambiarCantidad(i, l.cantidad + 1)}>+</button>
                  </div>
                </li>
              ))}
            </ul>
          )}

          <div className="space-y-1 border-t border-gray-200 pt-3 text-sm">
            {simulacion ? (
              <>
                <p className="flex justify-between"><span>Subtotal</span><span>{formatCOP(simulacion.subtotal)}</span></p>
                <p className="flex justify-between text-red-700"><span>Descuentos</span><span>−{formatCOP(simulacion.totalDescuento)}</span></p>
                <p className="flex justify-between text-base font-bold text-gray-900"><span>Total</span><span>{formatCOP(simulacion.total)}</span></p>
                {simulacion.advertencias?.map((a, i) => <p key={i} className="text-xs text-amber-700">⚠ {a}</p>)}
              </>
            ) : (
              <p className="flex justify-between text-gray-500"><span>Total</span><span>{carrito.length ? 'Calculando…' : '$0'}</span></p>
            )}
          </div>

          <Boton className="w-full" disabled={!caja.data || carrito.length === 0 || simular.isPending || (simulacion?.aprobada === false)}
            onClick={() => { setModalPago(true); }}>
            Cobrar
          </Boton>
        </div>
      </aside>

      {/* Modal de pago */}
      <Modal abierto={modalPago} titulo="Confirmar pago" ancho="max-w-md" onClose={() => setModalPago(false)}>
        <form onSubmit={handleSubmit(() => void confirmarVenta())} className="space-y-4">
          <p className="text-lg font-bold text-gray-900">Total a pagar: {totalSimulado != null ? formatCOP(totalSimulado) : '—'}</p>
          <div className="grid grid-cols-2 gap-3">
            <Campo etiqueta="Efectivo (COP)" error={register ? undefined : undefined}>
              <input type="number" min="0" step="100" className={claseInput()} {...register('efectivo')} autoFocus />
            </Campo>
            <Campo etiqueta="Tarjeta / electrónico (COP)">
              <input type="number" min="0" step="100" className={claseInput()} {...register('electronico')} />
            </Campo>
          </div>
          {sumasPagadas < (totalSimulado ?? Infinity) && (
            <p className="text-sm text-amber-800">Faltan {formatCOP((totalSimulado ?? 0) - sumasPagadas)} para cubrir el total.</p>
          )}
          {sumasPagadas > (totalSimulado ?? 0) && (
            <p className="text-sm font-medium text-emerald-800">Cambio sugerido en efectivo: {formatCOP(cambio)}</p>
          )}
          <p className="text-xs text-gray-500">
            El backend recalcula precios, descuentos y totales, verifica existencias y autorización de descuentos, registra los pagos,
            el movimiento de inventario y la entrada en caja. La clave de idempotencia evita ventas duplicadas por reintentos.
          </p>
          <div className="flex justify-end gap-2">
            <Boton type="button" variante="secundario" onClick={() => setModalPago(false)}>Volver</Boton>
            <Boton type="submit" variante="exito" cargando={crearVenta.isPending}>Confirmar venta</Boton>
          </div>
        </form>
      </Modal>
    </div>
  );
}

// Debounce simple para no saturar el endpoint de búsqueda.
function useDebounce<T>(valor: T, ms: number): T {
  const [debounced, setDebounced] = useState(valor);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(valor), ms);
    return () => clearTimeout(t);
  }, [valor, ms]);
  return debounced;
}
