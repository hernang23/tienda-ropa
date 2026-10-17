// Gestión de clientes con historial de compras.
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useClientes, useGuardarCliente, useHistorialCliente } from '../hooks/useApi';
import { errorMessage } from '../lib/apiClient';
import { etiqueta, formatCOP, formatFechaHora } from '../lib/format';
import type { ClienteDto } from '../types/api';
import {
  Boton, Cabecera, Campo, Cargando, Celda, ErrorAlCargar, EtiquetaEstado, Fila, Modal,
  Paginador, Tabla, Vacio, claseInput, notificar,
} from '../components/ui';

const esquema = z.object({
  tipo: z.enum(['NATURAL', 'EMPRESA']),
  nombre: z.string().min(3, 'Mínimo 3 caracteres.').max(150),
  documento: z.string().max(30).optional().or(z.literal('')),
  correo: z.string().email('Correo inválido.').optional().or(z.literal('')),
  telefono: z.string().max(30).optional().or(z.literal('')),
  direccion: z.string().max(200).optional().or(z.literal('')),
  activo: z.boolean(),
});
type FormValores = z.infer<typeof esquema>;

export default function ClientesPage() {
  const [q, setQ] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [page, setPage] = useState(0);
  const { data, isLoading, error, refetch } = useClientes(busqueda || undefined, page, 15);
  const guardar = useGuardarCliente();
  const [modalAbierto, setModalAbierto] = useState(false);
  const [editando, setEditando] = useState<ClienteDto | null>(null);
  const [historialDe, setHistorialDe] = useState<ClienteDto | null>(null);
  const historial = useHistorialCliente(historialDe?.id ?? null);

  const { register, handleSubmit, formState: { errors }, reset } = useForm<FormValores>({
    resolver: zodResolver(esquema),
    defaultValues: { tipo: 'NATURAL', activo: true },
  });

  function abrir(c?: ClienteDto) {
    setEditando(c ?? null);
    reset({
      tipo: (c?.tipo as 'NATURAL' | 'EMPRESA') ?? 'NATURAL',
      nombre: c?.nombre ?? '', documento: c?.documento ?? '', correo: c?.correo ?? '',
      telefono: c?.telefono ?? '', direccion: c?.direccion ?? '', activo: c?.activo ?? true,
    });
    setModalAbierto(true);
  }

  async function onSubmit(v: FormValores) {
    try {
      await guardar.mutateAsync({
        id: editando?.id,
        body: {
          tipo: v.tipo, nombre: v.nombre, documento: v.documento || undefined,
          correo: v.correo || undefined, telefono: v.telefono || undefined,
          direccion: v.direccion || undefined, activo: v.activo,
        },
      });
      notificar.exito('Cliente guardado.');
      setModalAbierto(false);
    } catch (e) { notificar.error(errorMessage(e)); }
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Clientes</h1>
        <Boton onClick={() => abrir()}>+ Nuevo cliente</Boton>
      </div>

      <div className="flex gap-2">
        <input
          aria-label="Buscar clientes"
          className="w-full max-w-xs rounded-lg border border-gray-300 px-3 py-2 text-sm"
          placeholder="Nombre o documento…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && (setBusqueda(q), setPage(0))} />
        <Boton variante="secundario" onClick={() => { setBusqueda(q); setPage(0); }}>Buscar</Boton>
      </div>

      {isLoading && <Cargando />}
      {error && <ErrorAlCargar error={error} onReintentar={() => void refetch()} />}
      {data && (
        <>
          {data.content.length === 0 ? <Vacio mensaje="No hay clientes registrados." /> : (
            <Tabla>
              <Cabecera>
                <Fila><Celda>Nombre</Celda><Celda>Tipo</Celda><Celda>Documento</Celda><Celda>Teléfono</Celda><Celda>Correo</Celda><Celda>Estado</Celda><Celda>Acciones</Celda></Fila>
              </Cabecera>
              <tbody>
                {data.content.map((c) => (
                  <Fila key={c.id}>
                    <Celda className="font-medium">{c.nombre}</Celda>
                    <Celda>{etiqueta(c.tipo)}</Celda>
                    <Celda>{c.documento ?? '—'}</Celda>
                    <Celda>{c.telefono ?? '—'}</Celda>
                    <Celda>{c.correo ?? '—'}</Celda>
                    <Celda><EtiquetaEstado valor={c.activo ? 'ACTIVA' : 'CANCELADA'} texto={c.activo ? 'Activo' : 'Inactivo'} /></Celda>
                    <Celda>
                      <div className="flex gap-1">
                        <Boton variante="fantasma" onClick={() => setHistorialDe(c)}>Historial</Boton>
                        <Boton variante="fantasma" onClick={() => abrir(c)}>Editar</Boton>
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

      <Modal abierto={modalAbierto} titulo={editando ? 'Editar cliente' : 'Nuevo cliente'} ancho="max-w-md" onClose={() => setModalAbierto(false)}>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Campo etiqueta="Tipo">
            <select className={claseInput} {...register('tipo')}>
              <option value="NATURAL">Persona natural</option>
              <option value="EMPRESA">Empresa</option>
            </select>
          </Campo>
          <Campo etiqueta="Nombre / razón social *" error={errors.nombre?.message}>
            <input className={claseInput(!!errors.nombre)} {...register('nombre')} autoFocus />
          </Campo>
          <div className="grid gap-4 sm:grid-cols-2">
            <Campo etiqueta="Documento / NIT"><input className={claseInput} {...register('documento')} /></Campo>
            <Campo etiqueta="Teléfono"><input className={claseInput} {...register('telefono')} /></Campo>
            <Campo etiqueta="Correo" error={errors.correo?.message}><input className={claseInput} {...register('correo')} /></Campo>
            <Campo etiqueta="Dirección"><input className={claseInput} {...register('direccion')} /></Campo>
          </div>
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" className="h-4 w-4 rounded border-gray-300" {...register('activo')} /> Activo</label>
          <div className="flex justify-end gap-2">
            <Boton type="button" variante="secundario" onClick={() => setModalAbierto(false)}>Cancelar</Boton>
            <Boton type="submit" cargando={guardar.isPending}>Guardar</Boton>
          </div>
        </form>
      </Modal>

      <Modal abierto={historialDe != null} titulo={`Compras de ${historialDe?.nombre ?? ''}`} onClose={() => setHistorialDe(null)}>
        {historial.isLoading && <Cargando />}
        {historial.error && <ErrorAlCargar error={historial.error} />}
        {historial.data && (historial.data.content.length === 0
          ? <Vacio mensaje="Este cliente no tiene compras registradas." />
          : (
            <ul className="divide-y divide-gray-100 text-sm">
              {historial.data.content.map((v) => (
                <li key={v.id} className="flex items-center justify-between py-2">
                  <span className="font-mono text-xs">{v.numero} · {formatFechaHora(v.fecha)}</span>
                  <span className="flex items-center gap-2">
                    <EtiquetaEstado valor={v.estado} texto={etiqueta(v.estado)} />
                    <strong>{formatCOP(v.total)}</strong>
                  </span>
                </li>
              ))}
            </ul>
          ))}
      </Modal>
    </div>
  );
}
