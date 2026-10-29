import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

interface AuthLayoutProps {
  title: string;
  subtitle: string;
  children: ReactNode;
  footer: ReactNode;
}

export default function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps) {
  return (
    <div className="auth-page">
      <aside className="auth-hero">
        <Link to="/" className="brand brand--light">
          <span className="brand__mark" aria-hidden="true">
            ✓
          </span>
          <span className="brand__name">TaskFlow</span>
        </Link>

        <div className="auth-hero__copy">
          <span className="eyebrow eyebrow--light">Gestor de tareas</span>
          <h1>
            Ordena tu día,
            <br />
            una tarea a la vez.
          </h1>
          <p>
            Crea, prioriza y completa tus tareas en un solo lugar. Tus datos son
            privados: cada cuenta ve únicamente sus propias tareas.
          </p>
        </div>

        <ul className="auth-hero__features">
          <li>
            <span aria-hidden="true">🔐</span> Autenticación con JWT
          </li>
          <li>
            <span aria-hidden="true">⚡</span> React + Spring Boot
          </li>
          <li>
            <span aria-hidden="true">🐳</span> Todo con un solo comando
          </li>
        </ul>
      </aside>

      <main className="auth-panel">
        <div className="auth-card">
          <div className="auth-card__heading">
            <h2>{title}</h2>
            <p className="muted">{subtitle}</p>
          </div>
          {children}
          <p className="auth-switch">{footer}</p>
        </div>
      </main>
    </div>
  );
}
