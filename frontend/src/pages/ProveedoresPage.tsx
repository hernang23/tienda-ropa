// Gestión de proveedores con búsqueda, creación y edición.
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../hooks/useAuth';
import { useGuardarProveedor, useProveedores } from '../hooks/useApi';
import { errorMessage } from '../lib/apiClient';
import type { ProveedorDto } from '../types/api';
import {
  Boton, Cabecera, Campo, Cargando, Celda, ErrorAlCargar, EtiquetaEstado, Fila, Modal,
  Paginador, Tabla, Vacio, claseInput, notificar,
} from '../components/ui';

const esquema = z.object({
  nombre: z.string().min(3, 'Mínimo 3 caracteres.').max(150),
  nit: z.string().max(30).optional().or(z.literal('')),
  contacto: z.string().max(100).optional().or(z.literal('')),
  telefono: z.string().max(30).optional().or(z.literal('')),
  correo: z.string().email('Correo inválido.').optional().or(z.literal('')),
  direccion: z.string().max(200).optional().or(z.literal('')),
  ciudad: z.string().max(80).optional().or(z.literal('')),
  observaciones: z.string().max(500).optional().or(z.literal('')),
  activo: z.boolean(),
});
type FormValores = z.infer<typeof esquema>;

export default function ProveedoresPage() {
  const { tieneRol } = useAuth();
  const editable = tieneRol('ADMIN', 'INVENTARIO');
  const [q, setQ] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [page, setPage] = useState(0);
  const { data, isLoading, error, refetch } = useProveedores(busqueda || undefined, page, 15);
  const guardar = useGuardarProveedor();
  const [modalAbierto, setModalAbierto] = useState(false);
  const [editando, setEditando] = useState<ProveedorDto | null>(null);

  const { register, handleSubmit, formState: { errors }, reset } = useForm<FormValores>({
    resolver: zodResolver(esquema),
    defaultValues: { activo: true },
  });

  function abrir(p?: ProveedorDto) {
    setEditando(p ?? null);
    reset({
      nombre: p?.nombre ?? '', nit: p?.nit ?? '', contacto: p?.contacto ?? '',
      telefono: p?.telefono ?? '', correo: p?.correo ?? '', direccion: p?.direccion ?? '',
      ciudad: p?.ciudad ?? '', observaciones: p?.observaciones ?? '', activo: p?.activo ?? true,
    });
    setModalAbierto(true);
  }

  async function onSubmit(v: FormValores) {
    try {
      await guardar.mutateAsync({
        id: editando?.id,
        body: {
          nombre: v.nombre, nit: v.nit || undefined, contacto: v.contacto || undefined,
          telefono: v.telefono || undefined, correo: v.correo || undefined,
          direccion: v.direccion || undefined, ciudad: v.ciudad || undefined,
          observaciones: v.observaciones || undefined, activo: v.activo,
        },
      });
      notificar.exito('Proveedor guardado.');
      setModalAbierto(false);
    } catch (e) { notificar.error(errorMessage(e)); }
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Proveedores</h1>
        {editable && <Boton onClick={() => abrir()}>+ Nuevo proveedor</Boton>}
      </div>

      <div className="flex gap-2">
        <input
          aria-label="Buscar proveedores"
          className="w-full max-w-xs rounded-lg border border-gray-300 px-3 py-2 text-sm"
          placeholder="Nombre o NIT…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && (setBusqueda(q), setPage(0))} />
        <Boton variante="secundario" onClick={() => { setBusqueda(q); setPage(0); }}>Buscar</Boton>
      </div>

      {isLoading && <Cargando />}
      {error && <ErrorAlCargar error={error} onReintentar={() => void refetch()} />}
      {data && (
        <>
          {data.content.length === 0 ? <Vacio mensaje="No hay proveedores registrados." /> : (
            <Tabla>
              <Cabecera>
                <Fila><Celda>Nombre</Celda><Celda>NIT</Celda><Celda>Contacto</Celda><Celda>Teléfono</Celda><Celda>Ciudad</Celda><Celda>Estado</Celda>{editable && <Celda>Acciones</Celda>}</Fila>
              </Cabecera>
              <tbody>
                {data.content.map((p) => (
                  <Fila key={p.id}>
                    <Celda className="font-medium">{p.nombre}</Celda>
                    <Celda>{p.nit ?? '—'}</Celda>
                    <Celda>{p.contacto ?? '—'}</Celda>
                    <Celda>{p.telefono ?? '—'}</Celda>
                    <Celda>{p.ciudad ?? '—'}</Celda>
                    <Celda><EtiquetaEstado valor={p.activo ? 'ACTIVA' : 'CANCELADA'} texto={p.activo ? 'Activo' : 'Inactivo'} /></Celda>
                    {editable && <Celda><Boton variante="fantasma" onClick={() => abrir(p)}>Editar</Boton></Celda>}
                  </Fila>
                ))}
              </tbody>
            </Tabla>
          )}
          <Paginador pagina={data.number} totalPaginas={data.totalPages} alCambiar={setPage} />
        </>
      )}

      <Modal abierto={modalAbierto} titulo={editando ? `Editar proveedor` : 'Nuevo proveedor'} onClose={() => setModalAbierto(false)}>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Campo etiqueta="Razón social / nombre *" error={errors.nombre?.message}>
            <input className={claseInput(!!errors.nombre)} {...register('nombre')} autoFocus />
          </Campo>
          <div className="grid gap-4 sm:grid-cols-2">
            <Campo etiqueta="NIT"><input className={claseInput} {...register('nit')} /></Campo>
            <Campo etiqueta="Persona de contacto"><input className={claseInput} {...register('contacto')} /></Campo>
            <Campo etiqueta="Teléfono"><input className={claseInput} {...register('telefono')} /></Campo>
            <Campo etiqueta="Correo" error={errors.correo?.message}><input className={claseInput} {...register('correo')} /></Campo>
            <Campo etiqueta="Ciudad"><input className={claseInput} {...register('ciudad')} /></Campo>
            <Campo etiqueta="Dirección"><input className={claseInput} {...register('direccion')} /></Campo>
          </div>
          <Campo etiqueta="Observaciones"><textarea rows={2} className={claseInput} {...register('observaciones')} /></Campo>
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" className="h-4 w-4 rounded border-gray-300" {...register('activo')} /> Activo</label>
          <div className="flex justify-end gap-2">
            <Boton type="button" variante="secundario" onClick={() => setModalAbierto(false)}>Cancelar</Boton>
            <Boton type="submit" cargando={guardar.isPending}>Guardar</Boton>
          </div>
        </form>
      </Modal>
    </div>
  );
}
