// Administración de categorías y marcas en una sola página con dos pestañas.
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../hooks/useAuth';
import { useCategorias, useGuardarCategoria, useGuardarMarca, useMarcas } from '../hooks/useApi';
import { errorMessage } from '../lib/apiClient';
import { formatFechaHora } from '../lib/format';
import type { CategoriaDto, MarcaDto, NombreDescActivoRequest } from '../types/api';
import { Boton, Campo, Cargando, ErrorAlCargar, EtiquetaEstado, Modal, Vacio, claseInput, notificar } from '../components/ui';

const esquema = z.object({
  nombre: z.string().min(2, 'Mínimo 2 caracteres.').max(100),
  descripcion: z.string().max(500).optional().or(z.literal('')),
  activo: z.boolean(),
});
type FormValores = z.infer<typeof esquema>;

interface EntidadBase { id: number; nombre: string; descripcion?: string | null; activo: boolean; creadoEn?: string | null; }

function FormularioEntidad<T extends EntidadBase>({ titulo, inicial, guardando, abierto, onGuardar, onClose }: {
  titulo: string; inicial: T | null; guardando: boolean; abierto: boolean;
  onGuardar: (v: NombreDescActivoRequest) => Promise<void>; onClose: () => void;
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<FormValores>({
    resolver: zodResolver(esquema),
    values: { nombre: inicial?.nombre ?? '', descripcion: inicial?.descripcion ?? '', activo: inicial?.activo ?? true },
  });
  return (
    <Modal abierto={abierto} titulo={titulo} ancho="max-w-md" onClose={onClose}>
      <form onSubmit={handleSubmit((v) => onGuardar({ nombre: v.nombre, descripcion: v.descripcion || undefined, activo: v.activo }))} className="space-y-4">
        <Campo etiqueta="Nombre *" error={errors.nombre?.message}><input className={claseInput(!!errors.nombre)} {...register('nombre')} autoFocus /></Campo>
        <Campo etiqueta="Descripción"><textarea rows={2} className={claseInput} {...register('descripcion')} /></Campo>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" className="h-4 w-4 rounded border-gray-300" {...register('activo')} /> Activo</label>
        <div className="flex justify-end gap-2">
          <Boton type="button" variante="secundario" onClick={onClose}>Cancelar</Boton>
          <Boton type="submit" cargando={guardando}>Guardar</Boton>
        </div>
      </form>
    </Modal>
  );
}

export default function CatalogoPage() {
  const [pestana, setPestana] = useState<'categorias' | 'marcas'>('categorias');
  const { tieneRol } = useAuth();
  const editable = tieneRol('ADMIN', 'INVENTARIO');
  const categorias = useCategorias();
  const marcas = useMarcas();
  const guardarCat = useGuardarCategoria();
  const guardarMar = useGuardarMarca();
  const [editCat, setEditCat] = useState<CategoriaDto | null>(null);
  const [nuevaCat, setNuevaCat] = useState(false);
  const [editMar, setEditMar] = useState<MarcaDto | null>(null);
  const [nuevaMar, setNuevaMar] = useState(false);

  const clasesPestana = (activa: boolean) =>
    `rounded-lg px-4 py-2 text-sm font-medium ${activa ? 'bg-marca-700 text-white' : 'bg-white text-gray-700 border border-gray-300'}`;

  async function salvarCategoria(v: NombreDescActivoRequest) {
    try {
      await guardarCat.mutateAsync({ id: editCat?.id, body: v });
      notificar.exito('Categoría guardada.');
      setEditCat(null); setNuevaCat(false);
    } catch (e) { notificar.error(errorMessage(e)); }
  }
  async function salvarMarca(v: NombreDescActivoRequest) {
    try {
      await guardarMar.mutateAsync({ id: editMar?.id, body: v });
      notificar.exito('Marca guardada.');
      setEditMar(null); setNuevaMar(false);
    } catch (e) { notificar.error(errorMessage(e)); }
  }

  const lista = pestana === 'categorias' ? categorias : marcas;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Categorías y marcas</h1>
        {editable && (
          pestana === 'categorias'
            ? <Boton onClick={() => { setEditCat(null); setNuevaCat(true); }}>+ Nueva categoría</Boton>
            : <Boton onClick={() => { setEditMar(null); setNuevaMar(true); }}>+ Nueva marca</Boton>
        )}
      </div>

      <div className="flex gap-2">
        <button className={clasesPestana(pestana === 'categorias')} onClick={() => setPestana('categorias')}>Categorías</button>
        <button className={clasesPestana(pestana === 'marcas')} onClick={() => setPestana('marcas')}>Marcas</button>
      </div>

      {lista.isLoading && <Cargando />}
      {lista.error && <ErrorAlCargar error={lista.error} />}
      {lista.data && lista.data.length === 0 && (
        <Vacio mensaje={pestana === 'categorias' ? 'No hay categorías registradas.' : 'No hay marcas registradas.'} />
      )}
      {lista.data && lista.data.length > 0 && (
        <ul className="divide-y divide-gray-100 rounded-xl border border-gray-200 bg-white shadow-sm">
          {lista.data.map((e) => (
            <li key={e.id} className="flex flex-wrap items-center justify-between gap-2 px-5 py-3">
              <div className="min-w-0">
                <p className="font-medium text-gray-900">{e.nombre}</p>
                {e.descripcion && <p className="truncate text-sm text-gray-500">{e.descripcion}</p>}
              </div>
              <div className="flex items-center gap-3">
                <span className="text-xs text-gray-500">{formatFechaHora(e.creadoEn)}</span>
                <EtiquetaEstado valor={e.activo ? 'ACTIVA' : 'CANCELADA'} texto={e.activo ? 'Activa' : 'Inactiva'} />
                {editable && (
                  <Boton variante="fantasma" onClick={() => pestana === 'categorias' ? setEditCat(e as CategoriaDto) : setEditMar(e as MarcaDto)}>
                    Editar
                  </Boton>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      <FormularioEntidad
        titulo={editCat ? 'Editar categoría' : 'Nueva categoría'}
        inicial={editCat} abierto={nuevaCat || editCat != null} guardando={guardarCat.isPending}
        onGuardar={salvarCategoria} onClose={() => { setNuevaCat(false); setEditCat(null); }} />
      <FormularioEntidad
        titulo={editMar ? 'Editar marca' : 'Nueva marca'}
        inicial={editMar} abierto={nuevaMar || editMar != null} guardando={guardarMar.isPending}
        onGuardar={salvarMarca} onClose={() => { setNuevaMar(false); setEditMar(null); }} />
    </div>
  );
}
