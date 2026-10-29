import { useAuth } from '../context/AuthContext';

export default function Header() {
  const { user, logout } = useAuth();
  const initials = user?.name
    ?.split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('');

  return (
    <header className="app-header">
      <div className="app-header__inner">
        <div className="brand">
          <span className="brand__mark" aria-hidden="true">
            ✓
          </span>
          <span className="brand__name">TaskFlow</span>
        </div>

        <div className="app-header__user">
          <span className="avatar" aria-hidden="true">
            {initials || '?'}
          </span>
          <span className="app-header__name">{user?.name}</span>
          <button type="button" className="btn btn--ghost btn--small" onClick={logout}>
            Salir
          </button>
        </div>
      </div>
    </header>
  );
}
