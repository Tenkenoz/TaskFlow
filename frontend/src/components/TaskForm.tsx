import { useState } from 'react';
import type { FormEvent } from 'react';
import { getErrorMessage } from '../api/client';
import type { Task, TaskPayload, TaskPriority, TaskStatus } from '../types';

interface TaskFormProps {
  initialTask: Task | null;
  onSubmit: (payload: TaskPayload) => Promise<void>;
  onCancelEdit?: () => void;
}

export default function TaskForm({ initialTask, onSubmit, onCancelEdit }: TaskFormProps) {
  const [title, setTitle] = useState(initialTask?.title ?? '');
  const [description, setDescription] = useState(initialTask?.description ?? '');
  const [priority, setPriority] = useState<TaskPriority>(initialTask?.priority ?? 'MEDIUM');
  const [status, setStatus] = useState<TaskStatus>(initialTask?.status ?? 'TODO');
  const [dueDate, setDueDate] = useState(initialTask?.dueDate ?? '');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const editing = Boolean(initialTask);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!title.trim()) {
      setError('El título es obligatorio.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await onSubmit({
        title: title.trim(),
        description: description.trim() ? description.trim() : null,
        priority,
        status,
        dueDate: dueDate || null,
      });

      if (!editing) {
        setTitle('');
        setDescription('');
        setPriority('MEDIUM');
        setStatus('TODO');
        setDueDate('');
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="form form--compact" onSubmit={handleSubmit} noValidate>
      {error && (
        <div className="alert alert--error" role="alert">
          {error}
        </div>
      )}

      <label className="field">
        <span>Título</span>
        <input
          type="text"
          name="title"
          placeholder="¿Qué necesitas hacer?"
          maxLength={150}
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          required
        />
      </label>

      <label className="field">
        <span>Descripción</span>
        <textarea
          name="description"
          rows={3}
          maxLength={2000}
          placeholder="Añade contexto (opcional)"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
        />
      </label>

      <div className="form-row">
        <label className="field">
          <span>Prioridad</span>
          <select
            name="priority"
            value={priority}
            onChange={(event) => setPriority(event.target.value as TaskPriority)}
          >
            <option value="LOW">Baja</option>
            <option value="MEDIUM">Media</option>
            <option value="HIGH">Alta</option>
          </select>
        </label>

        <label className="field">
          <span>Estado</span>
          <select
            name="status"
            value={status}
            onChange={(event) => setStatus(event.target.value as TaskStatus)}
          >
            <option value="TODO">Por hacer</option>
            <option value="IN_PROGRESS">En progreso</option>
            <option value="DONE">Completada</option>
          </select>
        </label>
      </div>

      <label className="field">
        <span>Fecha límite</span>
        <input
          type="date"
          name="dueDate"
          value={dueDate}
          onChange={(event) => setDueDate(event.target.value)}
        />
      </label>

      <div className="form-actions">
        <button className="btn btn--primary" type="submit" disabled={submitting}>
          {submitting ? 'Guardando…' : editing ? 'Guardar cambios' : 'Añadir tarea'}
        </button>
        {editing && onCancelEdit && (
          <button className="btn btn--ghost" type="button" onClick={onCancelEdit}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  );
}
