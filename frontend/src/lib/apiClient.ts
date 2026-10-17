// Cliente HTTP centralizado: Axios + token JWT + manejo uniforme de errores.
import axios, { AxiosError } from 'axios';

export interface ApiErrorField { campo: string; mensaje: string; }
export interface ApiErrorShape { status?: number; error?: string; message?: string; campos?: ApiErrorField[]; timestamp?: string; }

export const TOKEN_STORAGE_KEY = 'tienda-ropa-token';

const baseURL = (import.meta.env.VITE_API_URL as string | undefined) ?? '/api/v1';

export const api = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

// El token se conserva en memoria; sessionStorage solo como respaldo por recarga.
let memoryToken: string | null = null;

export function setToken(token: string | null): void {
  memoryToken = token;
  if (token) sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
  else sessionStorage.removeItem(TOKEN_STORAGE_KEY);
}

export function getToken(): string | null {
  if (memoryToken == null) memoryToken = sessionStorage.getItem(TOKEN_STORAGE_KEY);
  return memoryToken;
}

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

type Listener = () => void;
const expirateListeners = new Set<Listener>();
/** Suscribe acción cuando el backend responde 401 (sesión expirada o inválida). */
export function onSessionExpired(fn: Listener): () => void {
  expirateListeners.add(fn);
  return () => expirateListeners.delete(fn);
}

api.interceptors.response.use(
  (r) => r,
  (error: AxiosError<ApiErrorShape>) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      setToken(null);
      expirateListeners.forEach((fn) => fn());
    }
    return Promise.reject(error);
  },
);

/** Extrae un mensaje legible y seguro desde una respuesta de error del backend. */
export function errorMessage(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const data = e.response?.data as ApiErrorShape | undefined;
    if (data?.campos && data.campos.length > 0) {
      return `${data.message}: ${data.campos.map((c) => `${c.campo} — ${c.mensaje}`).join(' · ')}`;
    }
    if (data?.message) return data.message;
    if (e.code === 'ECONNABORTED') return 'El servidor tardó demasiado en responder. Intente de nuevo.';
    if (!e.response) return 'No hay conexión con el servidor. Verifique que el backend esté activo.';
    if (e.response.status === 403) return 'No tiene permisos para ejecutar esta operación.';
    return `Error ${e.response.status} del servidor.`;
  }
  return e instanceof Error ? e.message : 'Error inesperado.';
}

export interface ApiErrorField { campo: string; mensaje: string; }
export interface ApiErrorShape { status?: number; error?: string; message?: string; campos?: ApiErrorField[]; timestamp?: string; }
