package com.PMS.SP_Estimation.api;

import com.PMS.SP_Estimation.dto.request.TeamMemberRequest;
import com.PMS.SP_Estimation.dto.response.TeamMemberResponse;
import com.PMS.SP_Estimation.service.TeamMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TeamMemberController {

    private final TeamMemberService service;

    /**
     * Add a developer to a project. If the email matches an existing developer
     * in the global pool, that one is reused — otherwise a new developer is
     * created and added to the project.
     */
    @PostMapping("/api/v1/projects/{projectId}/team")
    public ResponseEntity<TeamMemberResponse> create(@PathVariable Long projectId,
                                                     @RequestBody @Valid TeamMemberRequest req) {
        return ResponseEntity.ok(service.create(projectId, req));
    }

    /** Assign an existing developer (by id, from the global pool) to a project. */
    @PostMapping("/api/v1/projects/{projectId}/team/{memberId}")
    public ResponseEntity<TeamMemberResponse> assign(@PathVariable Long projectId,
                                                     @PathVariable Long memberId) {
        return ResponseEntity.ok(service.assignToProject(projectId, memberId));
    }

    /** Remove a developer from a project (does not delete the developer). */
    @DeleteMapping("/api/v1/projects/{projectId}/team/{memberId}")
    public ResponseEntity<Void> unassign(@PathVariable Long projectId,
                                         @PathVariable Long memberId) {
        service.removeFromProject(projectId, memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/projects/{projectId}/team")
    public ResponseEntity<List<TeamMemberResponse>> listByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.listByProject(projectId));
    }

    /** Global developer pool — every developer across all projects. */
    @GetMapping("/api/v1/developers")
    public ResponseEntity<List<TeamMemberResponse>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/api/v1/team/{id}")
    public ResponseEntity<TeamMemberResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/api/v1/team/{id}")
    public ResponseEntity<TeamMemberResponse> update(@PathVariable Long id,
                                                     @RequestBody TeamMemberRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    /** Permanently delete a developer from the global pool (removes from every project). */
    @DeleteMapping("/api/v1/team/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
