// Componentes reutilizables de la interfaz administrativa.
import { useEffect, useRef, useState, type ReactNode } from 'react';
import { errorMessage } from '../lib/apiClient';

// ---------- Botones ----------
type VarianteBoton = 'primario' | 'secundario' | 'peligro' | 'exito' | 'fantasma';

const ESTILOS_BOTON: Record<VarianteBoton, string> = {
  primario: 'bg-marca-700 text-white hover:bg-marca-800 focus-visible:ring-marca-400',
  secundario: 'bg-white text-gray-800 border border-gray-300 hover:bg-gray-50 focus-visible:ring-marca-300',
  peligro: 'bg-red-700 text-white hover:bg-red-800 focus-visible:ring-red-400',
  exito: 'bg-emerald-700 text-white hover:bg-emerald-800 focus-visible:ring-emerald-400',
  fantasma: 'bg-transparent text-gray-700 hover:bg-gray-100 focus-visible:ring-gray-300',
};

export function Boton({
  variante = 'primario', cargando = false, children, className = '', ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variante?: VarianteBoton; cargando?: boolean }) {
  return (
    <button
      {...props}
      disabled={props.disabled || cargando}
      className={`inline-flex items-center justify-center gap-2 rounded-lg px-3.5 py-2 text-sm font-medium
        transition focus:outline-none focus-visible:ring-2 disabled:cursor-not-allowed disabled:opacity-50
        ${ESTILOS_BOTON[variante]} ${className}`}
    >
      {cargando && <span aria-hidden className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />}
      {children}
    </button>
  );
}

// ---------- Campos de formulario ----------
export function Campo({ etiqueta: texto, error, children, hint }:
  { etiqueta: string; error?: string; children: ReactNode; hint?: string }) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block font-medium text-gray-700">{texto}</span>
      {children}
      {hint && !error && <span className="mt-1 block text-xs text-gray-500">{hint}</span>}
      {error && <span role="alert" className="mt-1 block text-xs font-medium text-red-700">{error}</span>}
    </label>
  );
}

export const claseInput = (invalido?: boolean) =>
  `w-full rounded-lg border px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-2
   ${invalido ? 'border-red-400 focus:ring-red-300' : 'border-gray-300 focus:border-marca-500 focus:ring-marca-200'}`;

// ---------- Estados ----------
export function Cargando({ mensaje = 'Cargando…' }: { mensaje?: string }) {
  return (
    <div role="status" aria-live="polite" className="flex items-center justify-center gap-3 py-10 text-gray-600">
      <span className="h-6 w-6 animate-spin rounded-full border-2 border-marca-600 border-t-transparent" />
      {mensaje}
    </div>
  );
}

export function ErrorAlCargar({ error, onReintentar }: { error: unknown; onReintentar?: () => void }) {
  return (
    <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
      <p className="font-semibold">Ocurrió un problema al cargar la información</p>
      <p className="mt-1">{errorMessage(error)}</p>
      {onReintentar && (
        <button onClick={onReintentar} className="mt-2 rounded border border-red-300 px-3 py-1 font-medium hover:bg-red-100">
          Reintentar
        </button>
      )}
    </div>
  );
}

export function Vacio({ mensaje = 'No hay registros para mostrar.' }: { mensaje?: string }) {
  return (
    <div className="rounded-lg border border-dashed border-gray-300 bg-gray-50 p-8 text-center text-sm text-gray-600">
      {mensaje}
    </div>
  );
}

// ---------- Notificaciones (toasts) ----------
interface Toast { id: number; tipo: 'exito' | 'error' | 'info'; mensaje: string; }
let contadorToast = 0;
const suscriptores = new Set<(t: Toast[]) => void>();
let toasts: Toast[] = [];

function publicar() { suscriptores.forEach((fn) => fn([...toasts])); }

export const notificar = {
  exito: (m: string) => agregar('exito', m),
  error: (m: string) => agregar('error', m),
  info: (m: string) => agregar('info', m),
};

function agregar(tipo: Toast['tipo'], mensaje: string) {
  const t: Toast = { id: ++contadorToast, tipo, mensaje };
  toasts = [...toasts, t];
  publicar();
  setTimeout(() => { toasts = toasts.filter((x) => x.id !== t.id); publicar(); }, 5000);
}

export function ContenedorToasts() {
  const [lista, setLista] = useState<Toast[]>([]);
  useEffect(() => {
    suscriptores.add(setLista);
    return () => { suscriptores.delete(setLista); };
  }, []);
  return (
    <div aria-live="polite" className="fixed bottom-4 right-4 z-[100] flex w-80 flex-col gap-2">
      {lista.map((t) => (
        <div key={t.id} role="status"
          className={`rounded-lg px-4 py-3 text-sm font-medium text-white shadow-lg
            ${t.tipo === 'exito' ? 'bg-emerald-700' : t.tipo === 'error' ? 'bg-red-700' : 'bg-gray-800'}`}>
          {t.mensaje}
        </div>
      ))}
    </div>
  );
}

