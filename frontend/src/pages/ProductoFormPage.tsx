// Crear/editar producto con validación Zod + react-hook-form. Los errores del backend se muestran.
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, useParams } from 'react-router-dom';
import { useCategorias, useCrearProducto, useActualizarProducto, useMarcas, useProducto } from '../hooks/useApi';
import { errorMessage } from '../lib/apiClient';
import { Boton, Campo, Cargando, ErrorAlCargar, claseInput, notificar } from '../components/ui';

const esquema = z.object({
  nombre: z.string().min(2, 'El nombre debe tener al menos 2 caracteres.').max(150),
  descripcion: z.string().max(1000).optional().or(z.literal('')),
  categoriaId: z.coerce.number().positive('Seleccione una categoría.').optional(),
  marcaId: z.coerce.number().positive().optional(),
  genero: z.string().max(30).optional().or(z.literal('')),
  tipoPrenda: z.string().max(50).optional().or(z.literal('')),
  temporada: z.string().max(50).optional().or(z.literal('')),
  imagenUrl: z.string().url('Debe ser una URL válida.').optional().or(z.literal('')),
});
type FormValues = z.infer<typeof esquema>;

export default function ProductoFormPage() {
  const { id } = useParams();
  const esEdicion = id != null;
  const productoId = esEdicion ? Number(id) : null;
  const navigate = useNavigate();

  const { data: producto, isLoading, error } = useProducto(productoId);
  const categorias = useCategorias(true);
  const marcas = useMarcas(true);
  const crear = useCrearProducto();
  const actualizar = useActualizarProducto();

  const { register, handleSubmit, formState: { errors }, reset } = useForm<FormValues>({
    resolver: zodResolver(esquema),
  });

  useEffect(() => {
    if (producto) {
      reset({
        nombre: producto.nombre, descripcion: producto.descripcion ?? '',
        categoriaId: producto.categoriaId ?? undefined, marcaId: producto.marcaId ?? undefined,
        genero: producto.genero ?? '', tipoPrenda: producto.tipoPrenda ?? '',
        temporada: producto.temporada ?? '', imagenUrl: producto.imagenUrl ?? '',
      });
    }
  }, [producto, reset]);

  async function onSubmit(vals: FormValues) {
    const body = {
      nombre: vals.nombre,
      descripcion: vals.descripcion || undefined,
      categoriaId: vals.categoriaId || undefined,
      marcaId: vals.marcaId || undefined,
      genero: vals.genero || undefined,
      tipoPrenda: vals.tipoPrenda || undefined,
      temporada: vals.temporada || undefined,
      imagenUrl: vals.imagenUrl || undefined,
    };
    try {
      if (esEdicion && productoId != null) {
        await actualizar.mutateAsync({ id: productoId, body: { ...body, activo: producto?.activo ?? true } });
        notificar.exito('Producto actualizado.');
        navigate(`/productos/${productoId}`);
      } else {
        const creado = await crear.mutateAsync(body);
        notificar.exito('Producto creado. Ahora puede agregar sus variantes.');
        navigate(`/productos/${creado.id}`);
      }
    } catch (e) {
      notificar.error(errorMessage(e));
    }
  }

  if (esEdicion && isLoading) return <Cargando />;
  if (esEdicion && error) return <ErrorAlCargar error={error} />;

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <h1 className="text-2xl font-bold text-gray-900">{esEdicion ? 'Editar producto' : 'Nuevo producto'}</h1>
      {!esEdicion && (
        <p className="text-sm text-gray-600">
          Las variantes (talla/color/SKU/precios) se administran en la página del producto después de crearlo.
        </p>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <Campo etiqueta="Nombre *" error={errors.nombre?.message}>
          <input className={claseInput(!!errors.nombre)} {...register('nombre')} autoFocus />
        </Campo>
        <Campo etiqueta="Descripción" error={errors.descripcion?.message}>
          <textarea rows={3} className={claseInput(!!errors.descripcion)} {...register('descripcion')} />
        </Campo>
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo etiqueta="Categoría" error={errors.categoriaId?.message}>
            <select className={claseInput(!!errors.categoriaId)} {...register('categoriaId')}>
              <option value="">Sin categoría</option>
              {(categorias.data ?? []).map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </select>
          </Campo>
          <Campo etiqueta="Marca" error={errors.marcaId?.message}>
            <select className={claseInput} {...register('marcaId')}>
              <option value="">Sin marca</option>
              {(marcas.data ?? []).map((m) => <option key={m.id} value={m.id}>{m.nombre}</option>)}
            </select>
          </Campo>
          <Campo etiqueta="Género / público">
            <input placeholder="Masculino, Femenino, Unisex…" className={claseInput} {...register('genero')} />
          </Campo>
          <Campo etiqueta="Tipo de prenda">
            <input placeholder="Camiseta, Pantalón…" className={claseInput} {...register('tipoPrenda')} />
          </Campo>
          <Campo etiqueta="Temporada o colección">
            <input placeholder="Verano 2026…" className={claseInput} {...register('temporada')} />
          </Campo>
          <Campo etiqueta="URL de imagen" error={errors.imagenUrl?.message}>
            <input placeholder="https://…" className={claseInput} {...register('imagenUrl')} />
          </Campo>
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Boton type="button" variante="secundario" onClick={() => navigate(-1)}>Cancelar</Boton>
          <Boton type="submit" cargando={crear.isPending || actualizar.isPending}>
            {esEdicion ? 'Guardar cambios' : 'Crear producto'}
          </Boton>
        </div>
      </form>
    </div>
  );
}
