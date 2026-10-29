import type { Task } from '../types';
import { PRIORITY_LABELS, STATUS_LABELS, formatDueDate, isOverdue } from '../lib/format';

interface TaskCardProps {
  task: Task;
  busy: boolean;
  onToggle: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDelete: (task: Task) => void;
}

export default function TaskCard({ task, busy, onToggle, onEdit, onDelete }: TaskCardProps) {
  const done = task.status === 'DONE';

  return (
    <li className={`task-card task-card--${task.priority.toLowerCase()} ${done ? 'is-done' : ''} ${busy ? 'is-busy' : ''}`}>
      <button
        type="button"
        className={`task-check ${done ? 'is-checked' : ''}`}
        onClick={() => onToggle(task)}
        disabled={busy}
        aria-label={done ? 'Marcar como pendiente' : 'Marcar como completada'}
        aria-pressed={done}
      >
        {done ? '✓' : ''}
      </button>

      <div className="task-card__body">
        <div className="task-card__top">
          <h3>{task.title}</h3>
          <span className={`badge badge--priority badge--${task.priority.toLowerCase()}`}>
            {PRIORITY_LABELS[task.priority]}
          </span>
        </div>

        {task.description && <p className="task-card__description">{task.description}</p>}

        <div className="task-card__meta">
          <span className={`badge badge--status badge--status-${task.status.toLowerCase()}`}>
            {STATUS_LABELS[task.status]}
          </span>
          {task.dueDate && (
            <span className={`task-card__due ${isOverdue(task.dueDate, task.status) ? 'is-overdue' : ''}`}>
              <span aria-hidden="true">◷</span> {formatDueDate(task.dueDate)}
              {isOverdue(task.dueDate, task.status) && ' · vencida'}
            </span>
          )}
        </div>
      </div>

      <div className="task-card__actions">
        <button type="button" className="btn btn--icon" onClick={() => onEdit(task)} disabled={busy}>
          Editar
        </button>
        <button
          type="button"
          className="btn btn--icon btn--danger-ghost"
          onClick={() => onDelete(task)}
          disabled={busy}
        >
          Eliminar
        </button>
      </div>
    </li>
  );
}
