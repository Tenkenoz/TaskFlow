export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface User {
  id: number;
  name: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface Task {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TaskPayload {
  title: string;
  description?: string | null;
  status?: TaskStatus;
  priority?: TaskPriority;
  dueDate?: string | null;
}

export interface TaskFilters {
  status: TaskStatus | '';
  priority: TaskPriority | '';
}
