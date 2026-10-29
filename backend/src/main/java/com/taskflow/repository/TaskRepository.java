package com.taskflow.repository;

import com.taskflow.domain.Task;
import com.taskflow.domain.TaskPriority;
import com.taskflow.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Task> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, TaskStatus status);

    List<Task> findByUserIdAndPriorityOrderByCreatedAtDesc(Long userId, TaskPriority priority);

    List<Task> findByUserIdAndStatusAndPriorityOrderByCreatedAtDesc(
            Long userId, TaskStatus status, TaskPriority priority);

    Optional<Task> findByIdAndUserId(Long id, Long userId);
}
