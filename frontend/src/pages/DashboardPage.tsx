// Panel de indicadores: todas las cifras provienen de /reportes/dashboard (datos persistidos).
import { Link } from 'react-router-dom';
import { BarChart, Bar, CartesianGrid, LineChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useDashboard } from '../hooks/useApi';
import { formatCOP, formatFecha, formatNumero } from '../lib/format';
import { num } from '../types/api';
import { Cargando, ErrorAlCargar, Tarjeta, Vacio } from '../components/ui';

function Indicador({ titulo, valor, detalle }: { titulo: string; valor: string; detalle?: string }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
      <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">{titulo}</p>
      <p className="mt-1 text-xl font-bold text-gray-900">{valor}</p>
      {detalle && <p className="mt-0.5 text-xs text-gray-500">{detalle}</p>}
    </div>
  );
}

export default function DashboardPage() {
  const { data, isLoading, error, refetch } = useDashboard();

  if (isLoading) return <Cargando mensaje="Cargando indicadores…" />;
  if (error) return <ErrorAlCargar error={error} onReintentar={() => void refetch()} />;
  if (!data) return <Vacio />;

  const serie = data.serieUltimos30Dias.map((p) => ({
    dia: formatFecha(p.fecha), total: num(p.total), transacciones: p.transacciones,
  }));

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-4">
        <Indicador titulo="Ventas de hoy" valor={formatCOP(data.ventasHoy)} detalle={`${formatNumero(data.transaccionesHoy)} transacciones`} />
        <Indicador titulo="Ventas del mes" valor={formatCOP(data.ventasMes)} />
        <Indicador titulo="Ticket promedio (mes)" valor={formatCOP(data.ticketPromedioMes)} />
        <Indicador titulo="Ganancia bruta estimada (mes)" valor={formatCOP(data.gananciaBrutaMes)}
          detalle="Ingresos − costo de lo vendido; no es utilidad neta" />
        <Indicador titulo="Unidades vendidas (mes)" valor={formatNumero(data.unidadesVendidasMes)} />
        <Indicador titulo="Compras del mes" valor={formatNumero(data.comprasMes)} />
        <Indicador titulo="Inventario a costo" valor={formatCOP(data.valorInventarioCosto)} />
        <Indicador titulo="Alertas de stock" valor={`${formatNumero(data.variantesBajoMinimo)} bajo mínimo`}
          detalle={`${formatNumero(data.variantesAgotadas)} agotadas`} />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Tarjeta titulo="Ventas — últimos 30 días">
          {serie.length === 0 ? <Vacio mensaje="Aún no hay ventas registradas." /> : (
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={serie}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
                  <XAxis dataKey="dia" tick={{ fontSize: 11 }} interval="preserveStartEnd" />
                  <YAxis tick={{ fontSize: 11 }} tickFormatter={(v: number) => `$${(v / 1000).toFixed(0)}k`} />
                  <Tooltip formatter={(v) => formatCOP(Number(v))} />
                  <Line type="monotone" dataKey="total" name="Ventas" stroke="#7e22ce" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </Tarjeta>

        <Tarjeta titulo="Ingresos por método de pago (mes)">
          {data.ingresosPorMetodoMes.length === 0 ? <Vacio mensaje="Sin pagos registrados este mes." /> : (
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data.ingresosPorMetodoMes.map((m) => ({ metodo: m.metodo, total: num(m.total) }))}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
                  <XAxis dataKey="metodo" tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 11 }} tickFormatter={(v: number) => `$${(v / 1000).toFixed(0)}k`} />
                  <Tooltip formatter={(v) => formatCOP(Number(v))} />
                  <Bar dataKey="total" name="Ingresos" fill="#6b21a8" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </Tarjeta>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Tarjeta titulo="Productos más vendidos (mes)" accion={<Link to="/reportes" className="text-sm font-medium text-marca-700 hover:underline">Ver reportes</Link>}>
          {data.topProductos.length === 0 ? <Vacio mensaje="Sin ventas este período." /> : (
            <ul className="divide-y divide-gray-100 text-sm">
              {data.topProductos.slice(0, 8).map((p) => (
                <li key={`${p.sku}`} className="flex items-center justify-between py-2">
                  <span className="truncate pr-2">{p.producto} <span className="text-gray-500">({p.talla} · {p.color})</span></span>
                  <span className="whitespace-nowrap font-medium">{p.unidades} u · {formatCOP(p.ingresos)}</span>
                </li>
              ))}
            </ul>
          )}
        </Tarjeta>

        <Tarjeta titulo="Accesos rápidos">
          <div className="grid grid-cols-2 gap-3">
            <Link to="/pos" className="rounded-lg bg-marca-700 px-4 py-3 text-center text-sm font-semibold text-white hover:bg-marca-800">Nueva venta (POS)</Link>
            <Link to="/productos/nuevo" className="rounded-lg border border-gray-300 bg-white px-4 py-3 text-center text-sm font-semibold text-gray-800 hover:bg-gray-50">Crear producto</Link>
            <Link to="/compras" className="rounded-lg border border-gray-300 bg-white px-4 py-3 text-center text-sm font-semibold text-gray-800 hover:bg-gray-50">Compras</Link>
            <Link to="/inventario" className="rounded-lg border border-gray-300 bg-white px-4 py-3 text-center text-sm font-semibold text-gray-800 hover:bg-gray-50">Inventario</Link>
          </div>
        </Tarjeta>
      </div>
    </div>
  );
}
