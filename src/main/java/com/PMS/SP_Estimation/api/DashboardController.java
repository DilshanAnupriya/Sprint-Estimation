package com.PMS.SP_Estimation.api;

import com.PMS.SP_Estimation.dto.response.BacklogItemResponse;
import com.PMS.SP_Estimation.dto.response.BurndownResponse;
import com.PMS.SP_Estimation.dto.response.ProjectSummaryResponse;
import com.PMS.SP_Estimation.dto.response.VelocityHistoryPoint;
import com.PMS.SP_Estimation.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dashboard API — read-only analytics endpoints for the frontend.
 *
 * GET /api/v1/dashboard/sprints/{id}/burndown
 * GET /api/v1/dashboard/projects/{id}/velocity-history
 * GET /api/v1/dashboard/sprints/{id}/risks
 * GET /api/v1/dashboard/projects/{id}/summary
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Burndown snapshot for a sprint.
     *
     * Returns item counts and story points grouped by status (TODO, IN_PROGRESS,
     * DONE, BLOCKED, CANCELLED) plus a top-level completion rate.
     * Use this to draw the sprint burndown chart on the frontend.
     */
    @GetMapping("/sprints/{sprintId}/burndown")
    public ResponseEntity<BurndownResponse> burndown(@PathVariable Long sprintId) {
        return ResponseEntity.ok(dashboardService.getBurndown(sprintId));
    }

    /**
     * Velocity history for a project — one point per sprint sorted by end date.
     *
     * Returns predicted_velocity, actual_velocity, completion_rate and health_score
     * per sprint so the frontend can render a velocity trend line chart.
     */
    @GetMapping("/projects/{projectId}/velocity-history")
    public ResponseEntity<List<VelocityHistoryPoint>> velocityHistory(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(dashboardService.getVelocityHistory(projectId));
    }

    /**
     * High-risk backlog items in a sprint.
     *
     * An item is flagged as high-risk when ANY of:
     *  - estimation_risk = HIGH  (ML model uncertainty)
     *  - is_ambiguous = true     (too-short user story for complex task)
     *  - num_components ≥ 5      (very high structural complexity)
     *
     * Returns the full BacklogItemResponse so the frontend can display all fields.
     */
    @GetMapping("/sprints/{sprintId}/risks")
    public ResponseEntity<List<BacklogItemResponse>> risks(@PathVariable Long sprintId) {
        return ResponseEntity.ok(dashboardService.getHighRiskItems(sprintId));
    }

    /**
     * Aggregated project summary card.
     *
     * Returns sprint counts (total / completed / active / planned),
     * backlog health (total items, completed, unestimated, high-risk, ambiguous),
     * story-point totals, average velocity, and overall completion rate.
     */
    @GetMapping("/projects/{projectId}/summary")
    public ResponseEntity<ProjectSummaryResponse> projectSummary(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(dashboardService.getProjectSummary(projectId));
    }
}
