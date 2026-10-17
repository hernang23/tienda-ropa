// Listado de productos con búsqueda, filtros y paginación del servidor.
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useCategorias, useProductos, type FiltroProductos } from '../hooks/useApi';
import { formatCOP, formatNumero } from '../lib/format';
import { num } from '../types/api';
import { Boton, Cabecera, Cargando, Celda, ErrorAlCargar, EtiquetaEstado, Fila, Paginador, Tabla, Vacio } from '../components/ui';

export default function ProductosPage() {
  const navigate = useNavigate();
  const [filtros, setFiltros] = useState<FiltroProductos>({ q: '', page: 0, size: 10 });
  const [busqueda, setBusqueda] = useState('');
  const { data, isLoading, error, refetch } = useProductos(filtros);
  const categorias = useCategorias(true);
  const { tieneRol } = useAuth();

  function aplicarBusqueda() {
    setFiltros((f) => ({ ...f, q: busqueda, page: 0 }));
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Productos</h1>
        {tieneRol('ADMIN', 'INVENTARIO') && (
          <Link to="/productos/nuevo"><Boton>+ Nuevo producto</Boton></Link>
        )}
      </div>

      <div className="flex flex-wrap gap-2">
        <input
          aria-label="Buscar productos"
          className="w-full max-w-xs rounded-lg border border-gray-300 px-3 py-2 text-sm"
          placeholder="Nombre o SKU…"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && aplicarBusqueda()} />
        <select
          aria-label="Filtrar por categoría"
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          value={filtros.categoriaId ?? ''}
          onChange={(e) => setFiltros((f) => ({ ...f, categoriaId: e.target.value ? Number(e.target.value) : undefined, page: 0 }))}>
          <option value="">Todas las categorías</option>
          {(categorias.data ?? []).map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
        </select>
        <select
          aria-label="Filtrar por estado"
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
          value={filtros.soloActivos === undefined ? '' : String(filtros.soloActivos)}
          onChange={(e) => setFiltros((f) => ({ ...f, soloActivos: e.target.value === '' ? undefined : e.target.value === 'true', page: 0 }))}>
          <option value="">Todos los estados</option>
          <option value="true">Solo activos</option>
          <option value="false">Solo inactivos</option>
        </select>
        <Boton variante="secundario" onClick={aplicarBusqueda}>Buscar</Boton>
      </div>

      {isLoading && <Cargando />}
      {error && <ErrorAlCargar error={error} onReintentar={() => void refetch()} />}
      {data && (
        <>
          {data.content.length === 0 ? <Vacio mensaje="No se encontraron productos con esos criterios." /> : (
            <Tabla>
              <Cabecera>
                <Fila>
                  <Celda>Nombre</Celda><Celda>Categoría</Celda><Celda>Marca</Celda>
                  <Celda>Variantes</Celda><Celda>Existencias</Celda><Celda>Precio desde</Celda><Celda>Estado</Celda>
                </Fila>
              </Cabecera>
              <tbody>
                {data.content.map((p) => (
                  <Fila key={p.id} onClick={() => navigate(`/productos/${p.id}`)}>
                    <Celda className="font-medium text-gray-900">{p.nombre}</Celda>
                    <Celda>{p.categoriaNombre ?? '—'}</Celda>
                    <Celda>{p.marcaNombre ?? '—'}</Celda>
                    <Celda>{formatNumero(p.variantes.length)}</Celda>
                    <Celda className={p.totalExistencias === 0 ? 'font-semibold text-red-700' : ''}>{formatNumero(p.totalExistencias)}</Celda>
                    <Celda>{p.variantes.length > 0 ? formatCOP(Math.min(...p.variantes.map((v) => num(v.precioVenta)))) : '—'}</Celda>
                    <Celda><EtiquetaEstado valor={p.activo ? 'ACTIVA' : 'CANCELADA'} texto={p.activo ? 'Activo' : 'Inactivo'} /></Celda>
                  </Fila>
                ))}
              </tbody>
            </Tabla>
          )}
          <Paginador pagina={data.number} totalPaginas={data.totalPages} alCambiar={(p) => setFiltros((f) => ({ ...f, page: p }))} />
        </>
      )}
    </div>
  );
}
