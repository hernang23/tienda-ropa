// Contexto de autenticación basado en el backend real: perfil desde /auth/perfil,
// login/logout vía /auth/login y /auth/logout. Sin persistencia local de datos de negocio.
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { usePerfil, useLogin, logoutApi, authKeys } from '../hooks/useApi';
import { getToken, onSessionExpired } from '../lib/apiClient';
import type { UsuarioDto } from '../types/api';
import { useQueryClient } from '@tanstack/react-query';

interface AuthContextValue {
  usuario: UsuarioDto | null;
  cargando: boolean;
  tieneRol: (...roles: string[]) => boolean;
  esAdmin: boolean;
  iniciarSesion: (username: string, password: string) => Promise<void>;
  cerrarSesion: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient();
  const [hayToken, setHayToken] = useState<boolean>(() => getToken() != null);
  const perfil = usePerfil(hayToken);
  const login = useLogin();

  useEffect(() => onSessionExpired(() => setHayToken(false)), []);

  const iniciarSesion = useCallback(async (username: string, password: string) => {
    await login.mutateAsync({ username, password });
    setHayToken(true);
  }, [login]);

  const cerrarSesion = useCallback(async () => {
    await logoutApi();
    qc.clear();
    setHayToken(false);
  }, [qc]);

  // Si el perfil falla con 401, el interceptor ya limpió el token: sincronizamos estado.
  useEffect(() => {
    if (hayToken && perfil.isError && !getToken()) setHayToken(false);
  }, [hayToken, perfil.isError]);

  const valor = useMemo<AuthContextValue>(() => {
    const roles = perfil.data?.roles ?? [];
    return {
      usuario: perfil.data ?? null,
      cargando: hayToken && perfil.isLoading,
      tieneRol: (...r: string[]) => r.some((x) => roles.includes(x)),
      esAdmin: roles.includes('ADMIN'),
      iniciarSesion,
      cerrarSesion,
    };
  }, [perfil.data, perfil.isLoading, hayToken, iniciarSesion, cerrarSesion]);

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return ctx;
}

export { authKeys };
