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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskResponse create(Long userId, TaskRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Task task = new Task(
                request.title().trim(),
                normalizeDescription(request.description()),
                request.priority() != null ? request.priority() : TaskPriority.MEDIUM,
                request.dueDate(),
                user);
        task.setStatus(request.status() != null ? request.status() : TaskStatus.TODO);

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll(Long userId, TaskStatus status, TaskPriority priority) {
        List<Task> tasks;

        if (status != null && priority != null) {
            tasks = taskRepository.findByUserIdAndStatusAndPriorityOrderByCreatedAtDesc(
                    userId, status, priority);
        } else if (status != null) {
            tasks = taskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else if (priority != null) {
            tasks = taskRepository.findByUserIdAndPriorityOrderByCreatedAtDesc(userId, priority);
        } else {
            tasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long userId, Long taskId) {
        return TaskResponse.from(getOwnedTask(userId, taskId));
    }

    @Transactional
    public TaskResponse update(Long userId, Long taskId, TaskRequest request) {
        Task task = getOwnedTask(userId, taskId);

        task.setTitle(request.title().trim());
        task.setDescription(normalizeDescription(request.description()));
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setDueDate(request.dueDate());

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse toggleCompleted(Long userId, Long taskId) {
        Task task = getOwnedTask(userId, taskId);
        task.setStatus(task.getStatus() == TaskStatus.DONE ? TaskStatus.TODO : TaskStatus.DONE);

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long userId, Long taskId) {
        Task task = getOwnedTask(userId, taskId);
        taskRepository.delete(task);
    }

    private Task getOwnedTask(Long userId, Long taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
