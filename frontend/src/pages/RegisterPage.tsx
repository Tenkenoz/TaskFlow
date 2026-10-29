import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import AuthLayout from '../components/AuthLayout';

export default function RegisterPage() {
  const { register } = useAuth();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await register({ name: name.trim(), email: email.trim(), password });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Crea tu cuenta"
      subtitle="Tarda menos de un minuto y empieza a organizarte."
      footer={
        <>
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
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
          <span>Nombre</span>
          <input
            type="text"
            name="name"
            autoComplete="name"
            placeholder="Tu nombre"
            value={name}
            onChange={(event) => setName(event.target.value)}
            required
          />
        </label>

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
            autoComplete="new-password"
            placeholder="Mínimo 6 caracteres"
            minLength={6}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>

        <button className="btn btn--primary btn--block" type="submit" disabled={submitting}>
          {submitting ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
      </form>
    </AuthLayout>
  );
}
