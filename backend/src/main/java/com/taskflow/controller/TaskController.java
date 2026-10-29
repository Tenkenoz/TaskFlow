package com.taskflow.controller;

import com.taskflow.domain.TaskPriority;
import com.taskflow.domain.TaskStatus;
import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.security.UserPrincipal;
import com.taskflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> findAll(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestParam(required = false) TaskStatus status,
                                      @RequestParam(required = false) TaskPriority priority) {
        return taskService.findAll(principal.id(), status, priority);
    }

    @GetMapping("/{id}")
    public TaskResponse findById(@AuthenticationPrincipal UserPrincipal principal,
                                 @PathVariable Long id) {
        return taskService.findById(principal.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.create(principal.id(), request);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long id,
                               @Valid @RequestBody TaskRequest request) {
        return taskService.update(principal.id(), id, request);
    }

    @PatchMapping("/{id}/toggle")
    public TaskResponse toggleCompleted(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id) {
        return taskService.toggleCompleted(principal.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal,
                       @PathVariable Long id) {
        taskService.delete(principal.id(), id);
    }
}
