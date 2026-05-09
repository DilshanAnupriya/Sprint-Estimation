package com.PMS.SP_Estimation.api;

import com.PMS.SP_Estimation.dto.request.SprintCompletionRequest;
import com.PMS.SP_Estimation.dto.request.SprintMetricsRequest;
import com.PMS.SP_Estimation.dto.request.SprintRequest;
import com.PMS.SP_Estimation.dto.response.SprintResponse;
import com.PMS.SP_Estimation.service.SprintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SprintController {

    private final SprintService service;

    @PostMapping("/api/v1/projects/{projectId}/sprints")
    public ResponseEntity<SprintResponse> create(@PathVariable Long projectId,
                                                 @RequestBody @Valid SprintRequest req) {
        return ResponseEntity.ok(service.create(projectId, req));
    }

    @GetMapping("/api/v1/projects/{projectId}/sprints")
    public ResponseEntity<List<SprintResponse>> listByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.listByProject(projectId));
    }

    @GetMapping("/api/v1/sprints/{id}")
    public ResponseEntity<SprintResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/api/v1/sprints/{id}")
    public ResponseEntity<SprintResponse> update(@PathVariable Long id,
                                                 @RequestBody SprintRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/api/v1/sprints/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Move sprint to ACTIVE. */
    @PostMapping("/api/v1/sprints/{id}/start")
    public ResponseEntity<SprintResponse> start(@PathVariable Long id) {
        return ResponseEntity.ok(service.start(id));
    }

    /** Patch SV-style live metrics (availability, bugs, leave, etc). */
    @PatchMapping("/api/v1/sprints/{id}/metrics")
    public ResponseEntity<SprintResponse> updateMetrics(@PathVariable Long id,
                                                        @RequestBody SprintMetricsRequest req) {
        return ResponseEntity.ok(service.updateMetrics(id, req));
    }

    /** Settle sprint: auto-saves actual velocity, completion rate, carryover. */
    @PostMapping("/api/v1/sprints/{id}/complete")
    public ResponseEntity<SprintResponse> complete(@PathVariable Long id,
                                                   @RequestBody(required = false) SprintCompletionRequest req) {
        return ResponseEntity.ok(service.complete(id, req));
    }
}
