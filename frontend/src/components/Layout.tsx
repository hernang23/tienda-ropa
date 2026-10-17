// Estructura administrativa: menú lateral, barra superior y contenido (Outlet).
import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useConfiguracion } from '../hooks/useApi';
import { Boton } from './ui';

interface Item { a: string; texto: string; roles?: string[]; }

const SECCIONES: { titulo: string; items: Item[] }[] = [
  { titulo: 'General', items: [{ a: '/', texto: 'Dashboard' }] },
  {
    titulo: 'Operación', items: [
      { a: '/pos', texto: 'Punto de venta', roles: ['ADMIN', 'VENDEDOR'] },
      { a: '/ventas', texto: 'Ventas' },
      { a: '/devoluciones', texto: 'Devoluciones' },
      { a: '/caja', texto: 'Caja' },
    ],
  },
  {
    titulo: 'Catálogo e inventario', items: [
      { a: '/productos', texto: 'Productos' },
      { a: '/catalogo', texto: 'Categorías y marcas' },
      { a: '/inventario', texto: 'Inventario' },
      { a: '/movimientos', texto: 'Movimientos' },
      { a: '/compras', texto: 'Compras', roles: ['ADMIN', 'INVENTARIO'] },
    ],
  },
  {
    titulo: 'Personas', items: [
      { a: '/clientes', texto: 'Clientes' },
      { a: '/proveedores', texto: 'Proveedores' },
    ],
  },
  {
    titulo: 'Administración', items: [
      { a: '/reportes', texto: 'Reportes' },
      { a: '/usuarios', texto: 'Usuarios y roles', roles: ['ADMIN'] },
      { a: '/configuracion', texto: 'Configuración', roles: ['ADMIN'] },
      { a: '/auditoria', texto: 'Auditoría', roles: ['ADMIN'] },
    ],
  },
];

export default function Layout() {
  const { usuario, tieneRol, cerrarSesion } = useAuth();
  const config = useConfiguracion();
  const navigate = useNavigate();
  const [menuAbierto, setMenuAbierto] = useState(false);

  const claseEnlace = ({ isActive }: { isActive: boolean }) =>
    `block rounded-lg px-3 py-2 text-sm font-medium transition ${
      isActive ? 'bg-marca-700 text-white' : 'text-gray-200 hover:bg-marca-800/60'}`;

  async function salir() {
    try { await cerrarSesion(); } finally { navigate('/login', { replace: true }); }
  }

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Barra superior */}
      <header className="fixed inset-x-0 top-0 z-40 flex h-14 items-center justify-between bg-marca-900 px-4 text-white shadow">
        <div className="flex items-center gap-3">
          <button
            aria-label="Abrir menú"
            className="rounded p-2 hover:bg-marca-800 lg:hidden"
            onClick={() => setMenuAbierto((v) => !v)}>
            ☰
          </button>
          <span className="truncate text-sm font-semibold sm:text-base">
            {config.data?.nombreComercial ?? 'Tienda de Ropa'}
          </span>
        </div>
        <div className="flex items-center gap-3">
          <NavLink to="/perfil" className="hidden text-sm underline-offset-2 hover:underline sm:block">
            {usuario?.nombreCompleto ?? '—'}
          </NavLink>
          <Boton variante="secundario" onClick={salir}>Cerrar sesión</Boton>
        </div>
      </header>

      <div className="flex pt-14">
        {/* Menú lateral */}
        <aside
          className={`fixed bottom-0 left-0 top-14 z-30 w-64 overflow-y-auto bg-marca-950 p-3 transition-transform lg:translate-x-0 ${
            menuAbierto ? 'translate-x-0' : '-translate-x-full'}`}
          onClick={() => setMenuAbierto(false)}>
          <nav aria-label="Navegación principal" className="space-y-5">
            {SECCIONES.map((sec) => {
              const visibles = sec.items.filter((i) => !i.roles || tieneRol(...i.roles));
              if (visibles.length === 0) return null;
              return (
                <div key={sec.titulo}>
                  <p className="px-3 pb-1 text-[11px] font-semibold uppercase tracking-wider text-marca-300">{sec.titulo}</p>
                  <ul className="space-y-0.5">
                    {visibles.map((i) => (
                      <li key={i.a}>
                        <NavLink to={i.a} end={i.a === '/'} className={claseEnlace}>{i.texto}</NavLink>
                      </li>
                    ))}
                  </ul>
                </div>
              );
            })}
          </nav>
        </aside>

        {/* Fondo para menú móvil */}
        {menuAbierto && (
          <div className="fixed inset-0 top-14 z-20 bg-black/30 lg:hidden" onClick={() => setMenuAbierto(false)} aria-hidden />
        )}

        <main className="min-w-0 flex-1 p-4 sm:p-6 lg:ml-64">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
