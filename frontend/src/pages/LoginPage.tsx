// Inicio de sesión contra /auth/login del backend. Sin credenciales simuladas.
import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { errorMessage } from '../lib/apiClient';
import { Boton, Campo, claseInput } from '../components/ui';

export default function LoginPage() {
  const { usuario, iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const loc = useLocation() as { state?: { desde?: string } };
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (usuario) return <Navigate to={loc.state?.desde ?? '/'} replace />;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!username.trim() || !password) {
      setError('Ingrese usuario y contraseña.');
      return;
    }
    setEnviando(true);
    try {
      await iniciarSesion(username.trim(), password);
      navigate(loc.state?.desde ?? '/', { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-marca-950 p-4">
      <form
        onSubmit={onSubmit}
        className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-xl"
        aria-label="Formulario de inicio de sesión">
        <h1 className="text-center text-2xl font-bold text-marca-800">Tienda de Ropa</h1>
        <p className="mt-1 text-center text-sm text-gray-500">Sistema de administración</p>

        <div className="mt-6 space-y-4">
          <Campo etiqueta="Usuario">
            <input
              className={claseInput(!!error)}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              autoFocus
              placeholder="ej. admin" />
          </Campo>
          <Campo etiqueta="Contraseña">
            <input
              type="password"
              className={claseInput(!!error)}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              placeholder="••••••••" />
          </Campo>
        </div>

        {error && (
          <p role="alert" className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
            {error}
          </p>
        )}

        <Boton type="submit" cargando={enviando} className="mt-6 w-full">
          Iniciar sesión
        </Boton>
      </form>
    </div>
  );
}
