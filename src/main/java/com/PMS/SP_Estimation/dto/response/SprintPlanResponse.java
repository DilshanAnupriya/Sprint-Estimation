package com.PMS.SP_Estimation.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Combined sprint plan response.
 * Returned by POST /api/v1/estimation/sprint-plan
 *
 * Contains:
 *  - Velocity prediction for the upcoming sprint
 *  - SP ML estimates for any items estimated during this call (when estimateAll=true)
 *  - Feasibility summary comparing planned SP vs recommended commitment
 *  - Actionable warnings
 */
@Data
@Builder
public class SprintPlanResponse {

    private Long sprintId;
    private Long projectId;

    // ── Velocity prediction ──────────────────────────────────────────
    private VelocityPredictionResponse velocityPrediction;

    // ── Story point estimates (only items ML-estimated in this call) ─
    private List<StoryPointEstimateResponse> storyPointEstimates;
    private int estimatedItemCount;     // number of items newly estimated

    // ── Feasibility summary ──────────────────────────────────────────
    private int     totalPlannedSP;     // sum of story_points for all sprint items
    private int     unestimatedItems;   // items still missing story_points
    private int     recommendedMaxSP;   // predicted_velocity × 0.85
    private boolean isFeasible;         // totalPlannedSP ≤ recommendedMaxSP
    /** Positive = over-committed by this many SP; negative = headroom remaining. */
    private int     surplusSP;

    // ── Warnings ─────────────────────────────────────────────────────
    private List<String> warnings;
}
