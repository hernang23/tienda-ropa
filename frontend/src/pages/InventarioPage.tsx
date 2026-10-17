// Inventario: existencias por variante, alertas de stock bajo/agotados y ajustes auditables.
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../hooks/useAuth';
import { useAjusteInventario, useInventario, useResumenInventario, type FiltroInventario } from '../hooks/useApi';
import { errorMessage } from '../lib/apiClient';
import { formatCOP, formatNumero } from '../lib/format';
import type { VarianteDto } from '../types/api';
import {
  Boton, Cabecera, Campo, Cargando, Celda, ErrorAlCargar, EtiquetaEstado, Fila, Modal,
  Paginador, Tabla, Vacio, claseInput, notificar,
} from '../components/ui';

const esquemaAjuste = z.object({
  delta: z.coerce.number().int('La cantidad debe ser un número entero.').refine((v) => v !== 0, 'Indique un incremento (+) o decremento (−) distinto de cero.'),
  motivo: z.string().min(5, 'El motivo es obligatorio (mínimo 5 caracteres).'),
});
type AjusteForm = z.infer<typeof esquemaAjuste>;

export default function InventarioPage() {
  const { tieneRol } = useAuth();
  const puedeAjustar = tieneRol('ADMIN', 'INVENTARIO');
  const [filtros, setFiltros] = useState<FiltroInventario>({ page: 0, size: 15 });
  const [busqueda, setBusqueda] = useState('');
  const { data, isLoading, error, refetch } = useInventario(filtros);
  const resumen = useResumenInventario();
  const ajuste = useAjusteInventario();
  const [varianteAjustar, setVarianteAjustar] = useState<VarianteDto | null>(null);

  const { register, handleSubmit, watch, formState: { errors }, reset } = useForm<AjusteForm>({ resolver: zodResolver(esquemaAjuste) });
  const deltaEnVivo = Number(watch('delta')) || 0;

  function aplicarFiltros(nuevos: Partial<FiltroInventario>) {
    setFiltros((f) => ({ ...f, ...nuevos, page: 0 }));
  }

  async function onSubmitAjuste(v: AjusteForm) {
    if (!varianteAjustar) return;
    try {
      await ajuste.mutateAsync({ varianteId: varianteAjustar.id, body: { delta: v.delta, motivo: v.motivo } });
      notificar.exito('Ajuste registrado con movimiento auditable.');
      setVarianteAjustar(null);
      reset({ delta: 0, motivo: '' });
    } catch (e) { notificar.error(errorMessage(e)); }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">Inventario</h1>

      {resumen.data && (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <p className="text-xs font-semibold uppercase text-gray-500">Valor a costo</p>
            <p className="mt-1 text-lg font-bold">{formatCOP(resumen.data.valorAlCosto)}</p>
          </div>
          <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
            <p className="text-xs font-semibold uppercase text-gray-500">Unidades totales</p>
            <p className="mt-1 text-lg font-bold">{formatNumero(resumen.data.unidadesTotales)}</p>
          </div>
          <button onClick={() => aplicarFiltros({ bajoMinimo: true, agotado: undefined })}
            className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-left shadow-sm hover:bg-amber-100">
            <p className="text-xs font-semibold uppercase text-amber-700">Bajo mínimo</p>
            <p className="mt-1 text-lg font-bold text-amber-900">{formatNumero(resumen.data.variantesBajoMinimo)}</p>
          </button>
          <button onClick={() => aplicarFiltros({ agotado: true, bajoMinimo: undefined })}
            className="rounded-xl border border-red-200 bg-red-50 p-4 text-left shadow-sm hover:bg-red-100">
            <p className="text-xs font-semibold uppercase text-red-700">Agotadas</p>
            <p className="mt-1 text-lg font-bold text-red-900">{formatNumero(resumen.data.variantesAgotadas)}</p>
          </button>
        </div>
      )}

      <div className="flex flex-wrap gap-2">
        <input
          aria-label="Buscar por producto o SKU"
          className="w-full max-w-xs rounded-lg border border-gray-300 px-3 py-2 text-sm"
          placeholder="Nombre, SKU o código de barras…"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && aplicarFiltros({ q: busqueda })} />
        <input
          aria-label="Filtrar por talla"
          className="w-24 rounded-lg border border-gray-300 px-3 py-2 text-sm"
          placeholder="Talla"
          onBlur={(e) => aplicarFiltros({ talla: e.target.value || undefined })} />
        <input
          aria-label="Filtrar por color"
          className="w-28 rounded-lg border border-gray-300 px-3 py-2 text-sm"
          placeholder="Color"
          onBlur={(e) => aplicarFiltros({ color: e.target.value || undefined })} />
        <Boton variante="secundario" onClick={() => { setBusqueda(''); aplicarFiltros({ q: '', talla: undefined, color: undefined, bajoMinimo: undefined, agotado: undefined }); }}>Limpiar</Boton>
      </div>

      {isLoading && <Cargando />}
      {error && <ErrorAlCargar error={error} onReintentar={() => void refetch()} />}
      {data && (
        <>
          {data.content.length === 0 ? <Vacio mensaje="No hay variantes que coincidan con los filtros." /> : (
            <Tabla>
              <Cabecera>
                <Fila>
                  <Celda>Producto</Celda><Celda>SKU</Celda><Celda>Talla</Celda><Celda>Color</Celda>
                  <Celda>Precio</Celda><Celda>Existencias</Celda><Celda>Mínimo</Celda><Celda>Situación</Celda>
                  {puedeAjustar && <Celda>Ajuste</Celda>}
                </Fila>
              </Cabecera>
              <tbody>
                {data.content.map((v) => (
                  <Fila key={v.id}>
                    <Celda className="max-w-[220px] truncate font-medium">{v.productoNombre ?? v.sku}</Celda>
                    <Celda className="font-mono text-xs">{v.sku}</Celda>
                    <Celda>{v.talla}</Celda>
                    <Celda>{v.color}</Celda>
                    <Celda>{formatCOP(v.precioVenta)}</Celda>
                    <Celda className="font-semibold">{formatNumero(v.existencias)}</Celda>
                    <Celda>{formatNumero(v.stockMinimo)}</Celda>
                    <Celda>
                      {v.agotado
                        ? <EtiquetaEstado valor="AGOTADO" texto="Agotado" />
                        : v.bajoMinimo
                          ? <EtiquetaEstado valor="BAJO" texto="Bajo mínimo" />
                          : <EtiquetaEstado valor="ACTIVA" texto="Disponible" />}
                    </Celda>
                    {puedeAjustar && (
                      <Celda>
                        <Boton variante="fantasma" onClick={() => setVarianteAjustar(v)}>Registrar ajuste</Boton>
                      </Celda>
                    )}
                  </Fila>
                ))}
              </tbody>
            </Tabla>
          )}
          <Paginador pagina={data.number} totalPaginas={data.totalPages} alCambiar={(p) => setFiltros((f) => ({ ...f, page: p }))} />
        </>
      )}

      <Modal abierto={varianteAjustar != null} titulo={`Ajuste de inventario — ${varianteAjustar?.sku ?? ''}`} ancho="max-w-md"
        onClose={() => setVarianteAjustar(null)}>
        {varianteAjustar && (
          <form onSubmit={handleSubmit(onSubmitAjuste)} className="space-y-4">
            <p className="text-sm text-gray-600">
              Existencias actuales: <strong>{formatNumero(varianteAjustar.existencias)}</strong>.
              El ajuste genera un movimiento auditable; no se modifica el stock sin dejar trazabilidad.
            </p>
            <Campo etiqueta="Cantidad (+ entrada / − salida) *" error={errors.delta?.message}>
              <input type="number" step="1" className={claseInput(!!errors.delta)} {...register('delta')} autoFocus />
            </Campo>
            <Campo etiqueta="Motivo *" error={errors.motivo?.message} hint="Ej.: conteo físico, mercancía dañada, robo.">
              <textarea rows={2} className={claseInput(!!errors.motivo)} {...register('motivo')} />
            </Campo>
            <p className="text-xs text-gray-500">
              Resultado esperado: {formatNumero(varianteAjustar.existencias)} →{' '}
              {formatNumero(varianteAjustar.existencias + deltaEnVivo)}. El sistema impide saldos negativos.
            </p>
            <div className="flex justify-end gap-2">
              <Boton type="button" variante="secundario" onClick={() => setVarianteAjustar(null)}>Cancelar</Boton>
              <Boton type="submit" cargando={ajuste.isPending}>Registrar ajuste</Boton>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
}
