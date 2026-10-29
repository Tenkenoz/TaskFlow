import type { TaskFilters, TaskPriority, TaskStatus } from '../types';

interface FilterBarProps {
  filters: TaskFilters;
  onChange: (filters: TaskFilters) => void;
  onClear: () => void;
}

export default function FilterBar({ filters, onChange, onClear }: FilterBarProps) {
  const hasFilters = filters.status !== '' || filters.priority !== '';

  return (
    <div className="filter-bar">
      <label className="filter">
        <span>Estado</span>
        <select
          value={filters.status}
          onChange={(event) =>
            onChange({ ...filters, status: event.target.value as TaskStatus | '' })
          }
        >
          <option value="">Todos</option>
          <option value="TODO">Por hacer</option>
          <option value="IN_PROGRESS">En progreso</option>
          <option value="DONE">Completadas</option>
        </select>
      </label>

      <label className="filter">
        <span>Prioridad</span>
        <select
          value={filters.priority}
          onChange={(event) =>
            onChange({ ...filters, priority: event.target.value as TaskPriority | '' })
          }
        >
          <option value="">Todas</option>
          <option value="HIGH">Alta</option>
          <option value="MEDIUM">Media</option>
          <option value="LOW">Baja</option>
        </select>
      </label>

      {hasFilters && (
        <button type="button" className="btn btn--ghost btn--small" onClick={onClear}>
          Limpiar
        </button>
      )}
    </div>
  );
}
