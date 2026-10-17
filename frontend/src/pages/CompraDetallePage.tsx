// Detalle de una compra: líneas, estado y acciones de recepción/cancelación.
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useCambiarEstadoCompra, useCompra, useRecibirCompra } from '../hooks/useApi';
import { errorMessage } from '../lib/apiClient';
import { etiqueta, formatCOP, formatFechaHora, formatNumero } from '../lib/format';
import { num } from '../types/api';
import {
  Boton, Cabecera, Cargando, Celda, Confirmar, ErrorAlCargar, EtiquetaEstado, Fila,
  Tabla, Vacio, notificar,
} from '../components/ui';
import { useState } from 'react';

export default function CompraDetallePage() {
  const { id } = useParams();
  const compraId = Number(id);
  const { tieneRol } = useAuth();
  const { data: compra, isLoading, error, refetch } = useCompra(compraId);
  const recibir = useRecibirCompra();
  const cambiarEstado = useCambiarEstadoCompra();
  const [confRecibir, setConfRecibir] = useState(false);

  if (isLoading) return <Cargando />;
  if (error) return <ErrorAlCargar error={error} onReintentar={() => void refetch()} />;
  if (!compra) return <Vacio />;

  const editable = tieneRol('ADMIN', 'INVENTARIO');

  return (
    <div className="space-y-5">
      <div>
        <Link to="/compras" className="text-sm text-marca-700 hover:underline">← Compras</Link>
        <div className="mt-1 flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-bold text-gray-900">Compra {compra.numero}</h1>
          <EtiquetaEstado valor={compra.estado} texto={etiqueta(compra.estado)} />
        </div>
        <p className="mt-1 text-sm text-gray-600">
          Proveedor: <strong>{compra.proveedorNombre}</strong> · Registrada: {formatFechaHora(compra.fechaRegistro)}
          {compra.fechaEspera ? ` · Esperada: ${compra.fechaEspera}` : ''}
        </p>
        {compra.observaciones && <p className="mt-1 text-sm text-gray-600">Observaciones: {compra.observaciones}</p>}
      </div>

      <Tabla>
        <Cabecera>
          <Fila><Celda>Producto / SKU</Celda><Celda>Talla</Celda><Celda>Color</Celda><Celda>Cantidad</Celda><Celda>Costo unit.</Celda><Celda>Subtotal</Celda></Fila>
        </Cabecera>
        <tbody>
          {compra.detalles.map((d) => (
            <Fila key={d.id}>
              <Celda>{d.productoNombre}<span className="block font-mono text-xs text-gray-500">{d.sku}</span></Celda>
              <Celda>{d.talla}</Celda>
              <Celda>{d.color}</Celda>
              <Celda>{formatNumero(d.cantidad)}</Celda>
              <Celda>{formatCOP(d.costoUnitario)}</Celda>
              <Celda className="font-medium">{formatCOP(d.subtotal)}</Celda>
            </Fila>
          ))}
        </tbody>
      </Tabla>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-lg font-bold text-gray-900">Total: {formatCOP(num(compra.total))}</p>
        {editable && (
          <div className="flex gap-2">
            {compra.estado === 'BORRADOR' && (
              <>
                <Boton variante="secundario" onClick={async () => {
                  try { await cambiarEstado.mutateAsync({ id: compra.id, estado: 'PENDIENTE' }); notificar.info('Compra marcada como pendiente de recepción.'); void refetch(); }
                  catch (e) { notificar.error(errorMessage(e)); }
                }}>Marcar pendiente</Boton>
                <Boton variante="exito" onClick={() => setConfRecibir(true)}>Recibir mercancía</Boton>
              </>
            )}
            {compra.estado === 'PENDIENTE' && (
              <Boton variante="exito" onClick={() => setConfRecibir(true)}>Recibir mercancía</Boton>
            )}
            {(compra.estado === 'BORRADOR' || compra.estado === 'PENDIENTE') && (
              <Boton variante="peligro" onClick={async () => {
                try { await cambiarEstado.mutateAsync({ id: compra.id, estado: 'CANCELADA' }); notificar.info('Compra cancelada.'); void refetch(); }
                catch (e) { notificar.error(errorMessage(e)); }
              }}>Cancelar compra</Boton>
            )}
          </div>
        )}
      </div>

      <Confirmar
        abierto={confRecibir}
        titulo="Recepción de mercancía"
        descripcion="Se incrementarán las existencias según las cantidades recibidas y quedará registrado el movimiento de inventario. Esta operación no puede repetirse."
        confirmarTexto="Confirmar recepción"
        cargando={recibir.isPending}
        onCancelar={() => setConfRecibir(false)}
        onConfirmar={async () => {
          try {
            const c = await recibir.mutateAsync(compra.id);
            notificar.exito(`Compra ${c.numero} recibida correctamente.`);
            void refetch();
          } catch (e) { notificar.error(errorMessage(e)); }
          setConfRecibir(false);
        }}
      />
    </div>
  );
}
