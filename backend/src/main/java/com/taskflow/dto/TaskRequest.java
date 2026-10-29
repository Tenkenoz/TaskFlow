package com.taskflow.dto;

import com.taskflow.domain.TaskPriority;
import com.taskflow.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String title,

        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String description,

        TaskStatus status,

        TaskPriority priority,

        LocalDate dueDate
) {
}
