package com.taskflow.service;

import com.taskflow.domain.Task;
import com.taskflow.domain.TaskPriority;
import com.taskflow.domain.TaskStatus;
import com.taskflow.domain.User;
import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService - reglas de negocio de las tareas")
class TaskServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TASK_ID = 10L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Erick", "erick@example.com", "hashed-password");
        user.setId(USER_ID);
    }

    @Test
    @DisplayName("crea la tarea asignando dueño, estado TODO y prioridad MEDIUM por defecto")
    void createTask_assignsOwnerAndDefaults() {
        TaskRequest request = new TaskRequest("  Escribir los tests  ", "  con Mockito  ",
                null, null, LocalDate.of(2026, 12, 31));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(USER_ID, request);

        assertThat(response.title()).isEqualTo("Escribir los tests");
        assertThat(response.description()).isEqualTo("con Mockito");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 12, 31));

        ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isEqualTo(user);
        assertThat(saved.getValue().getTitle()).isEqualTo("Escribir los tests");
    }

    @Test
    @DisplayName("respeta el estado y la prioridad enviados al crear")
    void createTask_keepsExplicitStatusAndPriority() {
        TaskRequest request = new TaskRequest("Desplegar", null,
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(USER_ID, request);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.description()).isNull();
    }

    @Test
    @DisplayName("falla al crear si el usuario no existe")
    void createTask_throwsWhenUserDoesNotExist() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(USER_ID,
                new TaskRequest("Tarea", null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario no encontrado");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("lista solo las tareas del usuario autenticado")
    void findAll_returnsOnlyTasksOfTheUser() {
        Task task = task(TaskStatus.TODO, TaskPriority.LOW);
        when(taskRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(task));

        List<TaskResponse> result = taskService.findAll(USER_ID, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("Tarea de prueba");
        verify(taskRepository).findByUserIdOrderByCreatedAtDesc(USER_ID);
    }

    @Test
    @DisplayName("aplica el filtro por estado")
    void findAll_filtersByStatus() {
        when(taskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(USER_ID, TaskStatus.DONE))
                .thenReturn(List.of(task(TaskStatus.DONE, TaskPriority.MEDIUM)));

        List<TaskResponse> result = taskService.findAll(USER_ID, TaskStatus.DONE, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo(TaskStatus.DONE);
        verify(taskRepository).findByUserIdAndStatusOrderByCreatedAtDesc(USER_ID, TaskStatus.DONE);
    }

    @Test
    @DisplayName("aplica el filtro por prioridad")
    void findAll_filtersByPriority() {
        when(taskRepository.findByUserIdAndPriorityOrderByCreatedAtDesc(USER_ID, TaskPriority.HIGH))
                .thenReturn(List.of(task(TaskStatus.TODO, TaskPriority.HIGH)));

        List<TaskResponse> result = taskService.findAll(USER_ID, null, TaskPriority.HIGH);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    @DisplayName("combina los filtros de estado y prioridad")
    void findAll_filtersByStatusAndPriority() {
        when(taskRepository.findByUserIdAndStatusAndPriorityOrderByCreatedAtDesc(
                USER_ID, TaskStatus.TODO, TaskPriority.HIGH))
                .thenReturn(List.of(task(TaskStatus.TODO, TaskPriority.HIGH)));

        List<TaskResponse> result = taskService.findAll(USER_ID, TaskStatus.TODO, TaskPriority.HIGH);

        assertThat(result).hasSize(1);
        verify(taskRepository).findByUserIdAndStatusAndPriorityOrderByCreatedAtDesc(
                USER_ID, TaskStatus.TODO, TaskPriority.HIGH);
    }

    @Test
    @DisplayName("no expone tareas de otro usuario")
    void findById_throwsWhenTaskBelongsToAnotherUser() {
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(USER_ID, TASK_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tarea no encontrada");
    }

    @Test
    @DisplayName("actualiza los campos editables de una tarea propia")
    void updateTask_updatesFields() {
        Task task = task(TaskStatus.TODO, TaskPriority.LOW);
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.update(USER_ID, TASK_ID,
                new TaskRequest("Título nuevo", "Descripción nueva",
                        TaskStatus.IN_PROGRESS, TaskPriority.HIGH, LocalDate.of(2027, 1, 15)));

        assertThat(response.title()).isEqualTo("Título nuevo");
        assertThat(response.description()).isEqualTo("Descripción nueva");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2027, 1, 15));
    }

    @Test
    @DisplayName("no permite actualizar una tarea inexistente o ajena")
    void updateTask_throwsWhenTaskNotFound() {
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(USER_ID, TASK_ID,
                new TaskRequest("X", null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("marca una tarea como DONE al alternar su estado")
    void toggleCompleted_marksTaskDone() {
        Task task = task(TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM);
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.toggleCompleted(USER_ID, TASK_ID);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    @DisplayName("reabre una tarea completada al alternar su estado")
    void toggleCompleted_reopensCompletedTask() {
        Task task = task(TaskStatus.DONE, TaskPriority.MEDIUM);
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        TaskResponse response = taskService.toggleCompleted(USER_ID, TASK_ID);

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    @DisplayName("elimina una tarea propia")
    void deleteTask_deletesOwnedTask() {
        Task task = task(TaskStatus.TODO, TaskPriority.MEDIUM);
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_ID)).thenReturn(Optional.of(task));

        taskService.delete(USER_ID, TASK_ID);

        verify(taskRepository).delete(task);
    }

    @Test
    @DisplayName("no elimina tareas de otro usuario")
    void deleteTask_throwsWhenTaskNotFound() {
        when(taskRepository.findByIdAndUserId(TASK_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(USER_ID, TASK_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).delete(any(Task.class));
    }

    private Task task(TaskStatus status, TaskPriority priority) {
        Task task = new Task("Tarea de prueba", "descripción", priority, null, user);
        task.setId(TASK_ID);
        task.setStatus(status);
        return task;
    }
}
