package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Burndown chart data for a sprint.
 * Used by GET /api/v1/dashboard/sprints/{id}/burndown
 */
@Data
@Builder
public class BurndownResponse {

    private Long   sprintId;
    private String sprintName;

    // Totals
    private int    totalItems;
    private int    totalStoryPoints;

    // Completion snapshot
    private int    completedItems;
    private int    completedStoryPoints;
    private int    inProgressItems;
    private int    todoItems;
    private int    remainingStoryPoints;   // totalSP − completedSP
    private double completionRate;          // completedSP / totalSP  (0.0–1.0)

    // Per-status breakdown (one entry per BacklogItem.Status value present)
    private List<BurndownPoint> statusBreakdown;

    @Data
    @Builder
    public static class BurndownPoint {
        private String status;      // TODO | IN_PROGRESS | DONE | BLOCKED | CANCELLED
        private long   itemCount;
        private int    storyPoints;
    }
}
