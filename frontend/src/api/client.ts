import axios from 'axios';

export const TOKEN_KEY = 'taskflow.token';
export const USER_KEY = 'taskflow.user';
export const UNAUTHORIZED_EVENT = 'taskflow:unauthorized';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url: string = error.config?.url ?? '';
    if (status === 401 && !url.includes('/auth/')) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    return Promise.reject(error);
  },
);

interface ApiErrorBody {
  message?: string;
  errors?: Record<string, string>;
}

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    const body = error.response?.data;
    if (body?.errors && Object.keys(body.errors).length > 0) {
      return Object.values(body.errors).join(' ');
    }
    if (body?.message) {
      return body.message;
    }
    if (!error.response) {
      return 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
    }
  }
  return 'Ha ocurrido un error inesperado. Inténtalo de nuevo.';
}

export default api;
