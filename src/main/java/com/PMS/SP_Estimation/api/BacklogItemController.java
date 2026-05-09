package com.PMS.SP_Estimation.api;

import com.PMS.SP_Estimation.dto.request.BacklogItemRequest;
import com.PMS.SP_Estimation.dto.request.EstimationOverrideRequest;
import com.PMS.SP_Estimation.dto.response.BacklogItemResponse;
import com.PMS.SP_Estimation.dto.response.StoryPointEstimateResponse;
import com.PMS.SP_Estimation.service.BacklogItemService;
import com.PMS.SP_Estimation.service.StoryPointMLService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BacklogItemController {

    private final BacklogItemService    backlogService;
    private final StoryPointMLService   spMLService;

    @PostMapping("/api/v1/projects/{projectId}/backlog")
    public ResponseEntity<BacklogItemResponse> create(@PathVariable Long projectId,
                                                      @RequestBody @Valid BacklogItemRequest req) {
        return ResponseEntity.ok(backlogService.create(projectId, req));
    }

    @GetMapping("/api/v1/projects/{projectId}/backlog")
    public ResponseEntity<List<BacklogItemResponse>> listByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(backlogService.listByProject(projectId));
    }

    @GetMapping("/api/v1/sprints/{sprintId}/backlog")
    public ResponseEntity<List<BacklogItemResponse>> listBySprint(@PathVariable Long sprintId) {
        return ResponseEntity.ok(backlogService.listBySprint(sprintId));
    }

    @GetMapping("/api/v1/backlog/{id}")
    public ResponseEntity<BacklogItemResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(backlogService.get(id));
    }

    @PutMapping("/api/v1/backlog/{id}")
    public ResponseEntity<BacklogItemResponse> update(@PathVariable Long id,
                                                      @RequestBody BacklogItemRequest req) {
        return ResponseEntity.ok(backlogService.update(id, req));
    }

    @DeleteMapping("/api/v1/backlog/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        backlogService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Move a backlog item into a sprint. */
    @PostMapping("/api/v1/backlog/{id}/assign-sprint/{sprintId}")
    public ResponseEntity<BacklogItemResponse> assignSprint(@PathVariable Long id,
                                                            @PathVariable Long sprintId) {
        return ResponseEntity.ok(backlogService.assignToSprint(id, sprintId));
    }

    /** Pull a backlog item back out of its sprint. */
    @PostMapping("/api/v1/backlog/{id}/unassign-sprint")
    public ResponseEntity<BacklogItemResponse> unassignSprint(@PathVariable Long id) {
        return ResponseEntity.ok(backlogService.unassignFromSprint(id));
    }

    /** Mark a story DONE manually (auto-done is also fired by SubTaskService when all subtasks complete). */
    @PostMapping("/api/v1/backlog/{id}/mark-done")
    public ResponseEntity<BacklogItemResponse> markDone(@PathVariable Long id) {
        return ResponseEntity.ok(backlogService.markDone(id));
    }

    /**
     * Run the SP ML model for this backlog item. Stores ml_* fields and
     * (if unset) story_points.
     *
     * Optional body: any non-null field overrides the value derived from
     * the project / backlog item — use this when the frontend collects ML
     * inputs from the user (matches the notebook's interactive flow).
     * Send no body to use auto-derived values from project state.
     */
    @PostMapping("/api/v1/backlog/{id}/estimate")
    public ResponseEntity<StoryPointEstimateResponse> estimate(
            @PathVariable Long id,
            @RequestBody(required = false) EstimationOverrideRequest overrides) {
        return ResponseEntity.ok(spMLService.estimate(id, overrides));
    }
}
