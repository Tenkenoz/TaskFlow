import type { TaskPriority, TaskStatus } from '../types';

export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'Por hacer',
  IN_PROGRESS: 'En progreso',
  DONE: 'Completada',
};

export const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Baja',
  MEDIUM: 'Media',
  HIGH: 'Alta',
};

export function formatDueDate(value: string): string {
  const date = new Date(`${value}T00:00:00`);
  return new Intl.DateTimeFormat('es', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

export function isOverdue(dueDate: string | null, status: TaskStatus): boolean {
  if (!dueDate || status === 'DONE') {
    return false;
  }
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return new Date(`${dueDate}T00:00:00`) < today;
}
