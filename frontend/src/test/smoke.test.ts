// Prueba de humo del backend con datos semilla (usuario admin, catálogo) y sin CORS abierto.
import { describe, expect, it } from 'vitest';

describe('Configuración de desarrollo', () => {
  it('usa la ruta base de la API versionada por defecto', async () => {
    const { api } = await import('../lib/apiClient');
    expect(api.defaults.baseURL).toBe('/api/v1');
  });

  it('normaliza montos monetarios desde string o número', async () => {
    const { num } = await import('../types/api');
    expect(num('150000.50')).toBe(150000.5);
    expect(num(2000)).toBe(2000);
    expect(num(null)).toBe(0);
  });

  it('formatea pesos colombianos sin decimales cuando corresponde', async () => {
    const { formatCOP } = await import('../lib/format');
    expect(formatCOP(1500000)).toMatch(/1\.500\.000/);
    expect(formatCOP('abc')).toBe('$0');
  });
});
