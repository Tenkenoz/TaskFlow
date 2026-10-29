import { useCallback, useEffect, useMemo, useState } from 'react';
import api, { getErrorMessage } from '../api/client';
import FilterBar from '../components/FilterBar';
import Header from '../components/Header';
import TaskCard from '../components/TaskCard';
import TaskForm from '../components/TaskForm';
import { useAuth } from '../context/AuthContext';
import type { Task, TaskFilters, TaskPayload } from '../types';

const EMPTY_FILTERS: TaskFilters = { status: '', priority: '' };

export default function DashboardPage() {
  const { user } = useAuth();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [filters, setFilters] = useState<TaskFilters>(EMPTY_FILTERS);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [busyTaskId, setBusyTaskId] = useState<number | null>(null);

  const hasFilters = filters.status !== '' || filters.priority !== '';

  const loadTasks = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const params: Record<string, string> = {};
      if (filters.status) {
        params.status = filters.status;
      }
      if (filters.priority) {
        params.priority = filters.priority;
      }

      const { data } = await api.get<Task[]>('/tasks', { params });
      setTasks(data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [filters.status, filters.priority]);

  useEffect(() => {
    void loadTasks();
  }, [loadTasks]);

  async function handleCreate(payload: TaskPayload) {
    await api.post<Task>('/tasks', payload);
    await loadTasks();
  }

  async function handleUpdate(payload: TaskPayload) {
    if (!editingTask) {
      return;
    }
    await api.put<Task>(`/tasks/${editingTask.id}`, payload);
    setEditingTask(null);
    await loadTasks();
  }

  async function handleToggle(task: Task) {
    setBusyTaskId(task.id);
    setError(null);

    try {
      const { data } = await api.patch<Task>(`/tasks/${task.id}/toggle`);
      setTasks((current) => {
        // Si hay un filtro por estado, la tarea puede dejar de coincidir.
        if (filters.status && data.status !== filters.status) {
          return current.filter((item) => item.id !== task.id);
        }
        return current.map((item) => (item.id === task.id ? data : item));
      });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusyTaskId(null);
    }
  }

  async function handleDelete(task: Task) {
    const confirmed = window.confirm(`¿Eliminar la tarea "${task.title}"?`);
    if (!confirmed) {
      return;
    }

    setBusyTaskId(task.id);
    setError(null);

    try {
      await api.delete(`/tasks/${task.id}`);
      setTasks((current) => current.filter((item) => item.id !== task.id));
      if (editingTask?.id === task.id) {
        setEditingTask(null);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setBusyTaskId(null);
    }
  }

  const stats = useMemo(() => {
    const done = tasks.filter((task) => task.status === 'DONE').length;
    return {
      total: tasks.length,
      done,
      open: tasks.length - done,
    };
  }, [tasks]);

  const firstName = user?.name?.split(' ')[0] ?? '';

  return (
    <div className="app-shell">
      <Header />

      <main className="dashboard">
        <section className="dashboard__intro">
          <div>
            <span className="eyebrow">Tu tablero</span>
            <h1>Hola, {firstName} 👋</h1>
            <p className="muted">
              {stats.open === 0
                ? 'No tienes tareas pendientes. ¡Buen trabajo!'
                : `Tienes ${stats.open} ${stats.open === 1 ? 'tarea pendiente' : 'tareas pendientes'} por delante.`}
            </p>
          </div>

          <div className="stats">
            <div className="stat-card">
              <span className="stat-card__label">Total</span>
              <strong>{stats.total}</strong>
            </div>
            <div className="stat-card stat-card--open">
              <span className="stat-card__label">Pendientes</span>
              <strong>{stats.open}</strong>
            </div>
            <div className="stat-card stat-card--done">
              <span className="stat-card__label">Completadas</span>
              <strong>{stats.done}</strong>
            </div>
          </div>
        </section>

        <div className="dashboard__grid">
          <aside className="panel panel--form">
            <div className="panel__heading">
              <h2>{editingTask ? 'Editar tarea' : 'Nueva tarea'}</h2>
              <p className="muted">
                {editingTask
                  ? 'Actualiza los datos y guarda los cambios.'
                  : 'Añade una tarea y organízate mejor.'}
              </p>
            </div>

            <TaskForm
              key={editingTask?.id ?? 'new'}
              initialTask={editingTask}
              onSubmit={editingTask ? handleUpdate : handleCreate}
              onCancelEdit={editingTask ? () => setEditingTask(null) : undefined}
            />
          </aside>

          <section className="panel panel--list">
            <div className="list-heading">
              <h2>Tareas</h2>
              <span className="count-pill">{tasks.length}</span>
            </div>

            <FilterBar
              filters={filters}
              onChange={setFilters}
              onClear={() => setFilters(EMPTY_FILTERS)}
            />

            {error && (
              <div className="alert alert--error" role="alert">
                {error}
              </div>
            )}

            {loading ? (
              <div className="task-list task-list--loading" aria-busy="true">
                <div className="skeleton" />
                <div className="skeleton" />
                <div className="skeleton" />
              </div>
            ) : tasks.length === 0 ? (
              <div className="empty-state">
                <span className="empty-state__icon" aria-hidden="true">
                  {hasFilters ? '🔎' : '🌱'}
                </span>
                <h3>{hasFilters ? 'Sin resultados' : 'Todo despejado'}</h3>
                <p className="muted">
                  {hasFilters
                    ? 'Ninguna tarea coincide con los filtros seleccionados.'
                    : 'Crea tu primera tarea con el formulario de la izquierda.'}
                </p>
                {hasFilters && (
                  <button
                    type="button"
                    className="btn btn--ghost btn--small"
                    onClick={() => setFilters(EMPTY_FILTERS)}
                  >
                    Limpiar filtros
                  </button>
                )}
              </div>
            ) : (
              <ul className="task-list">
                {tasks.map((task) => (
                  <TaskCard
                    key={task.id}
                    task={task}
                    busy={busyTaskId === task.id}
                    onToggle={handleToggle}
                    onEdit={setEditingTask}
                    onDelete={handleDelete}
                  />
                ))}
              </ul>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
