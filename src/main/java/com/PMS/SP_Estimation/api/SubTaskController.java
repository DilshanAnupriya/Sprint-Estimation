package com.PMS.SP_Estimation.api;

import com.PMS.SP_Estimation.dto.request.SubTaskRequest;
import com.PMS.SP_Estimation.dto.response.SubTaskResponse;
import com.PMS.SP_Estimation.entity.SubTask;
import com.PMS.SP_Estimation.service.SubTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubTaskController {

    private final SubTaskService service;

    @PostMapping("/api/v1/backlog/{backlogItemId}/subtasks")
    public ResponseEntity<SubTaskResponse> create(@PathVariable Long backlogItemId,
                                                  @RequestBody @Valid SubTaskRequest req) {
        return ResponseEntity.ok(service.create(backlogItemId, req));
    }

    @GetMapping("/api/v1/backlog/{backlogItemId}/subtasks")
    public ResponseEntity<List<SubTaskResponse>> listByBacklog(@PathVariable Long backlogItemId) {
        return ResponseEntity.ok(service.listByBacklog(backlogItemId));
    }

    @GetMapping("/api/v1/team/{assigneeId}/subtasks")
    public ResponseEntity<List<SubTaskResponse>> listByAssignee(@PathVariable Long assigneeId) {
        return ResponseEntity.ok(service.listByAssignee(assigneeId));
    }

    @GetMapping("/api/v1/subtasks/{id}")
    public ResponseEntity<SubTaskResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/api/v1/subtasks/{id}")
    public ResponseEntity<SubTaskResponse> update(@PathVariable Long id,
                                                  @RequestBody SubTaskRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    /** Developer updates status. Cascades story-DONE when all sub-tasks finish. */
    @PatchMapping("/api/v1/subtasks/{id}/status")
    public ResponseEntity<SubTaskResponse> changeStatus(@PathVariable Long id,
                                                        @RequestParam SubTask.Status status) {
        return ResponseEntity.ok(service.changeStatus(id, status));
    }

    @DeleteMapping("/api/v1/subtasks/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
