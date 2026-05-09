package com.PMS.SP_Estimation.api;

import com.PMS.SP_Estimation.dto.response.BacklogAnalysisResponse;
import com.PMS.SP_Estimation.dto.response.ComplexityScoreResponse;
import com.PMS.SP_Estimation.dto.response.CompletionTimeEstimate;
import com.PMS.SP_Estimation.dto.response.SprintHealthResponse;
import com.PMS.SP_Estimation.dto.response.SprintReadinessResponse;
import com.PMS.SP_Estimation.dto.response.VelocityTrendResponse;
import com.PMS.SP_Estimation.service.ComplexityService;
import com.PMS.SP_Estimation.service.SprintHealthService;
import com.PMS.SP_Estimation.service.SprintReadinessService;
import com.PMS.SP_Estimation.service.VelocityAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ComplexityService         complexityService;
    private final SprintHealthService       healthService;
    private final VelocityAnalyticsService  velocityAnalytics;
    private final SprintReadinessService    readinessService;

    // ── Complexity ─────────────────────────────────────────────────

    /** Compute complexity score for a single backlog item */
    @GetMapping("/backlog/{itemId}/complexity")
    public ResponseEntity<ComplexityScoreResponse> complexity(
            @PathVariable Long itemId) {
        return ResponseEntity.ok(complexityService.computeComplexity(itemId));
    }

    /** Estimate completion hours from story points */
    @GetMapping("/story-points/{sp}/hours")
    public ResponseEntity<CompletionTimeEstimate> estimateHours(
            @PathVariable int sp,
            @RequestParam(defaultValue = "MEDIUM") String riskLevel) {
        return ResponseEntity.ok(complexityService.estimateHours(sp, riskLevel));
    }

    /** Full backlog analysis for a project */
    @GetMapping("/projects/{projectId}/backlog-analysis")
    public ResponseEntity<BacklogAnalysisResponse> backlogAnalysis(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "24.45") double predictedVelocity) {
        return ResponseEntity.ok(
            complexityService.analyseBacklog(projectId, predictedVelocity));
    }

    // ── Sprint Health ──────────────────────────────────────────────

    /** Full sprint health score and warnings */
    @GetMapping("/sprints/{sprintId}/health")
    public ResponseEntity<SprintHealthResponse> health(
            @PathVariable Long sprintId) {
        return ResponseEntity.ok(healthService.computeHealth(sprintId));
    }

    /** Sprint readiness checklist */
    @GetMapping("/sprints/{sprintId}/readiness")
    public ResponseEntity<SprintReadinessResponse> readiness(
            @PathVariable Long sprintId) {
        return ResponseEntity.ok(readinessService.checkReadiness(sprintId));
    }

    // ── Velocity Analytics ─────────────────────────────────────────

    /** Rolling avg_velocity_last3 and avg_velocity_last5 for a project */
    @GetMapping("/projects/{projectId}/rolling-velocity")
    public ResponseEntity<Map<String, Double>> rollingVelocity(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(
            velocityAnalytics.computeRollingVelocities(projectId));
    }

    /** Full velocity trend with domain benchmark comparison */
    @GetMapping("/projects/{projectId}/velocity-trend")
    public ResponseEntity<VelocityTrendResponse> velocityTrend(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(velocityAnalytics.getVelocityTrend(projectId));
    }
}
