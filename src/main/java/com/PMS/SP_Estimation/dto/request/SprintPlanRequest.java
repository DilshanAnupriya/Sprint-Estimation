package com.PMS.SP_Estimation.dto.request;

import lombok.Data;

/**
 * Request for the combined sprint planning endpoint.
 * POST /api/v1/estimation/sprint-plan
 *
 * Provide sprintId to let the service load backlog + sprint context automatically.
 * Any null velocity field falls back to sprint/project state (same as VelocityPredictionRequest).
 * Set estimateAll=true to trigger SP ML model for every unestimated item before summarising.
 */
@Data
public class SprintPlanRequest {

    /** Sprint to plan. Used to load sprint backlog + team context. */
    private Long   sprintId;

    /**
     * Fallback when sprintId is null — pulls all product-backlog items for this project.
     * Ignored when sprintId is set.
     */
    private Long   projectId;

    // ── Optional velocity overrides (null → derived from sprint/project state) ──
    private Double  avgVelocityLast3;
    private Double  avgVelocityLast5;
    private Integer teamSize;
    private Integer sprintDurationDays;
    private Double  completionRate;
    private Integer carryoverTasks;
    private Double  developerAvailability;
    private Integer leaveDays;
    private Integer plannedStoryPoints;
    private Integer numBugs;
    private Double  avgExperienceYears;
    private String  domain;

    /**
     * When true, the service calls the SP ML model for every backlog item that
     * does not yet have mlPointEstimate set, before computing the feasibility summary.
     * Default: false (summarises already-estimated items only).
     */
    private boolean estimateAll = false;
}
