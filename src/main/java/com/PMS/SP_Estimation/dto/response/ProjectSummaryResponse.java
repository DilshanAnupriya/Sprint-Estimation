package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * High-level project summary for the dashboard.
 * Used by GET /api/v1/dashboard/projects/{id}/summary
 */
@Data
@Builder
public class ProjectSummaryResponse {
    private Long   projectId;
    private String projectName;
    private String domain;
    private String defaultTechStack;
    private Integer teamSize;

    // Sprint stats
    private int totalSprints;
    private int completedSprints;
    private int activeSprints;
    private int plannedSprints;

    // Backlog stats
    private int totalBacklogItems;
    private int completedItems;
    private int inProgressItems;
    private int todoItems;
    private int totalStoryPoints;
    private int completedStoryPoints;
    private int unestimatedItems;        // items with no story_points set
    private int highRiskItems;           // estimation_risk = HIGH
    private int ambiguousItems;          // is_ambiguous = true

    // Velocity stats (from completed sprints)
    private double avgActualVelocity;    // mean across completed sprints
    private double avgVelocityLast3;
    private double overallCompletionRate; // completedSP / totalSP
}
