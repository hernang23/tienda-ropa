// Utilidades de formato regional (Colombia) — COP, fechas y números.
const copFormatter = new Intl.NumberFormat('es-CO', {
  style: 'currency', currency: 'COP', maximumFractionDigits: 0,
});

const copFormatterCentavos = new Intl.NumberFormat('es-CO', {
  style: 'currency', currency: 'COP', minimumFractionDigits: 2, maximumFractionDigits: 2,
});

/** Formatea pesos colombianos. Sin fracción salvo que el valor tenga centavos. */
export function formatCOP(value: number | string | null | undefined): string {
  const n = typeof value === 'string' ? Number(value) : value ?? 0;
  if (!Number.isFinite(n)) return '$0';
  const formatter = Math.abs(n % 1) > 0.004 ? copFormatterCentavos : copFormatter;
  return formatter.format(n);
}

export function formatNumero(value: number | null | undefined): string {
  return new Intl.NumberFormat('es-CO').format(value ?? 0);
}

/** Convierte un ISO string del backend a fecha-hora local legible. */
export function formatFechaHora(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
}

export function formatFecha(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString('es-CO', { dateStyle: 'medium' });
}

/** yyyy-MM-dd local para inputs[type=date] y parámetros de filtro. */
export function toISODate(d: Date): string {
  const pad = (x: number) => String(x).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function hoyISO(): string { return toISODate(new Date()); }

export function hace30DiasISO(): string {
  const d = new Date();
  d.setDate(d.getDate() - 29);
  return toISODate(d);
}

/** Traduce estados/enums del backend a texto en español. */
const ETIQUETAS: Record<string, string> = {
  BORRADOR: 'Borrador', PENDIENTE: 'Pendiente de recepción', RECIBIDA: 'Recibida', CANCELADA: 'Cancelada',
  CONFIRMADA: 'Confirmada', COMPLETA: 'Completa', ANULADA: 'Anulada', ABIERTA: 'Abierta', CERRADA: 'Cerrada',
  PENDIENTE_PAGO: 'Pendiente de pago', PARCIAL: 'Pago parcial', PAGADA: 'Pagada', ACTIVA: 'Activa',
  ENTRADA: 'Entrada', SALIDA: 'Salida', AJUSTE: 'Ajuste', DEVOLUCION: 'Devolución',
  EFECTIVO: 'Efectivo', TRANSFERENCIA: 'Transferencia', TARJETA: 'Tarjeta', OTRO: 'Otro',
  REEMBOLSO: 'Reembolso', CAMBIO: 'Cambio', SALDO_FAVOR: 'Saldo a favor',
  BUENO: 'Buen estado', DANADO: 'Dañado', NATURAL: 'Persona natural', EMPRESA: 'Empresa',
  INGRESO: 'Ingreso', EGRESO: 'Egreso', PROCESADO: 'Procesado',
};

export function etiqueta(v: string | null | undefined): string {
  if (!v) return '—';
  return ETIQUETAS[v] ?? v.charAt(0) + v.slice(1).toLowerCase();
}
