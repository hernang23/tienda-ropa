// Detalle del producto y administración de variantes (talla/color/SKU/precios/stock).
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../hooks/useAuth';
import {
  useActualizarProducto, useActualizarVariante, useCrearVariante, useDesactivarProducto, useProducto,
} from '../hooks/useApi';
import { errorMessage } from '../lib/apiClient';
import { formatCOP, formatFechaHora, formatNumero } from '../lib/format';
import { num, type VarianteDto } from '../types/api';
import {
  Boton, Cabecera, Campo, Cargando, Celda, Confirmar, ErrorAlCargar, EtiquetaEstado, Fila,
  Modal, Tabla, Vacio, claseInput, notificar,
} from '../components/ui';

const esquemaVariante = z.object({
  sku: z.string().min(3, 'El SKU debe tener al menos 3 caracteres.').max(60),
  codigoBarras: z.string().max(60).optional().or(z.literal('')),
  talla: z.string().min(1, 'Indique la talla.'),
  color: z.string().min(1, 'Indique el color.'),
  precioVenta: z.coerce.number().positive('El precio debe ser mayor que cero.'),
  costoUnitario: z.coerce.number().min(0, 'El costo no puede ser negativo.'),
  existenciasIniciales: z.coerce.number().int().min(0, 'Las existencias iniciales no pueden ser negativas.'),
  stockMinimo: z.coerce.number().int().min(0).default(0),
});
type VarianteForm = z.infer<typeof esquemaVariante>;

function EditarVarianteModal({ variante, cerrado }: { variante: VarianteDto; cerrado: () => void }) {
  const actualizar = useActualizarVariante();
  const { register, handleSubmit, formState: { errors } } = useForm<{
    precioVenta: number; costoUnitario: number; stockMinimo: number; codigoBarras: string; activo: boolean;
  }>({
    resolver: zodResolver(z.object({
      precioVenta: z.coerce.number().positive(),
      costoUnitario: z.coerce.number().min(0),
      stockMinimo: z.coerce.number().int().min(0),
      codigoBarras: z.string().max(60).optional().or(z.literal('')),
      activo: z.boolean(),
    })),
    defaultValues: {
      precioVenta: num(variante.precioVenta), costoUnitario: num(variante.costoUnitario),
      stockMinimo: variante.stockMinimo, codigoBarras: variante.codigoBarras ?? '', activo: variante.activo,
    },
  });

  async function onSubmit(v: { precioVenta: number; costoUnitario: number; stockMinimo: number; codigoBarras?: string; activo: boolean }) {
    try {
      await actualizar.mutateAsync({
        varianteId: variante.id,
        body: { precioVenta: v.precioVenta, costoUnitario: v.costoUnitario, stockMinimo: v.stockMinimo, codigoBarras: v.codigoBarras || undefined, activo: v.activo },
      });
      notificar.exito('Variante actualizada.');
      cerrado();
    } catch (e) { notificar.error(errorMessage(e)); }
  }

  return (
    <Modal abierto titulo={`Editar variante ${variante.sku}`} onClose={cerrado}>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <p className="text-sm text-gray-600">{formatNumero(variante.existencias)} unidades disponibles. Las existencias solo cambian con compras, ventas o ajustes registrados.</p>
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo etiqueta="Precio de venta" error={errors.precioVenta?.message}><input type="number" step="50" className={claseInput(!!errors.precioVenta)} {...register('precioVenta')} /></Campo>
          <Campo etiqueta="Costo unitario" error={errors.costoUnitario?.message}><input type="number" step="50" className={claseInput(!!errors.costoUnitario)} {...register('costoUnitario')} /></Campo>
          <Campo etiqueta="Stock mínimo"><input type="number" min="0" className={claseInput} {...register('stockMinimo')} /></Campo>
          <Campo etiqueta="Código de barras"><input className={claseInput} {...register('codigoBarras')} /></Campo>
        </div>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" className="h-4 w-4 rounded border-gray-300" {...register('activo')} /> Variante activa</label>
        <div className="flex justify-end gap-2">
          <Boton type="button" variante="secundario" onClick={cerrado}>Cancelar</Boton>
          <Boton type="submit" cargando={actualizar.isPending}>Guardar</Boton>
        </div>
      </form>
    </Modal>
  );
}

