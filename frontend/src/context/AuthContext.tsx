import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import api, { TOKEN_KEY, UNAUTHORIZED_EVENT, USER_KEY } from '../api/client';
import type { AuthResponse, User } from '../types';

interface Credentials {
  email: string;
  password: string;
}

interface RegisterPayload extends Credentials {
  name: string;
}

interface AuthContextValue {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (payload: Credentials) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function readStoredUser(): User | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as User;
  } catch {
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState<User | null>(() => readStoredUser());

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    window.addEventListener(UNAUTHORIZED_EVENT, clearSession);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, clearSession);
  }, [clearSession]);

  const persistSession = useCallback((auth: AuthResponse) => {
    localStorage.setItem(TOKEN_KEY, auth.token);
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
    setToken(auth.token);
    setUser(auth.user);
  }, []);

  const login = useCallback(
    async (payload: Credentials) => {
      const { data } = await api.post<AuthResponse>('/auth/login', payload);
      persistSession(data);
    },
    [persistSession],
  );

  const register = useCallback(
    async (payload: RegisterPayload) => {
      const { data } = await api.post<AuthResponse>('/auth/register', payload);
      persistSession(data);
    },
    [persistSession],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token && user),
      login,
      register,
      logout: clearSession,
    }),
    [user, token, login, register, clearSession],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return context;
}
