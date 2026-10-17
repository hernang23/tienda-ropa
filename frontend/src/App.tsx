// Enrutamiento principal: rutas públicas (login) y privadas protegidas por sesión y roles.
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { AuthProvider, useAuth } from './hooks/useAuth';
import { ContenedorToasts } from './components/ui';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ProductosPage from './pages/ProductosPage';
import ProductoFormPage from './pages/ProductoFormPage';
import ProductoDetallePage from './pages/ProductoDetallePage';
import CatalogoPage from './pages/CatalogoPage';
import InventarioPage from './pages/InventarioPage';
import MovimientosPage from './pages/MovimientosPage';
import ProveedoresPage from './pages/ProveedoresPage';
import ClientesPage from './pages/ClientesPage';
import ComprasPage from './pages/ComprasPage';
import CompraDetallePage from './pages/CompraDetallePage';
import PosPage from './pages/PosPage';
import VentasPage from './pages/VentasPage';
import VentaDetallePage from './pages/VentaDetallePage';
import DevolucionesPage from './pages/DevolucionesPage';
import CajaPage from './pages/CajaPage';
import ReportesPage from './pages/ReportesPage';
import UsuariosPage from './pages/UsuariosPage';
import ConfiguracionPage from './pages/ConfiguracionPage';
import AuditoriaPage from './pages/AuditoriaPage';
import PerfilPage from './pages/PerfilPage';

function Protegida({ children }: { children: ReactNode }) {
  const { usuario, cargando } = useAuth();
  const loc = useLocation();
  if (cargando) return <div className="p-10 text-center text-gray-600">Verificando sesión…</div>;
  if (!usuario) return <Navigate to="/login" state={{ desde: loc.pathname }} replace />;
  return <>{children}</>;
}

/** Restringe el acceso por rol (la autorización real se verifica además en el backend). */
function SoloRol({ roles, children }: { roles: string[]; children: ReactNode }) {
  const { tieneRol } = useAuth();
  if (!tieneRol(...roles)) {
    return (
      <div className="mx-auto max-w-lg rounded-xl border border-amber-200 bg-amber-50 p-8 text-center">
        <h2 className="text-lg font-semibold text-amber-900">Acceso denegado</h2>
        <p className="mt-2 text-sm text-amber-800">
          Su usuario no tiene los permisos necesarios para ver esta sección. Contacte al administrador.
        </p>
      </div>
    );
  }
  return <>{children}</>;
}

export default function App() {
  return (
    <AuthProvider>
      <ContenedorToasts />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={<Protegida><Layout /></Protegida>}
        >
          <Route index element={<DashboardPage />} />
          <Route path="productos" element={<ProductosPage />} />
          <Route path="productos/nuevo" element={<SoloRol roles={['ADMIN', 'INVENTARIO']}><ProductoFormPage /></SoloRol>} />
          <Route path="productos/:id" element={<ProductoDetallePage />} />
          <Route path="productos/:id/editar" element={<SoloRol roles={['ADMIN', 'INVENTARIO']}><ProductoFormPage /></SoloRol>} />
          <Route path="catalogo" element={<CatalogoPage />} />
          <Route path="inventario" element={<InventarioPage />} />
          <Route path="movimientos" element={<MovimientosPage />} />
          <Route path="proveedores" element={<ProveedoresPage />} />
          <Route path="clientes" element={<ClientesPage />} />
          <Route path="compras" element={<SoloRol roles={['ADMIN', 'INVENTARIO']}><ComprasPage /></SoloRol>} />
          <Route path="compras/:id" element={<SoloRol roles={['ADMIN', 'INVENTARIO']}><CompraDetallePage /></SoloRol>} />
          <Route path="pos" element={<SoloRol roles={['ADMIN', 'VENDEDOR']}><PosPage /></SoloRol>} />
          <Route path="ventas" element={<VentasPage />} />
          <Route path="ventas/:id" element={<VentaDetallePage />} />
          <Route path="devoluciones" element={<DevolucionesPage />} />
          <Route path="caja" element={<CajaPage />} />
          <Route path="reportes" element={<ReportesPage />} />
          <Route path="usuarios" element={<SoloRol roles={['ADMIN']}><UsuariosPage /></SoloRol>} />
          <Route path="configuracion" element={<SoloRol roles={['ADMIN']}><ConfiguracionPage /></SoloRol>} />
          <Route path="auditoria" element={<SoloRol roles={['ADMIN']}><AuditoriaPage /></SoloRol>} />
          <Route path="perfil" element={<PerfilPage />} />
          <Route path="*" element={<div className="p-10 text-center text-gray-600">Página no encontrada.</div>} />
        </Route>
      </Routes>
    </AuthProvider>
  );
}