export default function ProductoDetallePage() {
  const { id } = useParams();
  const productoId = Number(id);
  const navigate = useNavigate();
  const { tieneRol } = useAuth();
  const editable = tieneRol('ADMIN', 'INVENTARIO');
  const { data: producto, isLoading, error, refetch } = useProducto(productoId);
  const crearVariante = useCrearVariante();
  const desactivar = useDesactivarProducto();
  const actualizarProducto = useActualizarProducto();
  const [modalVariante, setModalVariante] = useState(false);
  const [editando, setEditando] = useState<VarianteDto | null>(null);
  const [confDesactivar, setConfDesactivar] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<VarianteForm>({
    resolver: zodResolver(esquemaVariante),
    defaultValues: { existenciasIniciales: 0, stockMinimo: 0 },
  });

  if (isLoading) return <Cargando />;
  if (error) return <ErrorAlCargar error={error} onReintentar={() => void refetch()} />;
  if (!producto) return <Vacio />;

  async function onSubmitVariante(v: VarianteForm) {
    try {
      await crearVariante.mutateAsync({
        productoId: producto.id,
        body: { ...v, codigoBarras: v.codigoBarras || undefined, activo: true },
      });
      notificar.exito('Variante creada.');
      setModalVariante(false);
    } catch (e) { notificar.error(errorMessage(e)); }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Link to="/productos" className="text-sm text-marca-700 hover:underline">← Productos</Link>
          <h1 className="mt-1 text-2xl font-bold text-gray-900">{producto.nombre}</h1>
          <p className="mt-1 text-sm text-gray-600">
            {[producto.categoriaNombre, producto.marcaNombre, producto.genero, producto.tipoPrenda, producto.temporada]
              .filter(Boolean).join(' · ') || 'Sin clasificación'}
          </p>
          {producto.descripcion && <p className="mt-2 max-w-2xl text-sm text-gray-700">{producto.descripcion}</p>}
        </div>
        <div className="flex flex-wrap gap-2">
          <EtiquetaEstado valor={producto.activo ? 'ACTIVA' : 'CANCELADA'} texto={producto.activo ? 'Activo' : 'Inactivo'} />
          {editable && (
            <>
              <Boton variante="secundario" onClick={() => navigate(`/productos/${producto.id}/editar`)}>Editar</Boton>
              {producto.activo && (
                <Boton variante="peligro" onClick={() => setConfDesactivar(true)}>Desactivar</Boton>
              )}
            </>
          )}
        </div>
      </div>

      {producto.imagenUrl && (
        <img src={producto.imagenUrl} alt={`Imagen de ${producto.nombre}`}
          className="h-48 w-48 rounded-lg border border-gray-200 object-cover"
          onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
      )}

      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-gray-800">
          Variantes ({formatNumero(producto.variantes.length)}) — Existencias totales: {formatNumero(producto.totalExistencias)}
        </h2>
        {editable && <Boton onClick={() => setModalVariante(true)}>+ Agregar variante</Boton>}
      </div>

      {producto.variantes.length === 0 ? (
        <Vacio mensaje="Este producto aún no tiene variantes. Cree combinaciones de talla y color para poder venderlo." />
      ) : (
        <Tabla>
          <Cabecera>
            <Fila>
              <Celda>SKU</Celda><Celda>Talla</Celda><Celda>Color</Celda><Celda>Precio</Celda>
              <Celda>Costo</Celda><Celda>Existencias</Celda><Celda>Mínimo</Celda><Celda>Estado</Celda>
              {editable && <Celda>Acciones</Celda>}
            </Fila>
          </Cabecera>
          <tbody>
            {producto.variantes.map((v) => (
              <Fila key={v.id}>
                <Celda className="font-mono text-xs">{v.sku}</Celda>
                <Celda>{v.talla}</Celda>
                <Celda>{v.color}</Celda>
                <Celda>{formatCOP(v.precioVenta)}</Celda>
                <Celda>{formatCOP(v.costoUnitario)}</Celda>
                <Celda className={v.agotado ? 'font-semibold text-red-700' : v.bajoMinimo ? 'font-semibold text-amber-700' : ''}>
                  {formatNumero(v.existencias)}
                  {v.agotado && <span className="ml-1 text-xs">(agotado)</span>}
                  {!v.agotado && v.bajoMinimo && <span className="ml-1 text-xs">(bajo mínimo)</span>}
                </Celda>
                <Celda>{formatNumero(v.stockMinimo)}</Celda>
                <Celda><EtiquetaEstado valor={v.activo ? 'ACTIVA' : 'CANCELADA'} texto={v.activo ? 'Activa' : 'Inactiva'} /></Celda>
                {editable && <Celda><Boton variante="fantasma" onClick={() => setEditando(v)}>Editar</Boton></Celda>}
              </Fila>
            ))}
          </tbody>
        </Tabla>
      )}

      <p className="text-xs text-gray-500">
        Creado: {formatFechaHora(producto.creadoEn)} · Última actualización: {formatFechaHora(producto.actualizadoEn)}
      </p>

      {/* Modal crear variante */}
      <Modal abierto={modalVariante} titulo="Nueva variante" onClose={() => setModalVariante(false)}>
        <form onSubmit={handleSubmit(onSubmitVariante)} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Campo etiqueta="SKU *" error={errors.sku?.message} hint="Debe ser único en todo el sistema.">
              <input className={claseInput(!!errors.sku)} {...register('sku')} autoFocus />
            </Campo>
            <Campo etiqueta="Código de barras"><input className={claseInput} {...register('codigoBarras')} /></Campo>
            <Campo etiqueta="Talla *" error={errors.talla?.message}><input className={claseInput} {...register('talla')} placeholder="S, M, L, XL…" /></Campo>
            <Campo etiqueta="Color *" error={errors.color?.message}><input className={claseInput} {...register('color')} placeholder="Negro, Azul…" /></Campo>
            <Campo etiqueta="Precio de venta (COP) *" error={errors.precioVenta?.message}><input type="number" step="50" className={claseInput(!!errors.precioVenta)} {...register('precioVenta')} /></Campo>
            <Campo etiqueta="Costo unitario (COP) *" error={errors.costoUnitario?.message}><input type="number" step="50" className={claseInput(!!errors.costoUnitario)} {...register('costoUnitario')} /></Campo>
            <Campo etiqueta="Existencias iniciales" error={errors.existenciasIniciales?.message} hint="Solo para inventario inicial; los cambios posteriores se registran como movimientos.">
              <input type="number" min="0" className={claseInput} {...register('existenciasIniciales')} />
            </Campo>
            <Campo etiqueta="Stock mínimo"><input type="number" min="0" className={claseInput} {...register('stockMinimo')} /></Campo>
          </div>
          <div className="flex justify-end gap-2">
            <Boton type="button" variante="secundario" onClick={() => setModalVariante(false)}>Cancelar</Boton>
            <Boton type="submit" cargando={crearVariante.isPending}>Crear variante</Boton>
          </div>
        </form>
      </Modal>

      {editando && <EditarVarianteModal variante={editando} cerrado={() => setEditando(null)} />}

      <Confirmar
        abierto={confDesactivar}
        titulo="Desactivar producto"
        peligroso
        descripcion="El producto no podrá venderse ni aparecerá en el POS. Sus variantes e historial se conservan; no se elimina físicamente."
        confirmarTexto="Desactivar"
        cargando={desactivar.isPending || actualizarProducto.isPending}
        onCancelar={() => setConfDesactivar(false)}
        onConfirmar={async () => {
          try {
            await desactivar.mutateAsync(producto.id);
            notificar.exito('Producto desactivado.');
            setConfDesactivar(false);
            void refetch();
          } catch (e) { notificar.error(errorMessage(e)); }
        }}
      />
    </div>
  );
}