// ---------- Modal con confirmación para operaciones sensibles ----------
export function Confirmar({ abierto, titulo, descripcion, confirmarTexto = 'Confirmar', peligroso = false,
  cargando = false, onConfirmar, onCancelar }: {
  abierto: boolean; titulo: string; descripcion: ReactNode; confirmarTexto?: string;
  peligroso?: boolean; cargando?: boolean; onConfirmar: () => void; onCancelar: () => void;
}) {
  const ref = useRef<HTMLButtonElement>(null);
  useEffect(() => { if (abierto) ref.current?.focus(); }, [abierto]);
  if (!abierto) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" onClick={onCancelar}>
      <div role="dialog" aria-modal="true" aria-label={titulo}
        className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}>
        <h2 className="text-lg font-semibold text-gray-900">{titulo}</h2>
        <div className="mt-2 text-sm text-gray-700">{descripcion}</div>
        <div className="mt-5 flex justify-end gap-2">
          <Boton ref={undefined} variante="secundario" onClick={onCancelar}>Cancelar</Boton>
          <Boton variante={peligroso ? 'peligro' : 'primario'} cargando={cargando} onClick={onConfirmar}>
            {confirmarTexto}
          </Boton>
        </div>
      </div>
    </div>
  );
}

// ---------- Modal genérico ----------
export function Modal({ abierto, titulo, children, ancho = 'max-w-2xl', onClose }:
  { abierto: boolean; titulo: string; children: ReactNode; ancho?: string; onClose: () => void }) {
  if (!abierto) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-4" onClick={onClose}>
      <div role="dialog" aria-modal="true" aria-label={titulo}
        className={`mt-8 w-full ${ancho} rounded-xl bg-white p-6 shadow-xl`} onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900">{titulo}</h2>
          <button aria-label="Cerrar" onClick={onClose} className="rounded p-1 text-gray-500 hover:bg-gray-100">✕</button>
        </div>
        <div className="mt-4">{children}</div>
      </div>
    </div>
  );
}

// ---------- Tarjeta ----------
export function Tarjeta({ titulo, accion, children }: { titulo?: string; accion?: ReactNode; children: ReactNode }) {
  return (
    <section className="rounded-xl border border-gray-200 bg-white shadow-sm">
      {(titulo || accion) && (
        <header className="flex items-center justify-between border-b border-gray-100 px-5 py-3">
          {titulo && <h2 className="font-semibold text-gray-800">{titulo}</h2>}
          {accion}
        </header>
      )}
      <div className="p-5">{children}</div>
    </section>
  );
}

// ---------- Etiquetas de estado ----------
const COLORES_ESTADO: Record<string, string> = {
  RECIBIDA: 'bg-emerald-100 text-emerald-800', CONFIRMADA: 'bg-emerald-100 text-emerald-800',
  COMPLETA: 'bg-emerald-100 text-emerald-800', PAGADA: 'bg-emerald-100 text-emerald-800',
  ABIERTA: 'bg-sky-100 text-sky-800', CERRADA: 'bg-gray-200 text-gray-700',
  BORRADOR: 'bg-amber-100 text-amber-800', PENDIENTE: 'bg-amber-100 text-amber-800',
  PENDIENTE_PAGO: 'bg-amber-100 text-amber-800', PARCIAL: 'bg-orange-100 text-orange-800',
  ANULADA: 'bg-red-100 text-red-800', CANCELADA: 'bg-red-100 text-red-800',
  AGOTADO: 'bg-red-100 text-red-800', BAJO: 'bg-amber-100 text-amber-800',
};

export function EtiquetaEstado({ valor, texto }: { valor: string; texto: string }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${COLORES_ESTADO[valor] ?? 'bg-gray-100 text-gray-700'}`}>
      {texto}
    </span>
  );
}

// ---------- Paginador ----------
export function Paginador({ pagina, totalPaginas, alCambiar }:
  { pagina: number; totalPaginas: number; alCambiar: (p: number) => void }) {
  if (totalPaginas <= 1) return null;
  return (
    <nav aria-label="Paginación" className="mt-4 flex items-center justify-between text-sm">
      <Boton variante="secundario" disabled={pagina === 0} onClick={() => alCambiar(pagina - 1)}>← Anterior</Boton>
      <span className="text-gray-600">Página {pagina + 1} de {totalPaginas}</span>
      <Boton variante="secundario" disabled={pagina + 1 >= totalPaginas} onClick={() => alCambiar(pagina + 1)}>Siguiente →</Boton>
    </nav>
  );
}

// ---------- Tabla adaptable ----------
export function Tabla({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <div className={`overflow-x-auto rounded-lg border border-gray-200 ${className}`}>
      <table className="min-w-full divide-y divide-gray-200 text-sm">{children}</table>
    </div>
  );
}

export function Cabecera({ children }: { children: ReactNode }) {
  return <thead className="bg-gray-50 text-left text-xs font-semibold uppercase tracking-wide text-gray-600">{children}</thead>;
}

export function Fila({ children, onClick }: { children: ReactNode; onClick?: () => void }) {
  return (
    <tr onClick={onClick} className={`divide-x divide-gray-100 bg-white odd:bg-white even:bg-gray-50 ${onClick ? 'cursor-pointer hover:bg-marca-50' : ''}`}>
      {children}
    </tr>
  );
}

export function Celda({ children, className = '' }: { children?: ReactNode; className?: string }) {
  return <td className={`whitespace-nowrap px-4 py-2.5 align-middle ${className}`}>{children}</td>;
}
