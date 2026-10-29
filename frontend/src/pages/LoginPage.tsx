import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import AuthLayout from '../components/AuthLayout';

export default function LoginPage() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await login({ email: email.trim(), password });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Inicia sesión"
      subtitle="Bienvenido de nuevo. Tus tareas te esperan."
      footer={
        <>
          ¿Aún no tienes cuenta? <Link to="/register">Regístrate gratis</Link>
        </>
      }
    >
      <form className="form" onSubmit={handleSubmit} noValidate>
        {error && (
          <div className="alert alert--error" role="alert">
            {error}
          </div>
        )}

        <label className="field">
          <span>Email</span>
          <input
            type="email"
            name="email"
            autoComplete="email"
            placeholder="tu@email.com"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </label>

        <label className="field">
          <span>Contraseña</span>
          <input
            type="password"
            name="password"
            autoComplete="current-password"
            placeholder="••••••••"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>

        <button className="btn btn--primary btn--block" type="submit" disabled={submitting}>
          {submitting ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </AuthLayout>
  );
}
