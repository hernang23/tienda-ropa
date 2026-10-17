// Caja: estado actual, apertura/cierre con arqueo y movimientos del turno.
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../hooks/useAuth';
import { useAbrirCaja, useCajaAbierta, useCerrarCaja, useMovimientosCaja } from '../hooks/useApi';
import { errorMessage } from '../lib/apiClient';
import { etiqueta, formatCOP, formatFechaHora, formatNumero } from '../lib/format';
import { num } from '../types/api';
import {
  Boton, Cabecera, Campo, Cargando, Celda, Confirmar, ErrorAlCargar, EtiquetaEstado, Fila,
  Modal, Tabla, Vacio, claseInput, notificar,
} from '../components/ui';

const esquemaAbrir = z.object({
  fondoInicial: z.coerce.number().min(0, 'El fondo inicial no puede ser negativo.'),
  observaciones: z.string().max(500).optional().or(z.literal('')),
});
const esquemaCerrar = z.object({
  efectivoContado: z.coerce.number().min(0, 'Indique el efectivo contado.'),
  observaciones: z.string().max(500).optional().or(z.literal('')),
});

export default function CajaPage() {
  const { tieneRol } = useAuth();
  const puedeGestionar = tieneRol('ADMIN', 'VENDEDOR');
  const caja = useCajaAbierta();
  const movimientos = useMovimientosCaja(caja.data?.id ?? null);
  const abrir = useAbrirCaja();
  const cerrar = useCerrarCaja();
  const [modalAbrir, setModalAbrir] = useState(false);
  const [modalCerrar, setModalCerrar] = useState(false);
  const [confCerrar, setConfCerrar] = useState(false);

  const fAbrir = useForm<z.infer<typeof esquemaAbrir>>({ resolver: zodResolver(esquemaAbrir), defaultValues: { fondoInicial: 0 } });
  const fCerrar = useForm<z.infer<typeof esquemaCerrar>>({ resolver: zodResolver(esquemaCerrar) });

  const esperadoEfectivo = caja.data
    ? num(caja.data.fondoInicial) + (caja.data.totalEntradasEfectivo != null ? num(caja.data.totalEntradasEfectivo) : 0) - (caja.data.totalSalidasEfectivo != null ? num(caja.data.totalSalidasEfectivo) : 0)
    : 0;

  async function onAbrir(v: z.infer<typeof esquemaAbrir>) {
    try {
      await abrir.mutateAsync({ fondoInicial: v.fondoInicial, observaciones: v.observaciones || undefined });
      notificar.exito('Caja abierta correctamente.');
      setModalAbrir(false);
      void caja.refetch();
    } catch (e) { notificar.error(errorMessage(e)); }
  }

  async function onCerrar(v: z.infer<typeof esquemaCerrar>) {
    if (!caja.data) return;
    try {
      const c = await cerrar.mutateAsync({ cajaId: caja.data.id, body: { efectivoContado: v.efectivoContado, observaciones: v.observaciones || undefined } });
      notificar.exito(`Caja cerrada. Diferencia: ${formatCOP(c.diferencia)}.`);
      setModalCerrar(false); setConfCerrar(false);
      void caja.refetch();
    } catch (e) { notificar.error(errorMessage(e)); }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Caja</h1>
        {puedeGestionar && (
          <div className="flex gap-2">
            {!caja.isLoading && !caja.data && <Boton onClick={() => setModalAbrir(true)}>Abrir caja</Boton>}
            {caja.data && <Boton variante="peligro" onClick={() => setModalCerrar(true)}>Cerrar caja</Boton>}
          </div>
        )}
      </div>

      {caja.isLoading && <Cargando />}
      {caja.error && <ErrorAlCargar error={caja.error} onReintentar={() => void caja.refetch()} />}

      {caja.data ? (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="font-semibold text-emerald-900">Turno abierto — Apertura: {formatFechaHora(caja.data.apertura)}</p>
            <EtiquetaEstado valor="ABIERTA" texto="Abierta" />
          </div>
          <div className="mt-3 grid grid-cols-2 gap-3 text-sm sm:grid-cols-4">
            <div><p className="text-xs uppercase text-emerald-700">Fondo inicial</p><p className="font-bold">{formatCOP(caja.data.fondoInicial)}</p></div>
            <div><p className="text-xs uppercase text-emerald-700">Entradas efectivo</p><p className="font-bold">{formatCOP(caja.data.totalEntradasEfectivo ?? 0)}</p></div>
            <div><p className="text-xs uppercase text-emerald-700">Salidas efectivo</p><p className="font-bold">{formatCOP(caja.data.totalSalidasEfectivo ?? 0)}</p></div>
            <div><p className="text-xs uppercase text-emerald-700">Esperado en cajón</p><p className="font-bold">{formatCOP(esperadoEfectivo)}</p></div>
          </div>
          {caja.data.observaciones && <p className="mt-2 text-sm text-emerald-800">Obs.: {caja.data.observaciones}</p>}
        </div>
      ) : (
        !caja.isLoading && (
          <div role="alert" className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
            No hay caja abierta. El POS requiere una caja abierta para registrar ventas.
          </div>
        )
      )}

      <div>
        <h2 className="text-lg font-semibold text-gray-800">Movimientos del turno</h2>
        {movimientos.isLoading && <Cargando />}
        {movimientos.error && <ErrorAlCargar error={movimientos.error} />}
        {movimientos.data && (movimientos.data.length === 0
          ? <Vacio mensaje="Sin movimientos de caja en este turno." />
          : (
            <Tabla>
              <Cabecera><Fila><Celda>Fecha</Celda><Celda>Tipo</Celda><Celda>Método</Celda><Celda>Monto</Celda><Celda>Referencia</Celda><Celda>Detalle</Celda></Fila></Cabecera>
              <tbody>
                {movimientos.data.map((m) => (
                  <Fila key={m.id}>
                    <Celda>{formatFechaHora(m.fecha)}</Celda>
                    <Celda><EtiquetaEstado valor={m.tipo} texto={etiqueta(m.tipo)} /></Celda>
                    <Celda>{etiqueta(m.metodo)}</Celda>
                    <Celda className={m.tipo === 'SALIDA' ? 'font-semibold text-red-700' : 'font-semibold text-emerald-700'}>{formatCOP(m.monto)}</Celda>
                    <Celda className="text-xs">{m.referencia ?? '—'}</Celda>
                    <Celda className="max-w-[260px] truncate text-xs">{m.detalle}</Celda>
                  </Fila>
                ))}
              </tbody>
            </Tabla>
          ))}
      </div>

      {/* Modal abrir */}
      <Modal abierto={modalAbrir} titulo="Abrir caja" ancho="max-w-md" onClose={() => setModalAbrir(false)}>
        <form onSubmit={fAbrir.handleSubmit(onAbrir)} className="space-y-4">
          <Campo etiqueta="Fondo inicial (efectivo en cajón) *" error={fAbrir.formState.errors.fondoInicial?.message}>
            <input type="number" min="0" step="1000" className={claseInput(!!fAbrir.formState.errors.fondoInicial)} {...fAbrir.register('fondoInicial')} autoFocus />
          </Campo>
          <Campo etiqueta="Observaciones"><textarea rows={2} className={claseInput} {...fAbrir.register('observaciones')} /></Campo>
          <p className="text-xs text-gray-500">Solo puede existir una caja abierta a la vez. El backend lo garantiza.</p>
          <div className="flex justify-end gap-2">
            <Boton type="button" variante="secundario" onClick={() => setModalAbrir(false)}>Cancelar</Boton>
            <Boton type="submit" cargando={abrir.isPending}>Abrir caja</Boton>
          </div>
        </form>
      </Modal>

      {/* Modal cerrar con arqueo */}
      <Modal abierto={modalCerrar} titulo="Cerrar caja — Arqueo" ancho="max-w-md" onClose={() => setModalCerrar(false)}>
        <form onSubmit={fCerrar.handleSubmit(() => setConfCerrar(true))} className="space-y-4">
          <p className="text-sm text-gray-600">Total esperado en cajón (fondo + entradas − salidas): <strong>{formatCOP(esperadoEfectivo)}</strong>. Las ventas pendientes de pago o sin caja asociada impedirán el cierre hasta resolverlas.</p>
          <Campo etiqueta="Efectivo contado *" error={fCerrar.formState.errors.efectivoContado?.message}>
            <input type="number" min="0" step="100" className={claseInput(!!fCerrar.formState.errors.efectivoContado)} {...fCerrar.register('efectivoContado')} autoFocus />
          </Campo>
          <Campo etiqueta="Observaciones"><textarea rows={2} className={claseInput} {...fCerrar.register('observaciones')} /></Campo>
          <div className="flex justify-end gap-2">
            <Boton type="button" variante="secundario" onClick={() => setModalCerrar(false)}>Cancelar</Boton>
            <Boton type="submit" variante="peligro">Cerrar caja</Boton>
          </div>
        </form>
      </Modal>

      <Confirmar
        abierto={confCerrar} titulo="Confirmar cierre de caja" peligroso
        descripcion={`Se registrará la diferencia de arqueo (${formatCOP(Math.max(0, (Number(fCerrar.watch('efectivoContado')) || 0) - esperadoEfectivo))} sobrante / faltante según signo) y no se podrán añadir movimientos a este turno.`}
        confirmarTexto="Sí, cerrar caja" cargando={cerrar.isPending}
        onCancelar={() => setConfCerrar(false)}
        onConfirmar={async () => {
          const v = fCerrar.getValues();
          await onCerrar({ efectivoContado: Number(v.efectivoContado) || 0, observaciones: v.observaciones ?? '' });
        }}
      />
    </div>
  );
}
